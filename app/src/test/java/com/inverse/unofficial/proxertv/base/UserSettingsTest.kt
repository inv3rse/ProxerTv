package com.inverse.unofficial.proxertv.base

import org.junit.Test
import rx.observers.TestSubscriber

/**
 * Test cases for the [UserSettings]
 */
class UserSettingsTest {

    val userSettings = UserSettingsMemory()

    @Test
    fun testObserveChanges() {
        val testSubscriber = TestSubscriber<User>()
        userSettings.observeAccount().take(3).subscribe(testSubscriber)

        userSettings.setAccount("test", "test")
        userSettings.clearUser()
        userSettings.setAccount("test2", "test2")

        testSubscriber.awaitTerminalEvent()
        testSubscriber.assertNoErrors()
    }
}