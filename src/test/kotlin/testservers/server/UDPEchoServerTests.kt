package testservers.server

import com.jasonernst.testservers.client.UdpEchoClient
import com.jasonernst.testservers.server.UdpEchoServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows

@Timeout(20)
class UDPEchoServerTests {
    @Test fun startStopTest() {
        val udpEchoServer = UdpEchoServer()
        udpEchoServer.start()
        udpEchoServer.stop()
    }

    @Test fun startAlreadyStarted() {
        val udpEchoServer = UdpEchoServer()
        udpEchoServer.start()
        assertThrows<IllegalStateException> { udpEchoServer.start() }
        udpEchoServer.stop()
    }

    @Test fun stopNotStarted() {
        val udpEchoServer = UdpEchoServer()
        assertThrows<IllegalStateException> { udpEchoServer.stop() }
    }

    @Test fun localUdpEcho() {
        val udpEchoServer = UdpEchoServer()
        udpEchoServer.start()

        val udpEchoClient = UdpEchoClient()
        val message = "Hello World"
        udpEchoClient.send(message)
        val recvMessage = udpEchoClient.recv()
        assertEquals(message, recvMessage)
        udpEchoClient.close()
        udpEchoServer.stop()
    }
}