package org.tonycox.ktor.banking.account

import java.time.LocalDateTime

private const val milliToNanoConst = 1000000

/**
 * As Kotlin.exposed supports only joda time for now,
 * this function converts joda.time.DateTime to java.time.LocalDateTime
 */
fun org.joda.time.DateTime.toJavaDateTime(): LocalDateTime {
    return java.time.LocalDateTime.of(
        this.year,
        this.monthOfYear,
        this.dayOfMonth,
        this.hourOfDay,
        this.minuteOfHour,
        this.secondOfMinute,
        this.millisOfSecond * milliToNanoConst
    )
}

/**
 * As Kotlin.exposed supports only joda time for now,
 * this function converts java.time.LocalDateTime to joda.time.DateTime.
 */
fun LocalDateTime.toJodaDate(): org.joda.time.DateTime {
    return org.joda.time.LocalDateTime(
        this.year,
        this.monthValue,
        this.dayOfMonth,
        this.hour,
        this.minute,
        this.second,
        this.nano / milliToNanoConst
    ).toDateTime()
}
