package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.NewPgCardDataDto
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.random.Random

/** NewPG 연동 클라이언트. partnerId=3을 지원합니다. 토큰 기반 결제를 시뮬레이션합니다. */
@Component
class NewPgClient : PgClientOutPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(partnerId: Long): Boolean = partnerId == 3L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        log.info("NewPg request: partnerId={}, amount={}", request.partnerId, request.amount)

        val cardData =
            request.pgCardData as? NewPgCardDataDto
                ?: error("NewPgCardDataDto is required for NewPg")

        // 토큰 기반 결제 시뮬레이션 (실제 구현에서는 외부 API 호출)
        log.info(
            "NewPg processing: merchantId={}, orderId={}",
            cardData.merchantId,
            cardData.orderId
        )

        val approvalCode = "NEW${Random.nextInt(100000, 999999)}"
        return PgApproveResult(
            approvalCode = approvalCode,
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED,
            // 토큰 기반 결제이므로 cardBin/cardLast4 없음
            cardBin = null,
            cardLast4 = null,
        )
    }
}
