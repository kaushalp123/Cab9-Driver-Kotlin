package com.cab9.driver.network


class MissingArgumentException(msg: String) : Exception(msg)

class ApiException(val msg: String) : Exception()

class NoInternetException : Exception()

/**
 * Throw this exception when cab9 access token is not available.
 */
class InvalidAccessTokenException : Exception()

class InvalidFirebaseTokenException : Exception()

class JobBidExpiredException : Exception()

class NoLocationFoundException : Exception()

class LocationUpdateSetupException(cause: Throwable) : Exception(cause)

class RefreshAuthTokenException : Exception {
    constructor(msg: String) : super(msg)
    constructor(ex: Throwable) : super(ex)
}

class WebViewException(msg: String) : Exception(msg)

class BookingOfferException : Exception {
    constructor(msg: String) : super(msg)
    constructor(ex: Throwable) : super(ex)
}

class BookingOfferReadException(ex: Throwable) : Exception(ex)

class TransactionInfoException(msg: String) : Exception(msg)

class SumUpException : Exception {
    constructor(msg: String) : super(msg)
    constructor(msg: String, ex: Throwable) : super(msg, ex)
}

class RemoteConfigException : Exception {
    constructor(cause: Throwable) : super(cause)

    constructor(msg: String, ex: Throwable) : super(msg, ex)
}

class InvalidBidItemException : Exception {
    constructor(msg: String) : super(msg)
    constructor(cause: Throwable) : super(cause)
}

class UnsupportedWebViewException : Exception("WebView does not support web listener!")

class SocketConnectionException(cause: Throwable) : Exception(cause)

class FirebaseTokenException(cause: Throwable) : Exception(cause)

class UpdateFirebaseTokenException(cause: Throwable) : Exception(cause)

class AppConfigException(cause: Throwable) : Exception(cause)

class Cab9TokenException(cause: Throwable) : Exception(cause)

class ServiceNotStartedException : Exception {
    constructor(msg: String) : super(msg)
    constructor(cause: Throwable) : super(cause)
}

class ChatConversationAPIException(cause: Throwable) : Exception(cause)
