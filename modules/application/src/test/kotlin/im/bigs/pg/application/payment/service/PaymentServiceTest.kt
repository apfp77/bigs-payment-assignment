package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

class 결제서비스Test {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgClient =
            object : PgClientOutPort {
                override fun supports(partnerId: Long) = true
                override fun approve(request: PgApproveRequest) =
                        PgApproveResult(
                                "APPROVAL-123",
                                LocalDateTime.of(2024, 1, 1, 0, 0),
                                PaymentStatus.APPROVED
                        )
            }

    private val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

    // ==================== 예외 테스트 ====================

    @Test
    @DisplayName("존재하지 않는 제휴사 ID로 결제 시 PartnerNotFoundException이 발생해야 한다")
    fun `존재하지 않는 제휴사 ID로 결제 시 PartnerNotFoundException이 발생해야 한다`() {
        every { partnerRepo.findById(999L) } returns null

        val exception =
                assertThrows<PartnerNotFoundException> {
                    service.pay(PaymentCommand(partnerId = 999L, amount = BigDecimal("10000")))
                }
        assertEquals("PARTNER_NOT_FOUND", exception.errorCode)
    }

    @Test
    @DisplayName("비활성 제휴사로 결제 시 PartnerInactiveException이 발생해야 한다")
    fun `비활성 제휴사로 결제 시 PartnerInactiveException이 발생해야 한다`() {
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", active = false)

        val exception =
                assertThrows<PartnerInactiveException> {
                    service.pay(PaymentCommand(partnerId = 1L, amount = BigDecimal("10000")))
                }
        assertEquals("PARTNER_INACTIVE", exception.errorCode)
    }

    @Test
    @DisplayName("수수료 정책이 없으면 FeePolicyNotFoundException이 발생해야 한다")
    fun `수수료 정책이 없으면 FeePolicyNotFoundException이 발생해야 한다`() {
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", active = true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns null

        val exception =
                assertThrows<FeePolicyNotFoundException> {
                    service.pay(PaymentCommand(partnerId = 1L, amount = BigDecimal("10000")))
                }
        assertEquals("FEE_POLICY_NOT_FOUND", exception.errorCode)
    }

    // ==================== 수수료 정책 적용 테스트 ====================

    @Test
    @DisplayName("수수료 정책에서 퍼센트만 적용되어야 한다")
    fun `수수료 정책에서 퍼센트만 적용되어야 한다`() {
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns
                FeePolicy(
                        id = 10L,
                        partnerId = 1L,
                        effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
                        percentage = BigDecimal("0.0235"),
                        fixedFee = null
                )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"))
        val res = service.pay(cmd)

        // 10000 * 0.0235 = 235 (HALF_UP)
        assertEquals(BigDecimal("235"), res.feeAmount)
        assertEquals(BigDecimal("9765"), res.netAmount)
        assertEquals(BigDecimal("0.0235"), res.appliedFeeRate)
    }

    @Test
    @DisplayName("수수료 정책에서 퍼센트와 정액 수수료가 함께 적용되어야 한다")
    fun `수수료 정책에서 퍼센트와 정액 수수료가 함께 적용되어야 한다`() {
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns
                FeePolicy(
                        id = 10L,
                        partnerId = 1L,
                        effectiveFrom =
                                LocalDateTime.ofInstant(
                                        Instant.parse("2020-01-01T00:00:00Z"),
                                        ZoneOffset.UTC
                                ),
                        percentage = BigDecimal("0.0300"),
                        fixedFee = BigDecimal("100")
                )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
        val res = service.pay(cmd)

        // 10000 * 0.03 = 300 + 100 = 400
        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(BigDecimal("0.0300"), res.appliedFeeRate)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }

    @Test
    @DisplayName("결제 생성 시 적용된 수수료율이 저장되어야 한다")
    fun `결제 생성 시 적용된 수수료율이 저장되어야 한다`() {
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns
                FeePolicy(
                        id = 10L,
                        partnerId = 1L,
                        effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
                        percentage = BigDecimal("0.0250"),
                        fixedFee = BigDecimal("50")
                )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers
                {
                    savedSlot.captured.copy(id = 100L)
                }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("5000"))
        val res = service.pay(cmd)

        // appliedFeeRate가 정책의 percentage와 일치해야 함
        assertEquals(BigDecimal("0.0250"), res.appliedFeeRate)
        // 5000 * 0.025 = 125 + 50 = 175
        assertEquals(BigDecimal("175"), res.feeAmount)
        assertEquals(BigDecimal("4825"), res.netAmount)
    }
}
