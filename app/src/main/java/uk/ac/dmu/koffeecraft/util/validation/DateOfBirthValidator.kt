package uk.ac.dmu.koffeecraft.util.validation

object DateOfBirthValidator {

    private val dobRegex = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun isValid(value: String): Boolean {
        return dobRegex.matches(value.trim())
    }
}