package io.slama.utils

import io.slama.core.BotConfiguration
import net.dv8tion.jda.api.entities.Member

fun Member.isAdmin(): Boolean =
    BotConfiguration.guilds[guild.idLong]?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.adminRoleID }
    } ?: false

fun Member.isManager(): Boolean =
    isAdmin() || BotConfiguration.guilds[guild.idLong]?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.managerRoleID }
    } ?: false

fun Member.isTeacher(): Boolean =
    isManager() || BotConfiguration.guilds[guild.idLong]?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.teacherRoleID }
    } ?: false

fun Member.isStudent(): Boolean =
    BotConfiguration.guilds[guild.idLong]?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.studentRoleID }
    } ?: false
