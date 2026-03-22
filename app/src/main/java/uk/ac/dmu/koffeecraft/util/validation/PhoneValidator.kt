package uk.ac.dmu.koffeecraft.util.validation

object PhoneValidator {

    fun isValid(phone: String): Boolean {
        val compact = phone.replace(" ", "")
        return compact.length in 7..20 && compact.matches(Regex("^[0-9+()\\- ]+$"))
    }
}