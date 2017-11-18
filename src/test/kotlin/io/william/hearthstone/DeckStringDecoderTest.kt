package io.william.hearthstone

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by williamwebb on 6/3/17.
 */

class DeckStringDecoderTest {

    val TEST_DECK_NAME = "DragonsLUL"
    val TEST_DECK_CLASS = "Priest"
    val TEST_DECK_CODE = "AAECAR8G+LEChwTmwgKhwgLZwgK7BQzquwKJwwKOwwKTwwK5tAK1A/4MqALsuwLrB86uAu0JAA=="

    val RAW_DECK_STRING = """
        |### $TEST_DECK_NAME
        |# Class: $TEST_DECK_CLASS
        |# Format: Standard
        |# Year of the Mammoth
        |#
        |# 1x (1) Crystalline Oracle
        |# 2x (1) Northshire Cleric
        |# 2x (1) Potion of Madness
        |# 2x (1) Power Word: Shield
        |# 2x (2) Netherspite Historian
        |# 2x (2) Radiant Elemental
        |# 1x (2) Shadow Visions
        |# 2x (2) Shadow Word: Pain
        |# 1x (3) Curious Glimmerroot
        |# 2x (3) Kabal Talonpriest
        |# 2x (4) Twilight Drake
        |# 2x (5) Drakonid Operative
        |# 1x (5) Holy Nova
        |# 1x (5) Lyra the Sunshard
        |# 2x (6) Book Wyrm
        |# 2x (6) Dragonfire Potion
        |# 1x (7) The Curator
        |# 1x (8) Primordial Drake
        |# 1x (9) Ysera
        |#
        |$TEST_DECK_CODE
        |#
        |# To use this deck, copy it to your clipboard and create a new deck in Hearthstone
        """.trimMargin()

    val TEST_CARDS = hashMapOf(
            (40426 to 2),  // Alleycat
            (41353 to 2),  // Jeweled Macaw
            (39160 to 1),  // Cat Trick
            (41358 to 2),  // Crackling Razormaw
            (41363 to 2),  // Dinomancy
            (519 to 1),  // Freezing Trap
            (39481 to 2),  // Kindly Grandmother
            (41318 to 1),  // Stubborn Gastropod
            (437 to 2),  // Animal Companion
            (1662 to 2),  // Eaglehorn Bow
            (41249 to 1),  // Eggnapper
            (296 to 2),  // Kill Command
            (40428 to 2),  // Rat Pack
            (1003 to 2),  // Houndmaster
            (38734 to 2),  // Infested Wolf
            (41305 to 1),  // Nesting Roc
            (699 to 1),  // Tundra Rhino
            (1261 to 2)  // Savannah Highmane
    )

    val TEST_HERO = 31 // Rexxar

    @Test
    fun testParseDeckForName() {
        val name = parseDeckForName(RAW_DECK_STRING)
        assertEquals(TEST_DECK_NAME, name)
    }

    @Test
    fun testParseDeckForClass() {
        val clazz = parseDeckForClass(RAW_DECK_STRING)
        assertEquals(TEST_DECK_CLASS, clazz)
    }

    @Test
    fun testParseDeckForCode() {
        val deckCode = parseDeckForCode(RAW_DECK_STRING)
        assertEquals(TEST_DECK_CODE, deckCode)
    }

    @Test
    fun testDecode() {
        val deck = parseDeck(RAW_DECK_STRING)

        assertEquals(deck.name, TEST_DECK_NAME)
        assertEquals(deck.`class`, TEST_DECK_CLASS)
        assertEquals(deck.version, 1)
        assertEquals(deck.format, 2)
        assertEquals(deck.cards, TEST_CARDS)
        assertEquals(deck.heros.size, 1)
        assertEquals(deck.heros, listOf(TEST_HERO))
    }

    @Test(expected = Exception::class)
    fun testInvalidDecode() {
        decodeDeckStringUnSafe("", "", "omg")
    }

    @Test
    fun testEncode() {
        val deck = decodeDeckStringUnSafe("name", "class", TEST_DECK_CODE)
        val encoded = encodeDeck(deck)
        val deck2 = decodeDeckStringUnSafe("name", "class", encoded)

        assertEquals(TEST_DECK_CODE, encoded)
        assertEquals(deck, deck2)
    }

}