package uk.ac.dmu.koffeecraft.util.validation

object UsernameValidator {

    fun isValid(username: String): Boolean {
        return username.matches(Regex("^[a-z0-9._-]{4,20}$"))
    }
}