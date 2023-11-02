package com.wafflestudio.seminar.spring2023._web.log

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

interface AlertSlowResponse {
    operator fun invoke(slowResponse: SlowResponse): Future<Boolean>
}

data class SlowResponse(
    val method: String,
    val path: String,
    val duration: Long,
)

/**
 * 스펙:
 *  1. 3초 이상 걸린 응답을 "[API-RESPONSE] GET /api/v1/playlists/7, took 3132ms, PFCJeong" 꼴로 로깅 (logging level은 warn)
 *  2. 3초 이상 걸린 응답을 "[API-RESPONSE] GET /api/v1/playlists/7, took 3132ms, PFCJeong" 꼴로 슬랙 채널에 전달 (http method, path, 걸린 시간, 본인의 깃허브 아이디)
 *  3. RestTemplate을 사용하여 아래와 같이 요청을 날린다.
 *      curl --location 'https://slack.com/api/chat.postMessage' \
 *           --header 'Authorization: Bearer $slackToken' \
 *           --header 'Content-Type: application/json' \
 *           --data '{ "text":"[API-RESPONSE] GET /api/v1/playlists/7, took 3132ms, PFCJeong", "channel": "#spring-assignment-channel"}'
 *  4. 위 요청의 응답은 "{ "ok": true }"로 온다. invoke 함수는 이 "ok" 응답 값을 반환.
 *  5. 슬랙 API의 성공 여부와 상관 없이, 우리 서버의 응답은 정상적으로 내려가야 한다.
 */
@Component
class AlertSlowResponseImpl(
    @Value("\${slacktoken}")
    private val token: String,
    @Value("\${github}")
    private val githubId: String,

    ) : AlertSlowResponse {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    private val url = "https://slack.com/api/chat.postMessage"

    override operator fun invoke(slowResponse: SlowResponse): Future<Boolean> {
        val msg = "[API-RESPONSE] ${slowResponse.method} ${slowResponse.path}, took ${slowResponse.duration}ms, $githubId"

        logger.warn(msg)

        return CompletableFuture.supplyAsync {
            var body = HashMap<String, String>()
            body["text"] = msg
            body["channel"] = "#spring-assignment-channel"

            var headers = HttpHeaders()
            headers.setBearerAuth(token)
            headers.contentType = MediaType.APPLICATION_JSON

            val request = HttpEntity(body, headers)

            // 슬랙 채널에 RESPONSE 날리기
            val postForEntity = restTemplate.postForEntity(
                url, request, SlackResponse::class.java
            )
            postForEntity.body?.ok?:false
        }
    }
}

data class SlackResponse(
    val ok: Boolean
)
