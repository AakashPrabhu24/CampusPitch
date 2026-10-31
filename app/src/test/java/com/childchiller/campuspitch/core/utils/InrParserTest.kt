package com.childchiller.campuspitch.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InrParserTest {

    @Test
    fun testOneCrore() {
        val result = InrParser.parse("I want to raise 1 cr to scale")
        assertEquals(10_000_000L, result.singleAmountInr)
        assertFalse(result.isRange)
    }

    @Test
    fun testFiftyLakh() {
        val result = InrParser.parse("Looking for 50 lakh in seed funding")
        assertEquals(5_000_000L, result.singleAmountInr)
        assertFalse(result.isRange)
    }

    @Test
    fun testTwoCroreRequirement() {
        val prompt = "I want to upload an app called MediCare. It manages hospitals, and I want to raise ₹2 crore"
        val result = InrParser.parse(prompt)
        assertEquals(20_000_000L, result.singleAmountInr)
        assertFalse(result.isRange)
    }

    @Test
    fun testInrFormattingRequirement() {
        assertEquals("2,00,00,000", InrParser.formatInr(20_000_000L))
        assertEquals("50,00,000", InrParser.formatInr(5_000_000L))
    }

    @Test
    fun testOneAndHalfCrore() {
        val result = InrParser.parse("Targeting one and a half crore for Series A")
        assertEquals(15_000_000L, result.singleAmountInr)
        assertFalse(result.isRange)
    }

    @Test
    fun testRangeBetweenOneAndFiveCrore() {
        val result = InrParser.parse("investment amount is between 1 cr and 5 cr")
        assertTrue(result.isRange)
        assertEquals(10_000_000L, result.minInr)
        assertEquals(50_000_000L, result.maxInr)
    }

    @Test
    fun testRangeOneToFiveCrore() {
        val result = InrParser.parse("raise 1 to 5 crore")
        assertTrue(result.isRange)
        assertEquals(10_000_000L, result.minInr)
        assertEquals(50_000_000L, result.maxInr)
    }

    @Test
    fun testHinglishDedhCrore() {
        val result = InrParser.parse("humko dedh crore chahiye")
        assertEquals(15_000_000L, result.singleAmountInr)
    }

    @Test
    fun testHinglishDhaiCr() {
        val result = InrParser.parse("raise dhai cr")
        assertEquals(25_000_000L, result.singleAmountInr)
    }

    @Test
    fun testThousand() {
        val result = InrParser.parse("grant of 500 thousand")
        assertEquals(500_000L, result.singleAmountInr)
    }
}
