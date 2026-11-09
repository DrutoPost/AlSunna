package dev.wolzard.recon.data.notice

import android.content.SharedPreferences

/** Raise when the notice text changes in a way users must re-accept. */
const val NOTICE_VERSION = 6 // 2: Gemini web sign-in replaced the API key (D22); 3: account-ban risk spelled out (D25); 4: link preview call (D26); 5: design rows (Recon UI 1b); 6: yt-dlp updates itself from GitHub after a failed download

/** T16: the accepted notice version, in prefs. [version] is a parameter only so tests can bump it. */
class Notice(private val prefs: SharedPreferences, private val version: Int = NOTICE_VERSION) {
    val needed: Boolean get() = prefs.getInt(ACCEPTED, 0) < version
    fun accept() { prefs.edit().putInt(ACCEPTED, version).apply() }

    companion object {
        private const val ACCEPTED = "acceptedVersion"
        const val PREFS = "notice"
        /** Bold lead and the rest of each notice row, in the design's order; the download/terms row is Recon's own (kept from v4). */
        val TEXT = listOf(
            "Hadith links are sent to Google Gemini." to "AlSunnah sends alsunna.net links to Google Gemini for translation and commentary.",
            "Authentic commentary and translation." to "Commentary from classical scholars is translated into Bengali with full references.",
            "Unofficial." to "AlSunnah uses Gemini web with your signed-in Google account.",
            "Use a secondary Google account." to "Automated requests may put the signed-in account at risk. Use an account you can afford to lose.",
            "Verify with classical texts." to "Always verify translations and sources against original Islamic scholarly texts.",
            "Dynamic multilingual typography." to "Bengali text is rendered with Bornomala font, Arabic text with Scheherazade font.",
            "Privacy on device." to "Session credentials stay securely encrypted on your phone. No tracking or telemetry.",
            "Outside the Play Store." to "Install updates only from official releases.",
        )
    }
}
