package im.bigs.pg.api.payment

import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryResponse
import im.bigs.pg.api.payment.dto.Summary
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/** 결제 API 컨트롤러. Swagger 문서는 [PaymentControllerDocs] 인터페이스에서 관리합니다. */
@RestController
@RequestMapping("/api/v1/payments")
@Validated
class PaymentController(
    private val paymentUseCase: PaymentUseCase,
    private val queryPaymentsUseCase: QueryPaymentsUseCase,
) : PaymentControllerDocs {

    @PostMapping
    override fun create(@RequestBody req: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val saved =
            paymentUseCase.pay(
                PaymentCommand(
                    partnerId = req.partnerId,
                    amount = req.amount,
                    cardBin = req.cardBin,
                    cardLast4 = req.cardLast4,
                    productName = req.productName,
                ),
            )
        return ResponseEntity.ok(PaymentResponse.from(saved))
    }

    @GetMapping
    override fun query(
        @RequestParam(required = false) partnerId: Long?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        to: LocalDateTime?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<QueryResponse> {
        val res =
            queryPaymentsUseCase.query(
                QueryFilter(partnerId, status, from, to, cursor, limit),
            )
        return ResponseEntity.ok(
            QueryResponse(
                items = res.items.map { PaymentResponse.from(it) },
                summary =
                Summary(
                    res.summary.count,
                    res.summary.totalAmount,
                    res.summary.totalNetAmount
                ),
                nextCursor = res.nextCursor,
                hasNext = res.hasNext,
            ),
        )
    }
}
