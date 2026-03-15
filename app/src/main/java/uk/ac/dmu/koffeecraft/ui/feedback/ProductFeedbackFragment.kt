package uk.ac.dmu.koffeecraft.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderFeedbackItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Feedback
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class ProductFeedbackFragment : Fragment(R.layout.fragment_product_feedback) {

    private var orderId: Long = 0L
    private var orderItemId: Long = 0L

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvCraftedBadge: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var currentItem: OrderFeedbackItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = requireArguments().getLong("orderId")
        orderItemId = requireArguments().getLong("orderItemId")

        tvTitle = view.findViewById(R.id.tvTitle)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        tvCraftedBadge = view.findViewById(R.id.tvCraftedBadge)
        ratingBar = view.findViewById(R.id.ratingBar)
        etComment = view.findViewById(R.id.etComment)
        tvError = view.findViewById(R.id.tvError)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener { findNavController().navigateUp() }

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvError.visibility = View.VISIBLE
            tvError.text = "You are not logged in as a customer."
            btnSave.isEnabled = false
            return
        }

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        loadCurrentItem(db)

        btnSave.setOnClickListener {
            saveFeedback(db, customerId)
        }
    }

    private fun loadCurrentItem(db: KoffeeCraftDatabase) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val item = db.orderItemDao().getFeedbackItemByOrderItemId(orderItemId)

            withContext(Dispatchers.Main) {
                if (item == null) {
                    tvError.visibility = View.VISIBLE
                    tvError.text = "This purchased product could not be found."
                    btnSave.isEnabled = false
                    return@withContext
                }

                currentItem = item
                tvError.visibility = View.GONE

                tvTitle.text = item.productName
                tvSubtitle.text = "Order #${item.orderId} • Quantity: ${item.quantity}"
                tvCraftedBadge.visibility = if (item.isCrafted) View.VISIBLE else View.GONE

                ratingBar.rating = (item.rating ?: 5).toFloat()
                etComment.setText(item.comment.orEmpty())
                btnSave.text = "Submit & Next"
            }
        }
    }

    private fun saveFeedback(db: KoffeeCraftDatabase, customerId: Long) {
        val item = currentItem ?: return

        val rating = ratingBar.rating.toInt().coerceIn(1, 5)
        val comment = etComment.text.toString().trim()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = db.feedbackDao().getByOrderItemId(item.orderItemId)

            db.feedbackDao().upsert(
                Feedback(
                    feedbackId = existing?.feedbackId ?: 0,
                    orderItemId = item.orderItemId,
                    customerId = customerId,
                    rating = rating,
                    comment = comment,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )

            val nextItem = db.orderItemDao().getNextUnreviewedItem(orderId)

            withContext(Dispatchers.Main) {
                val message = if (comment.isBlank()) {
                    "Thanks for your rating!"
                } else {
                    "Thanks for your rating and feedback!"
                }

                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

                if (nextItem != null) {
                    findNavController().navigate(
                        R.id.productFeedbackFragment,
                        bundleOf(
                            "orderId" to nextItem.orderId,
                            "orderItemId" to nextItem.orderItemId
                        )
                    )
                } else {
                    NotificationHelper.showOrderNotification(
                        context = requireContext(),
                        title = "Thank you!",
                        message = "All purchased products from order #$orderId have been reviewed.",
                        notificationId = 350000 + (orderId % 50000).toInt()
                    )

                    val popped = findNavController().popBackStack(R.id.feedbackFragment, false)
                    if (!popped) {
                        findNavController().navigate(
                            R.id.feedbackFragment,
                            bundleOf("orderId" to orderId)
                        )
                    }
                }
            }
        }
    }
}