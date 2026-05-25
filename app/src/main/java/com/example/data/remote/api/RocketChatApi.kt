package com.example.data.remote.api

import com.example.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface RocketChatApi {

    @POST("api/v1/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("api/v1/users.register")
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponse

    @POST("api/v1/chat.sendMessage")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Header("X-User-Id") userId: String,
        @Body message: SendMessageDto
    ): Any
}
