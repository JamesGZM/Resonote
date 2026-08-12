package com.resonote.core.network.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return null
        return if (element === JsonNull || (!primitive.isString && primitive.booleanOrNull != null)) null else primitive.contentOrNull
    }

    override fun serialize(encoder: Encoder, value: String?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

internal object FlexibleLongSerializer : KSerializer<Long?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long? {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return (element as? JsonPrimitive)?.let { it.longOrNull ?: it.contentOrNull?.toDoubleOrNull()?.toLong() }
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

internal object StringListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringList", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): List<String> {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val values = if (element is JsonArray) element else JsonArray(listOf(element))
        return values.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonArray(value.map(::JsonPrimitive)))
    }
}

internal object RelatedGoodsSerializer : KSerializer<List<RelatedGoodDto>> {
    override val descriptor: SerialDescriptor = ListSerializer(RelatedGoodDto.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<RelatedGoodDto> {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        return if (element is JsonArray) List(element.size) { RelatedGoodDto() } else emptyList()
    }

    override fun serialize(encoder: Encoder, value: List<RelatedGoodDto>) {
        (encoder as JsonEncoder).encodeJsonElement(JsonArray(List(value.size) { JsonObject(emptyMap()) }))
    }
}
