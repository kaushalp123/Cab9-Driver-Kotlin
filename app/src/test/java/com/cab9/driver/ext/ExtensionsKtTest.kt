package com.cab9.driver.ext

import org.junit.Test

class ExtensionsKtTest {

    @Test
    fun roundDownToMultipleOf() {
        assert(0.0F == 0.21F.roundDownToMultipleOf(0.5F))
        assert(0.5F == 0.55F.roundDownToMultipleOf(0.5F))
        assert(0.5F == 0.69F.roundDownToMultipleOf(0.5F))
        assert(0.5F == 0.5F.roundDownToMultipleOf(0.5F))

        assert(1F == 1.21F.roundDownToMultipleOf(0.5F))
        assert(1.5F == 1.55F.roundDownToMultipleOf(0.5F))
        assert(1.5F == 1.69F.roundDownToMultipleOf(0.5F))
        assert(1.5F == 1.5F.roundDownToMultipleOf(0.5F))
    }
}