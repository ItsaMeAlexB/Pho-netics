package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TonesTest {

    @Test
    fun southernDialectHasExactlyFiveTones() {
        assertEquals(5, VietTones.SOUTHERN.size)
    }

    @Test
    fun southernToneSetIsOrderedNgangHuyenSacHoiNang() {
        assertEquals(
            listOf("ngang", "huyền", "sắc", "hỏi", "nặng"),
            VietTones.SOUTHERN.map { it.name },
        )
    }

    @Test
    fun tonesAreDistinctMinimalPairsOnCarrierMa() {
        // All 5 forms must be distinct realizations of the same "ma" carrier.
        assertEquals(5, VietTones.SOUTHERN.map { it.diacritic }.toSet().size)
        VietTones.SOUTHERN.forEach { tone ->
            assertTrue(tone.diacritic.startsWith("m"))
        }
    }
}
