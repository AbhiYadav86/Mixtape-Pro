package com.example

import com.example.audio.LrcParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testLrcParsing() {
    val sampleLrc = """
      [00:04.20] Retro music playing through the stereo
      [00:10.50] Neon lights in 1999
      [00:25.00] Winamp equalizer pumping bass
    """.trimIndent()

    val parsed = LrcParser.parse(sampleLrc)
    assertEquals(3, parsed.size)
    assertEquals(4200L, parsed[0].timeMs)
    assertEquals("Retro music playing through the stereo", parsed[0].text)
    assertEquals(10500L, parsed[1].timeMs)
    assertEquals("Neon lights in 1999", parsed[1].text)
    assertEquals(25000L, parsed[2].timeMs)
    assertEquals("Winamp equalizer pumping bass", parsed[2].text)
  }

  @Test
  fun testPlainLyricsFallback() {
    val plainText = """
      Line one of the retro song
      Line two with no timestamps
    """.trimIndent()

    val parsed = LrcParser.parse(plainText)
    assertEquals(2, parsed.size)
    assertEquals("Line one of the retro song", parsed[0].text)
    assertEquals("Line two with no timestamps", parsed[1].text)
  }
}
