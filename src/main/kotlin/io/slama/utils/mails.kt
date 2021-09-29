package io.slama.utils

import com.notkamui.kourrier.imap.KourrierIMAPMessage
import javax.mail.internet.MimeUtility
import javax.mail.internet.ParseException

private const val WHITESPACE = "[\n\r\t]"

fun String.fromRFC2047(): String = this
    .replace(WHITESPACE.toRegex(), "")
    .split(" ")
    .joinToString("", transform = {
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

val KourrierIMAPMessage.senderName: String
    get() {
        val name = headers
            .filter { it.name == "From" }
            .joinToString(" ") { it.value }
            .replace("$WHITESPACE|\"".toRegex(), "")
            .fromRFC2047()
            .split("( \\(via| <)".toRegex()).first()
            .replace("\"", "")

        if (" " in name) {
            val fullName = name.split(" ")
            if (fullName[0] == fullName[0].uppercase()) {
                return "${fullName[1]} ${fullName[0]}".capitalize().trim()
            }
        }
        return name.trim()
    }

val KourrierIMAPMessage.courseName: String?
    get() = headers
        .filter { it.name == "Subject" }
        .joinToString(" ") { it.value }
        .fromRFC2047()
        .replaceBefore(":", "").drop(1)
        .replace("$WHITESPACE|\"".toRegex(), "")
        .takeUnless(String::isEmpty)
        ?.trim()

val KourrierIMAPMessage.courseID: String?
    get() = headers
        .firstOrNull { it.name == "X-Course-Id" }
        ?.value
