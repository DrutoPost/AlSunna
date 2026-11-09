package dev.wolzard.recon.feature.job

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.wolzard.recon.core.link.LinkRouter
import dev.wolzard.recon.core.link.displayHost
import dev.wolzard.recon.core.model.Answer
import dev.wolzard.recon.core.model.AnswerStatus
import dev.wolzard.recon.core.model.EntityType
import dev.wolzard.recon.core.model.Evidence
import dev.wolzard.recon.core.model.FailureReason
import dev.wolzard.recon.core.model.Finding
import dev.wolzard.recon.core.model.GhRepo
import dev.wolzard.recon.core.model.Job
import dev.wolzard.recon.core.model.JobResult
import dev.wolzard.recon.core.model.JobState
import dev.wolzard.recon.core.model.LinkPreview
import dev.wolzard.recon.core.model.MediaType
import dev.wolzard.recon.designsystem.ChipKind
import dev.wolzard.recon.designsystem.Mono
import dev.wolzard.recon.designsystem.StatusChip
import dev.wolzard.recon.designsystem.StatusIcons
import dev.wolzard.recon.designsystem.Symbols
import dev.wolzard.recon.designsystem.Thumb
import dev.wolzard.recon.designsystem.cornerTicks
import dev.wolzard.recon.designsystem.dotGrid
import dev.wolzard.recon.designsystem.status

/** Stateless: everything comes from [job]/[result]; [now] drives the step clock and whether Retry is enabled yet. */
import dev.wolzard.recon.designsystem.MarkdownView

@Composable
fun JobScreen(
    job: Job,
    result: JobResult?,
    now: Long,
    onIntent: (JobIntent) -> Unit,
) {
    val done = job.state == JobState.DONE && result != null
    Column(Modifier.fillMaxSize()) {
        TopBar(job, done, onIntent)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (job.state == JobState.FAILED) {
                Failed(job)
            } else if (result != null && job.state == JobState.DONE) {
                Done(job, result, onIntent)
            } else if (job.state == JobState.WAITING_QUOTA) {
                Waiting(job, now)
            } else {
                Running(job, now)
            }
        }
        when {
            job.state == JobState.FAILED -> FailedActions(job, now, onIntent)
            done -> Unit
            job.state == JobState.WAITING_QUOTA -> BottomBar {
                OutlinedButton(onClick = { onIntent(JobIntent.Cancel) }, Modifier.weight(1f).height(48.dp), shape = MaterialTheme.shapes.small) { Text("Cancel") }
                Button(onClick = { onIntent(JobIntent.SignIn) }, Modifier.weight(1f).height(48.dp), shape = MaterialTheme.shapes.small) { Text("Switch account") }
            }
            else -> BottomBar {
                OutlinedButton(onClick = { onIntent(JobIntent.Cancel) }, Modifier.weight(1f).height(48.dp), shape = MaterialTheme.shapes.small) { Text("Cancel") }
            }
        }
    }
}

/** The link as the user shared it, https only (anything else is never opened). */
private fun Job.originalLink(): String? = (url ?: LinkRouter.extractUrl(sharedText))?.takeIf { localUri == null && it.startsWith("https://") }

/** "instagram.com/reel/C9xQ2…": the link without scheme or www, for the mono TARGET line. */
private fun shortLink(link: String): String = link.substringAfter("://").removePrefix("www.").trimEnd('/')

/** "x.com/…/status/18402…" (design 1k): host and the last two path parts, for the narrow key/value card. */
private fun compactLink(link: String): String {
    val parts = shortLink(link).substringBefore('?').split('/')
    return if (parts.size <= 3) parts.joinToString("/") else "${parts.first()}/…/" + parts.takeLast(2).joinToString("/")
}

@Composable
private fun TopBar(job: Job, done: Boolean, onIntent: (JobIntent) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onIntent(JobIntent.Back) }) { Icon(Symbols.ArrowBack, "Back") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(job.displayHost(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp))
            Text(
                JobCopy.shortId(job.id) + if (done) " · TRANSLATION COMPLETE" else "",
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
            )
        }
        if (done) {
            job.originalLink()?.let { link ->
                IconButton(onClick = { onIntent(JobIntent.Open(link)) }) { Icon(Symbols.OpenInNew, "Open original") }
            }
            var menu by rememberSaveable { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Symbols.MoreVert, "More") }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onIntent(JobIntent.Cancel) })
                }
            }
        }
    }
}

@Composable
private fun BottomBar(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** A thumbnail inside the reticle corners (design TARGET block and result header). */
@Composable
private fun FramedThumb(url: String?, width: Dp, height: Dp) {
    Box(Modifier.cornerTicks(MaterialTheme.colorScheme.onSurface, 8.dp).padding(5.dp)) { Thumb(url, width, height, fallback = null) } // a video: the striped placeholder
}

// --- running (1f, 3c)

@Composable
private fun Target(job: Job) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FramedThumb(job.thumbnailUrl, 48.dp, 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("TARGET")
            Text(job.originalLink()?.let(::shortLink) ?: job.displayHost(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono))
            Text("Link received ${JobCopy.time(job.createdAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class Step(val title: String, val detail: String?, val doneAt: Long?)

@Composable
private fun Running(job: Job, now: Long) {
    Target(job)
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, line, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Translating hadith explanation...", style = MaterialTheme.typography.titleSmall)
                Text("Sending formatted prompt to Google Gemini", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text(JobCopy.LEAVE_SCREEN, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

// --- waiting for the usage limit (1g)

@Composable
private fun Waiting(job: Job, now: Long) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    StatusChip("Waiting · usage limit", ChipKind.Warn, icon = StatusIcons.Waiting)
    Text("Usage limit reached", style = MaterialTheme.typography.titleLarge)
    // A time is shown only when the backend gave one (retryAtEpochMs); never a guess.
    val resetAt = job.retryAtEpochMs
    Text(
        if (resetAt != null) "Your Google account has hit its Gemini limit. This job is saved and resumes by itself after the reset."
        else "Your Google account has hit its Gemini limit. ${JobCopy.TRY_LATER}: this job is saved and resumes by itself when the limit lifts.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionLabel("EXPECTED RESET")
        if (resetAt != null) {
            Text("~${JobCopy.time(resetAt)}", style = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp))
            Text("${JobCopy.waitFor(resetAt - now)} · estimate", color = muted, style = MaterialTheme.typography.bodySmall)
        } else Text(JobCopy.TRY_LATER, style = MaterialTheme.typography.bodyLarge)
    }
    Spacer(Modifier.height(4.dp))
    SectionLabel("WHAT YOU CAN DO")
    Column {
        listOf(
            Symbols.Schedule to "Leave it queued. You get a notification when it finishes.",
            Symbols.SwitchAccount to "Switch to another Google account and resume now.",
            Symbols.Close to "Cancel the job and share the link again later.",
        ).forEachIndexed { i, (icon, text) ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = muted)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- failed (1k)

@Composable
private fun Failed(job: Job) {
    val reason = job.failure ?: FailureReason.UNKNOWN
    StatusChip("Failed", ChipKind.Bad, icon = StatusIcons.Error)
    val msg = if (reason == FailureReason.UNSUPPORTED_LINK) "Only alsunna.net links are supported." else JobCopy.failureMessage(reason, job.retryAtEpochMs, job.failureDetail)
    Text(msg, style = MaterialTheme.typography.titleLarge)
}

/** The failure table's actions (plan → State machine) in the design's bottom bar: one filled action, then text actions. */
@Composable
private fun FailedActions(job: Job, now: Long, onIntent: (JobIntent) -> Unit) {
    val reason = job.failure ?: FailureReason.UNKNOWN
    var help by rememberSaveable { mutableStateOf(false) }
    val link = job.originalLink()
    val sharedFile = job.localUri != null
    val full = Modifier.fillMaxWidth().height(48.dp)
    val plain = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface) // design 1k: text actions in body ink
    val retry: @Composable () -> Unit = {
        Button(onClick = { onIntent(JobIntent.Retry) }, full, enabled = job.retryAtEpochMs?.let { now >= it } ?: true, shape = MaterialTheme.shapes.small) {
            Icon(Symbols.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Retry")
        }
    }
    if (reason == FailureReason.UNSUPPORTED_LINK) return
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (reason) {
            FailureReason.API_KEY_MISSING, FailureReason.API_KEY_INVALID -> Button(onClick = { onIntent(JobIntent.SignIn) }, full, shape = MaterialTheme.shapes.small) {
                    Text(if (reason == FailureReason.API_KEY_INVALID) "Sign in again" else "Sign in with Google")
            }
                else -> retry()
        }
        link?.let { TextButton(onClick = { onIntent(JobIntent.Open(it)) }, full, colors = plain) { Text("Open link in browser") } }
    }
}

// --- result (1h, 1i, 1j, 3d–3g)

/** Design 1h: the video's thumbnail, title, length, frames and time head the result. */
@Composable
private fun VideoHeader(job: Job) {
    Row(Modifier.fillMaxWidth().testTag("video_header"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        FramedThumb(job.thumbnailUrl, 40.dp, 54.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(job.videoTitle ?: job.displayHost(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 23.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(LinkPreview.formatDuration(job.videoDurationSec), job.frameCount?.takeIf { it > 0 }?.let { "$it frames" }, JobCopy.time(job.analysedAt ?: job.updatedAt)).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Design 3c: a hairline left rule while the job runs; 3d: a larger quote heading the result. */
@Composable
private fun YourQuestion(text: String, large: Boolean) {
    val quoted = "“$text”"
    if (large) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("YOUR QUESTION")
        Text(quoted, Modifier.testTag("your_question"), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 27.sp))
    } else {
        val rule = MaterialTheme.colorScheme.outlineVariant
        Column(
            Modifier.fillMaxWidth().drawBehind { drawLine(rule, Offset(0f, 0f), Offset(0f, size.height), 1.5.dp.toPx()) }.padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel("YOUR QUESTION")
            Text(quoted, Modifier.testTag("your_question"), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = OutlinedCard(
    modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content = content,
)

/** Bold name, then the muted qualifier on the same line (design 3e: "Paper Satellites film, 2019"). */
@Composable
private fun NameLine(name: String, qualifier: String?, modifier: Modifier = Modifier) = Text(
    if (qualifier.isNullOrBlank()) AnnotatedString(name) else buildAnnotatedString {
        append(name); append(" ")
        withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(qualifier) }
    },
    modifier, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp),
)

@Composable
private fun NoSource() = Row(Modifier.heightIn(min = 24.dp).testTag("no_source"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Icon(StatusIcons.LinkOff, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(JobCopy.NO_SOURCE, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

/** Design 3d/3f/3g. Answer sources are plain text (D23), numbered after the job's own sources in one shared list. */
@Composable
private fun AnswerCard(answer: Answer?, firstSourceNumber: Int) {
    val status = answer?.status ?: AnswerStatus.UNCLEAR
    val kind = when (status) {
        AnswerStatus.VERIFIED -> ChipKind.Ok
        AnswerStatus.UNVERIFIED -> ChipKind.Warn
        AnswerStatus.UNCLEAR -> ChipKind.Unclear
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outlineVariant
    CardBox(Modifier.testTag("answer_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                answer?.kind?.let { Icon(icon(it, answer.subtype), contentDescription = null, Modifier.size(16.dp), tint = muted) }
                SectionLabel(JobCopy.answerLabel(answer), Modifier.weight(1f).testTag("answer_label"))
                StatusChip(JobCopy.answerStatus(status), kind)
            }
            // Design 3d: with a title, the title line leads and the reply reads as its muted description.
            val title = answer?.title
            if (title != null) NameLine(title, null, Modifier.testTag("answer_title"))
            Text(answer?.text ?: JobCopy.NO_ANSWER, color = if (title != null) muted else Color.Unspecified, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp))
            val sources = answer?.sources.orEmpty()
            if (sources.isNotEmpty()) Column(Modifier.fillMaxWidth()) {
                sources.forEachIndexed { k, src ->
                    HorizontalDivider(color = line)
                    SourceLine(firstSourceNumber + k, src, minHeight = 46.dp)
                }
            } else if (status != AnswerStatus.UNCLEAR) NoSource()
        }
    }
}

/** One numbered source row; [onOpen] null = plain text, never opened as a link. */
@Composable
private fun SourceLine(number: Int, text: String, minHeight: Dp = 54.dp, onOpen: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = minHeight).then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("$number", Modifier.width(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = SourceText)
        Text(text, Modifier.weight(1f), style = SourceText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onOpen != null) Icon(StatusIcons.OpenInNew, contentDescription = "Opens in browser", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val SourceText = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 18.sp)

@Composable
private fun Done(job: Job, result: JobResult, onIntent: (JobIntent) -> Unit) {
    val content = result.markdown ?: result.analysis.summary
    CardBox {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Symbols.MenuBook, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Hadith Explanation & Bengali Translation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MarkdownView(text = content, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OnScreenText(lines: List<String>) {
    if (lines.isEmpty()) return
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) "Hide on-screen text" else "Show on-screen text") }
    if (open) lines.forEach { Text("“$it”", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
}

/** Design 1i EntityCard: type icon and label, status chip, title, why, then the source link. */
@Composable
private fun FindingCard(index: Int, f: Finding, onIntent: (JobIntent) -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val (label, kind) = JobCopy.verdictChip(f)
    CardBox {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val typeLabel = JobCopy.typeLabel(f.entity.type, f.entity.subtype)
                Icon(icon(f.entity.type, f.entity.subtype), contentDescription = typeLabel, Modifier.size(16.dp), tint = muted)
                Text(typeLabel, Modifier.weight(1f), color = muted, style = MaterialTheme.typography.labelSmall)
                StatusChip(label, kind)
            }
            // A repo reads by its name (the owner is in the link); other types carry their qualifier beside the name.
            val repo = f.entity.type == EntityType.REPO
            NameLine(if (repo) f.title.substringAfterLast('/') else f.title, f.entity.qualifier.takeIf { !repo && f.entity.type != EntityType.PERSON })
            f.whyInVideo.ifBlank { f.whatItIs }.takeIf { it.isNotBlank() }?.let { Text(it, color = muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp)) }
            if (f.candidates.isNotEmpty()) Candidates(index, f.candidates, onIntent)
            val link = f.primaryLink?.takeIf { it.startsWith("https://") }
            if (link != null) Row(
                Modifier.heightIn(min = 40.dp).clickable(role = Role.Button) { onIntent(JobIntent.Open(link)) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(StatusIcons.OpenInNew, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Text(shortLink(link), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, textDecoration = TextDecoration.Underline), maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (kind == ChipKind.Warn) NoSource()
        }
    }
}

/** Design 1i ClaimRow: chip, the claim as a quote, then "[n] source" (n = its place in the source list, when listed). */
@Composable
private fun ClaimRow(f: Finding, result: JobResult) {
    val (label, kind) = JobCopy.verdictChip(f)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip(label, kind)
        Text("“${f.title}”", style = MaterialTheme.typography.bodyLarge)
        val n = f.source?.let { s -> result.sources.indexOfFirst { it.title.equals(s, ignoreCase = true) }.takeIf { it >= 0 }?.plus(1) }
        val detail = if (n != null) "[$n] " + f.whatItIs.ifBlank { f.source!! } else listOfNotNull(f.source, f.whatItIs.takeIf { it.isNotBlank() }).joinToString(": ")
        if (detail.isNotBlank()) Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        f.entity.domain?.takeIf { it.sensitive }?.let { d ->
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).testTag("advice_line").background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Symbols.MedicalInformation, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(JobCopy.adviceLine(d), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Design 1i CandidatePicker: the whole row picks; "None of these" stays as a quiet last row. */
@Composable
private fun Candidates(index: Int, repos: List<GhRepo>, onIntent: (JobIntent) -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, line, MaterialTheme.shapes.small).clip(MaterialTheme.shapes.small)) {
        Text("${repos.size} CANDIDATES · PICK THE RIGHT ONE", Modifier.padding(horizontal = 12.dp, vertical = 10.dp), color = muted, style = MaterialTheme.typography.labelSmall)
        repos.forEach { r ->
            HorizontalDivider(color = line)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button) { onIntent(JobIntent.Pick(index, r)) }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioButton(selected = false, onClick = null, colors = RadioButtonDefaults.colors(unselectedColor = MaterialTheme.colorScheme.onSurface))
                Column {
                    Text(r.fullName, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp))
                    Text(listOfNotNull("★ ${JobCopy.stars(r.stars)}", r.description).joinToString(" · "), color = muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        HorizontalDivider(color = line)
        Text(
            "None of these", Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { onIntent(JobIntent.Pick(index, null)) }.padding(horizontal = 12.dp, vertical = 12.dp),
            color = muted, style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)

private fun icon(t: EntityType, subtype: MediaType? = null): ImageVector = when (t) {
    EntityType.REPO -> Symbols.Code
    EntityType.APP -> Symbols.Apps
    EntityType.PRODUCT -> Symbols.Sell
    EntityType.BOOK -> Symbols.MenuBook
    EntityType.PERSON -> Symbols.Person
    EntityType.PLACE -> Symbols.LocationOn
    EntityType.RECIPE -> Symbols.Restaurant
    EntityType.MEDIA -> when (subtype) {
        MediaType.MOVIE -> StatusIcons.Movie
        MediaType.SERIES -> StatusIcons.Series
        MediaType.SONG -> StatusIcons.Song
        MediaType.GAME -> StatusIcons.Game
        MediaType.ANIME -> StatusIcons.Anime
        MediaType.OTHER, null -> Symbols.Movie
    }
    EntityType.CLAIM -> Symbols.Campaign
    EntityType.OTHER -> Symbols.Category
}
