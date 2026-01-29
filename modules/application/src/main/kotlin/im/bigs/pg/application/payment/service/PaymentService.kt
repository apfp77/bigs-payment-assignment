package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.MockPgCardDataDto
import im.bigs.pg.application.pg.port.out.NewPgCardDataDto
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgAuthenticationException
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.application.pg.port.out.PgRejectedException
import im.bigs.pg.application.pg.port.out.PgServerException
import im.bigs.pg.application.pg.port.out.TestPgCardDataDto
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.InvalidPgCardDataException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 결제 생성 유스케이스 구현체.
 *
 * 제휴사 검증, 수수료 정책 조회, PG 승인 요청, 결제 저장까지의 전체 플로우를 처리합니다.
 *
 * @property partnerRepository 제휴사 조회 포트
 * @property feePolicyRepository 수수료 정책 조회 포트
 * @property paymentRepository 결제 저장 포트
 * @property pgClients PG 클라이언트 목록
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
     * 2. 수수료 정책 조회
     * 3. PG 클라이언트 조회
     * 4. pgCardData 타입 검증
     * 5. PG 승인 요청 (cardBin/cardLast4 포함)
     * 6. 수수료 계산
     * 7. 결제 저장
     *
     * @param command 결제 생성 명령 (partnerId, amount, pgCardData)
     * @return PG 승인 성공 후 저장된 결제 엔티티
     * @throws PartnerNotFoundException 제휴사가 존재하지 않는 경우
     * @throws PartnerInactiveException 제휴사가 비활성 상태인 경우
     * @throws FeePolicyNotFoundException 유효한 수수료 정책이 없는 경우
     * @throws PgClientNotFoundException 해당 partnerId를 지원하는 PG 클라이언트가 없는 경우
     * @throws InvalidPgCardDataException pgCardData 타입이 partnerId와 맞지 않는 경우
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

        // 3. PG 클라이언트 조회
        val pgClient =
            pgClients.firstOrNull { it.supports(partner.id) }
                ?: throw PgClientNotFoundException(partner.id)

        // 4. pgCardData 타입 검증
        validatePgCardData(partner.id, command)

        // 5. PG 승인 (실패 시 저장 후 재전파)
        val approve =
            try {
                pgClient.approve(
                    PgApproveRequest(
                        partnerId = partner.id,
                        amount = command.amount,
                        pgCardData = command.pgCardData,
                    ),
                )
            } catch (e: PgRejectedException) {
                // 실패 결제 저장
                val failedPayment =
                    Payment(
                        partnerId = partner.id,
                        amount = command.amount,
                        appliedFeeRate = policy.percentage,
                        feeAmount = BigDecimal.ZERO,
                        netAmount = BigDecimal.ZERO,
                        status = PaymentStatus.REJECTED,
                        failureCode = e.errorCode,
                        failureMessage = e.message,
                        failedAt = LocalDateTime.now(),
                    )
                paymentRepository.save(failedPayment)
                throw e // 422 응답 유지
            } catch (e: PgAuthenticationException) {
                // TODO: 알림 시스템 연동 - API-KEY 설정 오류 등 모니터링 필요
                throw e
            } catch (e: PgServerException) {
                // TODO: 알림 시스템 연동 - PG 서버 장애 모니터링 필요
                throw e
            }

        // 6. 수수료 계산 (정책 기반)
        val (fee, net) =
            FeeCalculator.calculateFee(
                command.amount,
                policy.percentage,
                policy.fixedFee,
            )

        // 7. 결제 저장 (cardBin/cardLast4는 PG 응답에서 가져옴)
        val payment =
            Payment(
                partnerId = partner.id,
                amount = command.amount,
                appliedFeeRate = policy.percentage,
                feeAmount = fee,
                netAmount = net,
                cardBin = approve.cardBin,
                cardLast4 = approve.cardLast4,
                approvalCode = approve.approvalCode,
                approvedAt = approve.approvedAt,
                status = PaymentStatus.APPROVED,
            )

        return paymentRepository.save(payment)
    }

    /**
     * PG별 pgCardData 타입을 검증합니다.
     *
     * @param partnerId 제휴사 ID
     * @param command 결제 명령
     * @throws InvalidPgCardDataException pgCardData 타입이 partnerId와 맞지 않는 경우
     *
     * 알 수 없는 partnerId는 [PgClientNotFoundException] 단계에서 처리되므로 여기서는 검증하지 않습니다.
     */
    private fun validatePgCardData(partnerId: Long, command: PaymentCommand) {
        val (expectedType, isValid) =
            when (partnerId) {
                1L -> "MockPgCardData" to (command.pgCardData is MockPgCardDataDto)
                2L -> "TestPgCardData" to (command.pgCardData is TestPgCardDataDto)
                3L -> "NewPgCardData" to (command.pgCardData is NewPgCardDataDto)
                else -> return // 알 수 없는 partnerId는 PgClientNotFoundException에서 처리
            }

        if (!isValid) {
            throw InvalidPgCardDataException(
                partnerId = partnerId,
                expectedType = expectedType,
                actualType = command.pgCardData::class.simpleName ?: "Unknown"
            )
        }
    }
}
