package com.inverse.unofficial.proxertv.base

import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import provideTestClient
import subscribeAssert

class ProxerRepositoryTest {
    private lateinit var mockServer: MockWebServer

    private lateinit var client: ProxerClient
    private lateinit var mySeriesDb: MySeriesDb
    private lateinit var seriesPrgressDb: SeriesProgressDb
    private lateinit var userSettings: UserSettings
    private lateinit var repository: ProxerRepository

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        client = provideTestClient(mockServer)
        mySeriesDb = Mockito.mock(MySeriesDb::class.java)
        seriesPrgressDb = Mockito.mock(SeriesProgressDb::class.java)
        userSettings = UserSettingsMemory()

        repository = ProxerRepository(client, mySeriesDb, seriesPrgressDb, userSettings)
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun testLogin() {
        mockServer.enqueue(getSuccessFulLoginResponse())

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
        mockServer.enqueue(getErrorResponse(3012, "Der User ist bereits angemeldet"))

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(ProxerRepository.Login(TEST_USER, TEST_PASSWORD, TEST_TOKEN))
                }

        assertEquals(User(TEST_USER, TEST_PASSWORD, TEST_TOKEN), userSettings.getUser())
    }

    @Test
    fun testLoginRetryAfterLogout() {
        mockServer.enqueue(getErrorResponse(3013, "Ein anderer User ist bereits eingeloggt"))
        mockServer.enqueue(getLogoutResponse())
        mockServer.enqueue(getSuccessFulLoginResponse())

        repository.login(TEST_USER, TEST_PASSWORD)
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(ProxerRepository.Login(TEST_USER, TEST_PASSWORD, TEST_TOKEN))
                }

        assertEquals(User(TEST_USER, TEST_PASSWORD, TEST_TOKEN), userSettings.getUser())
    }

    @Test
    fun testLogout() {
        mockServer.enqueue(getLogoutResponse())

        userSettings.setUser(TEST_USER, TEST_PASSWORD)
        userSettings.setUserToken(TEST_TOKEN)

        repository.logout()
                .subscribeAssert {
                    assertNoErrors()
                    assertValue(true)
                }

        assertEquals(null, userSettings.getUser())
    }

    private fun getSuccessFulLoginResponse(): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 0," +
                "\"message\": \"Login erfolgreich\"," +
                "\"data\": {" +
                "\"uid\": \"12345\"," +
                "\"avatar\": \"122345.jpg\"," +
                "\"token\": \"" + TEST_TOKEN + "\"" +
                "}}")
    }

    private fun getLogoutResponse(): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 0," +
                "\"message\": \"Logout successfull\"" +
                "}")
    }

    private fun getErrorResponse(errorCode: Int, msg: String): MockResponse {
        return MockResponse().setBody("{" +
                "\"error\": 1," +
                "\"message\": \"" + msg + "\"," +
                "\"code\": " + errorCode +
                "}")
    }

    companion object {
        private const val TEST_USER = "mockUser"
        private const val TEST_PASSWORD = "mockPassword"
        private const val TEST_TOKEN = "testToken"
    }
}