package im.bigs.pg.api.config

import im.bigs.pg.domain.exception.FeePolicyNotFoundException
import im.bigs.pg.domain.exception.PartnerInactiveException
import im.bigs.pg.domain.exception.PartnerNotFoundException
import im.bigs.pg.domain.exception.PgClientNotFoundException
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

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
  @DisplayName("PgClientNotFoundException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다")
  fun `PgClientNotFoundException은 500 INTERNAL_SERVER_ERROR를 반환해야 한다`() {
    val exception = PgClientNotFoundException(1L)
    val response = handler.handle(exception)

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    assertEquals("PG_CLIENT_NOT_FOUND", response.body?.code)
  }
}
