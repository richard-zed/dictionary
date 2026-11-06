package rip.failed.iris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rip.failed.iris.core.TextNormalizer

/**
 * Selection text from other apps is messy: trailing periods, quotes the user
 * dragged in, possessives, and multi-word selections. Everything here is a case
 * that would otherwise produce a spurious "not found".
 */
class NormalizerTest {

    @Test
    fun `trailing period is stripped`() {
        val c = TextNormalizer.candidates("word.")
        assertTrue(c.contains("word"))
    }

    @Test
    fun `punctuation run is stripped`() {
        val c = TextNormalizer.candidates("word?!")
        assertTrue(c.contains("word"))
    }

    @Test
    fun `wrapping quotes are stripped`() {
        val c = TextNormalizer.candidates("\"word\"")
        assertTrue(c.contains("word"))
    }

    @Test
    fun `possessive s is stripped`() {
        val c = TextNormalizer.candidates("runner's")
        assertTrue(c.contains("runner"))
    }

    @Test
    fun `curly apostrophe possessive is stripped`() {
        val c = TextNormalizer.candidates("runner\u2019s")
        assertTrue(c.contains("runner"))
    }

    @Test
    fun `the raw selection is always tried first`() {
        assertEquals("word.", TextNormalizer.candidates("word.").first())
    }

    @Test
    fun `multi word selection offers the longest word`() {
        val c = TextNormalizer.candidates("the inevitable outcome")
        assertTrue("expected inevitable", c.contains("inevitable"))
    }

    @Test
    fun `blank selection produces nothing`() {
        assertTrue(TextNormalizer.candidates("   ").isEmpty())
    }

    @Test
    fun `a single plain word is not duplicated`() {
        val c = TextNormalizer.candidates("word")
        assertEquals(1, c.size)
    }

    @Test
    fun `empty parens wrappers are stripped`() {
        val c = TextNormalizer.candidates("(word)")
        assertTrue(c.contains("word"))
    }
}
