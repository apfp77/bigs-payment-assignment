package im.bigs.pg.api.config

import im.bigs.pg.application.pg.port.out.PgAuthenticationException
import im.bigs.pg.application.pg.port.out.PgRejectedException
import im.bigs.pg.application.pg.port.out.PgServerException
import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.InvalidPgCardDataException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PaymentException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
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
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.errorCode, e.message))

    // ========== PG External Exceptions ==========

    /** PG에서 결제를 거절한 경우 (422). 한도 초과, 분실 카드 등. */
    @ExceptionHandler(PgRejectedException::class)
    fun handle(e: PgRejectedException): ResponseEntity<ErrorResponse> {
        val msg = if (e.referenceId != null) "${e.message} (ref: ${e.referenceId})" else e.message
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse(e.errorCode, msg))
    }

    /** PG 인증 실패 (500). API-KEY 설정 오류 등 서버 설정 문제. */
    @ExceptionHandler(PgAuthenticationException::class)
    fun handle(e: PgAuthenticationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("PG_AUTH_FAILED", e.message))

    /** PG 서버 오류 (502). 외부 PG 서버 장애. */
    @ExceptionHandler(PgServerException::class)
    fun handle(e: PgServerException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse("PG_SERVER_ERROR", e.message))

    // ========== Validation Exceptions ==========

    /** 요청 데이터 검증 실패 (400). 필드 검증 오류 메시지 반환. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message =
            e.bindingResult.fieldErrors.joinToString("; ") {
                "${it.field}: ${it.defaultMessage}"
            }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("VALIDATION_FAILED", message))
    }

    // ========== Fallback ==========

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
