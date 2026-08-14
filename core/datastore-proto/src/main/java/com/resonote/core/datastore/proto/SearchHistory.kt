package com.resonote.core.datastore.proto

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.InputStream
import java.io.OutputStream

/** Lite representation of search_history.proto using the standard protobuf wire format. */
data class SearchHistory(val queriesList: List<String> = emptyList()) {
    fun writeTo(output: OutputStream) {
        CodedOutputStream.newInstance(output).apply {
            queriesList.forEach { writeString(1, it) }
            flush()
        }
    }

    fun toBuilder(): Builder = Builder(queriesList.toMutableList())

    class Builder internal constructor(private val queries: MutableList<String>) {
        fun clearQueries() = apply { queries.clear() }

        fun addAllQueries(values: Iterable<String>) = apply { queries.addAll(values) }

        fun build(): SearchHistory = SearchHistory(queries.toList())
    }

    companion object {
        fun getDefaultInstance(): SearchHistory = SearchHistory()

        fun parseFrom(input: InputStream): SearchHistory {
            val coded = CodedInputStream.newInstance(input)
            val queries = mutableListOf<String>()
            while (!coded.isAtEnd) {
                when (val tag = coded.readTag()) {
                    0 -> break
                    10 -> queries += coded.readStringRequireUtf8()
                    else -> coded.skipField(tag)
                }
            }
            return SearchHistory(queries)
        }
    }
}
