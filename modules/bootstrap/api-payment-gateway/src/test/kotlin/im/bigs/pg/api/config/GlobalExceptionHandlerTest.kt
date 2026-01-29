package im.bigs.pg.api.config

import im.bigs.pg.application.pg.port.out.PgAuthenticationException
import im.bigs.pg.application.pg.port.out.PgRejectedException
import im.bigs.pg.application.pg.port.out.PgServerException
import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.InvalidPgCardDataException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("PartnerNotFoundException은 404 NOT_FOUND를 반환해야 한다")
    fun `PartnerNotFoundException은 404 NOT_FOUND를 반환해야 한다`() {
        val exception = PartnerNotFoundException(999L)
        val response = handler.handle(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("PARTNER_NOT_FOUND", response.body?.code)
        assertEquals("Partner not found: 999", response.body?.message)
    }

    @Test
    @DisplayName("PartnerInactiveException은 400 BAD_REQUEST를 반환해야 한다")
    fun `PartnerInactiveException은 400 BAD_REQUEST를 반환해야 한다`() {
        val exception = PartnerInactiveException(1L)
        val response = handler.handle(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("PARTNER_INACTIVE", response.body?.code)
    }

    @Test
    @DisplayName("FeePolicyNotFoundException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다")
    fun `FeePolicyNotFoundException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다`() {
        val exception = FeePolicyNotFoundException(1L)
        val response = handler.handle(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("FEE_POLICY_NOT_FOUND", response.body?.code)
    }

    @Test
    @DisplayName("PgClientNotFoundException은 400 BAD_REQUEST를 반환해야 한다")
    fun `PgClientNotFoundException은 400 BAD_REQUEST를 반환해야 한다`() {
        val exception = PgClientNotFoundException(1L)
        val response = handler.handle(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("PG_CLIENT_NOT_FOUND", response.body?.code)
    }

    @Test
    @DisplayName("InvalidPgCardDataException은 400 BAD_REQUEST를 반환해야 한다")
    fun `InvalidPgCardDataException은 400 BAD_REQUEST를 반환해야 한다`() {
        val exception = InvalidPgCardDataException(1L, "MockPgCardData", "TestPgCardData")
        val response = handler.handle(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_PG_CARD_DATA", response.body?.code)
    }

    @Test
    @DisplayName("PgRejectedException은 422 UNPROCESSABLE_ENTITY를 반환해야 한다")
    fun `PgRejectedException은 422 UNPROCESSABLE_ENTITY를 반환해야 한다`() {
        val exception = PgRejectedException("INSUFFICIENT_LIMIT", "한도 초과", "ref-123")
        val response = handler.handle(exception)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals("INSUFFICIENT_LIMIT", response.body?.code)
        assertTrue(response.body?.message?.contains("ref-123") == true)
    }

    @Test
    @DisplayName("PgAuthenticationException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다")
    fun `PgAuthenticationException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다`() {
        val exception = PgAuthenticationException("API KEY 오류")
        val response = handler.handle(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("PG_AUTH_FAILED", response.body?.code)
    }

    @Test
    @DisplayName("PgServerException은 502 BAD_GATEWAY를 반환해야 한다")
    fun `PgServerException은 502 BAD_GATEWAY를 반환해야 한다`() {
        val exception = PgServerException("PG 서버 오류")
        val response = handler.handle(exception)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("PG_SERVER_ERROR", response.body?.code)
    }
}
