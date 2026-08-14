package com.resonote.core.network.retrofit

import com.resonote.core.network.api.model.MusicSongDto
import com.resonote.core.network.model.NetworkSong

internal fun MusicSongDto.toNetworkSongOrNull(): NetworkSong? {
    val resolvedHash = hash ?: fileHash ?: deprecated?.hash ?: return null
    val resolvedFilename = filename ?: fileName ?: name.orEmpty()
    val (filenameArtist, filenameTitle) = splitArtistTitle(resolvedFilename)
    val resolvedTitle = originalAudioName ?: songname ?: originalSongName ?: songName ?: filenameTitle
    if (resolvedHash.isBlank() || resolvedTitle.isBlank()) return null
    val resolvedHighQualityHash = highQualityFileHash ?: highQualityHash
    val resolvedLosslessHash = losslessFileHash ?: sqhash ?: losslessHash
    val relatedGoodsCount = relatedGoods.size
    return NetworkSong(
        hash = resolvedHash,
        title = resolvedTitle,
        artist = (authorName ?: singerName ?: filenameArtist).takeIf(String::isNotBlank),
        coverUrl = transform?.unionCover ?: sizableCover ?: albumSizableCover ?: image ?: cover,
        albumId = albumId,
        albumAudioId = albumAudioId ?: audioId ?: mixsongid,
        durationMillis = normalizeDurationMillis(timeLength ?: duration ?: deprecated?.duration ?: timelength ?: timelen ?: searchDuration),
        highQualityHash = resolvedHighQualityHash,
        losslessHash = resolvedLosslessHash,
        vip = (privilege ?: searchPrivilege ?: deprecated?.payType ?: 0) >= 10,
        highQualityAvailable = relatedGoodsCount > 1 || !resolvedHighQualityHash.isNullOrBlank(),
        losslessAvailable = relatedGoodsCount > 2 || !resolvedLosslessHash.isNullOrBlank(),
        albumTitle = albuminfo?.name ?: albumName ?: albumname ?: remark,
        fileId = fileid,
        previewDurationMillis = transform?.hashOffset?.let { offset ->
            previewDurationMillis(offset.startMillis, offset.endMillis)
        },
    )
}

internal fun previewDurationMillis(startMillis: Long?, endMillis: Long?): Long? {
    val start = startMillis?.coerceAtLeast(0) ?: return null
    val end = endMillis ?: return null
    return (end - start).takeIf { it > 0 }
}

internal fun normalizeDurationMillis(value: Long?): Long =
    value?.takeIf { it > 0 }?.let { if (it < 10_000) it * 1_000 else it } ?: 0

private fun splitArtistTitle(filename: String): Pair<String, String> {
    val separator = filename.indexOf(" - ")
    return if (separator > 0) filename.substring(0, separator) to filename.substring(separator + 3) else "" to filename
}
