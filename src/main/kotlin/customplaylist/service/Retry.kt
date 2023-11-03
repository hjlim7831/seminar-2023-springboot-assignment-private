package com.wafflestudio.seminar.spring2023.customplaylist.service

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Component
import java.lang.annotation.ElementType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Retry(
    val value:Int = 20
)

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class RetryAspect {
    @Around("@annotation(retry)")
    fun atTarget(joinPoint: ProceedingJoinPoint, retry:Retry):Any? {
        val maxRetry = retry.value
        var exceptionHolder:Exception = RuntimeException()

        for (cnt in 1..maxRetry) {
            try {
                return joinPoint.proceed()
            } catch (e: ObjectOptimisticLockingFailureException) {
                println("[retry] try count = $cnt/$maxRetry")
                exceptionHolder = e
            }
        }
        throw exceptionHolder
    }

}