package dev.wolzard.recon.feature.job

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.wolzard.recon.core.model.AnalysisResult
import dev.wolzard.recon.core.model.FailureReason
import dev.wolzard.recon.core.model.Job
import dev.wolzard.recon.core.model.JobResult
import dev.wolzard.recon.core.model.JobState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JobScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun runningShowsProgress() {
        val job = Job("j1", "https://alsunna.net/hadith/772", state = JobState.ANALYSING, createdAt = 0, updatedAt = 0)
        compose.setContent {
            JobScreen(job, null, now = 1000L) {}
        }
        compose.onNodeWithText("Translating hadith explanation...").assertExists()
        compose.onNodeWithText("Sending formatted prompt to Google Gemini").assertExists()
    }

    @Test fun doneShowsMarkdownResult() {
        val markdown = "# Hadith 772\n\n**ব্যাখ্যা:** এটি একটি সহীহ হাদীস।"
        val job = Job("j1", "https://alsunna.net/hadith/772", state = JobState.DONE, createdAt = 0, updatedAt = 0)
        val result = JobResult(
            analysis = AnalysisResult(emptyList(), markdown, 1.0),
            markdown = markdown,
        )
        compose.setContent {
            JobScreen(job, result, now = 1000L) {}
        }
        compose.onNodeWithText("Hadith Explanation & Bengali Translation").assertExists()
        compose.onNodeWithText("Hadith 772").assertExists()
    }

    @Test fun failedShowsReason() {
        val job = Job("j1", "https://alsunna.net/hadith/772", state = JobState.FAILED, failure = FailureReason.UNSUPPORTED_LINK, createdAt = 0, updatedAt = 0)
        compose.setContent {
            JobScreen(job, null, now = 1000L) {}
        }
        compose.onNodeWithText("Only alsunna.net links are supported.").assertExists()
    }
}
