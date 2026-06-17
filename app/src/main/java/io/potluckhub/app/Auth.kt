package io.potluckhub.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Holds the signed-in session. Tokens persist in SharedPreferences and are restored
 * on launch; [Api.accessToken] is kept in sync for authenticated requests.
 */
class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("potluck_auth", Application.MODE_PRIVATE)

    var currentUser by mutableStateOf<User?>(null)
        private set
    var working by mutableStateOf(false)
        private set

    val isLoggedIn get() = currentUser != null

    init {
        prefs.getString(ACCESS, null)?.let { token ->
            Api.accessToken = token
            viewModelScope.launch {
                runCatching { Api.me() }
                    .onSuccess { currentUser = it }
                    .onFailure { signOut() }
            }
        }
    }

    suspend fun login(email: String, password: String) {
        working = true
        try {
            persist(Api.login(email, password))
        } finally {
            working = false
        }
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String) {
        working = true
        try {
            persist(Api.register(email, password, firstName, lastName))
        } finally {
            working = false
        }
    }

    fun signOut() {
        prefs.edit().clear().apply()
        Api.accessToken = null
        currentUser = null
    }

    suspend fun deleteAccount() {
        working = true
        try {
            Api.deleteAccount()
            signOut()
        } finally {
            working = false
        }
    }

    private fun persist(result: AuthResult) {
        prefs.edit()
            .putString(ACCESS, result.accessToken)
            .putString(REFRESH, result.refreshToken)
            .apply()
        Api.accessToken = result.accessToken
        currentUser = result.user
    }

    private companion object {
        const val ACCESS = "accessToken"
        const val REFRESH = "refreshToken"
    }
}
