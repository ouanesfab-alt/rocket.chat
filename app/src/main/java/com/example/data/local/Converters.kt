package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.Attachment
import com.example.domain.model.Reaction
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val reactionsListType = Types.newParameterizedType(List::class.java, Reaction::class.java)
    private val reactionsAdapter: JsonAdapter<List<Reaction>> = moshi.adapter(reactionsListType)
    private val attachmentAdapter: JsonAdapter<Attachment> = moshi.adapter(Attachment::class.java)

    @TypeConverter
    fun fromReactionsList(reactions: List<Reaction>?): String {
        return reactionsAdapter.toJson(reactions ?: emptyList())
    }

    @TypeConverter
    fun toReactionsList(json: String?): List<Reaction> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            reactionsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromAttachment(attachment: Attachment?): String? {
        if (attachment == null) return null
        return attachmentAdapter.toJson(attachment)
    }

    @TypeConverter
    fun toAttachment(json: String?): Attachment? {
        if (json.isNullOrEmpty()) return null
        return try {
            attachmentAdapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
