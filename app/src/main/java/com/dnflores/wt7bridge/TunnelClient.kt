package com.dnflores.wt7bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.xml.parsers.DocumentBuilderFactory

data class TunnelInfo(val port: Int, val firmware: String)

class TunnelClient {
    private fun trustAllContext(): SSLContext {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        })
        return SSLContext.getInstance("TLS").apply {
            init(null, trustAll, SecureRandom())
        }
    }

    private fun envelope(oac: String, command: String, content: String = "") =
        """<?xml version="1.0" encoding="UTF-8"?><envelope><header>""" +
        "<password>$oac</password><passwordencode>1</passwordencode>" +
        "<security>username</security><username>adminapp2</username>" +
        "</header><body><command>$command</command><content>$content</content>" +
        "</body></envelope>"

    private fun parseTag(xml: String, tag: String): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream())
        return doc.getElementsByTagName(tag).item(0)?.textContent ?: ""
    }

    private fun post(port: Int, oac: String, command: String, content: String = ""): Pair<String, String> {
        val socket = trustAllContext().socketFactory.createSocket() as SSLSocket
        socket.soTimeout = 4_000
        socket.connect(InetSocketAddress("127.0.0.1", port), 700)
        socket.startHandshake()

        val body = envelope(oac, command, content).toByteArray(Charsets.UTF_8)
        val request = buildString {
            append("POST /tdkcgi HTTP/1.1\r\n")
            append("Host: 127.0.0.1:$port\r\n")
            append("Content-Type: application/xml;charset=utf-8\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)

        val output = socket.outputStream
        output.write(request)
        output.write(body)
        output.flush()
        val response = socket.inputStream.bufferedReader().use { it.readText() }
        socket.close()
        val xml = response.substringAfter("\r\n\r\n", response)
        return parseTag(xml, "error") to xml
    }

    private suspend fun openTcpPorts(start: Int, end: Int): List<Int> = coroutineScope {
        (start..end).chunked(256).flatMap { chunk ->
            chunk.map { port ->
                async(Dispatchers.IO) {
                    try {
                        Socket().use { socket ->
                            socket.connect(InetSocketAddress("127.0.0.1", port), 70)
                        }
                        port
                    } catch (_: Throwable) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    suspend fun discover(oac: String): TunnelInfo = withContext(Dispatchers.IO) {
        val ranges = listOf(30000..50000, 1025..29999, 50001..65535)
        for (range in ranges) {
            val candidates = openTcpPorts(range.first, range.last)
            for (port in candidates) {
                try {
                    val (error, xml) = post(port, oac, "get.device.status")
                    if (error == "0") {
                        return@withContext TunnelInfo(
                            port = port,
                            firmware = parseTag(xml, "version"),
                        )
                    }
                } catch (_: Throwable) {
                }
            }
        }
        error("Túnel do Allo Plus não encontrado. Abra a imagem ao vivo do WT7.")
    }

    suspend fun open(
        tunnelPort: Int,
        oac: String,
        pin: String,
        lock: Int,
        door: Int = 1,
    ): String = withContext(Dispatchers.IO) {
        val pinHash = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val content = "<door>$door</door><locknumber>$lock</locknumber><password>$pinHash</password>"
        val (error, xml) = post(tunnelPort, oac, "set.device.opendoor", content)
        if (error != "0") error("WT7 retornou erro $error")
        xml
    }
}
