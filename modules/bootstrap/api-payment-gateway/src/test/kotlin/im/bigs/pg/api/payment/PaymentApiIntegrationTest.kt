package im.bigs.pg.api.payment

import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.MockPgCardData
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryResponse
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import im.bigs.pg.infra.persistence.payment.repository.PaymentJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 결제 API 통합 테스트.
 * - @SpringBootTest로 전체 컨텍스트 로드
 * - H2 인메모리 DB 사용
 * - MockPgClient 활용 (partnerId=1)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PaymentApiIntegrationTest {

    @Autowired lateinit var restTemplate: TestRestTemplate

    @Autowired lateinit var paymentRepository: PaymentJpaRepository

    @Autowired lateinit var partnerRepository: PartnerJpaRepository

    @Autowired lateinit var feePolicyRepository: FeePolicyJpaRepository

    private var testPartnerId: Long = 1L

    @BeforeEach
    fun setup() {
        // 기존 결제 데이터만 삭제
        paymentRepository.deleteAll()

        // 테스트용 제휴사 설정 (partnerId=1 for MockPgClient)
        if (partnerRepository.findById(1L).isEmpty) {
            val partner =
                partnerRepository.save(
                    PartnerEntity(code = "TEST1", name = "Test Partner 1", active = true)
                )
            feePolicyRepository.save(
                FeePolicyEntity(
                    partnerId = partner.id!!,
                    effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                    percentage = BigDecimal("0.0235"),
                    fixedFee = BigDecimal.ZERO,
                )
            )
            testPartnerId = partner.id!!
        } else {
            testPartnerId = 1L
        }
    }

    private val mockCardData =
        MockPgCardData(cardBin = "123456", cardLast4 = "4242", productName = "테스트 상품")

    // ==================== 결제 생성 테스트 ====================

    @Test
    @Order(1)
    @DisplayName("결제 생성이 성공해야 한다")
    fun `결제 생성이 성공해야 한다`() {
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = BigDecimal("10000"),
                pgCardData = mockCardData,
            )

        val response =
            restTemplate.postForEntity(
                "/api/v1/payments",
                request,
                PaymentResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        response.body!!.let {
            assertNotNull(it.id)
            assertEquals(testPartnerId, it.partnerId)
            assertEquals(BigDecimal("10000"), it.amount)
            assertEquals("4242", it.cardLast4)
            assertEquals("APPROVED", it.status.name)
        }
    }

    @Test
    @Order(2)
    @DisplayName("잘못된 cardBin 형식은 400 에러를 반환해야 한다")
    fun `잘못된 cardBin 형식은 400 에러를 반환해야 한다`() {
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = BigDecimal("10000"),
                pgCardData =
                MockPgCardData(cardBin = "12345", cardLast4 = "4242"), // 5자리 - 잘못됨
            )

        val response =
            restTemplate.postForEntity(
                "/api/v1/payments",
                request,
                Map::class.java,
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("VALIDATION_FAILED", (response.body as Map<*, *>)["code"])
    }

    @Test
    @Order(2)
    @DisplayName("결제 생성 시 수수료 정책이 올바르게 적용되어야 한다")
    fun `결제 생성 시 수수료 정책이 올바르게 적용되어야 한다`() {
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = BigDecimal("10000"),
                pgCardData = MockPgCardData(cardBin = "123456", cardLast4 = "1111"),
            )

        val response =
            restTemplate.postForEntity(
                "/api/v1/payments",
                request,
                PaymentResponse::class.java,
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            // 2.35% 정책 적용 확인 (compareTo로 스케일 무시)
            assertEquals(
                BigDecimal("0.0235").compareTo(it.appliedFeeRate),
                0,
                "appliedFeeRate expected 0.0235 but was ${it.appliedFeeRate}"
            )
            assertEquals(
                BigDecimal("235").compareTo(it.feeAmount),
                0,
                "feeAmount expected 235 but was ${it.feeAmount}"
            )
            assertEquals(
                BigDecimal("9765").compareTo(it.netAmount),
                0,
                "netAmount expected 9765 but was ${it.netAmount}"
            )
        }
    }

    @Test
    @Order(3)
    @DisplayName("결제 생성 후 DB에 정확히 저장되어야 한다")
    fun `결제 생성 후 DB에 정확히 저장되어야 한다`() {
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = BigDecimal("5000"),
                pgCardData = MockPgCardData(cardBin = "123456", cardLast4 = "9999"),
            )

        val response =
            restTemplate.postForEntity(
                "/api/v1/payments",
                request,
                PaymentResponse::class.java,
            )

        val savedPayment = paymentRepository.findById(response.body!!.id!!).orElse(null)
        assertNotNull(savedPayment)
        assertEquals(testPartnerId, savedPayment.partnerId)
        assertEquals(BigDecimal("5000"), savedPayment.amount)
        assertEquals("9999", savedPayment.cardLast4)
    }

    // ==================== 결제 조회 테스트 ====================

    @Test
    @Order(4)
    @DisplayName("결제 조회 시 전체 목록을 반환해야 한다")
    fun `결제 조회 시 전체 목록을 반환해야 한다`() {
        // Given: 3건 생성
        repeat(3) { createPayment() }

        // When
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(3, response.body!!.items.size)
        assertEquals(3L, response.body!!.summary.count)
    }

    @Test
    @Order(5)
    @DisplayName("조회 시 summary가 items와 동일한 집합을 집계해야 한다")
    fun `조회 시 summary가 items와 동일한 집합을 집계해야 한다`() {
        // Given: 10건 생성 (각 1000원)
        repeat(10) { createPayment(amount = BigDecimal("1000")) }

        // When: limit=5로 조회
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?limit=5",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            assertEquals(5, it.items.size) // 5건만 반환
            assertEquals(10L, it.summary.count) // 전체 10건 집계
            assertEquals(BigDecimal("10000"), it.summary.totalAmount)
            assertTrue(it.hasNext)
            assertNotNull(it.nextCursor)
        }
    }

    @Test
    @Order(6)
    @DisplayName("커서 페이지네이션으로 모든 데이터를 순회할 수 있어야 한다")
    fun `커서 페이지네이션으로 모든 데이터를 순회할 수 있어야 한다`() {
        // Given: 15건 생성
        repeat(15) { createPayment() }

        val allItems = mutableListOf<PaymentResponse>()
        var cursor: String? = null
        var pageCount = 0

        // When: limit=5로 순회
        do {
            val url =
                if (cursor == null) {
                    "/api/v1/payments?limit=5"
                } else {
                    "/api/v1/payments?limit=5&cursor=$cursor"
                }

            val response = restTemplate.getForEntity(url, QueryResponse::class.java)
            val body = response.body!!

            allItems.addAll(body.items)
            cursor = body.nextCursor
            pageCount++
        } while (body.hasNext)

        // Then
        assertEquals(3, pageCount) // 3페이지
        assertEquals(15, allItems.size) // 전체 15건
        assertEquals(15, allItems.map { it.id }.distinct().size) // 중복 없음
    }

    @Test
    @Order(7)
    @DisplayName("마지막 페이지에서 hasNext가 false여야 한다")
    fun `마지막 페이지에서 hasNext가 false여야 한다`() {
        // Given: 3건 생성
        repeat(3) { createPayment() }

        // When: limit=10로 조회 (전체보다 큼)
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?limit=10",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            assertEquals(3, it.items.size)
            assertFalse(it.hasNext)
            assertNull(it.nextCursor)
        }
    }

    @Test
    @Order(8)
    @DisplayName("partnerId 필터로 해당 제휴사만 조회해야 한다")
    fun `partnerId 필터로 해당 제휴사만 조회해야 한다`() {
        // Given: 5건 생성
        repeat(5) { createPayment() }

        // When
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?partnerId=$testPartnerId",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(5, response.body!!.items.size)
        response.body!!.items.forEach { assertEquals(testPartnerId, it.partnerId) }
    }

    @Test
    @Order(10)
    @DisplayName("status 필터로 APPROVED 상태만 조회해야 한다")
    fun `status 필터로 APPROVED 상태만 조회해야 한다`() {
        // Given: 결제 생성 (모두 APPROVED)
        repeat(3) { createPayment() }

        // When
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?status=APPROVED",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(3, response.body!!.items.size)
        response.body!!.items.forEach { assertEquals("APPROVED", it.status.name) }
    }

    @Test
    @Order(11)
    @DisplayName("정렬 순서가 createdAt desc, id desc여야 한다")
    fun `정렬 순서가 createdAt desc, id desc여야 한다`() {
        // Given: 여러 건 생성
        repeat(5) { createPayment() }

        // When
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?limit=10",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        val items = response.body!!.items
        for (i in 0 until items.size - 1) {
            // createdAt이 같거나 더 큰지 확인 (desc)
            assertTrue(
                items[i].createdAt!! >= items[i + 1].createdAt!!,
                "createdAt은 내림차순이어야 함"
            )
            // createdAt이 같으면 id로 비교
            if (items[i].createdAt == items[i + 1].createdAt) {
                assertTrue(
                    items[i].id!! > items[i + 1].id!!,
                    "같은 시간일 때 ID는 내림차순이어야 함"
                )
            }
        }
    }

    @Test
    @Order(12)
    @DisplayName("복합 필터로 조회해야 한다")
    fun `복합 필터로 조회해야 한다`() {
        // Given: 결제 생성
        repeat(3) { createPayment() }

        // When: partnerId + status 복합 필터
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?partnerId=$testPartnerId&status=APPROVED",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(3, response.body!!.items.size)
        assertEquals(3L, response.body!!.summary.count)
    }

    @Test
    @Order(13)
    @DisplayName("빈 결과 조회 시 빈 목록과 0 통계를 반환해야 한다")
    fun `빈 결과 조회 시 빈 목록과 0 통계를 반환해야 한다`() {
        // Given: 아무 데이터도 없음 (BeforeEach에서 삭제됨)

        // When: 존재하지 않는 partnerId로 조회
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?partnerId=99999",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            assertEquals(0, it.items.size)
            assertEquals(0L, it.summary.count)
            assertEquals(BigDecimal.ZERO, it.summary.totalAmount)
            assertEquals(BigDecimal.ZERO, it.summary.totalNetAmount)
            assertFalse(it.hasNext)
            assertNull(it.nextCursor)
        }
    }

    @Test
    @Order(14)
    @DisplayName("limit=1로 조회 시 한 건만 반환해야 한다")
    fun `limit=1로 조회 시 한 건만 반환해야 한다`() {
        // Given: 5건 생성
        repeat(5) { createPayment() }

        // When
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?limit=1",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            assertEquals(1, it.items.size)
            assertEquals(5L, it.summary.count) // 전체 통계
            assertTrue(it.hasNext)
            assertNotNull(it.nextCursor)
        }
    }

    @Test
    @Order(15)
    @DisplayName("summary는 필터와 동일한 조건으로 집계되어야 한다")
    fun `summary는 필터와 동일한 조건으로 집계되어야 한다`() {
        // Given: 5건 생성 (각 2000원)
        repeat(5) { createPayment(amount = BigDecimal("2000")) }

        // When: partnerId 필터 + limit=2
        val response =
            restTemplate.getForEntity(
                "/api/v1/payments?partnerId=$testPartnerId&limit=2",
                QueryResponse::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        response.body!!.let {
            assertEquals(2, it.items.size) // 2건만 반환
            assertEquals(5L, it.summary.count) // 전체 5건 집계
            assertEquals(BigDecimal("10000"), it.summary.totalAmount) // 2000 * 5
            assertTrue(it.hasNext)
        }
    }

    // ==================== E2E 시나리오 테스트 ====================

    @Test
    @Order(9)
    @DisplayName("결제 생성 후 바로 조회할 수 있어야 한다")
    fun `결제 생성 후 바로 조회할 수 있어야 한다`() {
        // Given: 결제 생성
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = BigDecimal("7777"),
                pgCardData = MockPgCardData(cardBin = "123456", cardLast4 = "5555"),
            )
        val createResponse =
            restTemplate.postForEntity(
                "/api/v1/payments",
                request,
                PaymentResponse::class.java,
            )
        val createdId = createResponse.body!!.id

        // When: 조회
        val queryResponse =
            restTemplate.getForEntity(
                "/api/v1/payments",
                QueryResponse::class.java,
            )

        // Then: 생성한 결제가 목록에 포함
        val found = queryResponse.body!!.items.any { it.id == createdId }
        assertTrue(found, "생성한 결제가 조회 결과에 포함되어야 함")
    }

    // ==================== 헬퍼 함수 ====================

    private fun createPayment(amount: BigDecimal = BigDecimal("1000")): PaymentResponse {
        val request =
            CreatePaymentRequest(
                partnerId = testPartnerId,
                amount = amount,
                pgCardData = MockPgCardData(cardBin = "123456", cardLast4 = "0000"),
            )
        return restTemplate.postForEntity(
            "/api/v1/payments",
            request,
            PaymentResponse::class.java,
        )
            .body!!
    }
}
