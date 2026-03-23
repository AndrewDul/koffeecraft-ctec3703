package uk.ac.dmu.koffeecraft.util.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object DateOfBirthValidator {

    private val dobFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT)

    fun isValid(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return false

        return try {
            val parsedDate = LocalDate.parse(trimmed, dobFormatter)
            !parsedDate.isAfter(LocalDate.now())
        } catch (_: DateTimeParseException) {
            false
        }
    }
}