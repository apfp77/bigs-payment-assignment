package im.bigs.pg.external.pg.dto

/** TestPg API 요청 페이로드 (평문). 암호화 전 JSON으로 직렬화됩니다. */
data class TestPgPayload(
    val cardNumber: String,
    val birthDate: String,
    val expiry: String,
    val password: String,
    val amount: Long,
)

/** TestPg API 성공 응답 (200). */
data class TestPgSuccessResponse(
    val approvalCode: String,
    val approvedAt: String,
    val maskedCardLast4: String,
    val amount: Long,
    val status: String,
)

/** TestPg API 에러 응답 (422). */
data class TestPgErrorResponse(
    val code: Int,
    val errorCode: String,
    val message: String,
    val referenceId: String? = null,
)
