package dev.wolzard.recon.core.prompt

import org.junit.Assert.assertTrue
import org.junit.Test

class HadithTranslationPromptTest {

    @Test
    fun hadithPromptFormatsStrictInstructionsAndQuotes() {
        val url = "https://alsunna.net/hadith/772"
        val prompt = Prompts.hadithTranslation(url)

        assertTrue(prompt.contains(url))
        assertTrue(prompt.contains("إخفاء شرح الحديث"))
        assertTrue(prompt.contains("কোনো ভূমিকা, প্রারম্ভিক ভূমিকা, উপসংহার বা ভবিষ্যৎ কোনো প্রশ্ন/জিজ্ঞাসা করবে না"))
        assertTrue(prompt.contains("Markdown"))
        assertTrue(prompt.contains("ব্লককোট (> ...)"))
        assertTrue(prompt.contains("তথ্যসূত্র ও উৎস (reference)"))
    }
}
