package com.resonote.core.playback.service

import com.resonote.core.playback.PlaybackItem

internal class PlaybackQueue {
    private var mutableItems = mutableListOf<PlaybackItem>()
    private val manuallyQueuedNextKeys = mutableListOf<String>()

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
        manuallyQueuedNextKeys.clear()
    }

    fun selectOrInsert(item: PlaybackItem) {
        val previousKey = currentItem?.queueKey
        val existingIndex = mutableItems.indexOfFirst { it.queueKey == item.queueKey }
        if (existingIndex >= 0) {
            mutableItems[existingIndex] = mutableItems[existingIndex].merge(item)
            currentIndex = existingIndex
            if (previousKey != item.queueKey) manuallyQueuedNextKeys.clear()
            return
        }

        val insertionIndex = if (currentIndex in mutableItems.indices) currentIndex + 1 else mutableItems.size
        mutableItems.add(insertionIndex, item)
        currentIndex = insertionIndex
        manuallyQueuedNextKeys.clear()
    }

    fun playNext(items: List<PlaybackItem>) {
        if (items.isEmpty()) return
        val selectedKey = currentItem?.queueKey
        if (selectedKey == null) {
            append(items)
            return
        }

        items.distinctBy(PlaybackItem::queueKey).forEach { item ->
            if (item.queueKey == selectedKey) {
                mutableItems[currentIndex] = mutableItems[currentIndex].merge(item)
                return@forEach
            }

            val queuedIndex = mutableItems.indexOfFirst { it.queueKey == item.queueKey }
            if (item.queueKey in manuallyQueuedNextKeys && queuedIndex >= 0) {
                mutableItems[queuedIndex] = mutableItems[queuedIndex].merge(item)
                return@forEach
            }
            manuallyQueuedNextKeys.remove(item.queueKey)

            if (queuedIndex >= 0) {
                mutableItems.removeAt(queuedIndex)
                currentIndex = mutableItems.indexOfFirst { it.queueKey == selectedKey }
            }
            val insertionIndex = currentIndex + 1 + manuallyQueuedNextKeys.size
            mutableItems.add(insertionIndex, item)
            manuallyQueuedNextKeys += item.queueKey
        }
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
        if (index != currentIndex) manuallyQueuedNextKeys.clear()
        currentIndex = index
        return mutableItems[index]
    }

    fun removeAt(index: Int): QueueRemoval? {
        if (index !in mutableItems.indices) return null
        val removedCurrent = index == currentIndex
        val removedKey = mutableItems[index].queueKey
        mutableItems.removeAt(index)
        currentIndex = when {
            mutableItems.isEmpty() -> -1
            index < currentIndex -> currentIndex - 1
            removedCurrent -> index.coerceAtMost(mutableItems.lastIndex)
            else -> currentIndex
        }
        if (removedCurrent) {
            manuallyQueuedNextKeys.clear()
        } else {
            manuallyQueuedNextKeys.remove(removedKey)
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
        manuallyQueuedNextKeys.clear()
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
        manuallyQueuedNextKeys.clear()
    }
}

private fun PlaybackItem.merge(update: PlaybackItem): PlaybackItem = if (update.resolvedSource != null) {
    update
} else {
    copy(metadata = update.metadata, origin = update.origin)
}

internal data class QueueRemoval(val removedCurrent: Boolean, val nextCurrentItem: PlaybackItem?)
