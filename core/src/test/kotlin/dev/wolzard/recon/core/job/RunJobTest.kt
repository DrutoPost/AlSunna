package dev.wolzard.recon.core.job

import dev.wolzard.recon.core.model.AnalysisInput
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
import dev.wolzard.recon.core.research.ResearchChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RunJobTest {
    @get:Rule val tmp = TemporaryFolder()

    private class Store : JobStore {
        val jobs = MutableStateFlow<Map<String, Job>>(emptyMap())
        val states = mutableListOf<JobState>()
        fun put(job: Job) { jobs.value = jobs.value + (job.id to job); states += job.state }
        override fun observeAll() = jobs.map { it.values.toList() }
        override fun observe(id: String) = jobs.map { it[id] }
        override suspend fun get(id: String) = jobs.value[id]
        override suspend fun create(sharedText: String, localUri: String?, userContext: String?, normalizedUrl: String?) = error("unused")
        override suspend fun findByNormalizedUrl(normalizedUrl: String) = emptyList<Job>()
        override suspend fun update(id: String, transform: (Job) -> Job) {
            val old = jobs.value.getValue(id)
            val new = transform(old)
            jobs.value = jobs.value + (id to new)
            if (new.state != old.state) states += new.state
        }
        override suspend fun delete(id: String) { jobs.value = jobs.value - id }
        override suspend fun findRunning() = emptyList<Job>()
    }

    private class FakePromptGen(private val reply: String = "অনুবাদ ও ব্যাখ্যা") : PromptGenerator {
        var lastPrompt: String? = null
        override suspend fun generate(prompt: String): String {
            lastPrompt = prompt
            return reply
        }
    }

    private class DummyAnalyzer : VideoAnalyzer {
        override val supportsYouTubeUrl = false
        override suspend fun analyze(input: AnalysisInput, language: String, userContext: String?): Analysis =
            Analysis(AnalysisResult(emptyList(), "dummy", 1.0), "dummy")
    }

    private class DummyFetcher : MediaFetcher {
        override suspend fun fetch(source: String, destDir: File, quality: FetchQuality, avoidFormatId: String?, onProgress: (Float) -> Unit): FetchedMedia =
            error("unused")
    }

    private class DummySampler : FrameSampler {
        override suspend fun sample(video: File, outDir: File): AnalysisInput.FramesAndAudio = error("unused")
    }

    private val store = Store()

    private fun runJob(promptGen: PromptGenerator? = FakePromptGen(), quotaWaitUntil: () -> Long? = { null }) = RunJob(
        store = store,
        resolver = object : LinkResolver { override suspend fun resolve(url: String) = url },
        fetchers = Fetchers(DummyFetcher(), DummyFetcher()),
        sampler = DummySampler(),
        analyzer = DummyAnalyzer(),
        research = ResearchChain(object : dev.wolzard.recon.core.port.Researcher {
            override val id = "dummy"
            override fun accepts(type: dev.wolzard.recon.core.model.EntityType) = false
            override suspend fun research(entities: List<dev.wolzard.recon.core.model.Entity>, ctx: dev.wolzard.recon.core.model.ResearchContext) = dev.wolzard.recon.core.port.ResearchOutput(emptyList())
        }, object : dev.wolzard.recon.core.port.Researcher {
            override val id = "dummy2"
            override fun accepts(type: dev.wolzard.recon.core.model.EntityType) = false
            override suspend fun research(entities: List<dev.wolzard.recon.core.model.Entity>, ctx: dev.wolzard.recon.core.model.ResearchContext) = dev.wolzard.recon.core.port.ResearchOutput(emptyList())
        }, object : dev.wolzard.recon.core.port.AiResearcher {
            override val id = "ai"
            override suspend fun research(items: List<dev.wolzard.recon.core.model.ResearchItem>, ctx: dev.wolzard.recon.core.model.ResearchContext) = dev.wolzard.recon.core.port.ResearchOutput(emptyList())
        }, null) { dev.wolzard.recon.core.port.GroundingCapability.UNAVAILABLE },
        language = { Language("Bengali", "bn") },
        quotaWaitUntil = quotaWaitUntil,
        workRoot = tmp.root,
        clock = { 1_000L },
        webClient = promptGen,
    )

    private fun job(text: String = "https://alsunna.net/hadith/772") =
        Job("j1", text, createdAt = 0, updatedAt = 0).also { store.put(it) }

    @Test fun alsunnaUrlSendsFormattedPromptAndCompletesWithMarkdown() = runTest {
        job("https://alsunna.net/hadith/772")
        val fakeGen = FakePromptGen("হাদীসের পূর্ণাঙ্গ বাংলা অনুবাদ")
        runJob(promptGen = fakeGen)("j1")

        assertEquals(listOf(JobState.QUEUED, JobState.ANALYSING, JobState.DONE), store.states)
        assertNotNull(fakeGen.lastPrompt)
        assertTrue(fakeGen.lastPrompt!!.contains("https://alsunna.net/hadith/772"))
        assertTrue(fakeGen.lastPrompt!!.contains("إخفاء شرح الحديث"))

        val job = store.get("j1")!!
        val result = Json.decodeFromString(JobResult.serializer(), job.resultJson!!)
        assertEquals("হাদীসের পূর্ণাঙ্গ বাংলা অনুবাদ", result.markdown)
        assertEquals(JobState.DONE, job.state)
    }

    @Test fun nonAlsunnaUrlFailsWithUnsupportedLink() = runTest {
        job("https://youtube.com/watch?v=123")
        runJob()("j1")
        val job = store.get("j1")!!
        assertEquals(JobState.FAILED, job.state)
        assertEquals(FailureReason.UNSUPPORTED_LINK, job.failure)
    }

    @Test fun quotaWaitEntersWaitingQuotaState() = runTest {
        job()
        runJob(quotaWaitUntil = { 5000L })("j1")
        val job = store.get("j1")!!
        assertEquals(JobState.WAITING_QUOTA, job.state)
        assertEquals(5000L, job.retryAtEpochMs)
    }
}
