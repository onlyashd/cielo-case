package com.example.cielocase.util.observability

/**
 * Removes Cielo credentials from any text before it reaches logs, the database or Sentry.
 *
 * The payment deep link carries `request=<base64>`, and that Base64 payload contains the
 * `accessToken`/`clientID`. Exception messages such as
 * `ActivityNotFoundException: No Activity found to handle Intent { act=VIEW dat=lio://payment?request=... }`
 * would otherwise leak those secrets.
 */
object SensitiveData {

    private const val REDACTED = "<redacted>"

    private val RULES: List<Pair<Regex, String>> = listOf(
        Regex("""(request=)[^&\s}]+""") to "$1$REDACTED",
        Regex("""("(?:accessToken|clientID)"\s*:\s*")[^"]*(")""") to "$1$REDACTED$2",
        Regex("""((?:accessToken|clientID|access_token)=)[^&\s,}]+""") to "$1$REDACTED",
    )

    fun redact(text: String?): String? = text?.let {
        RULES.fold(it) { redacted, (pattern, replacement) ->
            pattern.replace(redacted, replacement)
        }
    }
}
