package com.phonetics.vn.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneCurriculumTest {

    private val talkers = (1..6).map { "talker-$it" }
    private val carriers = listOf("ma")

    @Test
    fun hvptRequiresAtLeastSixTalkers() {
        try {
            ToneCurriculum.plan(carriers, listOf("a", "b", "c", "d", "e"))
            assertTrue("expected IllegalArgumentException", false)
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun planProducesTheRequestedTrialCount() {
        val plan = ToneCurriculum.plan(carriers, talkers, totalTrials = 400)
        assertEquals(400, plan.size)
        assertEquals(400, plan.last().id)
    }

    @Test
    fun planCoversAllFiveTonesAndAllTalkers() {
        val plan = ToneCurriculum.plan(carriers, talkers, totalTrials = 200)
        val toneKeys = plan.map { it.correctToneIndex }.toSet()
        val talkerKeys = plan.map { it.talkerId }.toSet()
        assertEquals((1..5).toSet(), toneKeys)
        assertEquals(talkers.toSet(), talkerKeys)
    }

    @Test
    fun noBackToBackSameTalkerOrSameTone() {
        val plan = ToneCurriculum.plan(carriers, talkers, totalTrials = 300)
        for (i in 1 until plan.size) {
            assertNotEquals(plan[i - 1].talkerId, plan[i].talkerId)
            assertNotEquals(plan[i - 1].correctToneIndex, plan[i].correctToneIndex)
        }
    }

    @Test
    fun trialIdsAreSequential() {
        val plan = ToneCurriculum.plan(carriers, talkers, totalTrials = 99)
        assertEquals((1..99).toList(), plan.map { it.id })
    }
}
