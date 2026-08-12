package com.resonote.core.playback.service

import com.resonote.core.playback.PlaybackItem

internal class PlaybackQueue {
    private var mutableItems = mutableListOf<PlaybackItem>()

    var currentIndex: Int = -1
        private set

    val items: List<PlaybackItem>
        get() = mutableItems.toList()

    val currentItem: PlaybackItem?
        get() = mutableItems.getOrNull(currentIndex)

    fun replace(items: List<PlaybackItem>, startIndex: Int) {
        require(items.isNotEmpty()) { "Playback queue must not be empty" }
        require(startIndex in items.indices) { "startIndex must point to an item" }
        mutableItems = items.distinctBy { it.song.hash }.toMutableList()
        val requestedHash = items[startIndex].song.hash
        currentIndex = mutableItems.indexOfFirst { it.song.hash == requestedHash }
    }

    fun selectOrInsert(item: PlaybackItem) {
        val existingIndex = mutableItems.indexOfFirst { it.song.hash == item.song.hash }
        if (existingIndex >= 0) {
            val existing = mutableItems[existingIndex]
            mutableItems[existingIndex] = if (item.resolvedSource != null) item else existing.copy(song = item.song)
            currentIndex = existingIndex
            return
        }

        val insertionIndex = if (currentIndex in mutableItems.indices) currentIndex + 1 else mutableItems.size
        mutableItems.add(insertionIndex, item)
        currentIndex = insertionIndex
    }

    fun append(items: List<PlaybackItem>) {
        val indexesByHash = mutableItems.withIndex().associate { it.value.song.hash to it.index }.toMutableMap()
        items.forEach { item ->
            val existingIndex = indexesByHash[item.song.hash]
            if (existingIndex == null) {
                indexesByHash[item.song.hash] = mutableItems.size
                mutableItems += item
            } else if (item.resolvedSource != null) {
                mutableItems[existingIndex] = item
            }
        }
    }

    fun select(index: Int): PlaybackItem? {
        if (index !in mutableItems.indices) return null
        currentIndex = index
        return mutableItems[index]
    }

    fun selectRandom(randomIndex: (Int) -> Int): PlaybackItem? {
        if (mutableItems.isEmpty()) return null
        if (mutableItems.size == 1) return select(0)
        val candidateIndexes = mutableItems.indices.filter { it != currentIndex }
        return select(candidateIndexes[randomIndex(candidateIndexes.size)])
    }

    fun next(wrap: Boolean): PlaybackItem? {
        if (mutableItems.isEmpty()) return null
        val nextIndex = currentIndex + 1
        return when {
            nextIndex in mutableItems.indices -> select(nextIndex)
            wrap -> select(0)
            else -> null
        }
    }

    fun previous(wrap: Boolean): PlaybackItem? {
        if (mutableItems.isEmpty()) return null
        val previousIndex = currentIndex - 1
        return when {
            previousIndex in mutableItems.indices -> select(previousIndex)
            wrap -> select(mutableItems.lastIndex)
            else -> null
        }
    }

    fun clear() {
        mutableItems.clear()
        currentIndex = -1
    }
}
