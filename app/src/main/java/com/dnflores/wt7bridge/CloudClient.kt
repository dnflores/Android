package com.dnflores.wt7bridge

import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.xml.parsers.DocumentBuilderFactory

data class CloudDevice(
    val id: String,
    val name: String,
    val model: String,
    val oac: String,
)

class CloudClient {
    private val host = "https://intelbras-4.qvcloud.net:443"
    private val appId = "4077"
    private var cookie: String = ""

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun insecureSslContext(): SSLContext {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        })
        return SSLContext.getInstance("TLS").apply {
            init(null, trustAll, SecureRandom())
        }
    }

    private fun post(path: String, body: String): String {
        val connection = URL(host + path).openConnection() as HttpsURLConnection
        connection.sslSocketFactory = insecureSslContext().socketFactory
        connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/xml;charset=utf-8")
        connection.setRequestProperty("User-Agent", "okhttp/3.12.13")
        if (cookie.isNotBlank()) connection.setRequestProperty("Cookie", cookie)
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        connection.headerFields["Set-Cookie"]?.firstOrNull()?.let {
            cookie = it.substringBefore(";")
        }
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = stream.bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) {
            error("Nuvem HTTP ${connection.responseCode}: ${text.take(200)}")
        }
        return text
    }

    private fun envelope(
        command: String,
        seq: Int,
        content: String,
        clientId: String,
        sessionId: String = "",
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <envelope><header>
        <flag>tdkcloud</flag><version>v1.24</version>
        <command>$command</command><seq>$seq</seq>
        <session>$sessionId</session><user-data></user-data>
        <client><id>$clientId</id><type>3</type>
        <oem>A0077,G0077</oem><app>$appId</app></client>
        </header>$content</envelope>
    """.trimIndent().replace("\n", "")

    private fun parse(xml: String) =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream())

    fun loginAndGetDevice(email: String, password: String, deviceName: String): CloudDevice {
        val clientId = "003-$appId-${UUID.randomUUID().toString().replace("-", "").take(16)}"
        post("/auth/user;jus_duplex=down", "")

        val loginContent = """
            <content><account>$email</account>
            <password>${sha256(password)}</password>
            <auth-type>0</auth-type><auth-code></auth-code>
            <ip-region-id>0</ip-region-id></content>
        """.trimIndent().replace("\n", "")

        post(
            "/auth/user;jus_duplex=up",
            envelope("login", 1, loginContent, clientId),
        )
        val loginXml = post("/auth/user;jus_duplex=down", "")
        val loginDoc = parse(loginXml)
        val result = loginDoc.getElementsByTagName("result").item(0)?.textContent ?: ""
        val sessionNode = loginDoc.getElementsByTagName("session").item(0)
        val session = if (sessionNode is org.w3c.dom.Element) {
            sessionNode.getElementsByTagName("id").item(0)?.textContent ?: ""
        } else {
            ""
        }
        if (result != "0" && result != "100") error("Login recusado: $result")

        val listContent = "<content><filter></filter><order>0</order><count>0</count><page>0</page><owner></owner></content>"
        post(
            "/auth/user;jus_duplex=up",
            envelope("get-device-list", 2, listContent, clientId, session),
        )
        val listXml = post("/auth/user;jus_duplex=down", "")
        val doc = parse(listXml)
        val nodes = doc.getElementsByTagName("device")
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as org.w3c.dom.Element
            fun tag(name: String): String =
                element.getElementsByTagName(name).item(0)?.textContent ?: ""
            val name = tag("name")
            if (deviceName.isBlank() || name == deviceName) {
                return CloudDevice(
                    id = tag("id"),
                    name = name,
                    model = tag("model"),
                    oac = tag("out-auth-code").ifBlank { tag("auth-code") },
                )
            }
        }
        error("Dispositivo '$deviceName' não encontrado")
    }
}
