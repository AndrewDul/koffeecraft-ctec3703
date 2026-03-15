package uk.ac.dmu.koffeecraft.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import uk.ac.dmu.koffeecraft.R

class SettingsInfoPageFragment : Fragment(R.layout.fragment_settings_info_page) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageType = requireArguments().getString("pageType").orEmpty()

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvContent = view.findViewById<TextView>(R.id.tvContent)

        view.findViewById<TextView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        when (pageType) {
            "help" -> {
                tvTitle.text = "Help"
                tvContent.text = helpContent()
            }
            "terms" -> {
                tvTitle.text = "Terms of Use"
                tvContent.text = termsContent()
            }
            "privacy" -> {
                tvTitle.text = "Privacy Statement"
                tvContent.text = privacyContent()
            }
            else -> {
                tvTitle.text = "Information"
                tvContent.text = ""
            }
        }
    }

    private fun helpContent(): String {
        return """
Ordering

• How do I place an order?
Browse the menu, customise your item if needed, add it to cart, and complete checkout.

• Can I review my order before paying?
Yes. The cart screen shows your selected products, sizes, add-ons, calories, and totals before checkout.

• What do order statuses mean?
Placed means the order was successfully submitted.
Preparing means it is being prepared.
Ready means it is ready for collection.
Collected means the order was picked up.

Rewards & Beans

• How do beans work?
Beans are earned on qualifying purchases and can be used on reward items when available.

• When can I redeem rewards?
Rewards can be redeemed when you have enough beans and an eligible reward product is available.

• Do birthday rewards depend on my date of birth?
Yes. Birthday rewards are based on the stored date of birth linked to your account.

Inbox & Notifications

• Why am I receiving notifications?
Notifications help you track order updates such as preparation and collection status.

• What are promotional messages?
Promotional messages include reward offers, seasonal updates, and admin communications if you allow them.

Feedback

• When can I leave feedback?
Feedback becomes available once your order reaches the Collected stage.

• Can I edit feedback later?
Yes. If feedback already exists, the app allows you to open it again and update it.
        """.trimIndent()
    }

    private fun termsContent(): String {
        return """
Welcome to KoffeeCraft.

By creating an account and using the app, you agree to use KoffeeCraft responsibly and lawfully.

1. Account Use
You are responsible for keeping your login credentials secure and for the activity performed under your account.

2. Orders and Payments
Orders placed through the app are subject to product availability, menu settings, and payment confirmation.

3. Rewards and Promotions
Beans, reward items, promotional inbox messages, and birthday offers are provided according to the app rules and may change over time.

4. Acceptable Use
You must not misuse the service, abuse rewards, manipulate offers, or attempt to interfere with the operation of the app.

5. Content and Feedback
Feedback submitted through the app should be honest, respectful, and relevant to your real experience.

6. Changes to the Service
KoffeeCraft may update features, menus, availability, rewards, or policies to improve the service.

7. Account Restriction or Removal
Accounts may be restricted or removed if the app is misused or if the service rules are intentionally violated.
        """.trimIndent()
    }

    private fun privacyContent(): String {
        return """
KoffeeCraft stores customer information to provide account, ordering, rewards, and communication features.

1. Data We Store
We may store:
- first name
- last name
- email address
- date of birth
- password hash and security data
- order history
- rewards and beans data
- inbox preferences
- feedback submitted in the app

2. Why We Store It
This information is used to:
- manage your account
- process orders
- display order progress
- support rewards and birthday offers
- manage inbox preferences
- improve customer experience

3. Security
Passwords are stored using secure password hashing rather than plain text.

4. Communications
Promotional inbox messages are controlled by your inbox preference setting and can be changed later in Settings.

5. Feedback and Activity
Order-related activity and feedback may be stored so the app can show purchase history and review history correctly.

6. Account Deletion
If you choose to delete your account, related customer data is removed from the app database as part of the deletion process.

7. Policy Updates
This privacy statement may be updated as the app evolves and new customer-facing features are added.
        """.trimIndent()
    }
}