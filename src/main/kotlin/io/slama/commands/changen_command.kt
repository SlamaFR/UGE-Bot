package io.slama.commands

import io.slama.core.ConfigFolders
import io.slama.utils.replySuccess
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

const val INITIAL_DELETION_TIMEOUT = 150 * 1000L
const val IMMINENT_DELETION_TIMEOUT = 30 * 1000L
private const val TMP_CHANNELS_FILE = "temporaryChannelsGenerator"

private val logger: Logger = LoggerFactory.getLogger("TemporaryChannels")

class ChanGenCommand : ListenerAdapter() {

    private val generators = mutableSetOf<String>()
    private val temporaryChannels = mutableMapOf<String, String>()
    private val deletionTasks = mutableMapOf<String, TimerTask>()

    init {
        val file = File(ConfigFolders.DATA_ROOT, TMP_CHANNELS_FILE)
        if (file.exists()) {
            file.useLines { lines ->
                lines.map {
                    it.trim('\n')
                }.forEach {
                    generators.add(it)
                    logger.info("Successfully created generator on $it")
                }
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "changen") return
        if (event.guild == null) return

        event.getOption("channel")?.asChannel?.let { channel ->
            if (channel.id in generators) {
                generators.remove(channel.id)
                event.replySuccess("Le générateur du salon **${channel.name}** a été supprimé.")
                    .setEphemeral(true)
                    .queue()
            } else {
                generators.add(channel.id)
                event.replySuccess("Le salon **${channel.name}** a été lié à un générateur.")
                    .setEphemeral(true)
                    .queue()
            }
            save()
        }
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.channelJoined == null) {
            onGuildVoiceLeave(event)
        } else if (event.channelLeft == null) {
            onGuildVoiceJoin(event)
        } else {
            onGuildVoiceMove(event)
        }
    }

    private fun onGuildVoiceJoin(event: GuildVoiceUpdateEvent) {
        event.channelJoined?.asVoiceChannel()?.let(::checkJoin)
        handleRequest(event)
    }

    private fun onGuildVoiceLeave(event: GuildVoiceUpdateEvent) {
        event.channelLeft?.asVoiceChannel()?.let(::checkLeave)
    }

    private fun onGuildVoiceMove(event: GuildVoiceUpdateEvent) {
        event.channelJoined?.asVoiceChannel()?.let(::checkJoin)
        event.channelLeft?.asVoiceChannel()?.let(::checkLeave)
        handleRequest(event)
    }

    private fun handleRequest(event: GuildVoiceUpdateEvent) {
        val channelJoined = event.channelJoined ?: return
        val member = event.member

        if (channelJoined.id !in generators) return

        temporaryChannels[member.id]?.let {
            member.guild.getVoiceChannelById(it)?.let { channel ->
                // Member already has a temporary channel, moving them.
                member.guild.moveVoiceMember(member, channel).queue()
                return
            } ?: run {
                // Previously registered channel no longer exists, removing it.
                temporaryChannels.remove(member.id)
            }
        }

        // Creating new temporary channel for the current member.
        channelJoined.parentCategory?.createVoiceChannel("\uD83D\uDD35 Salon de ${member.effectiveName}")?.queue {
            temporaryChannels[member.id] = it.id
            member.guild.moveVoiceMember(member, it).queue()
        }
    }

    private fun checkJoin(channel: VoiceChannel) {
        if (channel.id in deletionTasks.keys) {
            deletionTasks.remove(channel.id)?.cancel()
            if (channel.name.startsWith("\uD83D\uDD34")) {
                channel.manager.setName(channel.name.replace("\uD83D\uDD34", "\uD83D\uDD35")).queue()
            }
        }
    }

    private fun checkLeave(channel: VoiceChannel) {
        if (channel.id !in temporaryChannels.values) return
        if (channel.members.size > 0) return

        deletionTasks[channel.id] = Timer().schedule(INITIAL_DELETION_TIMEOUT) {
            channel.manager.setName(channel.name.replace("\uD83D\uDD35", "\uD83D\uDD34")).queue()
            deletionTasks[channel.id] = Timer().schedule(IMMINENT_DELETION_TIMEOUT) {
                channel.delete().queue {
                    deletionTasks.remove(channel.id)
                    temporaryChannels.remove(channel.id)
                }
            }
        }
    }

    private fun save() {
        val file = File(ConfigFolders.DATA_ROOT, TMP_CHANNELS_FILE)
        if (!file.exists()) file.createNewFile()
        file.writeText("")
        generators.forEach {
            file.appendText("$it\n")
        }
    }
}
