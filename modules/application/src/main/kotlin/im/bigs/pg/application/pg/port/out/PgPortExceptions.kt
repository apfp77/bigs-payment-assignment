package im.bigs.pg.application.pg.port.out

/**
 * PG에서 결제를 거절한 경우 발생하는 예외. 잔액 부족, 도난 카드, 한도 초과 등의 사유로 결제가 거부됨.
 *
 * @property errorCode PG 에러 코드 (예: INSUFFICIENT_LIMIT)
 * @property message 에러 메시지
 * @property referenceId PG 참조 ID
 */
class PgRejectedException(
    val errorCode: String,
    override val message: String,
    val referenceId: String? = null,
) : RuntimeException(message)

/** PG 인증 실패 시 발생하는 예외. API-KEY 누락, 포맷 오류, 미등록 등. */
class PgAuthenticationException(
    override val message: String = "PG authentication failed",
) : RuntimeException(message)

/** PG 서버 오류 시 발생하는 예외. 네트워크 장애, 서버 다운 등. */
class PgServerException(
    override val message: String = "PG server error",
    override val cause: Throwable? = null,
) : RuntimeException(message, cause)
