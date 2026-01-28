package im.bigs.pg.api.config

import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.InvalidPgCardDataException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PaymentException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

/** 전역 예외 처리기. 도메인 예외를 HTTP 응답으로 변환합니다. */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PartnerNotFoundException::class)
    fun handle(e: PartnerNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.errorCode, e.message))

    @ExceptionHandler(PartnerInactiveException::class)
    fun handle(e: PartnerInactiveException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.errorCode, e.message))

    @ExceptionHandler(InvalidPgCardDataException::class)
    fun handle(e: InvalidPgCardDataException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.errorCode, e.message))

    @ExceptionHandler(FeePolicyNotFoundException::class)
    fun handle(e: FeePolicyNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(e.errorCode, e.message))

    @ExceptionHandler(PgClientNotFoundException::class)
    fun handle(e: PgClientNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(e.errorCode, e.message))

    /** 기타 PaymentException 처리 (fallback) */
    @ExceptionHandler(PaymentException::class)
    fun handle(e: PaymentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(e.errorCode, e.message))
}

/**
 * 에러 응답 DTO.
 *
 * @property code 에러 식별 코드
 * @property message 에러 상세 메시지
 * @property timestamp 에러 발생 시각
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Instant = Instant.now(),
)
