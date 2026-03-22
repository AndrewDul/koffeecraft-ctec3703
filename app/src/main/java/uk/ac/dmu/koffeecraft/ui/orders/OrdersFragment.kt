package uk.ac.dmu.koffeecraft.ui.orders

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.repository.CustomerOrderDateFilter
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import kotlin.math.abs

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private lateinit var vm: CustomerOrdersViewModel
    private lateinit var adapter: OrdersAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var filterViews: Map<CustomerOrderDateFilter, TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBrowseMenu = view.findViewById<MaterialButton>(R.id.btnBrowseMenu)
        val rv = view.findViewById<RecyclerView>(R.id.rvOrders)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "You are not logged in as a customer."
            return
        }

        vm = ViewModelProvider(
            this,
            CustomerOrdersViewModelFactory(appContainer.customerOrdersRepository)
        )[CustomerOrdersViewModel::class.java]

        btnBrowseMenu.setOnClickListener {
            findNavController().navigateUp()
        }

        setupFilterViews(view)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)

        adapter = OrdersAdapter(
            items = emptyList(),
            detailsByOrderId = emptyMap(),
            feedbackSummaryByOrderId = emptyMap(),
            onOrderAgain = { order ->
                vm.orderAgain(order.orderId)
            },
            onOpenFeedback = { order ->
                findNavController().navigate(
                    R.id.feedbackFragment,
                    bundleOf("orderId" to order.orderId)
                )
            },
            onRemoveOrder = { order ->
                vm.hideOrder(order.orderId)
            }
        )

        rv.adapter = adapter
        attachSwipeToHide(rv)

        vm.start(customerId)

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                updateFilterSelectionUi(state.selectedFilter)

                adapter.submitData(
                    newItems = state.items,
                    newDetailsByOrderId = state.detailsByOrderId,
                    newFeedbackSummaryByOrderId = state.feedbackSummaryByOrderId
                )

                val isEmpty = state.items.isEmpty()
                tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                tvEmpty.text = state.emptyMessage
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.effects.collect { effect ->
                when (effect) {
                    is CustomerOrdersUiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    CustomerOrdersUiEffect.NavigateToCart -> {
                        findNavController().navigate(R.id.cartFragment)
                    }
                }
            }
        }
    }

    private fun setupFilterViews(root: View) {
        filterViews = mapOf(
            CustomerOrderDateFilter.ALL to root.findViewById(R.id.filterAll),
            CustomerOrderDateFilter.TODAY to root.findViewById(R.id.filterToday),
            CustomerOrderDateFilter.YESTERDAY to root.findViewById(R.id.filterYesterday),
            CustomerOrderDateFilter.TWO_DAYS_AGO to root.findViewById(R.id.filterTwoDaysAgo),
            CustomerOrderDateFilter.LAST_7_DAYS to root.findViewById(R.id.filterLast7Days),
            CustomerOrderDateFilter.LAST_14_DAYS to root.findViewById(R.id.filterLast14Days),
            CustomerOrderDateFilter.EARLIER to root.findViewById(R.id.filterEarlier)
        )

        filterViews.forEach { (filter, textView) ->
            textView.setOnClickListener {
                vm.selectFilter(filter)
            }
        }
    }

    private fun updateFilterSelectionUi(selectedFilter: CustomerOrderDateFilter) {
        filterViews.forEach { (filter, textView) ->
            val isSelected = filter == selectedFilter
            textView.setBackgroundResource(
                if (isSelected) R.drawable.bg_orders_filter_chip_selected
                else R.drawable.bg_orders_filter_chip
            )
            textView.setTextColor(
                color(
                    if (isSelected) R.color.kc_text_primary else R.color.kc_text_secondary
                )
            )
        }
    }

    private fun attachSwipeToHide(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val order = adapter.getOrderAt(position) ?: return
                vm.hideOrder(order.orderId)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                drawSwipeBackground(c, viewHolder.itemView, dX)
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    private fun drawSwipeBackground(canvas: Canvas, itemView: View, dX: Float) {
        if (dX == 0f) return
        if (abs(dX) < dp(24f)) return

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = color(R.color.kc_brand_button)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = color(R.color.kc_text_inverse)
            textAlign = Paint.Align.CENTER
            textSize = dp(14f)
            isFakeBoldText = true
        }

        val top = itemView.top + dp(8f)
        val bottom = itemView.bottom - dp(8f)

        val rect = if (dX > 0) {
            RectF(
                itemView.left + dp(8f),
                top,
                itemView.left + dX - dp(4f),
                bottom
            )
        } else {
            RectF(
                itemView.right + dX + dp(4f),
                top,
                itemView.right - dp(8f),
                bottom
            )
        }

        if (rect.width() <= 0f) return

        canvas.drawRoundRect(rect, dp(22f), dp(22f), backgroundPaint)

        val textY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("Hide", rect.centerX(), textY, textPaint)
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    private fun dp(value: Float): Float = value * requireContext().resources.displayMetrics.density
}