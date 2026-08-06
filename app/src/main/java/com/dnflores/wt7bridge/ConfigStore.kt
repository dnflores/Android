package com.dnflores.wt7bridge

import android.content.Context
import java.security.SecureRandom
import java.util.Base64

data class BridgeConfig(
    val email: String,
    val password: String,
    val pin: String,
    val deviceName: String,
    val apiToken: String,
    val listenPort: Int,
)

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("wt7_bridge", Context.MODE_PRIVATE)

    fun load(): BridgeConfig {
        var token = prefs.getString("api_token", null)
        if (token.isNullOrBlank()) {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
            prefs.edit().putString("api_token", token).apply()
        }

        return BridgeConfig(
            email = prefs.getString("email", "dnflores@gmail.com") ?: "dnflores@gmail.com",
            password = prefs.getString("password", "") ?: "",
            pin = prefs.getString("pin", "") ?: "",
            deviceName = prefs.getString("device_name", "Casa") ?: "Casa",
            apiToken = token,
            listenPort = prefs.getInt("listen_port", 8765),
        )
    }

    fun save(config: BridgeConfig) {
        prefs.edit()
            .putString("email", config.email)
            .putString("password", config.password)
            .putString("pin", config.pin)
            .putString("device_name", config.deviceName)
            .putString("api_token", config.apiToken)
            .putInt("listen_port", config.listenPort)
            .apply()
    }
}
