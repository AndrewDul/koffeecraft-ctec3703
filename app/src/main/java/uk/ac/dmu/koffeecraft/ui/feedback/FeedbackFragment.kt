package uk.ac.dmu.koffeecraft.ui.feedback

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class FeedbackFragment : Fragment(R.layout.fragment_feedback) {

    private lateinit var adapter: FeedbackProductsAdapter
    private var orderId: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderId = requireArguments().getLong("orderId")

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

        loadItems(tvEmpty)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.tvEmpty)?.let { loadItems(it) }
    }

    private fun loadItems(tvEmpty: TextView) {
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val items = db.orderItemDao().getFeedbackItemsForOrder(orderId)

            withContext(Dispatchers.Main) {
                adapter.submitList(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}