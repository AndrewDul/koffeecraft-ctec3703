package uk.ac.dmu.koffeecraft.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer

class FeedbackFragment : Fragment(R.layout.fragment_feedback) {

    private lateinit var viewModel: FeedbackViewModel
    private lateinit var adapter: FeedbackProductsAdapter
    private var orderId: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = requireArguments().getLong("orderId")

        viewModel = ViewModelProvider(
            this,
            FeedbackViewModel.Factory(appContainer.feedbackRepository)
        )[FeedbackViewModel::class.java]

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val rv = view.findViewById<RecyclerView>(R.id.rvFeedbackProducts)
        val btnBack = view.findViewById<MaterialButton>(R.id.btnBack)

        tvTitle.text = "Feedback for Order"
        tvSubtitle.text = "Order #$orderId"

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)

        adapter = FeedbackProductsAdapter(
            items = emptyList(),
            onOpen = { item ->
                findNavController().navigate(
                    R.id.productFeedbackFragment,
                    bundleOf(
                        "orderId" to item.orderId,
                        "orderItemId" to item.orderItemId
                    )
                )
            }
        )

        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitList(state.items)
                tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
            }
        }

        viewModel.load(orderId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.load(orderId)
    }
}