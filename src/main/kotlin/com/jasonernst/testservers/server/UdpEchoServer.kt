package com.jasonernst.testservers.server

import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import kotlin.isInitialized
import kotlin.jvm.javaClass

/**
 * A simple UDP echo server which receives a message and then sends it back to the client.
 * Used for testing only, not for anything production.
 */
class UdpEchoServer {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var socket: DatagramSocket
    private val buffer: ByteArray = ByteArray(DEFAULT_BUFFER_SIZE)
    private val packet: DatagramPacket = DatagramPacket(buffer, buffer.size)
    private lateinit var listenerThread: Thread
    @Volatile private var running: Boolean = false

    companion object {
        private val staticLogger = LoggerFactory.getLogger(javaClass)
        const val UDP_DEFAULT_PORT = 8899
        const val DEFAULT_BUFFER_SIZE = 1500

        @JvmStatic
        fun main(args: Array<String>) {
            val server = UdpEchoServer()
            server.start()
            Signal.handle(
                Signal("INT")
            )  // SIGINT
            { server.stop() }
            while (server.running) {
                staticLogger.debug("Waiting for server to stop running")
                Thread.sleep(100)
            }
        }
    }

    fun start() {
        if (running) {
            logger.error("Server is already running")
            throw IllegalStateException("Server is already running")
        }

        running = true
        socket = DatagramSocket(UDP_DEFAULT_PORT)
        socket.reuseAddress = true
        socket.broadcast = true
        listenerThread =
            Thread {
                while (running) {
                    logger.debug("UDP server: Listening for UDP data on port $UDP_DEFAULT_PORT")
                    try {
                        socket.receive(packet)
                        logger.debug("UDP server: Received ${packet.length} bytes from ${packet.socketAddress}")
                        if (packet.length > 0) {
                            val clientAddress: InetAddress = packet.address
                            val clientPort: Int = packet.port

                            val recv = ByteArray(packet.length)
                            System.arraycopy(packet.data, 0, recv, 0, packet.length)
                            val recvString = String(recv)
                            logger.debug("UDP Server: Got $recvString")
                            val response = DatagramPacket(recv, packet.length, clientAddress, clientPort)
                            socket.send(response)
                            logger.debug("UDP Server: Response sent back to: ${clientAddress.hostAddress}:$clientPort")
                        }
                    } catch (e: SocketException) {
                        logger.debug("UDP server: Socket closed, probably shutting down")
                        break
                    } catch (e: Exception) {
                        logger.error("Exception in UDP server: $e")
                    }
                }
                running = false
            }
        listenerThread.start()
    }

    fun stop() {
        if (running) {
            logger.debug("Stopping UDP server")
            running = false
            if (::socket.isInitialized && !socket.isClosed) {
                socket.close()
            }
            listenerThread.join()
            logger.debug("UDP Server stopped")
        } else {
            throw IllegalStateException("Can't stop UDP server, wasn't running")
        }
    }
}
