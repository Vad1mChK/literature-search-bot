package com.vad1mchk.litsearchbot.documents

import com.vad1mchk.litsearchbot.database.DatabaseTestBase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndexingServiceTest : DatabaseTestBase() {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var literatureDir: File
    private lateinit var indexingService: IndexingService

    @BeforeEach
    fun setupTests() {
        val litPath = tempDir.resolve("literature")
        if (!Files.exists(litPath)) {
            litPath.createDirectories()
        }

        listOf("testTxt.txt", "testDocx.docx", "testPdf.pdf").forEach { fileName ->
            val resourceStream = javaClass.classLoader.getResourceAsStream("literature/$fileName")
                ?: throw IllegalStateException("Resource $fileName not found")

            val target = litPath.resolve(fileName)
            resourceStream.use { input ->
                Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        }

        literatureDir = litPath.toFile()
        indexingService = IndexingService(literatureDir.absolutePath)
    }

    @ParameterizedTest
    @CsvSource(
        "testTxt.txt,Lorem ipsum dolor sit amet txt",
        "testPdf.pdf,Lorem ipsum dolor sit amet pdf",
        "testDocx.docx,Lorem ipsum dolor sit amet docx",
    )
    fun extractText_shouldReadSupportedFiles(fileName: String, expectedText: String) {
        val file = File(literatureDir, fileName)
        val content = indexingService.extractText(file)

        assertNotNull(content)
        assertContains(content, expectedText)
    }

    @Test
    fun reindex_afterAddingNewTextFile_deltaShouldCountAddedFile() {
        indexingService.reindex()
        val file = literatureDir.resolve("addFile.txt")
        file.writeText("Lorem Ipsum Dolor Sit Amet")

        val deltaStats = indexingService.reindex()
        assertEquals(1, deltaStats.added)
        assertEquals(0, deltaStats.updated)
        assertEquals(0, deltaStats.deleted)
        file.delete()
    }

    @Test
    fun reindex_afterDeletingTextFile_deltaShouldCountDeletedFile() {
        val file = literatureDir.resolve("deleteFile.txt")
        file.writeText("Lorem Ipsum Dolor Sit Amet")
        indexingService.reindex()

        file.delete()

        val deltaStats = indexingService.reindex()
        assertEquals(0, deltaStats.added)
        assertEquals(0, deltaStats.updated)
        assertEquals(1, deltaStats.deleted)
    }

    @Test
    fun computeTotalStats_afterDeletingFileOnDiskWithoutReindex_totalShouldCountDisappearedFile() {
        val file = literatureDir.resolve("deleteFile.txt")
        file.writeText("Lorem Ipsum Dolor Sit Amet")
        indexingService.reindex()

        file.delete()

        val totalStats = indexingService.computeTotalStats()
        assertEquals(1, totalStats.disappeared)
        assertEquals(-1, totalStats.totalOnDisk - totalStats.totalInDatabase)
    }

    @Test
    fun computeTotalStats_afterAddingFileOnDiskWithoutReindex_totalShouldCountUnindexedFile() {
        indexingService.reindex()

        val file = literatureDir.resolve("deleteFile.txt")
        file.writeText("Lorem Ipsum Dolor Sit Amet")

        val totalStats = indexingService.computeTotalStats()
        assertEquals(1, totalStats.unindexed)
        assertEquals(1, totalStats.totalOnDisk - totalStats.totalInDatabase)

        file.delete()
    }
}
