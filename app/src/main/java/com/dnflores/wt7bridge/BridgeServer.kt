package com.dnflores.wt7bridge

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference

class BridgeServer(
    private val configStore: ConfigStore,
    private val state: AtomicReference<String>,
) : NanoHTTPD(configStore.load().listenPort) {

    private val cloud = CloudClient()
    private val tunnel = TunnelClient()
    @Volatile private var cachedDevice: CloudDevice? = null
    @Volatile private var cachedTunnel: TunnelInfo? = null

    override fun serve(session: IHTTPSession): Response {
        val config = configStore.load()
        if (session.headers["authorization"] != "Bearer ${config.apiToken}") {
            return json(Response.Status.UNAUTHORIZED, false, "Unauthorized")
        }

        return try {
            when {
                session.method == Method.GET && session.uri == "/health" -> {
                    json(
                        Response.Status.OK,
                        true,
                        state.get(),
                        mapOf(
                            "listen_port" to config.listenPort,
                            "tunnel_port" to (cachedTunnel?.port ?: 0),
                            "firmware" to (cachedTunnel?.firmware ?: ""),
                        ),
                    )
                }

                session.method == Method.POST && session.uri.matches(Regex("/open/[12]")) -> {
                    val lock = session.uri.substringAfterLast("/").toInt()
                    val result = runBlocking {
                        val device = cachedDevice ?: cloud.loginAndGetDevice(
                            config.email,
                            config.password,
                            config.deviceName,
                        ).also { cachedDevice = it }

                        var bridge = cachedTunnel
                        if (bridge == null) {
                            bridge = tunnel.discover(device.oac)
                            cachedTunnel = bridge
                        }

                        try {
                            tunnel.open(bridge.port, device.oac, config.pin, lock)
                        } catch (_: Throwable) {
                            bridge = tunnel.discover(device.oac)
                            cachedTunnel = bridge
                            tunnel.open(bridge.port, device.oac, config.pin, lock)
                        }
                    }
                    json(Response.Status.OK, true, "Relé $lock acionado", mapOf("response" to result))
                }

                else -> json(Response.Status.NOT_FOUND, false, "Not found")
            }
        } catch (t: Throwable) {
            state.set("Erro: ${t.message}")
            json(Response.Status.INTERNAL_ERROR, false, t.message ?: t::class.java.name)
        }
    }

    private fun json(
        status: Response.Status,
        ok: Boolean,
        message: String,
        extra: Map<String, Any> = emptyMap(),
    ): Response {
        val body = JSONObject().apply {
            put("ok", ok)
            put("message", message)
            extra.forEach { (key, value) -> put(key, value) }
        }.toString()
        return newFixedLengthResponse(status, "application/json; charset=utf-8", body)
    }
}
