package io.slama.managers

import com.notkamui.kourrier.core.Kourrier
import com.notkamui.kourrier.core.KourrierAuthenticationException
import com.notkamui.kourrier.core.KourrierConnectException
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
import io.slama.utils.courseID
import io.slama.utils.courseName
import io.slama.utils.getCourseChannelByID
import io.slama.utils.isFromMoodle
import io.slama.utils.senderName
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Color

private val logger: Logger = LoggerFactory.getLogger("MailManager")
private const val SEPARATOR = "---------------------------------------------------------------------"

class MailManager(config: MailConfig, jda: JDA) {
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
            logger.info("Received a new mail !")
            if (!message.isFromMoodle) {
                logger.info("Not from Moodle, aborting...")
                return
            }
            logger.info("Mail is from Moodle !")
            message.dispatch(jda)
        }
    }

    private val session: KourrierIMAPSession? = try {
        Kourrier.imap(connectionInfo).also {
            logger.info("Opened IMAP session")
        }
    } catch (e: KourrierAuthenticationException) {
        logger.error("Authentication to mail server failed. Mail features unavailable.")
        null
    } catch (e: KourrierConnectException) {
        logger.error("Unknown host mail server. Mail features unavailable.")
        null
    }

    private val inbox: KourrierFolder? = try {
        session?.folder(
            name = "INBOX",
            mode = KourrierFolderMode.ReadOnly,
            keepAlive = true,
            listener = dispatcher
        )?.also {
            logger.info("Opened INBOX folder at ${connectionInfo.username}")
        }
    } catch (e: KourrierIMAPSessionStateException) {
        logger.error("Couldn't open INBOX since the session is closed. Mail features unavailable.")
        null
    }

    fun reOpen() {
        try {
            session?.open()
            inbox?.open(KourrierFolderMode.ReadOnly)
            logger.info("Re-opened session and INBOX folder")
            logger.warn("Folder listener may take time to restart again")
        } catch (e: KourrierIMAPSessionStateException) {
            logger.warn("Tried to open an opened session")
        } catch (e: KourrierIMAPFolderStateException) {
            logger.warn("Tried to open an opened folder")
        }
    }

    fun close() {
        try {
            inbox?.close()
            session?.close()
            logger.info("Closed session and INBOX folder")
        } catch (e: KourrierIMAPSessionStateException) {
            logger.warn("Tried to close a closed session")
        } catch (e: KourrierIMAPFolderStateException) {
            logger.warn("Tried to close a closed folder")
        }
    }
}

private fun KourrierIMAPMessage.dispatch(jda: JDA) {
    val content = with(body.split(SEPARATOR)) {
        if (size < 2) body.trim()
        else this[1].trim()
    }
    val courseID = courseID

    if (courseID == null) {
        logger.warn("No course ID detected, aborting...")
        return
    }
    logger.info("Course ID detected ! ($courseID)")

    val courseName = courseName ?: "Annonce"

    val channel = jda.guilds.firstNotNullOfOrNull {
        it.getCourseChannelByID(courseID)
    }

    if (channel == null) {
        logger.warn("No text channel found, aborting...")
        return
    }

    if (senderName.isEmpty()) return
    channel.guild
        .retrieveMembersByPrefix(senderName, 1)
        .onSuccess {
            if (it.isEmpty()) {
                channel.sendMail(courseName, content, senderName)
                return@onSuccess
            }
            val color = it.first().color
            val avatarUrl = it.first().user.avatarUrl

            channel.sendMail(courseName, content, senderName, avatarUrl, color)
        }.onError {
            logger.warn("Failed to retrieve member")
            channel.sendMail(courseName, content, senderName)
        }
}

private fun TextChannel.sendMail(
    courseName: String = "Annonce",
    content: String,
    senderName: String,
    avatarUrl: String? = null,
    color: Color? = null
) {
    logger.info("Sender name: $senderName")
    logger.info("Course name: $courseName")
    logger.info("Sending e-Learning announcement !")
    sendMessageEmbeds(
        EmbedBuilder()
            .setTitle(courseName)
            .setAuthor(senderName, null, avatarUrl)
            .setDescription(content)
            .setColor(color)
            .setFooter("Via e-Learning - Powered by Kourrier")
            .build()
    ).queue()
}
