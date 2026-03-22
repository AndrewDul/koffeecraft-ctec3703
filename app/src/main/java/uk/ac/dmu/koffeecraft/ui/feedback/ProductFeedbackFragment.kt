package uk.ac.dmu.koffeecraft.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.notifications.NotificationHelper

class ProductFeedbackFragment : Fragment(R.layout.fragment_product_feedback) {

    private var orderId: Long = 0L
    private var orderItemId: Long = 0L

    private lateinit var viewModel: ProductFeedbackViewModel

    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvCraftedBadge: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var etComment: EditText
    private lateinit var tvError: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = requireArguments().getLong("orderId")
        orderItemId = requireArguments().getLong("orderItemId")

        viewModel = ViewModelProvider(
            this,
            ProductFeedbackViewModel.Factory(appContainer.feedbackRepository)
        )[ProductFeedbackViewModel::class.java]

        tvTitle = view.findViewById(R.id.tvTitle)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        tvCraftedBadge = view.findViewById(R.id.tvCraftedBadge)
        ratingBar = view.findViewById(R.id.ratingBar)
        etComment = view.findViewById(R.id.etComment)
        tvError = view.findViewById(R.id.tvError)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        ratingBar.setOnRatingBarChangeListener { _, value, _ ->
            viewModel.updateRating(value)
        }

        btnSave.setOnClickListener {
            viewModel.updateComment(etComment.text.toString())
            viewModel.save()
        }

        observeState()

        viewModel.start(
            orderId = orderId,
            orderItemId = orderItemId,
            customerId = SessionManager.currentCustomerId
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvTitle.text = state.title
                tvSubtitle.text = state.subtitle
                tvCraftedBadge.visibility = if (state.craftedVisible) View.VISIBLE else View.GONE

                if (ratingBar.rating != state.rating) {
                    ratingBar.rating = state.rating
                }

                val currentComment = etComment.text?.toString().orEmpty()
                if (currentComment != state.comment) {
                    etComment.setText(state.comment)
                    etComment.setSelection(etComment.text.length)
                }

                tvError.visibility = if (state.errorVisible) View.VISIBLE else View.GONE
                tvError.text = state.errorText
                btnSave.isEnabled = state.saveEnabled
                btnSave.text = state.saveText
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is ProductFeedbackViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    is ProductFeedbackViewModel.UiEffect.NavigateNext -> {
                        findNavController().navigate(
                            R.id.productFeedbackFragment,
                            bundleOf(
                                "orderId" to effect.orderId,
                                "orderItemId" to effect.orderItemId
                            )
                        )
                    }

                    is ProductFeedbackViewModel.UiEffect.CompletedAll -> {
                        NotificationHelper.showCustomerOrderNotification(
                            context = requireContext(),
                            title = "Thank you!",
                            message = "All purchased products from order #${effect.orderId} have been reviewed.",
                            notificationId = 350000 + (effect.orderId % 50000).toInt(),
                            orderId = effect.orderId
                        )

                        val popped = findNavController().popBackStack(R.id.feedbackFragment, false)
                        if (!popped) {
                            findNavController().navigate(
                                R.id.feedbackFragment,
                                bundleOf("orderId" to effect.orderId)
                            )
                        }
                    }
                }
            }
        }
    }
}