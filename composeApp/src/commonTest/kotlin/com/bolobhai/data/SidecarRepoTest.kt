package com.bolobhai.data

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidecarRepoTest {

    private fun zipOf(dir: Path, entries: Map<String, String>): Path {
        val zip = dir.resolve("in.zip")
        ZipOutputStream(Files.newOutputStream(zip)).use { zos ->
            entries.forEach { (name, body) ->
                zos.putNextEntry(ZipEntry(name)); zos.write(body.toByteArray()); zos.closeEntry()
            }
        }
        return zip
    }

    @Test fun extract_writesFiles_cleanly() {
        val root = Files.createTempDirectory("sc")
        val repo = SidecarRepo(root)
        val target = root.resolve("windows").resolve("llama")
        // A stale file that a prior backend left behind must be gone after re-extract.
        Files.createDirectories(target); Files.writeString(target.resolve("old.dll"), "stale")

        val zip = zipOf(root, mapOf("llama-server.exe" to "bin", "ggml.dll" to "lib"))
        repo.extract(zip, target)

        assertEquals("bin", target.resolve("llama-server.exe").readText())
        assertTrue(target.resolve("ggml.dll").exists())
        assertFalse(target.resolve("old.dll").exists())
    }

    @Test fun extract_rejectsZipSlip() {
        val root = Files.createTempDirectory("sc")
        val repo = SidecarRepo(root)
        val zip = zipOf(root, mapOf("../../evil.exe" to "x"))
        assertFailsWith<IllegalArgumentException> { repo.extract(zip, root.resolve("windows").resolve("llama")) }
    }

    @Test fun sha256HexOf_matchesKnownDigest() {
        val root = Files.createTempDirectory("sc")
        val repo = SidecarRepo(root)
        val f = root.resolve("blob.bin")
        Files.writeString(f, "abc")
        // SHA-256("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", repo.sha256HexOf(f))
    }

    @Test fun installedBackend_readsMarker() {
        val root = Files.createTempDirectory("sc")
        val repo = SidecarRepo(root)
        val e = SidecarEntry("llama", "vulkan", "windows", "u", 1)
        assertFalse(repo.isInstalled(e))

        val m = root.resolve("windows").resolve("llama").resolve(".installed")
        Files.createDirectories(m.parent); Files.writeString(m, "vulkan")
        assertEquals("vulkan", repo.installedBackend("windows", "llama"))
        assertTrue(repo.isInstalled(e))
        assertFalse(repo.isInstalled(e.copy(backend = "cpu")))
    }
}
