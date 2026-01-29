package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueryPaymentsServiceTest {

    private val paymentRepo = mockk<PaymentOutPort>()
    private val service = QueryPaymentsService(paymentRepo)

    // ==================== 필터 조회 테스트 ====================

    @Test
    @DisplayName("필터 없이 전체 조회 시 모든 결제 내역을 반환해야 한다")
    fun `필터 없이 전체 조회 시 모든 결제 내역을 반환해야 한다`() {
        val payments =
            listOf(
                createPayment(1L, BigDecimal("10000")),
                createPayment(2L, BigDecimal("20000")),
            )
        every { paymentRepo.findBy(any()) } returns
            PaymentPage(
                items = payments,
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(
                count = 2L,
                totalAmount = BigDecimal("30000"),
                totalNetAmount = BigDecimal("29295"),
            )

        val result = service.query(QueryFilter())

        assertEquals(2, result.items.size)
        assertEquals(2L, result.summary.count)
        assertEquals(BigDecimal("30000"), result.summary.totalAmount)
        assertFalse(result.hasNext)
    }

    @Test
    @DisplayName("partnerId 필터로 해당 제휴사만 조회해야 한다")
    fun `partnerId 필터로 해당 제휴사만 조회해야 한다`() {
        every { paymentRepo.findBy(match { it.partnerId == 1L }) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.partnerId == 1L }) } returns
            PaymentSummaryProjection(
                count = 1L,
                totalAmount = BigDecimal("1000"),
                totalNetAmount = BigDecimal("976"),
            )

        val result = service.query(QueryFilter(partnerId = 1L))

        assertEquals(1, result.items.size)
        assertEquals(1L, result.summary.count)
    }

    @Test
    @DisplayName("status 필터로 해당 상태만 조회해야 한다")
    fun `status 필터로 해당 상태만 조회해야 한다`() {
        every { paymentRepo.findBy(match { it.status == PaymentStatus.APPROVED }) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.status == PaymentStatus.APPROVED }) } returns
            PaymentSummaryProjection(
                count = 1L,
                totalAmount = BigDecimal("1000"),
                totalNetAmount = BigDecimal("976"),
            )

        val result = service.query(QueryFilter(status = "APPROVED"))

        assertEquals(1, result.items.size)
    }

    @Test
    @DisplayName("from, to 기간 필터로 해당 기간만 조회해야 한다")
    fun `from, to 기간 필터로 해당 기간만 조회해야 한다`() {
        val from = LocalDateTime.of(2025, 1, 1, 0, 0)
        val to = LocalDateTime.of(2025, 1, 31, 23, 59, 59)

        every { paymentRepo.findBy(match { it.from == from && it.to == to }) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.from == from && it.to == to }) } returns
            PaymentSummaryProjection(
                count = 1L,
                totalAmount = BigDecimal("1000"),
                totalNetAmount = BigDecimal("976"),
            )

        val result = service.query(QueryFilter(from = from, to = to))

        assertEquals(1, result.items.size)
        assertEquals(1L, result.summary.count)
    }

    @Test
    @DisplayName("from만 있는 경우 해당 시점 이후 데이터만 조회해야 한다")
    fun `from만 있는 경우 해당 시점 이후 데이터만 조회해야 한다`() {
        val from = LocalDateTime.of(2025, 1, 15, 0, 0)

        every { paymentRepo.findBy(match { it.from == from && it.to == null }) } returns
            PaymentPage(
                items = listOf(createPayment(1L), createPayment(2L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.from == from && it.to == null }) } returns
            PaymentSummaryProjection(
                count = 2L,
                totalAmount = BigDecimal("2000"),
                totalNetAmount = BigDecimal("1952"),
            )

        val result = service.query(QueryFilter(from = from))

        assertEquals(2, result.items.size)
        assertEquals(2L, result.summary.count)
    }

    @Test
    @DisplayName("to만 있는 경우 해당 시점 이전 데이터만 조회해야 한다")
    fun `to만 있는 경우 해당 시점 이전 데이터만 조회해야 한다`() {
        val to = LocalDateTime.of(2025, 1, 15, 0, 0)

        every { paymentRepo.findBy(match { it.from == null && it.to == to }) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.from == null && it.to == to }) } returns
            PaymentSummaryProjection(
                count = 1L,
                totalAmount = BigDecimal("1000"),
                totalNetAmount = BigDecimal("976"),
            )

        val result = service.query(QueryFilter(to = to))

        assertEquals(1, result.items.size)
        assertEquals(1L, result.summary.count)
    }

    @Test
    @DisplayName("복합 필터 조합으로 조회해야 한다")
    fun `복합 필터 조합으로 조회해야 한다`() {
        val from = LocalDateTime.of(2025, 1, 1, 0, 0)
        val to = LocalDateTime.of(2025, 1, 31, 23, 59, 59)

        every {
            paymentRepo.findBy(
                match {
                    it.partnerId == 1L &&
                        it.status == PaymentStatus.APPROVED &&
                        it.from == from &&
                        it.to == to
                }
            )
        } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every {
            paymentRepo.summary(
                match {
                    it.partnerId == 1L &&
                        it.status == PaymentStatus.APPROVED &&
                        it.from == from &&
                        it.to == to
                }
            )
        } returns
            PaymentSummaryProjection(
                count = 1L,
                totalAmount = BigDecimal("1000"),
                totalNetAmount = BigDecimal("976"),
            )

        val result = service.query(
            QueryFilter(
                partnerId = 1L,
                status = "APPROVED",
                from = from,
                to = to,
            )
        )

        assertEquals(1, result.items.size)
        assertEquals(1L, result.summary.count)
    }

    // ==================== 페이지네이션 테스트 ====================

    @Test
    @DisplayName("커서 없이 첫 페이지 조회 시 최신 데이터를 반환해야 한다")
    fun `커서 없이 첫 페이지 조회 시 최신 데이터를 반환해야 한다`() {
        val payments = (1..10).map { createPayment(it.toLong()) }
        every { paymentRepo.findBy(match { it.cursorId == null }) } returns
            PaymentPage(
                items = payments,
                hasNext = true,
                nextCursorCreatedAt = LocalDateTime.of(2025, 1, 1, 12, 0),
                nextCursorId = 10L,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(35L, BigDecimal("35000"), BigDecimal("33950"))

        val result = service.query(QueryFilter(limit = 10))

        assertEquals(10, result.items.size)
        assertTrue(result.hasNext)
        assertNotNull(result.nextCursor)
    }

    @Test
    @DisplayName("마지막 페이지에서 hasNext가 false여야 한다")
    fun `마지막 페이지에서 hasNext가 false여야 한다`() {
        every { paymentRepo.findBy(any()) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(1L, BigDecimal("1000"), BigDecimal("976"))

        val result = service.query(QueryFilter())

        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    // ==================== 통계 테스트 ====================

    @Test
    @DisplayName("summary는 페이징과 무관하게 전체 집합을 집계해야 한다")
    fun `summary는 페이징과 무관하게 전체 집합을 집계해야 한다`() {
        val payments = (1..10).map { createPayment(it.toLong(), BigDecimal("1000")) }
        every { paymentRepo.findBy(any()) } returns
            PaymentPage(
                items = payments,
                hasNext = true,
                nextCursorCreatedAt = LocalDateTime.now(),
                nextCursorId = 10L,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(
                count = 35L,
                totalAmount = BigDecimal("35000"),
                totalNetAmount = BigDecimal("33950"),
            )

        val result = service.query(QueryFilter(limit = 10))

        assertEquals(10, result.items.size)
        assertEquals(35L, result.summary.count) // 페이징과 무관
        assertTrue(result.hasNext)
    }

    // ==================== 잘못된 입력 처리 테스트 ====================

    @Test
    @DisplayName("잘못된 status 값은 null로 처리해야 한다")
    fun `잘못된 status 값은 null로 처리해야 한다`() {
        every { paymentRepo.findBy(match { it.status == null }) } returns
            PaymentPage(
                items = emptyList(),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(match { it.status == null }) } returns
            PaymentSummaryProjection(0L, BigDecimal.ZERO, BigDecimal.ZERO)

        val result = service.query(QueryFilter(status = "INVALID_STATUS"))

        assertEquals(0, result.items.size)
    }

    @Test
    @DisplayName("잘못된 커서 형식은 첫 페이지로 처리해야 한다")
    fun `잘못된 커서 형식은 첫 페이지로 처리해야 한다`() {
        every {
            paymentRepo.findBy(match { it.cursorId == null && it.cursorCreatedAt == null })
        } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(1L, BigDecimal("1000"), BigDecimal("976"))

        val result = service.query(QueryFilter(cursor = "invalid_cursor_format"))

        assertEquals(1, result.items.size)
    }

    // ==================== 경계값 테스트 ====================

    @Test
    @DisplayName("빈 결과 조회 시 빈 목록과 0 통계를 반환해야 한다")
    fun `빈 결과 조회 시 빈 목록과 0 통계를 반환해야 한다`() {
        every { paymentRepo.findBy(any()) } returns
            PaymentPage(
                items = emptyList(),
                hasNext = false,
                nextCursorCreatedAt = null,
                nextCursorId = null,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(0L, BigDecimal.ZERO, BigDecimal.ZERO)

        val result = service.query(QueryFilter(partnerId = 999L))

        assertEquals(0, result.items.size)
        assertEquals(0L, result.summary.count)
        assertEquals(BigDecimal.ZERO, result.summary.totalAmount)
        assertEquals(BigDecimal.ZERO, result.summary.totalNetAmount)
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("limit=1로 조회 시 한 건만 반환해야 한다")
    fun `limit=1로 조회 시 한 건만 반환해야 한다`() {
        every { paymentRepo.findBy(match { it.limit == 1 }) } returns
            PaymentPage(
                items = listOf(createPayment(1L)),
                hasNext = true,
                nextCursorCreatedAt = LocalDateTime.now(),
                nextCursorId = 1L,
            )
        every { paymentRepo.summary(any()) } returns
            PaymentSummaryProjection(10L, BigDecimal("10000"), BigDecimal("9760"))

        val result = service.query(QueryFilter(limit = 1))

        assertEquals(1, result.items.size)
        assertEquals(10L, result.summary.count)
        assertTrue(result.hasNext)
    }

    // ==================== 헬퍼 함수 ====================

    private fun createPayment(id: Long, amount: BigDecimal = BigDecimal("1000")) =
        Payment(
            id = id,
            partnerId = 1L,
            amount = amount,
            appliedFeeRate = BigDecimal("0.0235"),
            feeAmount = amount.multiply(BigDecimal("0.0235")),
            netAmount = amount.subtract(amount.multiply(BigDecimal("0.0235"))),
            approvalCode = "TEST$id",
            approvedAt = LocalDateTime.now(),
            status = PaymentStatus.APPROVED,
        )
}
