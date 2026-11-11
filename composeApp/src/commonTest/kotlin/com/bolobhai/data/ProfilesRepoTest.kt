package com.bolobhai.data

import com.bolobhai.domain.Profile
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfilesRepoTest {

    private fun repo() = ProfilesRepo(Files.createTempDirectory("bolobhai-profiles"))

    @Test
    fun ensureDefaults_writesFiveShippedProfiles() {
        val r = repo()
        r.ensureDefaults()
        val names = r.list().map { it.fileName }.toSet()
        assertEquals(
            setOf("hinglish-clean.md", "numbered-list.md", "email.md", "whatsapp-message.md", "meeting-notes.md"),
            names,
        )
    }

    @Test
    fun ensureDefaults_isNoOpWhenFolderNonEmpty() {
        val r = repo()
        r.write(Profile("only.md", "Only", "body"))
        r.ensureDefaults()
        assertEquals(listOf("only.md"), r.list().map { it.fileName })
    }

    @Test
    fun write_thenRead_roundTrips() {
        val r = repo()
        r.write(Profile("email.md", "My Email", "Write an email."))
        val read = r.read("email.md")
        assertEquals("My Email", read?.name)
        assertEquals("Write an email.", read?.body)
    }

    @Test
    fun rename_renamesFileToSlug_andRewritesName() {
        val r = repo()
        r.write(Profile("old.md", "Old", "body"))
        val renamed = r.rename("old.md", "Fancy New Name")
        assertEquals("fancy-new-name.md", renamed.fileName)
        assertEquals("Fancy New Name", renamed.name)
        assertNull(r.read("old.md"))
        assertEquals("Fancy New Name", r.read("fancy-new-name.md")?.name)
    }

    @Test
    fun delete_removesFile() {
        val r = repo()
        r.write(Profile("gone.md", "Gone", "x"))
        r.delete("gone.md")
        assertTrue(r.list().isEmpty())
    }
}
