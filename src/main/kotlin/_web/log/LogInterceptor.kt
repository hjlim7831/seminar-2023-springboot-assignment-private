package com.wafflestudio.seminar.spring2023._web.log

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception

/**
 * 1. preHandle을 수정하여 logRequest
 * 2. preHandle, afterCompletion을 수정하여 logSlowResponse
 */
@Component
class LogInterceptor(
    private val logRequest: LogRequest,
    private val logSlowResponse: AlertSlowResponse,
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val currentTime = System.currentTimeMillis()
        request.setAttribute("beginTime", currentTime)

        val req = Request(method = request.method, path = request.requestURI)
        logRequest(req)
        return super.preHandle(request, response, handler)
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val currentTime = System.currentTimeMillis()

        val beginTime = request.getAttribute("beginTime") as Long
        val duration = currentTime - beginTime

        if (duration >= 3000) {
            val slowResponse = SlowResponse(
                method = request.method,
                path = request.requestURI,
                duration = duration
            )
            logSlowResponse(slowResponse)
        }

        super.afterCompletion(request, response, handler, ex)
    }
}
