package com.resonote.core.karaoke

import com.resonote.core.model.KaraokeProjectId

interface KaraokeExportController {
    fun export(projectIds: Set<KaraokeProjectId>): Boolean
}
