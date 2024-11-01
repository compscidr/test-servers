package com.jasonernst.testservers.server
import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.TrustManagerFactory
import kotlin.jvm.java
import kotlin.jvm.javaClass
import kotlin.text.toCharArray

// Note: if you want to run this locally, make sure you add your local IP address to the SAN
// and generate a new keypair. You can do this with the following commands:
// https://stackoverflow.com/questions/53323855/sslserversocket-and-certificate-setup
// keytool -genkeypair -alias server -keyalg RSA -keystore servercert.p12 -storetype pkcs12 -v -storepass abc123 -validity 10000 -ext san=ip:<insert your local IP>
// keytool -genkeypair -alias server -keyalg RSA -keystore servercert.p12 -storetype pkcs12 -v -storepass abc123 -validity 10000 -ext san=dns:localhost,dns:echo.bumpapp.xyz,ip:127.0.0.1,ip:<insert your local IP>
class SslEchoServer(
    val port: Int = SSL_DEFAULT_PORT,
    private val tlsVersion: String = "TLSv1.3",
    private val trustStoreName: String = "servercert.p12",
    private val trustStorePassword: String = "abc123",
    private val keyStoreName: String = "servercert.p12",
    private val keyStorePassword: String = "abc123",
) : TcpEchoServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private val staticLogger = LoggerFactory.getLogger(javaClass)
        const val SSL_DEFAULT_PORT = 8889

        @JvmStatic
        fun main(args: Array<String>) {
            val server = SslEchoServer()
            server.start()
            Signal.handle(
                Signal("INT"),
            ) // SIGINT
                { server.stop() }
            while (server.running) {
                staticLogger.debug("Waiting for server to stop running")
                Thread.sleep(100)
            }
        }
    }

    override fun start() {
        if (running) {
            logger.error("SSL server is already running")
            throw IllegalStateException("SSL server is already running")
        }
        running = true

        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val trustStoreInputstream = Companion::class.java.getResourceAsStream("/$trustStoreName")
        trustStore.load(trustStoreInputstream, trustStorePassword.toCharArray())
        trustStoreInputstream?.close()
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)

        val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        val keystoreInputstream = Companion::class.java.getResourceAsStream("/$keyStoreName")
        keystore.load(keystoreInputstream, keyStorePassword.toCharArray())
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keystore, keyStorePassword.toCharArray())

        val ctx = SSLContext.getInstance("TLS")
        ctx.init(kmf.keyManagers, tmf.trustManagers, SecureRandom.getInstanceStrong())

        val factory = ctx.serverSocketFactory
        try {
            serverSocket = factory.createServerSocket(port) as SSLServerSocket
            (serverSocket as SSLServerSocket).needClientAuth = true
            (serverSocket as SSLServerSocket).enabledProtocols = arrayOf(tlsVersion)

            logger.debug("SSL server listening on port: $port")

            listenerThread =
                Thread({
                    while (running) {
                        try {
                            val clientSocket = serverSocket.accept()
                            logger.debug("SSL server: accepted connection from: ${clientSocket.inetAddress.hostAddress}")
                            executor.submit {
                                handleConnection(
                                    clientSocket,
                                )
                            }
                        } catch (e: AsynchronousCloseException) {
                            logger.debug(
                                "${javaClass.simpleName}: AsynchronousCloseException, probably shutting " +
                                    "down: $e",
                            )
                            break
                        } catch (e: IOException) {
                            logger.error("${javaClass.simpleName}: IOException: $e")
                        }
                    }
                }, "SSL-Server-Listener")
            listenerThread.start()
        } catch (e: Exception) {
            logger.debug("SSL server: error creating server socket: ${e.message}", e)
            running = false
        }
    }
}
