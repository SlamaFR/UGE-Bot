package io.slama.utils

import io.slama.core.getConfigOrNull
import net.dv8tion.jda.api.entities.Member

fun isAdmin(member: Member): Boolean =
    member.guild.getConfigOrNull()?.let { config ->
        member.roles.map { it.idLong }.any { it == config.roles.adminRoleID }
    } ?: false

fun isManager(member: Member): Boolean =
    isAdmin(member) || member.guild.getConfigOrNull()?.let { config ->
        member.roles.map { it.idLong }.any { it == config.roles.managerRoleID }
    } ?: false

fun isTeacher(member: Member): Boolean =
    isManager(member) || member.guild.getConfigOrNull()?.let { config ->
        member.roles.map { it.idLong }.any { it == config.roles.teacherRoleID }
    } ?: false

fun isStudent(member: Member): Boolean =
    member.guild.getConfigOrNull()?.let { config ->
        member.roles.map { it.idLong }.any { it == config.roles.studentRoleID }
    } ?: false
