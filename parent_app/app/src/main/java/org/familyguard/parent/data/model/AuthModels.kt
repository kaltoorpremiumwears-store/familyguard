package org.familyguard.parent.data.model

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val hasConsentedToParentalTerms: Boolean = true
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val message: String,
    val user: UserDto,
    val accessToken: String,
    val tokenType: String,
    val expiresIn: String
)

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val parentId: String?,
    val familyCode: String?
)
