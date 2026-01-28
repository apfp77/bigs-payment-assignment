package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.math.BigDecimal

@Schema(description = "결제 생성 요청")
data class CreatePaymentRequest(
    @get:Schema(description = "제휴사 ID", example = "1", required = true) val partnerId: Long,
    @get:Schema(description = "결제 금액 (1 이상)", example = "10000", required = true)
    @field:Min(1)
    val amount: BigDecimal,
    @get:Schema(description = "PG별 카드 데이터 (partnerId에 따라 type 결정)", required = true)
    val pgCardData: PgCardData,
)
