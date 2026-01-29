package im.bigs.pg.api.payment.dto

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

@Schema(description = "결제 응답")
data class PaymentResponse(
    @get:Schema(description = "결제 ID", example = "1") val id: Long?,
    @get:Schema(description = "제휴사 ID", example = "1") val partnerId: Long,
    @get:Schema(description = "결제 금액", example = "10000") val amount: BigDecimal,
    @get:Schema(description = "적용된 수수료율", example = "0.0235") val appliedFeeRate: BigDecimal,
    @get:Schema(description = "수수료 금액", example = "235") val feeAmount: BigDecimal,
    @get:Schema(description = "정산 금액 (결제 금액 - 수수료)", example = "9765")
    val netAmount: BigDecimal,
    @get:Schema(description = "카드 번호 마지막 4자리", example = "4242") val cardLast4: String?,
    @get:Schema(description = "PG 승인 코드 (실패 시 null)", example = "10080728")
    val approvalCode: String?,
    @get:Schema(description = "PG 승인 시각 (실패 시 null)", example = "2025-01-27 10:00:00")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val approvedAt: LocalDateTime?,
    @get:Schema(description = "결제 상태", example = "APPROVED") val status: PaymentStatus,
    @get:Schema(description = "실패 코드 (실패 시에만)", example = "INSUFFICIENT_LIMIT")
    val failureCode: String?,
    @get:Schema(description = "실패 메시지 (실패 시에만)", example = "한도가 초과되었습니다.")
    val failureMessage: String?,
    @get:Schema(description = "실패 시각 (실패 시에만)", example = "2025-01-27 10:00:00")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val failedAt: LocalDateTime?,
    @get:Schema(description = "생성 시각", example = "2025-01-27 10:00:00")
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(p: Payment) =
            PaymentResponse(
                id = p.id,
                partnerId = p.partnerId,
                amount = p.amount,
                appliedFeeRate = p.appliedFeeRate,
                feeAmount = p.feeAmount,
                netAmount = p.netAmount,
                cardLast4 = p.cardLast4,
                approvalCode = p.approvalCode,
                approvedAt = p.approvedAt,
                status = p.status,
                failureCode = p.failureCode,
                failureMessage = p.failureMessage,
                failedAt = p.failedAt,
                createdAt = p.createdAt,
            )
    }
}
