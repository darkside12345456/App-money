package com.despesas.gestor.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT = Locale("pt", "PT")

object Money {
    private val currency: NumberFormat =
        NumberFormat.getCurrencyInstance(PT)

    /** Formata um valor em euros, ex.: 12.5 -> "12,50 €". */
    fun format(value: Double): String = currency.format(value)
}

object Dates {
    private val dayMonth = DateTimeFormatter.ofPattern("dd MMM yyyy", PT)
    private val dayMonthShort = DateTimeFormatter.ofPattern("dd MMM", PT)
    private val monthLong = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT)

    val zone: ZoneId = ZoneId.systemDefault()

    fun toLocalDate(epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatDate(epochMillis: Long): String =
        dayMonth.format(toLocalDate(epochMillis))

    fun formatDateShort(epochMillis: Long): String =
        dayMonthShort.format(toLocalDate(epochMillis))

    /** Chave de mês no formato "2026-08". */
    fun monthKey(epochMillis: Long): String =
        YearMonth.from(toLocalDate(epochMillis)).toString()

    fun monthKey(month: YearMonth): String = month.toString()

    fun currentMonthKey(): String = YearMonth.now(zone).toString()

    fun monthLabel(monthKey: String): String =
        monthLong.format(YearMonth.parse(monthKey)).replaceFirstChar { it.uppercase() }

    fun parseMonth(monthKey: String): YearMonth = YearMonth.parse(monthKey)
}
