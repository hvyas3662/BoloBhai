package com.bolobhai.platform

import com.bolobhai.log.Logger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Prevents a second app instance: double-tapping the shortcut otherwise opened two windows + two
 * trays, each spawning its own sidecar servers. The first instance holds an exclusive OS lock on a
 * file in app-data for its whole lifetime; a later instance fails to take it and bails out — but
 * first it pokes the running instance over a loopback socket so it can bring its window to front.
 */
object SingleInstance {
    // Held for the process lifetime so the lock isn't garbage-collected (which would release it).
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /** Set by the owning instance; called (off the UI thread) when another launch asks us to show. */
    @Volatile
    var onActivate: (() -> Unit)? = null

    /**
     * True if we got the lock (we're the only instance). False if another instance already holds it —
     * in which case we've signalled it to surface its window and the caller should exit.
     */
    fun acquire(lockFile: Path, portFile: Path): Boolean = try {
        lockFile.parent?.let { Files.createDirectories(it) }
        val ch = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        val l = ch.tryLock()
        if (l == null) {
            ch.close()
            signalExisting(portFile)
            false
        } else {
            channel = ch
            lock = l
            startSignalServer(portFile)
            true
        }
    } catch (_: Exception) {
        // Never hard-block a launch on a lock error — degrade to allowing it.
        true
    }

    /** Listen on an ephemeral loopback port; each connection means "another launch — show yourself". */
    private fun startSignalServer(portFile: Path) {
        val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        Files.writeString(portFile, server.localPort.toString())
        Thread {
            while (!server.isClosed) {
                try {
                    server.accept().close()
                    onActivate?.invoke()
                } catch (_: Exception) {
                }
            }
        }.apply { isDaemon = true; name = "single-instance-signal"; start() }
    }

    /** Tell the already-running instance (via the port it wrote) to bring its window to front. */
    private fun signalExisting(portFile: Path) {
        try {
            val port = Files.readString(portFile).trim().toInt()
            Socket(InetAddress.getLoopbackAddress(), port).use { it.getOutputStream().write(1) }
        } catch (e: Exception) {
            Logger.warn("single-instance: could not signal the running instance — ${e.message}")
        }
    }
}
