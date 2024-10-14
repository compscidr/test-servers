package com.jasonernst.testservers.client

import com.jasonernst.testservers.server.TcpEchoServer.Companion.TCP_DEFAULT_PORT
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import kotlin.collections.sliceArray
import kotlin.jvm.javaClass
import kotlin.ranges.until
import kotlin.text.toByteArray

/**
 * A simple echo client which attempts to connect to the TCP echo server, sends a simple message
 * and then disconnects. Used for testing only, not to be used in the main lib.
 *
 * @param host The host to connect to.
 * @param port The port to connect to, defaults to TCP_DEFAULT_PORT.
 */
open class TcpEchoClient(
    private val host: String = "127.0.0.1",
    private val port: Int = TCP_DEFAULT_PORT,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var socketChannel: SocketChannel = SocketChannel.open()

    init {
        logger.debug("TCP client: Connecting to $host:$port")
        socketChannel.connect(InetSocketAddress(host, port))
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val client = TcpEchoClient()
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
        logger.debug("TCP client: Sending message: $message")
        socketChannel.write(ByteBuffer.wrap(message.toByteArray()))
    }

    /**
     * Blocking function until a message is received.
     * @return the message received.
     */
    fun recv(): String {
        logger.debug("TCP client: Waiting for message")
        val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
        val bytesRead = socketChannel.read(buffer)
        logger.debug("TCP client: Received $bytesRead bytes")
        return String(buffer.array().sliceArray(0 until bytesRead))
    }

    fun close() {
        socketChannel.close()
    }

    fun reopen() {
        socketChannel = SocketChannel.open()
        socketChannel.connect(InetSocketAddress(host, port))
    }
}
