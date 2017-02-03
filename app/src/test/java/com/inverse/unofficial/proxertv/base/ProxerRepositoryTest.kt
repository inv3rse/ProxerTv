package com.inverse.unofficial.proxertv.base

import ApiResponses
import com.github.salomonbrys.kotson.fromJson
import com.inverse.unofficial.proxertv.base.client.ProxerClient
import com.inverse.unofficial.proxertv.base.client.util.ApiErrorException
import com.inverse.unofficial.proxertv.base.db.MySeriesDb
import com.inverse.unofficial.proxertv.base.db.SeriesProgressDb
import com.inverse.unofficial.proxertv.model.*
import com.nhaarman.mockito_kotlin.*
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun testMoveSeriesToListOffline() {
        userSettings.clearUser()
        whenever(mySeriesDb.insertOrUpdateSeries(any())).thenReturn(Observable.just(Unit))
        whenever(mySeriesDb.removeSeries(any())).thenReturn(Observable.just(Unit))

        val reZero = SeriesCover(13975, "Re:Zero kara Hajimeru Isekai Seikatsu")

        // update or insert the series
        repository.moveSeriesToList(reZero, SeriesList.WATCHLIST).subscribeAssert {
            assertNoErrors()
        }
        verify(mySeriesDb).insertOrUpdateSeries(eq(SeriesDbEntry(reZero.id, reZero.name, SeriesList.WATCHLIST)))

        // no list should delete
        repository.moveSeriesToList(reZero, SeriesList.NONE).subscribeAssert {
            assertNoErrors()
        }
        verify(mySeriesDb).removeSeries(eq(reZero.id))
    }

    @Test
    fun testMoveSeriesToListOnline() {
        userSettings.setUser(TEST_USER, TEST_PASSWORD)

        val reZero = SeriesCover(13975, "Re:Zero kara Hajimeru Isekai Seikatsu")
        val reZeroOnline = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.WATCHLIST, 12345678)

        // entry does not exists, needs to be created
        whenever(mySeriesDb.getSeries(any())).thenReturn(Observable.error(MySeriesDb.NoSeriesEntryException()))
        // success create response
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("COM_PROXER_SUCCESS"))
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Abfrage erfolgreich", getListReZero(UserListSeriesEntry.STATE_USER_FINISHED)))

        // the comment state is 0 (= FINISHED) and needs to be updated
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Erfolgreich bearbeitet!"))
        whenever(mySeriesDb.overrideWithSeriesList(any())).thenReturn(Observable.just(Unit))

        repository.moveSeriesToList(reZero, SeriesList.WATCHLIST).subscribeAssert {
            assertNoErrors()
        }

        // the local db should get updated with the latest data + our changes
        verify(mySeriesDb).overrideWithSeriesList(eq(listOf(reZeroOnline)))

        // get the last requests
        mockServer.takeRequest()
        mockServer.takeRequest()
        val updateRequest = mockServer.takeRequest()

        // the request should have multiple query parameters
        assertTrue(updateRequest.requestLine.contains("format=json&json=edit"))

        // the complete comment must be send to update
        val comment = client.gson.fromJson<Comment>(updateRequest.body.readUtf8())
        assertEquals("", comment.comment)
        assertEquals(26, comment.episode)
        assertEquals(2, comment.state)
        assertEquals(0, comment.rating)
        assertEquals(1, comment.ratingGenre)
        assertEquals(1, comment.ratingStory)
        assertEquals(2, comment.ratingAnimation)
        assertEquals(1, comment.ratingCharacters)
        assertEquals(1, comment.ratingMusic)
    }

    @Test
    fun testMoveSeriesToListOnlineExists() {
        userSettings.setUser(TEST_USER, TEST_PASSWORD)

        val reZero = SeriesCover(13975, "Re:Zero kara Hajimeru Isekai Seikatsu")
        val reZeroDb = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.WATCHLIST, 12345678)
        val reZeroOnline = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.FINISHED, 12345678)

        // entry does exist, no create request necessary
        whenever(mySeriesDb.getSeries(any())).thenReturn(Observable.just(reZeroDb))
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Abfrage erfolgreich", getListReZero(UserListSeriesEntry.STATE_USER_BOOKMARKED)))

        // the comment state is 2 (= Bookmarked) and needs to be updated
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Erfolgreich bearbeitet!"))
        whenever(mySeriesDb.overrideWithSeriesList(any())).thenReturn(Observable.just(Unit))

        repository.moveSeriesToList(reZero, SeriesList.FINISHED).subscribeAssert {
            assertNoErrors()
        }

        // the local db should get updated with the latest data + our changes
        verify(mySeriesDb).overrideWithSeriesList(eq(listOf(reZeroOnline)))
    }

    @Test
    fun testMoveSeriesToListOnlineLogin() {
        userSettings.setUser(TEST_USER, TEST_PASSWORD)

        val reZero = SeriesCover(13975, "Re:Zero kara Hajimeru Isekai Seikatsu")
        val reZeroDb = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.WATCHLIST, 12345678)
        val reZeroOnline = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.FINISHED, 12345678)

        // entry does exist, no create request necessary
        whenever(mySeriesDb.getSeries(any())).thenReturn(Observable.just(reZeroDb))

        // list request failed, not logged in
        mockServer.enqueue(ApiResponses.getErrorResponse(3004, "Der User ist nicht eingeloggt"))
        mockServer.enqueue(ApiResponses.getSuccessFulLoginResponse(TEST_TOKEN))
        // list request success after login
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Abfrage erfolgreich", getListReZero(UserListSeriesEntry.STATE_USER_FINISHED)))

        // the comment state is finished and does not need to be updated
        whenever(mySeriesDb.overrideWithSeriesList(any())).thenReturn(Observable.just(Unit))

        repository.moveSeriesToList(reZero, SeriesList.FINISHED).subscribeAssert {
            assertNoErrors()
        }

        // the local db should get updated with the latest data + our changes
        verify(mySeriesDb).overrideWithSeriesList(eq(listOf(reZeroOnline)))
    }

    @Test
    fun testMoveSeriesToListOnlineChange() {
        userSettings.setUser(TEST_USER, TEST_PASSWORD)

        val reZero = SeriesCover(13975, "Re:Zero kara Hajimeru Isekai Seikatsu")
        val reZeroDb = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.WATCHLIST, 12345678)
        val reZeroOnline = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.FINISHED, 12345678)

        // local entry does exist on first call, but is removed on the second try
        whenever(mySeriesDb.getSeries(any()))
                .thenReturn(Observable.just(reZeroDb))
                .thenReturn(Observable.error(MySeriesDb.NoSeriesEntryException()))

        // overriding the local list is always successful
        whenever(mySeriesDb.overrideWithSeriesList(any())).thenReturn(Observable.just(Unit))

        // list request success, but the series entry was deleted online before the last sync
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Abfrage erfolgreich", "[]"))
        // create entry response
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("COM_PROXER_SUCCESS"))
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Abfrage erfolgreich", getListReZero(UserListSeriesEntry.STATE_USER_BOOKMARKED)))

        // the comment state is bookmarked and does need to be updated
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Erfolgreich bearbeitet!"))

        repository.moveSeriesToList(reZero, SeriesList.FINISHED).subscribeAssert {
            assertNoErrors()
        }

        // the local db should get updated with the latest data + our changes
        verify(mySeriesDb).overrideWithSeriesList(eq(listOf(reZeroOnline)))
    }

    @Test
    fun testRemoveSeriesFromListOffline() {
        userSettings.clearUser()
        whenever(mySeriesDb.removeSeries(any())).thenReturn(Observable.just(Unit))

        repository.removeSeriesFromList(123).subscribeAssert {
            assertNoErrors()
        }

        verify(mySeriesDb).removeSeries(eq(123))
    }

    @Test
    fun testRemoveSeriesFromListOnline() {
        val reZeroDb = SeriesDbEntry(13975, "Re:Zero kara Hajimeru Isekai Seikatsu", SeriesList.WATCHLIST, 12345678)
        userSettings.setUser(TEST_USER, TEST_PASSWORD)

        whenever(mySeriesDb.getSeries(any())).thenReturn(Observable.just(reZeroDb))
        whenever(mySeriesDb.removeSeries(any())).thenReturn(Observable.just(Unit))
        mockServer.enqueue(ApiResponses.getSuccessFullResponse("Eintrag gelöscht"))

        repository.removeSeriesFromList(123).subscribeAssert {
            assertNoErrors()
        }

        assertEquals(1, mockServer.requestCount)
        verify(mySeriesDb).removeSeries(eq(123))

        // check for query parameters
        val request = mockServer.takeRequest()
        assertTrue(request.path.contains("format=json&json=delete"))
    }

    private fun getListReZero(state: Int): String {
        return """
                [{
                    "count": "26",
                    "medium": "animeseries",
                    "estate": "1",
                    "cid": "12345678",
                    "name": "Re:Zero kara Hajimeru Isekai Seikatsu",
                    "id": "13975",
                    "comment": "",
                    "state": "$state",
                    "episode": "26",
                    "data": "{\"genre\":\"1\",\"story\":\"1\",\"animation\":\"2\",\"characters\":\"1\",\"music\":\"1\"}",
                    "rating": "0",
                    "timestamp": "1475871721"
                }]
                """
                .trimIndent()
    }

    companion object {
        private const val TEST_USER = "mockUser"
        private const val TEST_PASSWORD = "mockPassword"
        private const val TEST_TOKEN = "testToken"
    }
}