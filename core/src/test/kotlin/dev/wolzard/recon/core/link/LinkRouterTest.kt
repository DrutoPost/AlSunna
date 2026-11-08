package dev.wolzard.recon.core.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkRouterTest {
    @Test fun trailingPunctuationIsTrimmed() {
        assertEquals("https://youtu.be/abc?si=x", LinkRouter.extractUrl("check this https://youtu.be/abc?si=x)."))
        assertEquals("https://www.instagram.com/reel/DeJ_pr9CNuO/", LinkRouter.extractUrl("wow https://www.instagram.com/reel/DeJ_pr9CNuO/…"))
        assertEquals("https://x.com/a/status/1", LinkRouter.extractUrl("(see https://x.com/a/status/1)"))
    }

    @Test fun balancedParenthesesAreKept() {
        assertEquals("https://en.wikipedia.org/wiki/Mercury_(planet)", LinkRouter.extractUrl("https://en.wikipedia.org/wiki/Mercury_(planet)."))
    }

    @Test fun firstOfSeveralUrls() {
        assertEquals("https://a.com/1", LinkRouter.extractUrl("https://a.com/1 and http://b.com/2"))
    }

    @Test fun noUrlOrSchemelessGivesNull() {
        assertNull(LinkRouter.extractUrl("just some text"))
        assertNull(LinkRouter.extractUrl("youtube.com/watch?v=abc"))
        assertNull(LinkRouter.extractUrl("ftp://files.example.com/x"))
    }

    private val nonWeb = listOf(
        "file:///data/data/dev.wolzard.recon/databases/recon.db", "file://host/x", "intent://scan/#Intent;scheme=zxing;end",
        "javascript:alert(1)", "content://media/external/video/1", "ftp://example.com/a.mp4", "-o/etc/x", "//example.com/a",
    )

    @Test fun onlyAlsunnaNetIsAccepted() {
        assertTrue(LinkRouter.isAlsunnaUrl("https://alsunna.net/hadith/772"))
        assertTrue(LinkRouter.isAlsunnaUrl("http://alsunna.net/hadith/772"))
        assertFalse(LinkRouter.isAlsunnaUrl("https://www.alsunna.net/hadith/772"))
        assertFalse(LinkRouter.isAlsunnaUrl("https://sub.alsunna.net/hadith/772"))
        assertFalse(LinkRouter.isAlsunnaUrl("https://google.com/test"))
    }

    @Test fun validLinkOnlyAcceptsAlsunnaNet() {
        assertEquals("https://alsunna.net/hadith/772", LinkRouter.validLink("Check this: https://alsunna.net/hadith/772!"))
        assertEquals("http://alsunna.net/hadith/1", LinkRouter.validLink("http://alsunna.net/hadith/1"))
        assertNull(LinkRouter.validLink("https://www.alsunna.net/hadith/772"))
        assertNull(LinkRouter.validLink("https://youtube.com/watch?v=123"))
        assertNull(LinkRouter.validLink("no link here"))
    }

    @Test fun validLinkIsStableWhenRunTwice() {
        val first = LinkRouter.validLink("see https://alsunna.net/hadith/772).")!!
        assertEquals(first, LinkRouter.validLink(first))
    }
}
