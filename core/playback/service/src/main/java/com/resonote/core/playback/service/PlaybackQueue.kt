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
        mutableItems = items.distinctBy(PlaybackItem::queueKey).toMutableList()
        val requestedKey = items[startIndex].queueKey
        currentIndex = mutableItems.indexOfFirst { it.queueKey == requestedKey }
    }

    fun selectOrInsert(item: PlaybackItem) {
        val existingIndex = mutableItems.indexOfFirst { it.queueKey == item.queueKey }
        if (existingIndex >= 0) {
            val existing = mutableItems[existingIndex]
            mutableItems[existingIndex] = if (item.resolvedSource != null) {
                item
            } else {
                existing.copy(metadata = item.metadata, origin = item.origin)
            }
            currentIndex = existingIndex
            return
        }

        val insertionIndex = if (currentIndex in mutableItems.indices) currentIndex + 1 else mutableItems.size
        mutableItems.add(insertionIndex, item)
        currentIndex = insertionIndex
    }

    fun append(items: List<PlaybackItem>) {
        val indexesByKey = mutableItems.withIndex().associate { it.value.queueKey to it.index }.toMutableMap()
        items.forEach { item ->
            val existingIndex = indexesByKey[item.queueKey]
            if (existingIndex == null) {
                indexesByKey[item.queueKey] = mutableItems.size
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

    fun removeAt(index: Int): QueueRemoval? {
        if (index !in mutableItems.indices) return null
        val removedCurrent = index == currentIndex
        mutableItems.removeAt(index)
        currentIndex = when {
            mutableItems.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            removedCurrent -> index.coerceAtMost(mutableItems.lastIndex)
            else -> currentIndex
        }
        return QueueRemoval(
            removedCurrent = removedCurrent,
            nextCurrentItem = currentItem,
        )
    }

    fun move(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in mutableItems.indices || toIndex !in mutableItems.indices) return false
        if (fromIndex == toIndex) return false
        val selectedKey = currentItem?.queueKey
        val moved = mutableItems.removeAt(fromIndex)
        mutableItems.add(toIndex, moved)
        currentIndex = selectedKey?.let { key -> mutableItems.indexOfFirst { it.queueKey == key } } ?: -1
        return true
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

internal data class QueueRemoval(
    val removedCurrent: Boolean,
    val nextCurrentItem: PlaybackItem?,
)
