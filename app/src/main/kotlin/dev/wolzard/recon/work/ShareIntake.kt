package dev.wolzard.recon.work

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import dev.wolzard.recon.core.link.LinkRouter
import dev.wolzard.recon.core.port.JobStore
import dev.wolzard.recon.data.media.ContentUriFetcher
import dev.wolzard.recon.data.notice.Notice
import java.io.File
import java.util.UUID

/** What a share turned into: a job already running, or a valid link waiting for its optional context (D23). */
sealed interface ShareOutcome {
    data class Started(val jobId: String) : ShareOutcome
}

/**
 * Share intake. Accepts shared alsunna.net links directly, creates a job, and enqueues it.
 */
class ShareIntake(private val context: Context, private val store: JobStore) {

    suspend fun accept(intent: Intent, fileContext: Boolean = false): ShareOutcome? {
        if (intent.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return null
        val link = LinkRouter.validLink(text) ?: return null
        val id = store.create(link, null)
        JobQueue.enqueue(context, id)
        return ShareOutcome.Started(id)
    }
}

/**
 * T16: until the personal-use notice is accepted, a share is held (raw intent, unvalidated) and creates no job.
 * [accepted] releases it through the same [intake] as any fresh share, so it is validated then, not when held.
 * ponytail: held in memory (+ the activity's saved state); a process death before accept drops it.
 */
class ShareGate(private val notice: Notice, private val intake: suspend (Intent) -> ShareOutcome?) {
    var held: Intent? = null

    /** @return the outcome, or null if held or not a usable share. */
    suspend fun share(intent: Intent): ShareOutcome? {
        if (notice.needed) { held = intent; return null }
        return intake(intent)
    }

    suspend fun accepted(): ShareOutcome? {
        notice.accept()
        return held?.also { held = null }?.let { intake(it) }
    }
}
