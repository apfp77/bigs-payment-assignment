package im.bigs.pg.api.payment

import im.bigs.pg.api.config.ErrorResponse
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

/** 결제 API 문서 인터페이스. */
@Tag(name = "Payment", description = "결제 생성 및 조회 API")
interface PaymentControllerDocs {

    /**
     * 결제를 생성합니다.
     *
     * 제휴사 ID와 결제 금액을 받아 PG 승인을 요청하고, 수수료 정책을 적용하여 결제를 저장합니다.
     *
     * @param req 결제 생성 요청 (제휴사ID, 금액, 카드정보 등)
     * @return 생성된 결제 정보
     * @throws PartnerNotFoundException 제휴사가 존재하지 않는 경우 (404)
     * @throws PartnerInactiveException 제휴사가 비활성 상태인 경우 (400)
     * @throws FeePolicyNotFoundException 수수료 정책이 없는 경우 (500)
     * @throws PgClientNotFoundException 적합한 PG 클라이언트가 없는 경우 (500)
     * @throws PgRejectedException PG에서 결제를 거절한 경우 (422)
     */
    @Operation(
        summary = "결제 생성",
        description =
        """
결제를 생성합니다.

## pgCardData 조건
| partnerId | PG | type | 필드 |
|-----------|-----|------|------|
| 1 | MockPG | MOCK | cardBin, cardLast4, productName |
| 2 | TestPG | TEST_PG | cardNumber, birthDate, expiry, cardPassword |
| 3 | NewPG | NEW_PG | encryptedCardToken, merchantId, orderId |
"""
    )
    @ApiResponses(
        value =
        [
            ApiResponse(
                responseCode = "200",
                description = "결제 생성 성공",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            PaymentResponse::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 - PartnerInactiveException: 비활성 제휴사",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            ErrorResponse::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "404",
                description = "리소스 없음 - PartnerNotFoundException: 제휴사를 찾을 수 없음",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            ErrorResponse::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "422",
                description = "처리 불가 - PgRejectedException: PG 승인 거부",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            ErrorResponse::class
                        )
                    )
                ]
            ),
            ApiResponse(
                responseCode = "500",
                description =
                "서버 오류 - FeePolicyNotFoundException, PgClientNotFoundException 등",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            ErrorResponse::class
                        )
                    )
                ]
            ),
        ]
    )
    @RequestBody(
        required = true,
        content =
        [
            Content(
                mediaType = "application/json",
                schema = Schema(implementation = CreatePaymentRequest::class),
            )
        ]
    )
    fun create(@Valid req: CreatePaymentRequest): ResponseEntity<PaymentResponse>

    /**
     * 결제 이력을 조회합니다.
     *
     * 커서 기반 페이지네이션과 필터링을 지원하며, 전체 통계(summary)를 함께 반환합니다.
     *
     * @param partnerId 제휴사 ID 필터 (선택)
     * @param status 결제 상태 필터 (선택, APPROVED/CANCELED 등)
     * @param from 조회 시작 시각 (선택, yyyy-MM-dd HH:mm:ss)
     * @param to 조회 종료 시각 (선택, yyyy-MM-dd HH:mm:ss)
     * @param cursor 다음 페이지 커서 (선택, 이전 응답의 nextCursor)
     * @param limit 페이지 크기 (기본값 20)
     * @return 결제 목록, 통계, 페이지 정보
     */
    @Operation(
        summary = "결제 조회",
        description = "결제 이력을 조회합니다. 커서 기반 페이지네이션과 필터링을 지원하며, 전체 통계(summary)를 함께 반환합니다."
    )
    @ApiResponses(
        value =
        [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                [
                    Content(
                        mediaType = "application/json",
                        schema =
                        Schema(
                            implementation =
                            QueryResponse::class
                        )
                    )
                ]
            ),
        ]
    )
    fun query(
        @Parameter(description = "제휴사 ID 필터") partnerId: Long?,
        @Parameter(description = "결제 상태 필터 (APPROVED, CANCELED 등)") status: String?,
        @Parameter(description = "조회 시작 시각 (yyyy-MM-dd HH:mm:ss)") from: LocalDateTime?,
        @Parameter(description = "조회 종료 시각 (yyyy-MM-dd HH:mm:ss)") to: LocalDateTime?,
        @Parameter(description = "다음 페이지 커서 (이전 응답의 nextCursor 값)") cursor: String?,
        @Parameter(description = "페이지 크기", example = "20") limit: Int,
    ): ResponseEntity<QueryResponse>
}
