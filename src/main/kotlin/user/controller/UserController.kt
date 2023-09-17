package com.wafflestudio.seminar.spring2023.user.controller

import com.wafflestudio.seminar.spring2023.user.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController(
    private val userService: UserService,
) {

    @PostMapping("/api/v1/signup")
    fun signup(
        @RequestBody request: SignUpRequest,
    ): ResponseEntity<Unit> {
        try {
            userService.signUp(request.username, request.password, request.image)
        } catch (e:SignUpBadUsernameException) {
            return ResponseEntity(Unit, HttpStatus.BAD_REQUEST)
        } catch (e:SignUpBadPasswordException) {
            return ResponseEntity(Unit, HttpStatus.BAD_REQUEST)
        } catch (e:SignUpUsernameConflictException) {
            return ResponseEntity(Unit, HttpStatusCode.valueOf(409))
        }
        return ResponseEntity(Unit, HttpStatus.OK)
    }

    @PostMapping("/api/v1/signin")
    fun signIn(
        @RequestBody request: SignInRequest,
    ): ResponseEntity<SignInResponse> {
        return try {
            val user = userService.signIn(request.username, request.password)
            ResponseEntity.ok(SignInResponse(user.getAccessToken()))
        } catch(e:SignInUserNotFoundException) {
            ResponseEntity(SignInResponse("user-not-found"), HttpStatus.NOT_FOUND)
        } catch(e:SignInInvalidPasswordException) {
            ResponseEntity(SignInResponse("invalid-password"), HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/api/v1/users/me")
    fun me(
        @RequestHeader(name = "Authorization", required = false) authorizationHeader: String?,
    ): ResponseEntity<UserMeResponse> {
        if ((authorizationHeader == null) || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity(UserMeResponse("", ""), HttpStatus.UNAUTHORIZED)
        }

        val token = authorizationHeader.substring(7)

        return try {
            val user = userService.authenticate(token)
            ResponseEntity.ok(UserMeResponse(user.username, user.image))
        } catch(e:AuthenticateException) {
            ResponseEntity(UserMeResponse("", ""), HttpStatus.UNAUTHORIZED)
        }
    }
}

data class UserMeResponse(
    val username: String,
    val image: String,
)

data class SignUpRequest(
    val username: String,
    val password: String,
    val image: String,
)

data class SignInRequest(
    val username: String,
    val password: String,
)

data class SignInResponse(
    val accessToken: String,
)
