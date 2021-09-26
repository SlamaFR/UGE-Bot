package io.slama.managers

import com.notkamui.kourrier.core.Kourrier
import com.notkamui.kourrier.core.KourrierConnectionInfo
import com.notkamui.kourrier.core.KourrierIMAPFolderStateException
import com.notkamui.kourrier.core.KourrierIMAPSessionStateException
import com.notkamui.kourrier.imap.KourrierFolder
import com.notkamui.kourrier.imap.KourrierFolderAdapter
import com.notkamui.kourrier.imap.KourrierFolderListener
import com.notkamui.kourrier.imap.KourrierFolderMode
import com.notkamui.kourrier.imap.KourrierIMAPMessage
import com.notkamui.kourrier.imap.KourrierIMAPSession
import com.notkamui.kourrier.imap.imap
import io.slama.core.MailConfig
import javax.mail.AuthenticationFailedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("MailManager")

class MailManager(config: MailConfig) {
    private val connectionInfo = KourrierConnectionInfo(
        config.hostname,
        config.port,
        config.username,
        config.password,
        config.debugMode,
        config.enableSSL
    )

    private val dispatcher: KourrierFolderListener = object : KourrierFolderAdapter() {
        override fun onMessageReceived(message: KourrierIMAPMessage) {
            // TODO dispatch
        }
    }

    private val session: KourrierIMAPSession? = try {
        Kourrier.imap(connectionInfo).also {
            logger.info("Opened IMAP session")
        }
    } catch (e: AuthenticationFailedException) {
        logger.error("Authentication to mail server failed")
        null
    }

    private val inbox: KourrierFolder? = try {
        session?.folder(
            name = "INBOX",
            mode = KourrierFolderMode.ReadOnly,
            keepAlive = true,
            listener = dispatcher
        ).also {
            logger.info("Opened INBOX folder at ${connectionInfo.username}")
        }
    } catch (e: KourrierIMAPSessionStateException) {
        logger.error("Couldn't open INBOX since the session is closed")
        null
    }

    fun reOpen() {
        try {
            session?.open()
            inbox?.open(KourrierFolderMode.ReadOnly)
        } catch (e: KourrierIMAPSessionStateException) {
            logger.info("Tried to open an opened session")
        } catch (e: KourrierIMAPFolderStateException) {
            logger.info("Tried to open an opened folder")
        }
    }

    fun close() {
        try {
            inbox?.close()
            session?.close()
        } catch (e: KourrierIMAPSessionStateException) {
            logger.info("Tried to close an closed session")
        } catch (e: KourrierIMAPFolderStateException) {
            logger.info("Tried to close an closed folder")
        }
    }
}