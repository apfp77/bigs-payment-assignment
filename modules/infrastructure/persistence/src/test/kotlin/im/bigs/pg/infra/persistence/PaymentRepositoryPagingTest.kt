package im.bigs.pg.infra.persistence

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.payment.entity.PaymentEntity
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class])
@DisplayName("결제저장소커서페이징Test")
class PaymentRepositoryPagingTest
@Autowired
constructor(
    val paymentRepo: PaymentJpaRepository,
) {

    @BeforeEach
    fun setup() {
        paymentRepo.deleteAll()
    }

    @Test
    @DisplayName("커서 페이징과 통계가 일관되어야 한다")
    fun `커서 페이징과 통계가 일관되어야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")
        repeat(35) { i ->
            paymentRepo.save(
                PaymentEntity(
                    partnerId = 1L,
                    amount = BigDecimal("1000"),
                    appliedFeeRate = BigDecimal("0.0300"),
                    feeAmount = BigDecimal("30"),
                    netAmount = BigDecimal("970"),
                    cardBin = null,
                    cardLast4 = "%04d".format(i),
                    approvalCode = "A$i",
                    approvedAt = baseTs.plusSeconds(i.toLong()),
                    status = "APPROVED",
                    createdAt = baseTs.plusSeconds(i.toLong()),
                    updatedAt = baseTs.plusSeconds(i.toLong()),
                ),
            )
        }

        val first =
            paymentRepo.pageBy(1L, "APPROVED", null, null, null, null, PageRequest.of(0, 21))
        assertEquals(21, first.size)
        val lastOfFirst = first[20]
        val second =
            paymentRepo.pageBy(
                1L,
                "APPROVED",
                null,
                null,
                lastOfFirst.createdAt,
                lastOfFirst.id,
                PageRequest.of(0, 21),
            )
        assertTrue(second.isNotEmpty())

        val sumList = paymentRepo.summary(1L, "APPROVED", null, null)
        val row = sumList.first()
        assertEquals(35L, (row[0] as Number).toLong())
        assertEquals(BigDecimal("35000"), row[1] as BigDecimal)
        assertEquals(BigDecimal("33950"), row[2] as BigDecimal)
    }

    @Test
    @DisplayName("기간 필터로 해당 기간 데이터만 조회해야 한다")
    fun `기간 필터로 해당 기간 데이터만 조회해야 한다`() {
        val baseTs = Instant.parse("2024-01-15T00:00:00Z")

        // 1월 1일 ~ 1월 31일에 걸쳐 데이터 생성
        listOf(
            Instant.parse("2024-01-01T00:00:00Z"), // 범위 밖 (이전)
            Instant.parse("2024-01-10T00:00:00Z"), // 범위 내
            Instant.parse("2024-01-15T00:00:00Z"), // 범위 내 (경계)
            Instant.parse("2024-01-20T00:00:00Z"), // 범위 내
            Instant.parse("2024-02-01T00:00:00Z"), // 범위 밖 (이후)
        ).forEachIndexed { i, ts ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "%04d".format(i),
                    createdAt = ts,
                )
            )
        }

        val fromAt = Instant.parse("2024-01-10T00:00:00Z")
        val toAt = Instant.parse("2024-01-25T00:00:00Z")

        // 페이징 조회
        val result = paymentRepo.pageBy(
            null, null, fromAt, toAt, null, null, PageRequest.of(0, 10)
        )

        assertEquals(3, result.size) // 1/10, 1/15, 1/20 세 건만

        // 통계 조회
        val sumList = paymentRepo.summary(null, null, fromAt, toAt)
        val row = sumList.first()
        assertEquals(3L, (row[0] as Number).toLong())
    }

    @Test
    @DisplayName("partnerId 필터로 해당 제휴사 데이터만 조회해야 한다")
    fun `partnerId 필터로 해당 제휴사 데이터만 조회해야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")

        // 파트너 1: 3건, 파트너 2: 2건
        repeat(3) { i ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "1$i",
                    createdAt = baseTs.plusSeconds(i.toLong()),
                )
            )
        }
        repeat(2) { i ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 2L,
                    cardLast4 = "2$i",
                    createdAt = baseTs.plusSeconds((i + 10).toLong()),
                )
            )
        }

        // 파트너 1만 조회
        val partner1Result = paymentRepo.pageBy(
            1L, null, null, null, null, null, PageRequest.of(0, 10)
        )
        assertEquals(3, partner1Result.size)

        // 파트너 1 통계
        val partner1Sum = paymentRepo.summary(1L, null, null, null)
        assertEquals(3L, (partner1Sum.first()[0] as Number).toLong())

        // 파트너 2만 조회
        val partner2Result = paymentRepo.pageBy(
            2L, null, null, null, null, null, PageRequest.of(0, 10)
        )
        assertEquals(2, partner2Result.size)

        // 파트너 2 통계
        val partner2Sum = paymentRepo.summary(2L, null, null, null)
        assertEquals(2L, (partner2Sum.first()[0] as Number).toLong())
    }

    @Test
    @DisplayName("정렬 순서가 createdAt desc, id desc여야 한다")
    fun `정렬 순서가 createdAt desc, id desc여야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")

        // 동일 시간에 여러 건 생성
        repeat(5) { i ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "%04d".format(i),
                    createdAt = baseTs, // 모두 같은 시간
                )
            )
        }

        val result = paymentRepo.pageBy(
            null, null, null, null, null, null, PageRequest.of(0, 10)
        )

        // ID 내림차순 확인
        for (i in 0 until result.size - 1) {
            assertTrue(
                result[i].id!! > result[i + 1].id!!,
                "ID는 내림차순이어야 함: ${result[i].id} > ${result[i + 1].id}"
            )
        }
    }

    @Test
    @DisplayName("다른 시간대 데이터는 createdAt desc로 정렬되어야 한다")
    fun `다른 시간대 데이터는 createdAt desc로 정렬되어야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")

        listOf(0L, 100L, 50L, 200L, 150L).forEachIndexed { i, offset ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "%04d".format(i),
                    createdAt = baseTs.plusSeconds(offset),
                )
            )
        }

        val result = paymentRepo.pageBy(
            null, null, null, null, null, null, PageRequest.of(0, 10)
        )

        // createdAt 내림차순 확인
        for (i in 0 until result.size - 1) {
            assertTrue(
                result[i].createdAt!! >= result[i + 1].createdAt!!,
                "createdAt은 내림차순이어야 함"
            )
        }
    }

    @Test
    @DisplayName("status 필터로 해당 상태 데이터만 조회해야 한다")
    fun `status 필터로 해당 상태 데이터만 조회해야 한다`() {
        val baseTs = Instant.parse("2024-01-01T00:00:00Z")

        // APPROVED 3건
        repeat(3) { i ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "A$i",
                    createdAt = baseTs.plusSeconds(i.toLong()),
                    status = "APPROVED",
                )
            )
        }
        // CANCELED 2건
        repeat(2) { i ->
            paymentRepo.save(
                createPaymentEntity(
                    partnerId = 1L,
                    cardLast4 = "C$i",
                    createdAt = baseTs.plusSeconds((i + 10).toLong()),
                    status = "CANCELED",
                )
            )
        }

        // APPROVED만 조회
        val approvedResult = paymentRepo.pageBy(
            null, "APPROVED", null, null, null, null, PageRequest.of(0, 10)
        )
        assertEquals(3, approvedResult.size)

        // CANCELED만 조회
        val canceledResult = paymentRepo.pageBy(
            null, "CANCELED", null, null, null, null, PageRequest.of(0, 10)
        )
        assertEquals(2, canceledResult.size)
    }

    @Test
    @DisplayName("복합 필터로 조회해야 한다")
    fun `복합 필터로 조회해야 한다`() {
        val baseTs = Instant.parse("2024-01-15T00:00:00Z")

        // 파트너 1, APPROVED, 1/15
        paymentRepo.save(
            createPaymentEntity(1L, "1111", baseTs, "APPROVED")
        )
        // 파트너 1, CANCELED, 1/15
        paymentRepo.save(
            createPaymentEntity(1L, "2222", baseTs.plusSeconds(1), "CANCELED")
        )
        // 파트너 2, APPROVED, 1/15
        paymentRepo.save(
            createPaymentEntity(2L, "3333", baseTs.plusSeconds(2), "APPROVED")
        )
        // 파트너 1, APPROVED, 1/1 (범위 밖)
        paymentRepo.save(
            createPaymentEntity(
                1L, "4444",
                Instant.parse("2024-01-01T00:00:00Z"),
                "APPROVED"
            )
        )

        val fromAt = Instant.parse("2024-01-10T00:00:00Z")
        val toAt = Instant.parse("2024-01-20T00:00:00Z")

        // 파트너 1 + APPROVED + 기간 내
        val result = paymentRepo.pageBy(
            1L, "APPROVED", fromAt, toAt, null, null, PageRequest.of(0, 10)
        )

        assertEquals(1, result.size)
        assertEquals("1111", result.first().cardLast4)

        // 통계도 동일 필터
        val sumList = paymentRepo.summary(1L, "APPROVED", fromAt, toAt)
        assertEquals(1L, (sumList.first()[0] as Number).toLong())
    }

    @Test
    @DisplayName("빈 결과일 때 빈 목록과 0 통계를 반환해야 한다")
    fun `빈 결과일 때 빈 목록과 0 통계를 반환해야 한다`() {
        // 아무 데이터도 없음
        val result = paymentRepo.pageBy(
            999L, null, null, null, null, null, PageRequest.of(0, 10)
        )
        assertEquals(0, result.size)

        val sumList = paymentRepo.summary(999L, null, null, null)
        val row = sumList.first()
        assertEquals(0L, (row[0] as Number).toLong())
        assertEquals(BigDecimal.ZERO.compareTo(row[1] as BigDecimal), 0)
        assertEquals(BigDecimal.ZERO.compareTo(row[2] as BigDecimal), 0)
    }

    // ==================== 헬퍼 함수 ====================

    private fun createPaymentEntity(
        partnerId: Long,
        cardLast4: String,
        createdAt: Instant,
        status: String = "APPROVED",
    ) = PaymentEntity(
        partnerId = partnerId,
        amount = BigDecimal("1000"),
        appliedFeeRate = BigDecimal("0.0300"),
        feeAmount = BigDecimal("30"),
        netAmount = BigDecimal("970"),
        cardBin = null,
        cardLast4 = cardLast4,
        approvalCode = "A$cardLast4",
        approvedAt = createdAt,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
