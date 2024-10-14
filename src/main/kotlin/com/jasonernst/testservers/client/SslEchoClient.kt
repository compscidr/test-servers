package com.jasonernst.testservers.client

import com.jasonernst.testservers.server.SslEchoServer.Companion.SSL_DEFAULT_PORT
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory
import kotlin.collections.sliceArray
import kotlin.jvm.java
import kotlin.jvm.javaClass
import kotlin.ranges.until
import kotlin.text.toByteArray
import kotlin.text.toCharArray

class SslEchoClient(
    host: String = "echo.bumpapp.xyz",
    port: Int = SSL_DEFAULT_PORT,
    tlsVersion: String = "TLSv1.3",
    trustStoreName: String = "servercert.p12",
    trustStorePassword: String = "abc123",
    keyStoreName: String = "servercert.p12",
    keyStorePassword: String = "abc123",
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val sslSocket: SSLSocket

    init {
        val trustStore = KeyStore.getInstance(KEYSTORE_TYPE)
        val trustStoreInputstream = Companion::class.java.getResourceAsStream("/$trustStoreName")
        trustStore.load(trustStoreInputstream, trustStorePassword.toCharArray())
        trustStoreInputstream?.close()
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val keystore = KeyStore.getInstance(KEYSTORE_TYPE)
        val keystoreInputstream = Companion::class.java.getResourceAsStream("/$keyStoreName")
        keystore.load(keystoreInputstream, keyStorePassword.toCharArray())
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keystore, keyStorePassword.toCharArray())

        val ctx = SSLContext.getInstance("TLS")
        ctx.init(kmf.keyManagers, tmf.trustManagers, SecureRandom.getInstanceStrong())

        val serverAddress = InetAddress.getByName(host)
        sslSocket = ctx.socketFactory.createSocket(serverAddress, port) as SSLSocket
        sslSocket.enabledProtocols = arrayOf(tlsVersion)
        val sslParams = SSLParameters()
        sslParams.endpointIdentificationAlgorithm = "HTTPS"
        sslSocket.sslParameters = sslParams
    }

    companion object {
        // if we don't define this explicitly and use the default, ubuntu 22.04 uses a different
        // default keytype than Android and this breaks when Android tries to talk to the server
        const val KEYSTORE_TYPE = "PKCS12"

        @JvmStatic
        fun main(args: Array<String>) {
            val client = SslEchoClient()
            val message = "Hello World"
            client.send(message)
            val recvMessage = client.recv()
            println("Received: $recvMessage")
        }
    }

    /**
     * Sends a message to the server. Does not wait for a response.
     */
    fun send(message: String) {
        logger.debug("SSL client: Sending message: $message")
        val outputStream = sslSocket.outputStream
        outputStream.write(message.toByteArray())
    }

    /**
     * Blocking function until a message is received.
     * @return the message received.
     */
    fun recv(): String {
        logger.debug("SSL client: Waiting for message")
        val inputStream = sslSocket.inputStream
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val bytesRead = inputStream.read(buffer)
        logger.debug("SSL client: Received $bytesRead bytes")
        return String(buffer.sliceArray(0 until bytesRead))
    }

    fun close() {
        sslSocket.close()
    }
}
