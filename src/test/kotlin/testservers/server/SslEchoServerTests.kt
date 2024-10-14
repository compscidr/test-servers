package testservers.server

import com.jasonernst.testservers.client.SslEchoClient
import com.jasonernst.testservers.server.SslEchoServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows

@Timeout(20)
class SslEchoServerTests {
    @Test fun startStopTest() {
        val sslEchoServer = SslEchoServer()
        sslEchoServer.start()
        Thread.sleep(100)
        sslEchoServer.stop()
    }

    @Test fun startAlreadyStarted() {
        val sslEchoServer = SslEchoServer()
        sslEchoServer.start()
        assertThrows<IllegalStateException> { sslEchoServer.start() }
        sslEchoServer.stop()
    }

    @Test fun stopNotStarted() {
        val sslEchoServer = SslEchoServer()
        assertThrows<IllegalStateException> { sslEchoServer.stop() }
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
