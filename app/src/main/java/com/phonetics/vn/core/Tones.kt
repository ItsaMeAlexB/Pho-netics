package com.phonetics.vn.core

/**
 * The phonemic tones of Southern Vietnamese.
 *
 * Standard Vietnamese has 6 tones, but the ngã and hỏi tones **merge** in the Southern
 * dialect, so learners of the Southern variety acquire 5 phonemic tones.
 */
data class Tone(
    /** 1-based position in the Southern tone set (used as the answer key in HVPT trials). */
    val index: Int,
    /** Tone name in Vietnamese. */
    val name: String,
    /** Example carrier syllable "ma" carrying this tone (minimal pair). */
    val diacritic: String,
)

object VietTones {
    val SOUTHERN: List<Tone> = listOf(
        Tone(1, "ngang", "ma"),
        Tone(2, "huyền", "mà"),
        Tone(3, "sắc", "má"),
        Tone(4, "hỏi", "mả"),
        Tone(5, "nặng", "mạ"),
    )
}
