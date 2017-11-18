package io.william.hearthstone

import org.apache.commons.codec.binary.Base64
import java.io.*

/**
 * Created by williamwebb on 6/3/17.
 */

typealias Cards = LinkedHashMap<Int, Int>

class DecodeException(exp: Exception) : Throwable(exp)

data class Deck(val name: String, val `class`: String, val version: Int, val format: Int, val cards: Cards, val heros: List<Int>)

fun ByteArrayInputStream.readVarInt() = getVarInt(this)

// Static Java Bindings
class DeckCode {
    companion object {
        @JvmStatic fun decode(deck: String): Deck = io.william.hearthstone.parseDeck(deck)
        @JvmStatic fun encode(deck: Deck): String = io.william.hearthstone.encodeDeck(deck)
    }
}

// Parse given line of deckString for the header.
private fun parseDeckFor(header: String, lineNumber: Int, deckString: String): String {
    val line = deckString.split("\n").takeIf { it.size >= lineNumber }?.get(lineNumber) ?: throw Exception("Provided line less then requested($lineNumber) found(${deckString.length})")
    val name = line.takeIf { it.startsWith(header) }?.substring(header.length, line.length) ?: throw Exception("Invalid Header! requested($header) for line: $line")
    return name
}

fun parseDeckForName(deckString: String) = parseDeckFor("### ", 0, deckString)
fun parseDeckForClass(deckString: String) = parseDeckFor("# Class: ", 1, deckString)

// unused
fun parseDeckForFormat(deckString: String) = parseDeckFor("# Format: ", 2, deckString)
fun parseDeckForYear(deckString: String) = parseDeckFor("# ", 3, deckString)

// strip all lines starting with #
fun parseDeckForCode(string: String): String {
    val lines = string.split("\n").filter {
        !it.trimStart().startsWith("#")
    }

    return lines[0].takeIf { lines.isNotEmpty() } ?: throw Exception("Unable to parse deck!")
}

@Throws(DecodeException::class)
fun parseDeck(deckString: String): Deck {
    try {
        val name = parseDeckForName(deckString)
        val clazz = parseDeckForClass(deckString)
        val encodedDeckString = parseDeckForCode(deckString)

        return decodeDeckStringUnSafe(name, clazz, encodedDeckString)
    }catch (exp: Exception) {
        throw DecodeException(exp)
    }
}

fun decodeDeckStringUnSafe(title: String, clazz: String, deckString: String): Deck {
    val bytes = Base64.decodeBase64(deckString.toByteArray())
    val byteStream = ByteArrayInputStream(bytes)

    //Zero byte
    byteStream.read()

    //Version - currently unused, always 1
    val version = byteStream.readVarInt()

    //Format - determined dynamically
    val format = byteStream.readVarInt()

    //Num Heroes - always 1
    val numHeros = byteStream.readVarInt()
    val heros = (0..numHeros - 1).map { byteStream.readVarInt() }

    val cards = LinkedHashMap<Int, Int>()

    // Read Single Cards
    val singleCardCount = byteStream.readVarInt()
    repeat(singleCardCount) {
        val id = byteStream.readVarInt()

        // add card with quantity of 1
        cards.put(id, 1)
    }

    // Read Double Cards
    val doubleCardCount = byteStream.readVarInt()
    repeat(doubleCardCount) {
        val id = byteStream.readVarInt()

        // add card with quantity of 2
        cards.put(id, 2)
    }

    // Read Multi Cards
    val multiCardCount = byteStream.readVarInt()
    repeat(multiCardCount) {
        val id = byteStream.readVarInt()
        val count = byteStream.readVarInt()

        // add card with quantity of count
        cards.put(id, count)
    }

    return Deck(title, clazz, version, format, cards, heros)
}

fun encodeDeck(deck: Deck): String {
    val byteBuffer = ByteArrayOutputStream()

    // zero byte
    putVarInt(0, byteBuffer)

    putVarInt(deck.version, byteBuffer)
    putVarInt(deck.format, byteBuffer)
    putVarInt(deck.heros.size, byteBuffer)
    deck.heros.forEach {
        putVarInt(it, byteBuffer)
    }

    val (ones, twos, multi) = sort(deck.cards)

    putVarInt(ones.size, byteBuffer)
    ones.forEach {
        putVarInt(it, byteBuffer)
    }

    putVarInt(twos.size, byteBuffer)
    twos.forEach {
        putVarInt(it, byteBuffer)
    }

    putVarInt(multi.size, byteBuffer)
    multi.forEach {
        putVarInt(it.first, byteBuffer)
        putVarInt(it.second, byteBuffer)
    }

    val encoded = Base64.encodeBase64(byteBuffer.toByteArray())

    return String(encoded)
}

/** Utility Methods & Classes */

@Throws(IOException::class)
private fun getVarInt(inputStream: InputStream): Int {
    var result = 0
    var shift = 0
    var b: Int
    do {
        if (shift >= 32) {
            // Out of range
            throw IndexOutOfBoundsException("varint too long")
        }
        // Get 7 bits from next byte
        b = inputStream.read()
        result = result or (b and 0x7F shl shift)
        shift += 7
    } while (b and 0x80 != 0)
    return result
}

@Throws(IOException::class)
fun putVarInt(v: Int, outputStream: OutputStream) {
    val bytes = ByteArray(varIntSize(v))
    putVarInt(v, bytes, 0)
    outputStream.write(bytes)
}

fun varIntSize(i: Int): Int {
    var i = i
    var result = 0
    do {
        result++
        i = i ushr 7
    } while (i != 0)
    return result
}

fun putVarInt(v: Int, sink: ByteArray, offset: Int): Int {
    var v = v
    var offset = offset
    do {
        // Encode next 7 bits + terminator bit
        val bits = v and 0x7F
        v = v ushr 7
        val b = (bits + if (v != 0) 0x80 else 0).toByte()
        sink[offset++] = b
    } while (v != 0)
    return offset
}

fun sort(cards: Cards): SortedCards {
    val ones = ArrayList<Int>()
    val twos = ArrayList<Int>()
    val multi = ArrayList<Pair<Int,Int>>()

    cards.entries.forEach { (cardId, quantity) ->
        when(quantity) {
            1-> ones.add(cardId)
            2-> twos.add(cardId)
            else -> multi.add(cardId to quantity)
        }
    }
    return SortedCards(ones, twos, multi)
}

data class SortedCards(val ones: List<Int>, val twos: List<Int>, val multi: List<Pair<Int,Int>>)