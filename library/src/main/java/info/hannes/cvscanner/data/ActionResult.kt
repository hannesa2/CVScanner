package info.hannes.cvscanner.data

sealed class ActionResult {
    data class Success(val paymentMethod: PaymentMethod) : ActionResult()
    data class Pending(val paymentMethod: PaymentMethod) : ActionResult()
    object PinAuthenticationNeeded : ActionResult()
    object ActionDenied : ActionResult()
}

class PaymentMethod {

}
