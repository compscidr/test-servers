package com.jasonernst.testservers.client

import com.jasonernst.testservers.server.UdpEchoServer.Companion.UDP_DEFAULT_PORT
import org.slf4j.LoggerFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.collections.sliceArray
import kotlin.jvm.javaClass
import kotlin.ranges.until
import kotlin.text.toByteArray

class UdpEchoClient(
    host: String = "127.0.0.1",
    port: Int = UDP_DEFAULT_PORT,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val socket = DatagramSocket()

    init {
        logger.debug("UDP client: Connecting to $host:$port")
        socket.connect(InetSocketAddress(host, port))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val client = UdpEchoClient()
            val message = "Hello World"
            client.send(message)
            val recvMethodError = client.recv()

            if (recvMethodError != message) {
                throw RuntimeException("Received message does not match sent message")
            }
        }
    }

    /**
     * Sends a message to the server. Does not wait for a response.
     */
    fun send(message: String) {
        logger.debug("Sending message: $message")
        val buffer = message.toByteArray()
        val packet = DatagramPacket(buffer, buffer.size)
        socket.send(packet)
    }

    /**
     * Blocking function until a message is received.
     * @return the message received
     */
    fun recv(): String {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val packet = DatagramPacket(buffer, DEFAULT_BUFFER_SIZE)
        socket.receive(packet)
        val message = String(packet.data.sliceArray(0 until packet.length))
        logger.debug("Received message: $message")
        return message
    }

    fun close() {
        socket.close()
    }
}
