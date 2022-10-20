import UDPServer.ServerConstants.UDP_DEFAULT_PORT
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import sun.misc.Signal
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * A simple UDP echo server which can be used for testing.
 */
@SpringBootApplication
open class UDPServer {
    object ServerConstants {
        const val UDP_DEFAULT_PORT = 8899
        const val DEFAULT_BUFFER_SIZE = 1500
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private val socket: DatagramSocket = DatagramSocket(UDP_DEFAULT_PORT)
    private val buffer: ByteArray = ByteArray(ServerConstants.DEFAULT_BUFFER_SIZE)
    private val packet: DatagramPacket = DatagramPacket(buffer, buffer.size)
    private lateinit var listenerThread: Thread

    @Volatile private var running: Boolean = false

    init {
        socket.reuseAddress = true
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val server = UDPServer()
            server.start()
            Signal.handle(
                Signal("INT")
            )  // SIGINT
            { server.stop() }
            while (server.isRunning()) {
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
        listenerThread = Thread {
            while (running) {
                logger.debug("UDP server: Listening for UDP data")
                try {
                    socket.receive(packet)
                    logger.debug("UDP server: Received ${packet.length} bytes from ${packet.socketAddress}")
                    if (packet.length > 0) {
                        val clientAddress: InetAddress = packet.address
                        val clientPort: Int = packet.port

                        val recv = ByteArray(packet.length)
                        System.arraycopy(packet.data, 0, recv, 0, packet.length)
                        val recv_string = String(recv)
                        logger.debug("UDP Server: Got $recv_string")

                        val response = DatagramPacket(recv, recv.size, clientAddress, clientPort)
                        socket.send(response)
                        logger.debug("UDP Server: Response sent back to: ${clientAddress.hostAddress}:$clientPort")
                    }
                } catch (e: Exception) {
                    logger.error("Exception in UDP server: $e")
                }
            }
            running = false
        }
        listenerThread.start()
    }

    fun isRunning(): Boolean { return running }

    fun stop() {
        if (running) {
            logger.debug("Stopping UDP server")
            running = false
            socket.close()
            listenerThread.join()
            logger.debug("UDP Server stopped")
        } else {
            logger.debug("Can't stop UDP server, wasn't running")
        }
    }
}
