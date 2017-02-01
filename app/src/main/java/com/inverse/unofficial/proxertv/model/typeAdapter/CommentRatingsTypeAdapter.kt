package com.inverse.unofficial.proxertv.model.typeAdapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.inverse.unofficial.proxertv.model.CommentRatings
import java.io.StringReader

/**
 * Adapter that reads the values from the nested Object/Array inside the string.
 */
class CommentRatingsTypeAdapter : TypeAdapter<CommentRatings>() {

    override fun read(input: JsonReader): CommentRatings {
        val arrayString = input.nextString()
        val reader = JsonReader(StringReader(arrayString))

        var genre: Int? = null
        var story: Int? = null
        var animation: Int? = null
        var characters: Int? = null
        var music: Int? = null

        val isArray = reader.peek() == JsonToken.BEGIN_ARRAY

        if (isArray) {
            reader.beginArray()
        } else {
            reader.beginObject()
        }

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "genre" -> genre = reader.nextInt()
                "story" -> story = reader.nextInt()
                "animation" -> animation = reader.nextInt()
                "characters" -> characters = reader.nextInt()
                "music" -> music = reader.nextInt()
            }
        }

        if (isArray) {
            reader.endArray()
        } else {
            reader.endObject()
        }

        return CommentRatings(genre, story, animation, characters, music)
    }

    override fun write(out: JsonWriter?, value: CommentRatings?) {
        throw UnsupportedOperationException("not implemented")
    }

}