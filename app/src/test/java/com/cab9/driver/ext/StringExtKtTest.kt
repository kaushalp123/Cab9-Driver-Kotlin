package com.cab9.driver.ext

import com.google.common.truth.Truth
import org.junit.Test

class StringExtKtTest {

    @Test
    fun isValidMobileNumber() {
        // India number
        Truth.assertThat("7001390734".isValidMobileNumber()).isTrue() // valid
        Truth.assertThat("+917001390734".isValidMobileNumber()).isTrue() // valid
        Truth.assertThat("+91 7001390734".isValidMobileNumber()).isTrue() // valid
        Truth.assertThat("+91 7001 390734".isValidMobileNumber()).isFalse() // invalid
        Truth.assertThat("+91 7001 3907".isValidMobileNumber()).isFalse() // invalid
    }

    @Test
    fun formatToTwoDigit() {
        Truth.assertThat(formatToTwoDigit(0)).isEqualTo("00")
        Truth.assertThat(formatToTwoDigit(1)).isEqualTo("01")
        Truth.assertThat(formatToTwoDigit(2)).isEqualTo("02")
        Truth.assertThat(formatToTwoDigit(10)).isEqualTo("10")
        Truth.assertThat(formatToTwoDigit(100)).isEqualTo("100")
    }

    @Test
    fun formatToSingleDigitDecimal() {
        Truth.assertThat(formatToSingleDigitDecimal(0.1F)).isEqualTo("0.1")
        Truth.assertThat(formatToSingleDigitDecimal(0.11F)).isEqualTo("0.1")
        Truth.assertThat(formatToSingleDigitDecimal(0.111F)).isEqualTo("0.1")
    }
}