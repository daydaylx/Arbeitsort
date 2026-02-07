package de.montagezeit.app.logging

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RingBufferLoggerTest {

    @Test
    fun `rotation keeps newest lines when file exceeds max size`() = runTest {
        val tempRoot = Files.createTempDirectory("ringbuffer-test-").toFile()
        try {
            val context = mockk<Context>(relaxed = true)
            every { context.filesDir } returns tempRoot

            val logger = RingBufferLogger(context)
            val logFile = File(tempRoot, "logs/debug.log")

            val largeContent = buildString {
                repeat(40_000) { index ->
                    append("seed-")
                    append(index)
                    append(' ')
                    append("x".repeat(64))
                    append('\n')
                }
            }
            logFile.parentFile?.mkdirs()
            logFile.writeText(largeContent)

            logger.log(
                level = RingBufferLogger.Level.INFO,
                tag = "RingBufferLoggerTest",
                message = "latest-entry"
            )

            val content = logFile.readText()
            val lines = content.lineSequence().filter { it.isNotBlank() }.toList()

            assertFalse(lines.any { it.contains("seed-0 ") })
            assertTrue(lines.any { it.contains("seed-39999 ") })
            assertTrue(lines.any { it.contains("latest-entry") })
        } finally {
            tempRoot.deleteRecursively()
        }
    }
}
