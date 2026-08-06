package com.dnflores.wt7bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicReference
import fi.iki.elonen.NanoHTTPD

class BridgeService : Service() {
    private var server: BridgeServer? = null
    private val state = AtomicReference("Servidor iniciando")

    override fun onCreate() {
        super.onCreate()
        val channelId = "wt7_bridge"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "WT7 Bridge", NotificationManager.IMPORTANCE_LOW)
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("WT7 Bridge ativo")
            .setContentText("API local protegida por token")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        startForeground(1001, notification)

        server = BridgeServer(ConfigStore(this), state).also {
            it.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        }
        state.set("Servidor ativo")
    }

    override fun onDestroy() {
        server?.stop()
        server = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
