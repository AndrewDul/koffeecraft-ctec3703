package uk.ac.dmu.koffeecraft.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class FeedbackFragment : Fragment(R.layout.fragment_feedback) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = requireArguments().getLong("orderId")
        val customerId = SessionManager.currentCustomerId

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = view.findViewById<EditText>(R.id.etComment)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnBack = view.findViewById<Button>(R.id.btnBack)

        tvTitle.text = "Feedback for Order #$orderId"

        btnBack.setOnClickListener { findNavController().navigateUp() }

        if (customerId == null) {
            tvError.visibility = View.VISIBLE
            tvError.text = "You are not logged in as a customer."
            btnSave.isEnabled = false
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        // Prefill if feedback exists
        lifecycleScope.launch(Dispatchers.IO) {
            val existing = db.feedbackDao().getByOrderId(orderId)
            withContext(Dispatchers.Main) {
                if (existing != null) {
                    ratingBar.rating = existing.rating.toFloat()
                    etComment.setText(existing.comment)
                    btnSave.text = "Save changes"
                } else {
                    ratingBar.rating = 5f
                }
            }
        }

        btnSave.setOnClickListener {
            val rating = ratingBar.rating.toInt().coerceIn(1, 5)
            val comment = etComment.text.toString().trim()

            lifecycleScope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val existing = db.feedbackDao().getByOrderId(orderId)

                val feedback = Feedback(
                    feedbackId = existing?.feedbackId ?: 0,
                    orderId = orderId,
                    customerId = customerId,
                    rating = rating,
                    comment = comment,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )

                db.feedbackDao().upsert(feedback)

                withContext(Dispatchers.Main) {
                    val msg = if (comment.isBlank()) {
                        "Thanks for your rating!"
                    } else {
                        "Thanks for your rating and feedback!"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    NotificationHelper.showOrderNotification(
                        context = requireContext(),
                        title = "Thank you!",
                        message = "Order #$orderId: $msg",
                        notificationId = 300000 + (orderId % 50000).toInt()
                    )
                    findNavController().navigateUp()
                }
            }
        }
    }
}