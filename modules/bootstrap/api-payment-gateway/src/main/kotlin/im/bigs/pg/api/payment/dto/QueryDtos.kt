package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "결제 조회 응답")
data class QueryResponse(
    @get:Schema(description = "결제 목록")
    val items: List<PaymentResponse>,

    @get:Schema(description = "전체 통계 (필터 조건에 해당하는 전체 데이터 기준)")
    val summary: Summary,

    @get:Schema(
        description = "다음 페이지 커서 (다음 페이지가 없으면 null)",
        example = "MjAyNS0wMS0yN1QxMDowMDowMF8x"
    )
    val nextCursor: String?,

    @get:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)

@Schema(description = "결제 통계")
data class Summary(
    @get:Schema(description = "총 건수", example = "100")
    val count: Long,

    @get:Schema(description = "총 결제 금액", example = "1000000")
    val totalAmount: BigDecimal,

    @get:Schema(description = "총 정산 금액", example = "976500")
    val totalNetAmount: BigDecimal,
)
