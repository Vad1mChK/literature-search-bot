package com.vad1mchk.litsearchbot.documents

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.assertTrue

class IndexingServiceTest {
    companion object {
        private lateinit var indexingService: IndexingService
        private lateinit var literatureDir: File

        @JvmStatic
        @BeforeAll
        fun setupTests(
            @TempDir tempDir: Path,
        ) {
            // Create a subfolder in the temp directory for literature
            val litPath = tempDir.resolve("literature").createDirectories()

            // Copy files from resources to the temp directory
            listOf("testTxt.txt", "testDocx.docx", "testPdf.pdf").forEach { fileName ->
                val resourceStream = this::class.java.classLoader.getResourceAsStream("literature/$fileName")
                    ?: throw IllegalStateException("Resource $fileName not found")

                val target = litPath.resolve(fileName)
                resourceStream.use { input ->
                    java.nio.file.Files.copy(input, target)
                }
            }

            literatureDir = litPath.toFile()
            indexingService = IndexingService(literatureDir.absolutePath)
        }
    }

    @Test
    fun `test text extraction from TXT`() {
        val file = File(literatureDir, "testTxt.txt")
        val content = indexingService.extractText(file)

        assertTrue(content != null, "Content should not be null")
        assertTrue(
            content.contains("Lorem ipsum dolor sit amet txt"),
            "Content was: $content",
        )
    }

    @Test
    fun `test text extraction from PDF`() {
        val file = File(literatureDir, "testPdf.pdf")
        val content = indexingService.extractText(file)

        assertTrue(content != null, "Content should not be null")
        assertTrue(
            content.contains("Lorem ipsum dolor sit amet pdf"),
            "Content was: $content",
        )
    }

    @Test
    fun `test text extraction from DOCX`() {
        val file = File(literatureDir, "testDocx.docx")
        val content = indexingService.extractText(file)

        assertTrue(content != null, "Content should not be null")
        assertTrue(
            content.contains("Lorem ipsum dolor sit amet docx"),
            "Content was: $content",
        )
    }
}
