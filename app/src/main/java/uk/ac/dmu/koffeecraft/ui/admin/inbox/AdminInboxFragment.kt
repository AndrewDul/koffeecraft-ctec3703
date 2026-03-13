package uk.ac.dmu.koffeecraft.ui.admin.inbox

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.CustomerInboxTarget
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage

class AdminInboxFragment : Fragment(R.layout.fragment_admin_inbox) {

    private lateinit var spinnerAudience: Spinner
    private lateinit var tvSearchHint: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var tvSelectedTarget: TextView
    private lateinit var tvTargetsEmpty: TextView
    private lateinit var etMessageTitle: EditText
    private lateinit var etMessageBody: EditText
    private lateinit var btnSendMessage: Button

    private var selectedTarget: CustomerInboxTarget? = null
    private var currentAudienceMode: AudienceMode = AudienceMode.ALL_USERS

    private enum class AudienceMode {
        ALL_USERS,
        BIRTHDAY_TODAY,
        ORDER_NUMBER,
        CUSTOMER_ID
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerAudience = view.findViewById(R.id.spinnerAudience)
        tvSearchHint = view.findViewById(R.id.tvSearchHint)
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        tvSelectedTarget = view.findViewById(R.id.tvSelectedTarget)
        tvTargetsEmpty = view.findViewById(R.id.tvTargetsEmpty)
        etMessageTitle = view.findViewById(R.id.etMessageTitle)
        etMessageBody = view.findViewById(R.id.etMessageBody)
        btnSendMessage = view.findViewById(R.id.btnSendMessage)

        setupAudienceSpinner()

        btnSearch.setOnClickListener {
            performTargetLookup()
        }

        btnSendMessage.setOnClickListener {
            sendMessages()
        }
    }

    private fun setupAudienceSpinner() {
        val options = listOf(
            "All users",
            "Birthday today",
            "Find by order number",
            "Find by customer ID"
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerAudience.adapter = spinnerAdapter

        spinnerAudience.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentAudienceMode = when (position) {
                    1 -> AudienceMode.BIRTHDAY_TODAY
                    2 -> AudienceMode.ORDER_NUMBER
                    3 -> AudienceMode.CUSTOMER_ID
                    else -> AudienceMode.ALL_USERS
                }

                resetSelectionUi()

                when (currentAudienceMode) {
                    AudienceMode.ALL_USERS -> {
                        tvSearchHint.visibility = View.GONE
                        etSearch.visibility = View.GONE
                        btnSearch.visibility = View.GONE
                    }

                    AudienceMode.BIRTHDAY_TODAY -> {
                        tvSearchHint.visibility = View.GONE
                        etSearch.visibility = View.GONE
                        btnSearch.visibility = View.GONE
                    }

                    AudienceMode.ORDER_NUMBER -> {
                        tvSearchHint.visibility = View.VISIBLE
                        tvSearchHint.text = "Find customer by order number"
                        etSearch.visibility = View.VISIBLE
                        etSearch.hint = "Enter order number"
                        etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        btnSearch.visibility = View.VISIBLE
                    }

                    AudienceMode.CUSTOMER_ID -> {
                        tvSearchHint.visibility = View.VISIBLE
                        tvSearchHint.text = "Find customer by customer ID"
                        etSearch.visibility = View.VISIBLE
                        etSearch.hint = "Enter customer ID"
                        etSearch.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        btnSearch.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun resetSelectionUi() {
        selectedTarget = null
        tvSelectedTarget.visibility = View.GONE
        tvTargetsEmpty.visibility = View.GONE
        etSearch.setText("")
    }

    private fun performTargetLookup() {
        val query = etSearch.text.toString().trim()
        if (query.isBlank()) {
            Toast.makeText(requireContext(), "Enter a value first.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = when (currentAudienceMode) {
                AudienceMode.ORDER_NUMBER -> {
                    val orderId = query.toLongOrNull()
                    if (orderId == null) null else db.customerDao().getInboxTargetByOrderId(orderId)
                }

                AudienceMode.CUSTOMER_ID -> {
                    val customerId = query.toLongOrNull()
                    if (customerId == null) null else db.customerDao().getInboxTargetByCustomerId(customerId)
                }

                else -> null
            }

            withContext(Dispatchers.Main) {
                if (result == null) {
                    selectedTarget = null
                    tvSelectedTarget.visibility = View.GONE
                    tvTargetsEmpty.visibility = View.VISIBLE
                } else {
                    selectedTarget = result
                    tvTargetsEmpty.visibility = View.GONE
                    tvSelectedTarget.visibility = View.VISIBLE
                    tvSelectedTarget.text =
                        "Selected: Customer #${result.customerId} • ${result.firstName} ${result.lastName}"
                }
            }
        }
    }

    private fun sendMessages() {
        val title = etMessageTitle.text.toString().trim()
        val body = etMessageBody.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(requireContext(), "Enter a message title.", Toast.LENGTH_SHORT).show()
            return
        }

        if (body.isBlank()) {
            Toast.makeText(requireContext(), "Enter a message body.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val targets = when (currentAudienceMode) {
                AudienceMode.ALL_USERS -> db.customerDao().getAllInboxTargets()

                AudienceMode.BIRTHDAY_TODAY -> {
                    val monthDay = SimpleDateFormat("MM-dd", Locale.UK).format(Date())
                    db.customerDao().getBirthdayInboxTargets(monthDay)
                }

                AudienceMode.ORDER_NUMBER,
                AudienceMode.CUSTOMER_ID -> selectedTarget?.let { listOf(it) } ?: emptyList()
            }

            if (targets.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No target customers found.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val deliveryType = when (currentAudienceMode) {
                AudienceMode.ALL_USERS -> "BROADCAST"
                AudienceMode.BIRTHDAY_TODAY -> "BIRTHDAY"
                AudienceMode.ORDER_NUMBER,
                AudienceMode.CUSTOMER_ID -> "DIRECT"
            }

            val messages = targets
                .distinctBy { it.customerId }
                .map { target ->
                    InboxMessage(
                        recipientCustomerId = target.customerId,
                        title = title,
                        body = body,
                        deliveryType = deliveryType
                    )
                }

            db.inboxMessageDao().insertAll(messages)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    "Message sent to ${messages.size} recipient(s).",
                    Toast.LENGTH_SHORT
                ).show()

                etMessageTitle.setText("")
                etMessageBody.setText("")
                resetSelectionUi()
            }
        }
    }
}