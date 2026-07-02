package io.potluckhub.app

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * The Potluck API sends rating values inconsistently — a String ("4.91") at chef level
 * but a JSON number on menus. This serializer accepts either and yields a Double.
 */
object FlexDoubleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jd = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        val el = jd.decodeJsonElement()
        if (el is JsonNull) return null
        val p = el as? JsonPrimitive ?: return null
        return p.content.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        encoder.encodeDouble(value ?: 0.0)
    }
}

@Serializable
data class Pagination(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ChefUser(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
) {
    val fullName get() = "$firstName $lastName".trim()
}

@Serializable
data class Chef(
    val id: String,
    val userId: String? = null,
    val bio: String? = null,
    val specialties: List<String>? = null,
    val city: String? = null,
    val instagramUrl: String? = null,
    val tiktokUrl: String? = null,
    @Serializable(with = FlexDoubleSerializer::class) val averageRating: Double? = null,
    val totalReviews: Int? = null,
    val isVerified: Boolean? = null,
    val isAvailable: Boolean? = null,
    val user: ChefUser,
    val menus: List<Menu>? = null,
) {
    val rating get() = averageRating ?: 0.0
    val reviewCount get() = totalReviews ?: 0
}

@Serializable
data class Category(val id: String, val name: String, val slug: String? = null)

@Serializable
data class MenuChef(
    val id: String,
    @Serializable(with = FlexDoubleSerializer::class) val averageRating: Double? = null,
    val totalReviews: Int? = null,
    val isVerified: Boolean? = null,
    val user: ChefUser? = null,
)

@Serializable
data class Menu(
    val id: String,
    val chefId: String? = null,
    val name: String,
    val description: String? = null,
    val price: Int = 0,
    val currency: String? = "SGD",
    val images: List<String>? = null,
    val isVegetarian: Boolean? = null,
    val isVegan: Boolean? = null,
    val isGlutenFree: Boolean? = null,
    @Serializable(with = FlexDoubleSerializer::class) val averageRating: Double? = null,
    val category: Category? = null,
    val chef: MenuChef? = null,
) {
    val firstImage get() = images?.firstOrNull()
    val rating get() = averageRating ?: 0.0
    val displayPrice get() = money(price, currency ?: "SGD")
    val dietaryTags: List<String>
        get() = buildList {
            if (isVegetarian == true) add("Vegetarian")
            if (isVegan == true) add("Vegan")
            if (isGlutenFree == true) add("Gluten-Free")
        }
}

@Serializable
data class ReviewUser(
    val id: String,
    val firstName: String = "",
    val lastName: String = "",
    val avatarUrl: String? = null,
) {
    val fullName get() = "$firstName $lastName".trim()
}

@Serializable
data class ReviewMenu(val id: String, val name: String)

@Serializable
data class Review(
    val id: String,
    val rating: Int = 0,
    val title: String? = null,
    val comment: String? = null,
    val chefResponse: String? = null,
    val createdAt: String? = null,
    val customer: ReviewUser? = null,
    val menu: ReviewMenu? = null,
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val role: String = "customer",
    val avatarUrl: String? = null,
) {
    val fullName get() = "$firstName $lastName".trim()
    val initials: String
        get() = (firstName.take(1) + lastName.take(1)).uppercase()
}

@Serializable
data class AuthResult(val user: User, val accessToken: String, val refreshToken: String)

@Serializable
data class Booking(
    val id: String,
    val bookingNumber: String? = null,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val guestCount: Int? = null,
    val total: Int? = null,
    val status: String? = null,
)

// ---- Checkout (website endpoints — plain JSON, no envelope) ----

@Serializable
data class CheckoutRequest(
    val menuId: String,
    val guests: Int,
    val scheduledDate: String,
    val scheduledTime: String,
    val specialRequests: String? = null,
    val customerName: String,
    val customerEmail: String,
    val customerPhone: String? = null,
    val provider: String,
    val platform: String,
)

@Serializable
data class CheckoutAmount(
    val subtotal: Int = 0,
    val platformFee: Int = 0,
    val total: Int = 0,
    val currency: String = "SGD",
)

@Serializable
data class CheckoutOrder(
    val orderId: String,
    val orderNumber: String = "",
    val amount: CheckoutAmount = CheckoutAmount(),
    val redirectUrl: String = "",
)

@Serializable
data class CheckoutStatus(
    val orderId: String = "",
    val orderNumber: String = "",
    val status: String = "pending_payment",
    val provider: String? = null,
    val total: Int = 0,
    val currency: String = "SGD",
    val menuName: String? = null,
    val chefName: String? = null,
    val scheduledDate: String? = null,
    val scheduledTime: String? = null,
    val guests: Int = 0,
    val paidAt: String? = null,
)

// ---- Reviews (website endpoints — plain JSON, no envelope) ----

@Serializable
data class WebReview(
    val id: String,
    val authorName: String = "",
    val rating: Int = 0,
    val title: String? = null,
    val body: String = "",
    val createdAt: String? = null,
    val verifiedBooking: Boolean = false,
)

@Serializable
data class WebReviewList(
    val reviews: List<WebReview> = emptyList(),
    val total: Int = 0,
    @Serializable(with = FlexDoubleSerializer::class) val average: Double? = null,
)

@Serializable
data class WebReviewRequest(
    val chefId: String,
    val authorName: String,
    val authorEmail: String? = null,
    val rating: Int,
    val title: String? = null,
    val body: String,
    val platform: String = "android",
)

@Serializable
data class WebReviewResponse(val review: WebReview)

/** Prices are stored in cents. */
fun money(cents: Int, currency: String = "SGD"): String {
    val symbol = if (currency == "SGD") "S$" else "$"
    return symbol + String.format("%.2f", cents / 100.0)
}
