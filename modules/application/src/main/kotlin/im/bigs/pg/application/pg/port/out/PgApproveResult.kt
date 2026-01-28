package im.bigs.pg.application.pg.port.out

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/**
 * PG 승인 결과.
 *
 * @property approvalCode PG로부터 받은 승인 코드
 * @property approvedAt 승인 시각
 * @property status 결제 상태 (기본: APPROVED)
 * @property cardBin 카드 BIN (앞 6자리), 없으면 null
 * @property cardLast4 카드 번호 마지막 4자리, 없으면 null
 */
data class PgApproveResult(
    val approvalCode: String,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") val approvedAt: LocalDateTime,
    val status: PaymentStatus = PaymentStatus.APPROVED,
    val cardBin: String? = null,
    val cardLast4: String? = null,
)
