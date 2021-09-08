package io.slama.utils

import io.slama.core.getConfigOrNull
import net.dv8tion.jda.api.entities.Member

fun Member.isAdmin(): Boolean =
    guild.getConfigOrNull()?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.adminRoleID }
    } ?: false

fun Member.isManager(): Boolean =
    isAdmin() || guild.getConfigOrNull()?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.managerRoleID }
    } ?: false

fun Member.isTeacher(): Boolean =
    isManager() || guild.getConfigOrNull()?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.teacherRoleID }
    } ?: false

fun Member.isStudent(): Boolean =
    guild.getConfigOrNull()?.let { config ->
        roles.map { it.idLong }.any { it == config.roles.studentRoleID }
    } ?: false
