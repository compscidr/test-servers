package testservers.server

import com.jasonernst.testservers.client.SslEchoClient
import com.jasonernst.testservers.server.SslEchoServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

@Timeout(20)
class SslEchoServerTests {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Test fun startStopTest() {
        logger.debug("start stop test")
        val sslEchoServer = SslEchoServer()
        sslEchoServer.start()
        Thread.sleep(100)
        sslEchoServer.stop()
        logger.debug("end start stop test")
    }

    @Test fun startAlreadyStarted() {
        logger.debug("already started test")
        val sslEchoServer = SslEchoServer()
        sslEchoServer.start()
        assertThrows<IllegalStateException> { sslEchoServer.start() }
        sslEchoServer.stop()
        logger.debug("end already started test")
    }

    @Test fun stopNotStarted() {
        logger.debug("stop not started test")
        val sslEchoServer = SslEchoServer()
        assertThrows<IllegalStateException> { sslEchoServer.stop() }
        logger.debug("end stop not started test")
    }

    @Test fun localSslEcho() {
        val sslEchoServer = SslEchoServer()
        sslEchoServer.start()

        val sslEchoClient = SslEchoClient()
        val message = "Hello World"
        sslEchoClient.send(message)
        val recvMessage = sslEchoClient.recv()
        assertEquals(message, recvMessage)
        sslEchoClient.close()
        sslEchoServer.stop()
    }
}
