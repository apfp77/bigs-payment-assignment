package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service

/**
 * 결제 생성 유스케이스 구현체.
 *
 * 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다. 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 *
 * @property partnerRepository 제휴사 조회 포트
 * @property feePolicyRepository 수수료 정책 조회 포트
 * @property paymentRepository 결제 저장 포트
 * @property pgClients 외부 PG 클라이언트 목록
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgClients: List<PgClientOutPort>,
) : PaymentUseCase {

    /**
     * 결제를 생성합니다.
     *
     * 1. 제휴사 검증 (존재 여부, 활성 상태)
     * 2. 수수료 정책 조회 (effective_from 기준 최신)
     * 3. PG 승인 요청
     * 4. 수수료 계산 (정책 기반)
     * 5. 결제 저장
     *
     * @param command 결제 생성 커맨드
     * @return 저장된 결제 정보
     * @throws PartnerNotFoundException 제휴사를 찾을 수 없는 경우
     * @throws PartnerInactiveException 제휴사가 비활성 상태인 경우
     * @throws FeePolicyNotFoundException 수수료 정책이 없는 경우
     * @throws PgClientNotFoundException PG 클라이언트를 찾을 수 없는 경우
     */
    override fun pay(command: PaymentCommand): Payment {
        // 1. 제휴사 검증
        val partner =
            partnerRepository.findById(command.partnerId)
                ?: throw PartnerNotFoundException(command.partnerId)
        if (!partner.active) {
            throw PartnerInactiveException(partner.id)
        }

        // 2. 수수료 정책 조회
        val policy =
            feePolicyRepository.findEffectivePolicy(partner.id)
                ?: throw FeePolicyNotFoundException(partner.id)

        // 3. PG 승인
        val pgClient =
            pgClients.firstOrNull { it.supports(partner.id) }
                ?: throw PgClientNotFoundException(partner.id)
        val approve =
            pgClient.approve(
                PgApproveRequest(
                    partnerId = partner.id,
                    amount = command.amount,
                    cardBin = command.cardBin,
                    cardLast4 = command.cardLast4,
                    productName = command.productName,
                ),
            )

        // 4. 수수료 계산 (정책 기반)
        val (fee, net) =
            FeeCalculator.calculateFee(
                command.amount,
                policy.percentage,
                policy.fixedFee,
            )

        // 5. 결제 저장
        val payment =
            Payment(
                partnerId = partner.id,
                amount = command.amount,
                appliedFeeRate = policy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                approvalCode = approve.approvalCode,
                approvedAt = approve.approvedAt,
                status = PaymentStatus.APPROVED,
            )

        return paymentRepository.save(payment)
    }
}
