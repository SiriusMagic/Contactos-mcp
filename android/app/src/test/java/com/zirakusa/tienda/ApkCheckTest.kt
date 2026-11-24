package com.zirakusa.tienda

import com.zirakusa.tienda.data.ApkCheck
import com.zirakusa.tienda.data.ApkFacts
import com.zirakusa.tienda.data.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApkCheckTest {
    private val reloj = app("reloj", "Reloj", "com.zirakusa.reloj", 5, "0.3.2")
    private val own = setOf(ApkCheck.sha256("family-debug-key".toByteArray()))
    private val other = setOf(ApkCheck.sha256("someone-else".toByteArray()))

    @Test fun sameAppSameVersionSameSignature() {
        assertNull(ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 5, own), reloj, own))
    }

    @Test fun refusesEverythingElse() {
        assertEquals(Problem.UNREADABLE, ApkCheck.verify(null, reloj, own))
        assertEquals(Problem.WRONG_PACKAGE, ApkCheck.verify(ApkFacts("com.evil.reloj", 5, own), reloj, own))
        assertEquals(Problem.WRONG_VERSION, ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 4, own), reloj, own))
        assertEquals(Problem.WRONG_SIGNATURE, ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 5, other), reloj, own))
        // An extra signer is not the same signature either.
        assertEquals(Problem.WRONG_SIGNATURE, ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 5, own + other), reloj, own))
        assertEquals(Problem.UNREADABLE, ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 5, emptySet()), reloj, own))
        assertEquals(Problem.UNREADABLE, ApkCheck.verify(ApkFacts("com.zirakusa.reloj", 5, own), reloj, emptySet()))
    }

    @Test fun sha256IsLowercaseHex() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ApkCheck.sha256("abc".toByteArray()))
    }
}
