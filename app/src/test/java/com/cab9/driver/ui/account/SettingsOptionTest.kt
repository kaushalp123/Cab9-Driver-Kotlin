package com.cab9.driver.ui.account

import org.junit.Test


class SettingsOptionTest {

    @Test
    fun `setting option list without Biometric`() {
        val options = SettingsOption.values().filter { it != SettingsOption.BIOMETRIC }
        assert(
            options.contains(SettingsOption.DISPLAY)
                    && options.contains(SettingsOption.VEHICLES)
                    && options.contains(SettingsOption.DIAGNOSTICS)
        )
    }

}