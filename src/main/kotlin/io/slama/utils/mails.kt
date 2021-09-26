package io.slama.utils

import com.notkamui.kourrier.imap.KourrierIMAPMessage
import javax.mail.internet.MimeUtility
import javax.mail.internet.ParseException

fun String.fromRFC2047(): String = this
    .replace("[\n\r\t]".toRegex(), "")
    .split(" ")
    .joinToString(transform = {
        try {
            MimeUtility.decodeWord(it)
        } catch (e: ParseException) {
            "$it "
        }
    }).trim()

val KourrierIMAPMessage.isFromMoodle: Boolean
    get() {
        headers
            .filter { it.name == "X-Course-Name" }
            .also { if (it.isEmpty()) return false }

        return headers
            .filter { it.name == "List-Id" }
            .also { if (it.isEmpty()) return false }
            .map { it.value }
            .any { it.lowercase().matches("(.*annonces?.*)".toRegex()) }
    }