package com.jasonernst.testservers.server

import org.slf4j.LoggerFactory
import sun.misc.Signal
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.collections.set
import kotlin.isInitialized
import kotlin.jvm.javaClass

/**
 * A simple TCP echo server which listens for incoming connections and echoes back the received
 * message. Can optionally close the connection after sending the message, or keep it open until
 * the client closes the connection.
 *
 * Used for testing only, not to be used in the main lib.
 */
@Suppress("NewApi") // this isn't intended to run on Android anyway, so it doesn't matter
open class TcpEchoServer {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var listenerThread: Thread
    private lateinit var serverSocket: ServerSocket
    protected val executor = Executors.newFixedThreadPool(THREADS)
    protected val sessionMap = ConcurrentHashMap<Socket, Socket>()

    @Volatile
    private var shutDownAfterReply = false

    @Volatile
    private var shutDownAfterAccept = false

    @Volatile
    private var running: Boolean = false

    companion object {
        private val staticLogger = LoggerFactory.getLogger(javaClass)
        const val TCP_DEFAULT_PORT = 8888
        const val THREADS = 5

        @JvmStatic
        fun main(args: Array<String>) {
            val server = TcpEchoServer()
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

    /**
     * Note: if you change this after the server has started, it won't take effect until the next
     * restart.
     */
    fun setShutDownAfterReply(shutDownAfterReply: Boolean) {
        this.shutDownAfterReply = shutDownAfterReply
    }

    /**
     * Note: if you change this after the server has started, it won't take effect until the next
     * restart.
     */
    fun setShutDownAfterAccept(shutDownAfterAccept: Boolean) {
        this.shutDownAfterAccept = shutDownAfterAccept
    }

    fun connectedClients(): Int = sessionMap.size

    /**
     * Starts the TCP server. The server is blocking until a connection is made, then to handle the
     * connection, a new thread is spawned via the [executor] to handle the connection. Each session
     * is kept track of until they are complete so that when the server shuts down, all outstanding
     * sessions are closed.
     */
    open fun start() {
        if (running) {
            logger.error("$javaClass is already running")
            throw IllegalStateException("TCP server is already running")
        }
        serverSocket = ServerSocket()
        running = true

        logger.debug("$javaClass: Starting server version TODO")
        serverSocket.bind(InetSocketAddress(TCP_DEFAULT_PORT))
        // the line below gives a warning, but this isn't running on Android so it's fine
        serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true)

        listenerThread =
            Thread({
                while (running) {
                    logger.debug(
                        "$javaClass: waiting for connection on port: $TCP_DEFAULT_PORT " +
                            "shutDownAfterAccept?: $shutDownAfterAccept " +
                            "shutdownAfterReply? $shutDownAfterReply",
                    )
                    try {
                        val clientSocket = serverSocket.accept()
                        if (clientSocket == null) {
                            logger.debug("$javaClass: null client socket on accept")
                        } else {
                            logger.debug(
                                "$javaClass: Got connection from" +
                                    " ${clientSocket.remoteSocketAddress}. Serving " +
                                    "${connectedClients()} clients",
                            )

                            if (shutDownAfterAccept) {
                                logger.debug("$javaClass: shutting down after accept")
                                try {
                                    clientSocket.close()
                                } catch (e: Exception) {
                                    // ignore if we can't close, maybe other side already did or
                                    // something
                                }
                            } else {
                                executor.submit { handleConnection(clientSocket) }
                            }
                        }
                    } catch (e: AsynchronousCloseException) {
                        logger.debug(
                            "$javaClass: AsynchronousCloseException, probably shutting " +
                                "down: $e",
                        )
                        break
                    } catch (e: IOException) {
                        logger.error("$javaClass: IOException: $e")
                    }
                }
                logger.debug("TCP server stopped")
            }, "TCPServer Read")
        listenerThread.start()
    }

    /**
     * Handle a connection from a client. The sessions in this expect the client to send a message
     * first, and then we will send it back. It will continue until the client side closes the
     * connection.
     */
    protected fun handleConnection(clientSocket: Socket) {
        sessionMap[clientSocket] = clientSocket
        clientSocket.soTimeout = 5000
        val buffer: ByteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
        val remoteAddress = clientSocket.remoteSocketAddress
        while (clientSocket.isConnected) {
            try {
                val bytesRead = clientSocket.getInputStream().read(buffer.array())
                if (bytesRead == -1) {
                    logger.debug("${javaClass.simpleName}: Client $remoteAddress closed connection")
                    break
                }
                if (bytesRead > 0) {
                    buffer.rewind()
                    buffer.limit(bytesRead)
                    logger.debug(
                        "${javaClass.simpleName}: Read $bytesRead bytes on $javaClass " +
                            "from: $remoteAddress",
                    )
//                    logger.debug(
//                        "$javaClass: Payload: ${String(
//                            buffer.array(),
//                            0,
//                            bytesRead,
//                        )}",
//                    )
                    clientSocket.getOutputStream().write(buffer.array(), 0, bytesRead)
                    clientSocket.getOutputStream().flush()
                    logger.debug(
                        "${javaClass.simpleName}: Wrote $bytesRead bytes from $javaClass " +
                            "to: $remoteAddress",
                    )
                    buffer.clear()
                    if (shutDownAfterReply) {
                        logger.debug(
                            "$javaClass: shutting down connection after reply " +
                                "to: $remoteAddress",
                        )
                        clientSocket.close()
                        sessionMap.remove(clientSocket)
                        return
                    }
                }
            } catch (e: AsynchronousCloseException) {
                logger.debug(
                    "${javaClass.simpleName}: AsynchronousCloseException, on read / write to existing " +
                        "connection: $remoteAddress, probably connection closed from client" +
                        " side: $e",
                )
                break
            } catch (e: TimeoutException) {
                logger.warn(
                    "${javaClass.simpleName}: timed out on read / write to existing connection: " +
                        "$remoteAddress, probably connection closed from client side: $e",
                )
                break
            } catch (e: IOException) {
                logger.error(
                    "${javaClass.simpleName}: IO Exception on read / write to existing" +
                        " connection $remoteAddress: $e",
                )
                break
            }
        }
        logger.debug("${javaClass.simpleName}: Done handling session: $remoteAddress")
        sessionMap.remove(clientSocket)
        logger.debug("${javaClass.simpleName}: Connection closed")
    }

    fun stop() {
        if (running) {
            running = false
            if (::serverSocket.isInitialized) {
                try {
                    serverSocket.close()
                } catch (e: IOException) {
                    logger.error("${javaClass.simpleName}: IOException on close: $e")
                }
            }
            if (::listenerThread.isInitialized) {
                if (listenerThread.isAlive) {
                    listenerThread.join()
                }
            }
            for (session in sessionMap.keys) {
                session.close()
            }
            logger.debug("${javaClass.simpleName} stopped")
        } else {
            throw IllegalStateException("Can't stop ${javaClass.simpleName}, wasn't running")
        }
    }
}
