package io.potluckhub.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.KSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(message: String) : Exception(message)

/** Thin coroutine wrapper around the Potluck REST API. */
object Api {
    private const val BASE = "https://api.potluckhub.io/api/v1"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Bearer token set by AuthRepository after login. */
    @Volatile var accessToken: String? = null

    private val JSON_MEDIA = "application/json".toMediaType()

    private suspend fun raw(
        method: String,
        path: String,
        body: String? = null,
        authenticated: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url("$BASE/$path")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "{}").toRequestBody(JSON_MEDIA))
            else -> builder.method(method, body?.toRequestBody(JSON_MEDIA))
        }
        if (authenticated) accessToken?.let { builder.header("Authorization", "Bearer $it") }

        try {
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (resp.code == 401) throw ApiException("Please sign in to continue.")
                text
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Network error. Check your connection and try again.")
        }
    }

    /** Unwraps the { success, data, error } envelope and decodes `data` with [serializer]. */
    private suspend fun <T> request(
        method: String,
        path: String,
        serializer: KSerializer<T>,
        body: String? = null,
        authenticated: Boolean = false,
    ): T {
        val text = raw(method, path, body, authenticated)
        val root = try {
            json.parseToString(text)
        } catch (e: Exception) {
            throw ApiException("Could not read the server response.")
        }
        val success = (root["success"] as? JsonPrimitive)?.content == "true"
        if (!success) {
            val msg = (root["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
            throw ApiException(msg ?: "Something went wrong.")
        }
        val data = root["data"] ?: throw ApiException("Empty response.")
        return json.decodeFromJsonElement(serializer, data)
    }

    private fun Json.parseToString(text: String): JsonObject =
        parseToJsonElement(text).jsonObject

    // ---- Chefs ----
    suspend fun featuredChefs() = request("GET", "chefs/featured", ListSerializer(Chef.serializer()))

    suspend fun chefs(search: String? = null, category: String? = null): List<Chef> {
        val q = buildQuery("page" to "1", "limit" to "20", "search" to search, "category" to category)
        return request("GET", "chefs$q", ListSerializer(Chef.serializer()))
    }

    suspend fun chef(id: String) = request("GET", "chefs/$id", Chef.serializer())

    // ---- Menus ----
    suspend fun featuredMenus() = request("GET", "menus/featured", ListSerializer(Menu.serializer()))

    suspend fun menus(search: String? = null, category: String? = null): List<Menu> {
        val q = buildQuery("page" to "1", "limit" to "20", "search" to search, "category" to category)
        return request("GET", "menus$q", ListSerializer(Menu.serializer()))
    }

    suspend fun menu(id: String) = request("GET", "menus/$id", Menu.serializer())

    // ---- Auth ----
    suspend fun login(email: String, password: String): AuthResult {
        val body = json.encodeToString(LoginBody.serializer(), LoginBody(email, password))
        return request("POST", "auth/login", AuthResult.serializer(), body)
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String): AuthResult {
        val body = json.encodeToString(
            RegisterBody.serializer(),
            RegisterBody(email, password, firstName, lastName, "customer"),
        )
        return request("POST", "auth/register", AuthResult.serializer(), body)
    }

    suspend fun me() = request("GET", "auth/me", User.serializer(), authenticated = true)

    suspend fun deleteAccount() {
        val root = try {
            json.parseToString(raw("DELETE", "auth/account", authenticated = true))
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw ApiException("Could not read the server response.")
        }
        val success = (root["success"] as? JsonPrimitive)?.content == "true"
        if (!success) {
            val msg = (root["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
            throw ApiException(msg ?: "Something went wrong.")
        }
    }

    suspend fun myBookings() =
        request("GET", "bookings", ListSerializer(Booking.serializer()), authenticated = true)

    // ---- Checkout (website origin — plain JSON, no envelope) ----

    private const val WEB_BASE = "https://potluckhub.io/api"

    /** Creates a checkout order and returns the hosted-payment redirect URL. */
    suspend fun createCheckout(request: CheckoutRequest): CheckoutOrder = webRequest(
        Request.Builder()
            .url("$WEB_BASE/checkout")
            .post(json.encodeToString(CheckoutRequest.serializer(), request).toRequestBody(JSON_MEDIA))
            .build(),
        CheckoutOrder.serializer(),
    )

    /** Polls the payment status of a checkout order. */
    suspend fun checkoutStatus(orderId: String): CheckoutStatus = webRequest(
        Request.Builder().url("$WEB_BASE/checkout/$orderId").get().build(),
        CheckoutStatus.serializer(),
    )

    // ---- Reviews (website origin — plain JSON, no envelope) ----

    /** Fetches the diner reviews for a chef from the website. */
    suspend fun webReviews(chefId: String): WebReviewList = webRequest(
        Request.Builder()
            .url("$WEB_BASE/reviews?chefId=${java.net.URLEncoder.encode(chefId, "UTF-8")}")
            .get()
            .build(),
        WebReviewList.serializer(),
    )

    /** Submits a diner review; the server responds 201 with { "review": {...} }. */
    suspend fun postWebReview(request: WebReviewRequest): WebReview = webRequest(
        Request.Builder()
            .url("$WEB_BASE/reviews")
            .post(json.encodeToString(WebReviewRequest.serializer(), request).toRequestBody(JSON_MEDIA))
            .build(),
        WebReviewResponse.serializer(),
    ).review

    /** Website endpoints return the object directly; errors are non-2xx with { "error": "message" }. */
    private suspend fun <T> webRequest(request: Request, serializer: KSerializer<T>): T =
        withContext(Dispatchers.IO) {
            val (code, text) = try {
                client.newCall(request).execute().use { resp ->
                    resp.code to resp.body?.string().orEmpty()
                }
            } catch (e: Exception) {
                throw ApiException("Network error. Check your connection and try again.")
            }
            if (code !in 200..299) {
                val msg = runCatching {
                    json.parseToString(text)["error"]?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                throw ApiException(msg ?: "Something went wrong.")
            }
            try {
                json.decodeFromString(serializer, text)
            } catch (e: Exception) {
                throw ApiException("Could not read the server response.")
            }
        }

    private fun buildQuery(vararg params: Pair<String, String?>): String {
        val parts = params.filter { !it.second.isNullOrEmpty() }
            .joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" }
        return if (parts.isEmpty()) "" else "?$parts"
    }
}

@kotlinx.serialization.Serializable
private data class LoginBody(val email: String, val password: String)

@kotlinx.serialization.Serializable
private data class RegisterBody(
    val email: String, val password: String,
    val firstName: String, val lastName: String, val role: String,
)
