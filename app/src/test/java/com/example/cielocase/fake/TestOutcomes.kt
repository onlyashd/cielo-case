package com.example.cielocase.fake

import com.example.cielocase.domain.model.PaymentFailure
import com.example.cielocase.domain.model.PaymentFailureKind
import com.example.cielocase.domain.model.PaymentRecord
import com.example.cielocase.domain.payment.PaymentOutcome

fun approved(
    reference: String?,
    amountInCents: Long,
    authCode: String = "123456",
) = PaymentOutcome.Approved(
    record = PaymentRecord(
        orderId = "order-1",
        paymentId = "payment-1",
        authCode = authCode,
        cieloCode = "999999",
        brand = "Visa",
        maskedCard = "************0000",
        productName = "CREDITO A VISTA - I",
        installments = 0,
        amountInCents = amountInCents,
        terminal = "69000007",
        paidAtEpochMillis = 1_700_000_000_000,
    ),
    reference = reference,
)

fun rejected(
    kind: PaymentFailureKind,
    reference: String? = null,
    code: Int? = null,
    reason: String? = null,
) = PaymentOutcome.Rejected(
    failure = PaymentFailure(kind = kind, code = code, reason = reason),
    reference = reference,
)
