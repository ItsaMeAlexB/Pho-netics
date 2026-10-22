package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgettingTest {

    @Test
    fun retrievabilityStartsAtOne() {
        assertEquals(1.0, Forgetting.retrievability(0.0, 5.0), 1e-9)
    }

    @Test
    fun retrievabilityDecaysOverTime() {
        assertTrue(Forgetting.retrievability(10.0, 5.0) < Forgetting.retrievability(0.0, 5.0))
    }

    @Test
    fun greaterStabilityYieldsHigherRetrievability() {
        assertTrue(Forgetting.retrievability(10.0, 20.0) > Forgetting.retrievability(10.0, 5.0))
    }

    @Test
    fun daysToTargetIsInverseOfRetrievability() {
        val s = 10.0
        val target = 0.9
        val d = Forgetting.daysToTarget(s, target)
        assertEquals(target, Forgetting.retrievability(d, s), 1e-9)
    }
}
