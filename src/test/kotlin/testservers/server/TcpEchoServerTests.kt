package testservers.server

import com.jasonernst.testservers.client.TcpEchoClient
import com.jasonernst.testservers.server.TcpEchoServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows

@Timeout(20)
class TcpEchoServerTests {
    @Test fun startStopTest() {
        val tcpEchoServer = TcpEchoServer()
        tcpEchoServer.start()
        tcpEchoServer.stop()
    }

    @Test fun startAlreadyStarted() {
        val tcpEchoServer = TcpEchoServer()
        tcpEchoServer.start()
        assertThrows<IllegalStateException> { tcpEchoServer.start() }
        tcpEchoServer.stop()
    }

    @Test fun stopNotStarted() {
        val tcpEchoServer = TcpEchoServer()
        assertThrows<IllegalStateException> { tcpEchoServer.stop() }
    }

    @Test fun localTcpEcho() {
        val tcpEchoServer = TcpEchoServer()
        tcpEchoServer.start()

        val tcpEchoClient = TcpEchoClient()
        val message = "Hello World"
        tcpEchoClient.send(message)
        val recvMessage = tcpEchoClient.recv()
        assertEquals(message, recvMessage)
        tcpEchoClient.close()
        tcpEchoServer.stop()
    }
}
