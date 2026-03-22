package uk.ac.dmu.koffeecraft.util.validation

import android.util.Patterns

object EmailValidator {

    fun isValid(email: String): Boolean {
        val value = email.trim()
        return value.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(value).matches()
    }
}