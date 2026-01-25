package im.bigs.pg.domain.exception

/**
 * 결제 도메인 예외 기반 클래스.
 *
 * @property errorCode 에러 식별 코드
 * @property message 에러 상세 메시지
 */
sealed class PaymentException(
        val errorCode: String,
        override val message: String,
) : RuntimeException(message)

/** 제휴사를 찾을 수 없음. */
class PartnerNotFoundException(partnerId: Long) :
        PaymentException(
                errorCode = "PARTNER_NOT_FOUND",
                message = "Partner not found: $partnerId"
        )

/** 제휴사가 비활성 상태. */
class PartnerInactiveException(partnerId: Long) :
        PaymentException(
                errorCode = "PARTNER_INACTIVE",
                message = "Partner is inactive: $partnerId"
        )

/** 수수료 정책을 찾을 수 없음. */
class FeePolicyNotFoundException(partnerId: Long) :
        PaymentException(
                errorCode = "FEE_POLICY_NOT_FOUND",
                message = "No fee policy found for partner: $partnerId"
        )

/** PG 클라이언트를 찾을 수 없음. */
class PgClientNotFoundException(partnerId: Long) :
        PaymentException(
                errorCode = "PG_CLIENT_NOT_FOUND",
                message = "No PG client for partner: $partnerId"
        )
