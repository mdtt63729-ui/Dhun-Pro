package dev.brahmkshatriya.echo.extensions.exceptions

import dev.brahmkshatriya.echo.common.Extension
import dev.brahmkshatriya.echo.common.helpers.ClientException
import dev.brahmkshatriya.echo.common.models.Metadata

sealed class AppException : Exception() {

    abstract val extension: Metadata

    open class LoginRequired(
        override val extension: Metadata
    ) : AppException()

    class Unauthorized(
        override val extension: Metadata,
        val userId: String
    ) : LoginRequired(extension)

    class NotSupported(
        override val cause: Throwable,
        override val extension: Metadata,
        val operation: String
    ) : AppException() {
        override val message: String
            get() = "$operation is not supported in ${extension.name}"
    }

    class Other(
        override val cause: Throwable,
        override val extension: Metadata
    ) : AppException() {
        override val message: String
            get() = "${cause.message} error in ${extension.name}"
    }

    companion object {
        fun Throwable.toAppException(extension: Extension<*>) = toAppException(extension.metadata)
        fun Throwable.toAppException(extension: Metadata): AppException = when (this) {
            is ClientException.Unauthorized -> Unauthorized(extension, userId)
            is ClientException.LoginRequired -> LoginRequired(extension)
            is ClientException.NotSupported -> NotSupported(this, extension, operation)
            else -> {
                // Detect unauthorized errors from error message (e.g. Spotify throwing generic exceptions)
                val msg = this.message?.lowercase() ?: ""
                if (msg.contains("unauthorized") || msg.contains("401") ||
                    msg.contains("not authenticated") || msg.contains("auth required")) {
                    LoginRequired(extension)
                } else {
                    Other(this, extension)
                }
            }
        }
    }
}