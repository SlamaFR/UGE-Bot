package io.slama.utils

import io.slama.core.BotConfiguration
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel

val Guild.announcementChannel: TextChannel?
    get() = BotConfiguration.guilds[idLong]?.let {
        getTextChannelById(it.channels.announcementsChannelID)
    }

fun Guild.getCourseChannelByID(courseID: String): TextChannel? {
    val config = BotConfiguration.guilds[idLong] ?: return null
    val channelID = config.channels.moodleAnnouncementsChannelsIDs[courseID] ?: return null
    return getTextChannelById(channelID)
}