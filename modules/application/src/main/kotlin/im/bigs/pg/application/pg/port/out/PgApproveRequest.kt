package im.bigs.pg.application.pg.port.out

import java.math.BigDecimal

/** PG 승인 요청 정보. */
data class PgApproveRequest(
    val partnerId: Long,
    val amount: BigDecimal,
    val pgCardData: PgCardDataDto,
)
