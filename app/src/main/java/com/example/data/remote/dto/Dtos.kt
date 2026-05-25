package com.example.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val user: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val status: String,
    val data: LoginData?
)

@JsonClass(generateAdapter = true)
data class LoginData(
    val authToken: String,
    val userId: String,
    val me: UserDto?
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val _id: String,
    val username: String,
    val name: String,
    val status: String?,
    val emails: List<EmailDto>?
)

@JsonClass(generateAdapter = true)
data class EmailDto(
    val address: String,
    val verified: Boolean
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val name: String,
    val email: String,
    val username: String,
    val pass: String
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val success: Boolean,
    val user: UserDto?
)

@JsonClass(generateAdapter = true)
data class SendMessageDto(
    val rid: String,
    val msg: String
)
