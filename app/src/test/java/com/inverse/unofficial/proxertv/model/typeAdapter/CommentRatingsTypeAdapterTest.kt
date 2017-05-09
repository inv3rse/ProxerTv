package com.inverse.unofficial.proxertv.model.typeAdapter

import com.google.gson.stream.JsonReader
import com.inverse.unofficial.proxertv.model.CommentRatings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader

/**
 * Test cases for the [CommentRatingsTypeAdapter]
 */
class CommentRatingsTypeAdapterTest {

    private val adapter = CommentRatingsTypeAdapter()

    @Test
    fun testEmptyString() {
        val ratings = readString("\"\"")
        assertEquals(EMPTY_RATINGS, ratings)
    }

    @Test
    fun testEmptyArray() {
        val ratings = readString("\"[]\"")
        assertEquals(EMPTY_RATINGS, ratings)
    }

    @Test
    fun testEmptyObject() {
        val ratings = readString("\"{}\"")
        assertEquals(EMPTY_RATINGS, ratings)
    }

    @Test
    fun testCompleteObject() {
        // triple " to keep the escape characters
        val ratings = readString(""""{\"genre\":\"1\",\"story\":\"1\",\"animation\":\"2\",\"characters\":\"1\",\"music\":\"1\"}"""")
        assertEquals(CommentRatings(1, 1, 2, 1, 1), ratings)
    }

    private fun readString(input: String): CommentRatings {
        return adapter.read(JsonReader(StringReader(input)))
    }

    companion object {
        val EMPTY_RATINGS = CommentRatings(null, null, null, null, null)
    }
}