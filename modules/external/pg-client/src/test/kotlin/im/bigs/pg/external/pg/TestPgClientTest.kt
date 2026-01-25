package im.bigs.pg.external.pg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.config.TestPgProperties
import im.bigs.pg.external.pg.exception.PgAuthenticationException
import im.bigs.pg.external.pg.exception.PgRejectedException
import im.bigs.pg.external.pg.exception.PgServerException
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestTemplate

class TestPgClientTest {

  private lateinit var wireMockServer: WireMockServer
  private lateinit var client: TestPgClient

  private val testApiKey = "11111111-1111-4111-8111-111111111111"
  private val testIv = "AAAAAAAAAAAAAAAA"

  @BeforeEach
  fun setup() {
    wireMockServer = WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort())
    wireMockServer.start()

    val props =
            TestPgProperties(
                    baseUrl = "http://localhost:${wireMockServer.port()}",
                    apiKey = testApiKey,
                    iv = testIv,
            )
    client = TestPgClient(props, RestTemplate(), jacksonObjectMapper())
  }

  @AfterEach
  fun teardown() {
    wireMockServer.stop()
  }

  @Test
  @DisplayName("짝수 partnerId만 지원해야 한다")
  fun `짝수 partnerId만 지원해야 한다`() {
    assertEquals(false, client.supports(1L))
    assertEquals(true, client.supports(2L))
    assertEquals(false, client.supports(3L))
    assertEquals(true, client.supports(100L))
  }

  @Test
  @DisplayName("성공 카드로 승인 요청 시 APPROVED 반환")
  fun `성공 카드로 승인 요청 시 APPROVED 반환`() {
    wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/pay/credit-card"))
                    .withHeader("API-KEY", equalTo(testApiKey))
                    .willReturn(
                            aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(
                                            """
                            {
                                "approvalCode": "10080728",
                                "approvedAt": "2025-01-01T12:00:00",
                                "maskedCardLast4": "1111",
                                "amount": 10000,
                                "status": "APPROVED"
                            }
                        """
                                    )
                    )
    )

    val request =
            PgApproveRequest(
                    partnerId = 2L,
                    amount = BigDecimal("10000"),
                    cardBin = "111111",
                    cardLast4 = "1111",
                    productName = "테스트",
            )

    val result = client.approve(request)

    assertEquals("10080728", result.approvalCode)
    assertEquals(PaymentStatus.APPROVED, result.status)
    assertNotNull(result.approvedAt)
  }

  @Test
  @DisplayName("422 응답 시 PgRejectedException 발생")
  fun `422 응답 시 PgRejectedException 발생`() {
    wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/pay/credit-card"))
                    .willReturn(
                            aResponse()
                                    .withStatus(422)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(
                                            """
                            {
                                "code": 1002,
                                "errorCode": "INSUFFICIENT_LIMIT",
                                "message": "한도가 초과되었습니다.",
                                "referenceId": "ref-123"
                            }
                        """
                                    )
                    )
    )

    val request =
            PgApproveRequest(
                    partnerId = 2L,
                    amount = BigDecimal("10000"),
                    cardBin = "222222",
                    cardLast4 = "2222",
                    productName = null,
            )

    val exception = assertThrows<PgRejectedException> { client.approve(request) }

    assertEquals("INSUFFICIENT_LIMIT", exception.errorCode)
    assertEquals("한도가 초과되었습니다.", exception.message)
    assertEquals("ref-123", exception.referenceId)
  }

  @Test
  @DisplayName("401 응답 시 PgAuthenticationException 발생")
  fun `401 응답 시 PgAuthenticationException 발생`() {
    wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/pay/credit-card")).willReturn(aResponse().withStatus(401))
    )

    val request =
            PgApproveRequest(
                    partnerId = 2L,
                    amount = BigDecimal("10000"),
                    cardBin = null,
                    cardLast4 = null,
                    productName = null,
            )

    assertThrows<PgAuthenticationException> { client.approve(request) }
  }

  @Test
  @DisplayName("5xx 응답 시 PgServerException 발생")
  fun `5xx 응답 시 PgServerException 발생`() {
    wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/pay/credit-card")).willReturn(aResponse().withStatus(500))
    )

    val request =
            PgApproveRequest(
                    partnerId = 2L,
                    amount = BigDecimal("10000"),
                    cardBin = null,
                    cardLast4 = null,
                    productName = null,
            )

    assertThrows<PgServerException> { client.approve(request) }
  }

  @Test
  @DisplayName("요청에 enc 필드가 포함되어야 한다")
  fun `요청에 enc 필드가 포함되어야 한다`() {
    wireMockServer.stubFor(
            post(urlEqualTo("/api/v1/pay/credit-card"))
                    .withRequestBody(matchingJsonPath("$.enc"))
                    .willReturn(
                            aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(
                                            """
                            {
                                "approvalCode": "12345678",
                                "approvedAt": "2025-01-01T12:00:00",
                                "maskedCardLast4": "1111",
                                "amount": 5000,
                                "status": "APPROVED"
                            }
                        """
                                    )
                    )
    )

    val request =
            PgApproveRequest(
                    partnerId = 2L,
                    amount = BigDecimal("5000"),
                    cardBin = null,
                    cardLast4 = null,
                    productName = null,
            )

    val result = client.approve(request)
    assertEquals("12345678", result.approvalCode)

    // enc 필드가 포함된 요청이 전송되었는지 확인
    wireMockServer.verify(
            postRequestedFor(urlEqualTo("/api/v1/pay/credit-card"))
                    .withRequestBody(matchingJsonPath("$.enc"))
    )
  }
}
