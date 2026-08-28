package com.resonote.core.karaoke.service

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.resonote.core.karaoke.KaraokeExportController
import com.resonote.core.model.KaraokeProjectId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WorkManagerKaraokeExportController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : KaraokeExportController {
    override fun export(projectIds: Set<KaraokeProjectId>): Boolean {
        if (projectIds.isEmpty()) return false
        val requests = projectIds.map { id ->
            OneTimeWorkRequestBuilder<KaraokeExportWorker>()
                .setInputData(Data.Builder().putString(KaraokeExportWorker.PROJECT_ID, id.value).build())
                .addTag("karaoke-export:${id.value}")
                .build()
        }
        WorkManager.getInstance(context).enqueue(requests)
        return true
    }
}
