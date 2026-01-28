package im.bigs.pg.api.payment.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema

/** PG별 결제용 카드 데이터 (저장/로깅 금지). type 필드로 서브타입 구분. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = MockPgCardData::class, name = "MOCK"),
    JsonSubTypes.Type(value = TestPgCardData::class, name = "TEST_PG"),
    JsonSubTypes.Type(value = NewPgCardData::class, name = "NEW_PG"),
)
@Schema(
    description = "PG별 결제용 카드 데이터",
    // oneOf = [MockPgCardData::class, TestPgCardData::class, NewPgCardData::class],
    discriminatorProperty = "type",
    discriminatorMapping =
    [
        DiscriminatorMapping(value = "MOCK", schema = MockPgCardData::class),
        DiscriminatorMapping(
            value = "TEST_PG",
            schema = TestPgCardData::class
        ),
        DiscriminatorMapping(
            value = "NEW_PG",
            schema = NewPgCardData::class
        ),
    ],
)
sealed interface PgCardData

/** MockPG (partnerId=1) 결제용 카드 정보. */
@Schema(description = "MockPG 결제용 카드 정보")
data class MockPgCardData(
    @get:Schema(description = "구분자", example = "MOCK", allowableValues = ["MOCK"]) val type: String = "MOCK",
    @get:Schema(description = "카드 BIN (앞 6자리)", example = "123456") val cardBin: String,
    @get:Schema(description = "카드 번호 마지막 4자리", example = "4242") val cardLast4: String,
    @get:Schema(description = "상품명", example = "테스트 상품") val productName: String? = null,
) : PgCardData

/** TestPG (partnerId=2) 결제용 카드 정보 (저장/로깅 금지). */
@Schema(description = "TestPG 결제용 카드 정보")
data class TestPgCardData(
    @get:Schema(description = "구분자", example = "TEST_PG", allowableValues = ["TEST_PG"]) val type: String = "TEST_PG",
    @get:Schema(description = "카드번호", example = "1111-1111-1111-1111") val cardNumber: String,
    @get:Schema(description = "생년월일 (YYYYMMDD)", example = "19900101") val birthDate: String,
    @get:Schema(description = "유효기간 (MMYY)", example = "1227") val expiry: String,
    @get:Schema(description = "카드 비밀번호 앞 2자리", example = "12") val cardPassword: String,
) : PgCardData {
    override fun toString() = "TestPgCardData(****)"
}

/** NewPG (partnerId=3) 결제용 토큰 정보. */
@Schema(description = "NewPG 결제용 토큰 정보")
data class NewPgCardData(
    @get:Schema(description = "구분자", example = "NEW_PG", allowableValues = ["NEW_PG"]) val type: String = "NEW_PG",

    @get:Schema(description = "암호화된 카드 토큰", example = "enc_token_xxx")
    val encryptedCardToken: String,
    @get:Schema(description = "가맹점 ID", example = "M001") val merchantId: String,
    @get:Schema(description = "주문 ID", example = "ORD-001") val orderId: String,
) : PgCardData
