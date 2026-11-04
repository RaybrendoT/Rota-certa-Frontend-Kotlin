package com.rotacerta.mobile.core.util

import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Formatters {
    private val weekdayNames = mapOf(
        "MON" to "Seg", "TUE" to "Ter", "WED" to "Qua", "THU" to "Qui",
        "FRI" to "Sex", "SAT" to "Sáb", "SUN" to "Dom",
    )
    private val weekdayOrder = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM HH:mm")
    private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM")

    /** ["MON","TUE","WED","THU","FRI"] → "Seg a Sex"; ["MON","WED"] → "Seg, Qua". */
    fun weekdays(days: List<String>): String {
        val sorted = days.map { it.uppercase() }.filter { it in weekdayNames }.sortedBy { weekdayOrder.indexOf(it) }
        if (sorted.isEmpty()) return ""
        if (sorted.size == 7) return "Todos os dias"
        val indexes = sorted.map { weekdayOrder.indexOf(it) }
        val contiguous = indexes.zipWithNext().all { (a, b) -> b == a + 1 }
        return if (contiguous && sorted.size >= 3) {
            "${weekdayNames.getValue(sorted.first())} a ${weekdayNames.getValue(sorted.last())}"
        } else {
            sorted.joinToString(", ") { weekdayNames.getValue(it) }
        }
    }

    /** "agora", "há 5 min", "há 2 h", "ontem" ou "dd/MM HH:mm". */
    fun relativeTime(isoTimestamp: String, now: OffsetDateTime = OffsetDateTime.now()): String {
        val time = runCatching { OffsetDateTime.parse(isoTimestamp) }.getOrNull() ?: return ""
        val minutes = Duration.between(time, now).toMinutes()
        return when {
            minutes < 1 -> "agora"
            minutes < 60 -> "há $minutes min"
            minutes < 24 * 60 -> "há ${minutes / 60} h"
            minutes < 48 * 60 -> "ontem"
            else -> time.atZoneSameInstant(ZoneId.systemDefault()).format(dateTimeFormat)
        }
    }

    /** Hora local "HH:mm" de um timestamp ISO. */
    fun localTime(isoTimestamp: String): String = runCatching {
        OffsetDateTime.parse(isoTimestamp).atZoneSameInstant(ZoneId.systemDefault()).format(timeFormat)
    }.getOrDefault("")

    /** "2026-10-30" → "30/10" (data de rota avulsa); devolve o texto original se não for uma data. */
    fun isoDateShort(isoDate: String): String = runCatching {
        LocalDate.parse(isoDate).format(shortDateFormat)
    }.getOrDefault(isoDate)

    /** "Ana Souza Lima" → "Ana Souza". */
    fun shortName(name: String): String = name.trim().split(Regex("\\s+")).take(2).joinToString(" ")

    /** Remove o sufixo depois de " - " ("Ponto A - Rua X" → "Ponto A"), como o app Flutter fazia. */
    fun beforeDash(value: String): String = value.substringBefore(" - ").trim()
}
