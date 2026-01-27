package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.math.BigDecimal

@Schema(description = "결제 생성 요청")
data class CreatePaymentRequest(
    @get:Schema(description = "제휴사 ID", example = "1", required = true)
    val partnerId: Long,

    @get:Schema(description = "결제 금액 (1 이상)", example = "10000", required = true)
    @field:Min(1)
    val amount: BigDecimal,

    @get:Schema(description = "카드 BIN (앞 6자리)", example = "123456")
    val cardBin: String? = null,

    @get:Schema(description = "카드 번호 마지막 4자리", example = "4242")
    val cardLast4: String? = null,

    @get:Schema(description = "상품명", example = "테스트 상품")
    val productName: String? = null,
)
