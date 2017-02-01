package com.inverse.unofficial.proxertv.base

import ApiResponses
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import com.nhaarman.mockito_kotlin.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import provideTestClient
import rx.Observable
import subscribeAssert

/**
 * Test cases for the [ProxerRepository]
 */
class ProxerRepositoryTest {
    private lateinit var mockServer: MockWebServer

    private lateinit var client: ProxerClient
    private lateinit var mySeriesDb: MySeriesDb
    private lateinit var seriesProgressDb: SeriesProgressDb
    private lateinit var userSettings: UserSettings
    private lateinit var repository: ProxerRepository

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        client = provideTestClient(mockServer)
        mySeriesDb = mock()
        seriesProgressDb = mock()
        userSettings = UserSettingsMemory()

        repository = ProxerRepository(client, mySeriesDb, seriesProgressDb, userSettings)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun testLogin() {
        mockServer.enqueue(ApiResponses.getSuccessFulLoginResponse(TEST_TOKEN))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(ProxerRepository.Login(TEST_USER, TEST_PASSWORD, TEST_TOKEN))
                }

        assertEquals(User(TEST_USER, TEST_PASSWORD, TEST_TOKEN), userSettings.getUser())
    }

    @Test
    fun testAlreadyLoggedInIsSuccessful() {
        userSettings.setUserToken(TEST_TOKEN)
        mockServer.enqueue(ApiResponses.getErrorResponse(3012, "Der User ist bereits angemeldet"))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(ProxerRepository.Login(TEST_USER, TEST_PASSWORD, TEST_TOKEN))
                }

        assertEquals(User(TEST_USER, TEST_PASSWORD, TEST_TOKEN), userSettings.getUser())
    }

    @Test
    fun testLoginRetryAfterLogout() {
        mockServer.enqueue(ApiResponses.getErrorResponse(3013, "Ein anderer User ist bereits eingeloggt"))
        mockServer.enqueue(ApiResponses.getLogoutResponse())
        mockServer.enqueue(ApiResponses.getSuccessFulLoginResponse(TEST_TOKEN))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(ProxerRepository.Login(TEST_USER, TEST_PASSWORD, TEST_TOKEN))
                }

        assertEquals(User(TEST_USER, TEST_PASSWORD, TEST_TOKEN), userSettings.getUser())
    }

    @Test
    fun testLoginFailure() {
        mockServer.enqueue(ApiResponses.getErrorResponse(3001, "Ungültige Login-Daten"))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertError(ApiErrorException::class.java)
                }

        // should retry at max once
        mockServer.enqueue(ApiResponses.getErrorResponse(3013, "Ein anderer User ist bereits eingeloggt"))
        mockServer.enqueue(ApiResponses.getLogoutResponse())
        mockServer.enqueue(ApiResponses.getErrorResponse(3013, "Ein anderer User ist bereits eingeloggt"))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertError(ApiErrorException::class.java)
                }
    }

    @Test
    fun testLogout() {
        mockServer.enqueue(ApiResponses.getLogoutResponse())

        userSettings.setUser(TEST_USER, TEST_PASSWORD)
        userSettings.setUserToken(TEST_TOKEN)

        repository.logout()
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(true)
                }

        assertEquals(null, userSettings.getUser())
    }

    @Test
    fun testSetProgress() {
        // no user, offline only
        userSettings.clearUser()
        whenever(seriesProgressDb.setProgress(any(), any())).thenReturn(Observable.empty())

        repository.setSeriesProgress(1234, 5)
                .subscribeAssert {
                    assertNoErrors()
                    assertNoValues()
                }

        verify(seriesProgressDb).setProgress(eq(1234), eq(5))
    }

    companion object {
        private const val TEST_USER = "mockUser"
        private const val TEST_PASSWORD = "mockPassword"
        private const val TEST_TOKEN = "testToken"
    }
}