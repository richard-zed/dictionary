package rip.failed.iris

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import rip.failed.iris.core.Dictionary

/**
 * The suffix rules over-generate on purpose and rely on the dictionary to
 * reject bad candidates. These pin the over-generation itself: a rule that
 * stops producing "runn" would also stop finding "run" via the doubled-
 * consonant path, and the failure would look like a missing word rather than
 * a broken rule.
 */
class LemmaTest {

    @Test
    fun `doubled consonant is undone for ing`() {
        val c = Dictionary.lemmaCandidates("running")
        assertTrue("expected runn", c.contains("runn"))
        assertTrue("expected run", c.contains("run"))
    }

    @Test
    fun `doubled consonant is undone for ed`() {
        val c = Dictionary.lemmaCandidates("stopped")
        assertTrue(c.contains("stopp"))
        assertTrue(c.contains("stop"))
    }

    @Test
    fun `e restoration is offered`() {
        val c = Dictionary.lemmaCandidates("making")
        assertTrue("expected make", c.contains("make"))
    }

    @Test
    fun `ies becomes y`() {
        val c = Dictionary.lemmaCandidates("puppies")
        assertTrue("expected puppy", c.contains("puppy"))
    }

    @Test
    fun `plural s is stripped`() {
        assertTrue(Dictionary.lemmaCandidates("dogs").contains("dog"))
    }

    @Test
    fun `es stripping is offered before s`() {
        val c = Dictionary.lemmaCandidates("boxes")
        assertTrue(c.contains("box"))
    }

    @Test
    fun `short words produce no candidates`() {
        assertTrue(Dictionary.lemmaCandidates("at").isEmpty())
    }

    @Test
    fun `the form itself is never echoed as a candidate`() {
        assertTrue(Dictionary.lemmaCandidates("running").none { it == "running" })
    }

    @Test
    fun `adverb ly is stripped`() {
        assertTrue(Dictionary.lemmaCandidates("quickly").contains("quick"))
    }

    @Test
    fun `candidate order puts the bare stem first`() {
        // Not an aesthetic point: resolve() returns the FIRST candidate that
        // exists, so this order decides which entry wins when several are
        // valid. "mak" is tried before "make" and only loses because it is not
        // a headword -- which is exactly the verify step doing its job.
        val c = Dictionary.lemmaCandidates("making")
        assertEquals("mak", c.first())
    }
}
