package it.ministerodellasalute.verificaC19sdk

/**
 *
 * This class represents the custom exception, that is thrown when the version of the app using the
 * SDK doesn't match the minimum one. Its constructor accepts an error [message] to be shown when
 * the exception is raised.
 *
 */
class VerificaMinVersionException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}