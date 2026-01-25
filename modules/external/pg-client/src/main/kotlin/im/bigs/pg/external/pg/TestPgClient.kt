package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.external.pg.config.TestPgProperties
import im.bigs.pg.external.pg.crypto.AesGcmCrypto
import im.bigs.pg.external.pg.dto.TestPgErrorResponse
import im.bigs.pg.external.pg.dto.TestPgPayload
import im.bigs.pg.external.pg.dto.TestPgSuccessResponse
import im.bigs.pg.external.pg.exception.PgAuthenticationException
import im.bigs.pg.external.pg.exception.PgRejectedException
import im.bigs.pg.external.pg.exception.PgServerException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

/**
 * TestPg API 연동 클라이언트. 짝수 partnerId를 지원합니다 (홀수는 MockPgClient가 처리).
 *
 * - AES-256-GCM 암호화로 요청 본문 보호
 * - 민감정보 로깅 금지
 */
@Component
@EnableConfigurationProperties(TestPgProperties::class)
@ConditionalOnProperty(prefix = "pg.test", name = ["api-key"])
class TestPgClient(
        private val props: TestPgProperties,
        private val restTemplate: RestTemplate,
        private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) : PgClientOutPort {

  private val log = LoggerFactory.getLogger(javaClass)
  private val key = AesGcmCrypto.deriveKey(props.apiKey)
  private val iv = AesGcmCrypto.decodeIv(props.iv)

  override fun supports(partnerId: Long): Boolean = partnerId % 2L == 0L

  override fun approve(request: PgApproveRequest): PgApproveResult {
    log.info("TestPg request: partnerId={}, amount={}", request.partnerId, request.amount)

    // 1. 평문 페이로드 생성 (테스트용 고정 카드정보 사용)
    val payload =
            TestPgPayload(
                    cardNumber = "1111-1111-1111-1111",
                    birthDate = "19900101",
                    expiry = "1227",
                    password = "12",
                    amount = request.amount.toLong(),
            )

    // 2. 암호화 (민감정보 로깅 금지)
    val plainJson = objectMapper.writeValueAsString(payload)
    val enc = AesGcmCrypto.encrypt(plainJson, key, iv)

    // 3. HTTP 요청
    val headers =
            HttpHeaders().apply {
              contentType = MediaType.APPLICATION_JSON
              set("API-KEY", props.apiKey)
            }
    val body = mapOf("enc" to enc)
    val entity = HttpEntity(body, headers)

    return try {
      val response =
              restTemplate.postForObject(
                      "${props.baseUrl}/api/v1/pay/credit-card",
                      entity,
                      TestPgSuccessResponse::class.java,
              )!!

      log.info("TestPg success: approvalCode={}", response.approvalCode)

      PgApproveResult(
              approvalCode = response.approvalCode,
              approvedAt =
                      LocalDateTime.parse(
                              response.approvedAt,
                              DateTimeFormatter.ISO_LOCAL_DATE_TIME
                      ),
              status = PaymentStatus.APPROVED,
      )
    } catch (e: HttpClientErrorException) {
      handleClientError(e)
    } catch (e: HttpServerErrorException) {
      log.error("TestPg server error: {}", e.statusCode)
      throw PgServerException("PG server error: ${e.statusCode}", e)
    }
  }

  private fun handleClientError(e: HttpClientErrorException): Nothing {
    when (e.statusCode) {
      HttpStatus.UNAUTHORIZED -> {
        log.warn("TestPg authentication failed")
        throw PgAuthenticationException("API-KEY authentication failed")
      }
      HttpStatus.UNPROCESSABLE_ENTITY -> {
        val errorBody =
                objectMapper.readValue(e.responseBodyAsString, TestPgErrorResponse::class.java)
        log.warn(
                "TestPg rejected: errorCode={}, message={}",
                errorBody.errorCode,
                errorBody.message
        )
        throw PgRejectedException(
                errorCode = errorBody.errorCode,
                message = errorBody.message,
                referenceId = errorBody.referenceId,
        )
      }
      else -> {
        log.error("TestPg unexpected error: {}", e.statusCode)
        throw PgServerException("Unexpected error: ${e.statusCode}", e)
      }
    }
  }
}
