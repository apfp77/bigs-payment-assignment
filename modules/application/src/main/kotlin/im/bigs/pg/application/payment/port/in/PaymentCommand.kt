package im.bigs.pg.application.payment.port.`in`

import im.bigs.pg.application.pg.port.out.PgCardDataDto
import java.math.BigDecimal

/**
 * 결제 생성에 필요한 최소 입력.
 *
 * @property partnerId 제휴사 식별자
 * @property amount 결제 금액
 * @property pgCardData PG별 카드 데이터
 */
data class PaymentCommand(
    val partnerId: Long,
    val amount: BigDecimal,
    val pgCardData: PgCardDataDto,
)
