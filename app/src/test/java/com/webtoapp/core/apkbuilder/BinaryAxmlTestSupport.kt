package com.webtoapp.core.apkbuilder

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object BinaryAxmlTestSupport {
    data class Attribute(
        val name: String,
        val value: String,
        val type: Int,
        val data: Int,
        val namespaceUri: String?,
        val resourceId: Int
    )

    data class Element(
        val name: String,
        val attributes: List<Attribute>,
        val children: List<Element>
    )

    fun parse(data: ByteArray): Element {
        val strings = mutableListOf<String>()
        val resourceIds = mutableListOf<Int>()
        val root = ArrayDeque<ElementBuilder>()
        var result: Element? = null
        var offset = 8

        while (offset + 8 <= data.size) {
            val type = u16(data, offset)
            val headerSize = u16(data, offset + 2)
            val size = i32(data, offset + 4)
            if (size < headerSize || offset + size > data.size) break

            when (type) {
                0x0001 -> readStringPool(data, offset, strings)
                0x0180 -> {
                    var pos = offset + 8
                    while (pos + 4 <= offset + size) {
                        resourceIds += i32(data, pos)
                        pos += 4
                    }
                }
                0x0102 -> {
                    val nameIndex = i32(data, offset + 20)
                    val attrStart = u16(data, offset + 24)
                    val attrSize = u16(data, offset + 26)
                    val attrCount = u16(data, offset + 28)
                    val attrs = buildList {
                        var pos = offset + 16 + attrStart
                        repeat(attrCount) {
                            val namespaceIndex = i32(data, pos)
                            val attrNameIndex = i32(data, pos + 4)
                            val rawIndex = i32(data, pos + 8)
                            val valueType = data[pos + 15].toInt() and 0xff
                            val valueData = i32(data, pos + 16)
                            val value = if (rawIndex >= 0) {
                                strings.getOrElse(rawIndex) { "" }
                            } else if (valueType == 0x03) {
                                strings.getOrElse(valueData) { "" }
                            } else {
                                valueData.toString()
                            }
                            add(
                                Attribute(
                                    name = strings.getOrElse(attrNameIndex) { "" },
                                    value = value,
                                    type = valueType,
                                    data = valueData,
                                    namespaceUri = strings.getOrNull(namespaceIndex),
                                    resourceId = resourceIds.getOrElse(attrNameIndex) { 0 }
                                )
                            )
                            pos += attrSize
                        }
                    }
                    root.addLast(ElementBuilder(strings.getOrElse(nameIndex) { "" }, attrs))
                }
                0x0103 -> {
                    val finished = root.removeLastOrNull()?.build() ?: error("Unbalanced AXML")
                    if (root.isEmpty()) result = finished else root.last().children += finished
                }
            }
            offset += size
        }
        return result ?: error("No root element in AXML")
    }

    private class ElementBuilder(
        val name: String,
        val attributes: List<Attribute>,
        val children: MutableList<Element> = mutableListOf()
    ) {
        fun build() = Element(name, attributes, children.toList())
    }

    private fun readStringPool(data: ByteArray, offset: Int, output: MutableList<String>) {
        val count = i32(data, offset + 8)
        val flags = i32(data, offset + 16)
        val stringsStart = i32(data, offset + 20)
        repeat(count) { index ->
            val stringOffset = i32(data, offset + 28 + index * 4)
            val pos = offset + stringsStart + stringOffset
            output += if ((flags and 0x100) != 0) readUtf8(data, pos) else readUtf16(data, pos)
        }
    }

    private fun readUtf8(data: ByteArray, offset: Int): String {
        var pos = offset
        pos += if ((data[pos].toInt() and 0x80) == 0) 1 else 2
        val length = if ((data[pos].toInt() and 0x80) == 0) {
            data[pos++].toInt() and 0xff
        } else {
            ((data[pos++].toInt() and 0x7f) shl 8) or (data[pos++].toInt() and 0xff)
        }
        val bytes = data.copyOfRange(pos, pos + length)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun readUtf16(data: ByteArray, offset: Int): String {
        var pos = offset
        val first = u16(data, pos)
        val length = if ((first and 0x8000) == 0) {
            pos += 2
            first
        } else {
            val second = u16(data, pos + 2)
            pos += 4
            ((first and 0x7fff) shl 16) or second
        }
        return data.copyOfRange(pos, pos + length * 2).toString(Charsets.UTF_16LE)
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xff) or ((data[offset + 1].toInt() and 0xff) shl 8)

    private fun i32(data: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    class Builder {
        private val chunks = mutableListOf<ByteArray>()
        private var stringPoolBytes: ByteArray? = null
        private var resourceMapBytes: ByteArray? = null

        data class Attr(
            val nsIdx: Int,
            val nameIdx: Int,
            val rawIdx: Int,
            val typedValueType: Int,
            val typedValueData: Int
        )

        fun stringPool(strings: List<String>) = apply {
            val offsets = IntArray(strings.size)
            val blob = ByteArrayOutputStream()
            strings.forEachIndexed { index, value ->
                offsets[index] = blob.size()
                val bytes = value.toByteArray(Charsets.UTF_8)
                blob.write(value.length)
                blob.write(bytes.size)
                blob.write(bytes)
                blob.write(0)
            }
            while (blob.size() % 4 != 0) blob.write(0)
            val header = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
                .putShort(0x0001)
                .putShort(28)
                .putInt(28 + offsets.size * 4 + blob.size())
                .putInt(strings.size)
                .putInt(0)
                .putInt(0x100)
                .putInt(28 + offsets.size * 4)
                .putInt(0)
                .array()
            stringPoolBytes = ByteArrayOutputStream().apply {
                write(header)
                offsets.forEach { write(intLe(it)) }
                write(blob.toByteArray())
            }.toByteArray()
        }

        fun resourceMap(ids: IntArray) = apply {
            resourceMapBytes = ByteArrayOutputStream().apply {
                write(shortLe(0x0180))
                write(shortLe(8))
                write(intLe(8 + ids.size * 4))
                ids.forEach { write(intLe(it)) }
            }.toByteArray()
        }

        fun startNamespace(prefixIdx: Int, uriIdx: Int) = apply {
            chunks += nodeChunk(0x0100, prefixIdx, uriIdx)
        }

        fun endNamespace(prefixIdx: Int, uriIdx: Int) = apply {
            chunks += nodeChunk(0x0101, prefixIdx, uriIdx)
        }

        fun startElement(namespaceIdx: Int, nameIdx: Int, attributes: List<Attr>) = apply {
            val out = ByteArrayOutputStream()
            writeHeader(out, 0x0102, 16, 36 + attributes.size * 20)
            out.write(intLe(0))
            out.write(intLe(-1))
            out.write(intLe(namespaceIdx))
            out.write(intLe(nameIdx))
            out.write(shortLe(0x14))
            out.write(shortLe(20))
            out.write(shortLe(attributes.size))
            out.write(shortLe(0))
            out.write(shortLe(0))
            out.write(shortLe(0))
            attributes.forEach { attr ->
                out.write(intLe(attr.nsIdx))
                out.write(intLe(attr.nameIdx))
                out.write(intLe(attr.rawIdx))
                out.write(shortLe(8))
                out.write(0)
                out.write(attr.typedValueType)
                out.write(intLe(attr.typedValueData))
            }
            chunks += out.toByteArray()
        }

        fun endElement(namespaceIdx: Int, nameIdx: Int) = apply {
            val out = ByteArrayOutputStream()
            writeHeader(out, 0x0103, 16, 24)
            out.write(intLe(0))
            out.write(intLe(-1))
            out.write(intLe(namespaceIdx))
            out.write(intLe(nameIdx))
            chunks += out.toByteArray()
        }

        fun build(): ByteArray {
            val body = ByteArrayOutputStream().apply {
                stringPoolBytes?.let { write(it) }
                resourceMapBytes?.let { write(it) }
                chunks.forEach { write(it) }
            }
            return ByteArrayOutputStream().apply {
                write(shortLe(0x0003))
                write(shortLe(8))
                write(intLe(8 + body.size()))
                write(body.toByteArray())
            }.toByteArray()
        }

        private fun nodeChunk(type: Int, prefixIdx: Int, uriIdx: Int): ByteArray =
            ByteArrayOutputStream().apply {
                writeHeader(this, type, 16, 24)
                write(intLe(0))
                write(intLe(-1))
                write(intLe(prefixIdx))
                write(intLe(uriIdx))
            }.toByteArray()

        private fun writeHeader(out: ByteArrayOutputStream, type: Int, header: Int, size: Int) {
            out.write(shortLe(type))
            out.write(shortLe(header))
            out.write(intLe(size))
        }
    }

    private fun shortLe(value: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()

    private fun intLe(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
}
