package io.slama.utils

import com.notkamui.kourrier.imap.KourrierIMAPMessage

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