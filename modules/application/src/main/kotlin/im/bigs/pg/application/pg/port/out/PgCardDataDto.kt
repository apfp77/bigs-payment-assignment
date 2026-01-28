package im.bigs.pg.application.pg.port.out

/** Application Layer용 PG별 카드 데이터. */
sealed interface PgCardDataDto

/** MockPG (partnerId=1) 결제용 */
data class MockPgCardDataDto(
    val cardBin: String,
    val cardLast4: String,
    val productName: String?,
) : PgCardDataDto

/** TestPG (partnerId=2) 결제용 (민감정보 - 로깅 금지) */
data class TestPgCardDataDto(
    val cardNumber: String,
    val birthDate: String,
    val expiry: String,
    val cardPassword: String,
) : PgCardDataDto {
    override fun toString() = "TestPgCardDataDto(****)"
}

/** NewPG (partnerId=3) 결제용 */
data class NewPgCardDataDto(
    val encryptedCardToken: String,
    val merchantId: String,
    val orderId: String,
) : PgCardDataDto
