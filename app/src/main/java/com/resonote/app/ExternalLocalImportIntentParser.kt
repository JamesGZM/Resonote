package com.resonote.app

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

internal object ExternalLocalImportIntentParser {
    fun parse(intent: Intent): List<String> {
        val candidates = when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data) + intent.clipDataUris()
            Intent.ACTION_SEND -> listOfNotNull(
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java),
            ) + intent.clipDataUris()
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty() +
                    intent.clipDataUris()
            else -> emptyList()
        }
        return candidates
            .asSequence()
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            .map(Uri::toString)
            .distinct()
            .toList()
    }

    private fun Intent.clipDataUris(): List<Uri> {
        val data = clipData ?: return emptyList()
        return buildList(data.itemCount) {
            repeat(data.itemCount) { index -> data.getItemAt(index).uri?.let(::add) }
        }
    }
}
