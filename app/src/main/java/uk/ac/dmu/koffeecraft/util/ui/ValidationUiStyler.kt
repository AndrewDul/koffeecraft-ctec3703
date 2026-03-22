package uk.ac.dmu.koffeecraft.util.ui

import android.widget.TextView
import androidx.core.content.ContextCompat
import uk.ac.dmu.koffeecraft.R

object ValidationUiStyler {

    fun applyPasswordRuleStyle(view: TextView, isValid: Boolean) {
        if (isValid) {
            view.setBackgroundResource(R.drawable.bg_rule_valid)
            view.setTextColor(
                ContextCompat.getColor(view.context, R.color.kc_success_text)
            )
        } else {
            view.setBackgroundResource(R.drawable.bg_rule_invalid)
            view.setTextColor(
                ContextCompat.getColor(view.context, R.color.kc_danger_text)
            )
        }
    }
}