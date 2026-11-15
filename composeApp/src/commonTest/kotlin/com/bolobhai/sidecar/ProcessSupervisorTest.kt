package com.bolobhai.sidecar

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** A minimal fake Process for supervisor tests. */
private class FakeProcess : Process() {
    var destroyed = false
    override fun getOutputStream() = java.io.OutputStream.nullOutputStream()
    override fun getInputStream() = java.io.InputStream.nullInputStream()
    override fun getErrorStream() = java.io.InputStream.nullInputStream()
    override fun waitFor() = 0
    override fun exitValue() = 0
    override fun destroy() { destroyed = true }
    override fun isAlive() = !destroyed
}

class ProcessSupervisorTest {

    @Test
    fun missingBinary_isNotInstalled() = runBlocking {
        val s = ProcessSupervisor(launch = { null }, healthCheck = { false })
        assertEquals(SidecarStatus.NotInstalled, s.start())
        assertEquals(SidecarStatus.NotInstalled, s.status.value)
    }

    @Test
    fun healthyImmediately_isReady() = runBlocking {
        val s = ProcessSupervisor(
            launch = { FakeProcess() },
            healthCheck = { true },
            delayer = {},
        )
        assertEquals(SidecarStatus.Ready, s.start())
    }

    @Test
    fun neverHealthy_exhaustsBackoff_thenFailed() = runBlocking {
        var launches = 0
        val s = ProcessSupervisor(
            launch = { launches++; FakeProcess() },
            healthCheck = { false },
            maxHealthChecks = 2,
            delayer = {}, // no real waiting
        )
        assertEquals(SidecarStatus.Failed, s.start())
        // initial attempt + 3 backoff retries (1s,2s,5s) = 4 launches
        assertEquals(4, launches)
    }

    @Test
    fun gpuNeverHealthy_fallsBackToCpuLaunch_andBecomesReady() = runBlocking {
        var gpuLaunches = 0
        var cpuLaunches = 0
        val s = ProcessSupervisor(
            launch = { gpuLaunches++; FakeProcess() },      // GPU: never healthy
            fallbackLaunch = { cpuLaunches++; FakeProcess() },
            healthCheck = { cpuLaunches > 0 },              // CPU process passes health
            maxHealthChecks = 2,
            delayer = {},
        )
        assertEquals(SidecarStatus.Ready, s.start())
        assertEquals(1, gpuLaunches)  // tried GPU once
        assertEquals(1, cpuLaunches)  // then fell back to CPU
    }

    @Test
    fun stopDuringStartup_haltsRetryLoop_noRelaunch() = runBlocking {
        var launches = 0
        val parked = CompletableDeferred<Unit>()   // the loop parked on its first health retry
        val release = CompletableDeferred<Unit>()   // let it resume after we call stop()
        val s = ProcessSupervisor(
            launch = { launches++; FakeProcess() },
            healthCheck = { false },
            maxHealthChecks = 10,
            delayer = { if (!parked.isCompleted) parked.complete(Unit); release.await() },
        )
        val job = launch(Dispatchers.Default) { s.start() }
        parked.await()        // one launch has happened; loop is waiting
        s.stop()              // supersede the running loop
        release.complete(Unit)
        job.join()
        assertEquals(1, launches)                       // never relaunched after stop
        assertEquals(SidecarStatus.Stopped, s.status.value)
    }
}
