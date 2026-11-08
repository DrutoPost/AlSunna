package dev.wolzard.recon.core.job

import dev.wolzard.recon.core.link.LinkRouter
import dev.wolzard.recon.core.model.AnalysisResult
import dev.wolzard.recon.core.model.AnalyzerException
import dev.wolzard.recon.core.model.FailureReason
import dev.wolzard.recon.core.model.Job
import dev.wolzard.recon.core.model.JobResult
import dev.wolzard.recon.core.model.JobState
import dev.wolzard.recon.core.port.Analysis
import dev.wolzard.recon.core.port.FetchQuality
import dev.wolzard.recon.core.port.FetchedMedia
import dev.wolzard.recon.core.port.FrameSampler
import dev.wolzard.recon.core.port.JobStore
import dev.wolzard.recon.core.port.LinkResolver
import dev.wolzard.recon.core.port.MediaFetcher
import dev.wolzard.recon.core.port.PromptGenerator
import dev.wolzard.recon.core.port.VideoAnalyzer
import dev.wolzard.recon.core.prompt.Prompts
import dev.wolzard.recon.core.research.ResearchChain
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Fetchers(val link: MediaFetcher, val sharedFile: MediaFetcher)
data class Language(val display: String, val tag: String)
data class Timeouts(
    val pathA: Duration = 150.seconds,
    val pathB: Duration = 120.seconds,
    val maxPathABytes: Long = 50L * 1024 * 1024,
)

enum class UserAction {
    RUN,             // first run, Retry, or resume after process death
    RESEARCH_ONLY,   // "Check again"
    TRY_SHARPER,
}

@Serializable
data class SavedAnalysis(val result: AnalysisResult, val model: String, val path: String)

/**
 * RunJob executes the hadith translation request:
 * Takes the alsunna.net URL, formats the prompt, and queries Google Gemini.
 */
class RunJob(
    private val store: JobStore,
    private val resolver: LinkResolver,
    private val fetchers: Fetchers,
    private val sampler: FrameSampler,
    private val analyzer: VideoAnalyzer,
    private val research: ResearchChain,
    private val language: () -> Language,
    private val quotaWaitUntil: () -> Long?,
    private val workRoot: File,
    private val sharedRoot: File = File(workRoot, "shared"),
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeouts: Timeouts = Timeouts(),
    private val webClient: PromptGenerator? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(jobId: String, action: UserAction = UserAction.RUN) {
        val job = store.get(jobId) ?: return
        try {
            quotaWaitUntil()?.let { retryAt ->
                store.update(jobId) { it.copy(state = JobState.WAITING_QUOTA, retryAtEpochMs = retryAt, progress = null) }
                return
            }

            val url = LinkRouter.validLink(job.url ?: job.sharedText)
            if (url == null) {
                fail(jobId, FailureReason.UNSUPPORTED_LINK, "Only alsunna.net links are supported", null)
                return
            }

            store.update(jobId) { it.copy(state = JobState.ANALYSING, startedAt = clock(), progress = null) }
            val prompt = Prompts.hadithTranslation(url)
            val reply = if (webClient != null) {
                webClient.generate(prompt)
            } else {
                // Fallback for tests or interfaces using analyzer
                analyzer.analyze(dev.wolzard.recon.core.model.AnalysisInput.YouTubeUrl(url), "bn", null).result.summary
            }

            val jobResult = JobResult(
                analysis = AnalysisResult(entities = emptyList(), summary = reply, confidence = 1.0, onScreenText = emptyList(), answer = null),
                markdown = reply,
                path = "gemini-web",
                models = mapOf("analyse" to "gemini-web"),
            )

            store.update(jobId) {
                it.copy(
                    state = JobState.DONE,
                    resultJson = json.encodeToString(JobResult.serializer(), jobResult),
                    analysedAt = clock(),
                    failure = null,
                    failureDetail = null,
                    retryAtEpochMs = null,
                    progress = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AnalyzerException) {
            fail(jobId, e.reason, e.message, e.retryAtEpochMs)
        } catch (e: Exception) {
            fail(jobId, FailureReason.UNKNOWN, "${e::class.simpleName}: ${e.message}", null)
        }
    }

    private suspend fun fail(id: String, reason: FailureReason, detail: String?, retryAt: Long?) {
        store.update(id) { it.copy(state = JobState.FAILED, failure = reason, failureDetail = detail, retryAtEpochMs = retryAt, progress = null) }
    }
}
