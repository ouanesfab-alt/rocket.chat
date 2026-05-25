package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.remote.api.RocketChatApi
import com.example.data.remote.websocket.RocketChatWebSocket
import com.example.data.repository.*
import com.example.domain.repository.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    val retrofit: Retrofit by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl("https://open.rocket.chat/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: RocketChatApi by lazy {
        retrofit.create(RocketChatApi::class.java)
    }

    val webSocket: RocketChatWebSocket by lazy {
        RocketChatWebSocket(okHttpClient)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(context, api, database)
    }

    val messageRepository: MessageRepository by lazy {
        MessageRepositoryImpl(database, api)
    }

    val channelRepository: ChannelRepository by lazy {
        ChannelRepositoryImpl(database)
    }

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(database)
    }
}
