package uk.ac.dmu.koffeecraft.ui.orders

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Order
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.data.settings.HiddenOrdersStore
import java.util.Calendar

class OrdersFragment : Fragment(R.layout.fragment_orders) {

    private lateinit var adapter: OrdersAdapter
    private lateinit var db: KoffeeCraftDatabase
    private lateinit var tvEmpty: TextView

    private var customerId: Long? = null
    private var allOrders: List<Order> = emptyList()
    private var selectedFilter: OrderDateFilter = OrderDateFilter.ALL
    private var renderJob: Job? = null

    private lateinit var filterViews: Map<OrderDateFilter, TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBrowseMenu = view.findViewById<MaterialButton>(R.id.btnBrowseMenu)
        val rv = view.findViewById<RecyclerView>(R.id.rvOrders)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        btnBrowseMenu.setOnClickListener { findNavController().navigateUp() }

        customerId = SessionManager.currentCustomerId
        if (customerId == null) {
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "You are not logged in as a customer."
            return
        }

        db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        setupFilterViews(view)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)

        adapter = OrdersAdapter(
            items = emptyList(),
            detailsByOrderId = emptyMap(),
            feedbackSummaryByOrderId = emptyMap(),
            onOrderAgain = { order ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val reorderItems = db.orderItemDao().getReorderItems(order.orderId)

                    CartManager.clear()
                    reorderItems.forEach { CartManager.add(it.product, it.quantity) }

                    withContext(Dispatchers.Main) {
                        findNavController().navigate(R.id.cartFragment)
                    }
                }
            },
            onOpenFeedback = { order ->
                findNavController().navigate(
                    R.id.feedbackFragment,
                    bundleOf("orderId" to order.orderId)
                )
            },
            onRemoveOrder = { order ->
                hideOrder(order.orderId)
            }
        )

        rv.adapter = adapter
        attachSwipeToHide(rv)

        viewLifecycleOwner.lifecycleScope.launch {
            db.orderDao().observeByCustomer(customerId!!).collectLatest { orders ->
                allOrders = orders
                renderOrders()
            }
        }
    }

    private fun setupFilterViews(root: View) {
        filterViews = mapOf(
            OrderDateFilter.ALL to root.findViewById(R.id.filterAll),
            OrderDateFilter.TODAY to root.findViewById(R.id.filterToday),
            OrderDateFilter.YESTERDAY to root.findViewById(R.id.filterYesterday),
            OrderDateFilter.TWO_DAYS_AGO to root.findViewById(R.id.filterTwoDaysAgo),
            OrderDateFilter.LAST_7_DAYS to root.findViewById(R.id.filterLast7Days),
            OrderDateFilter.LAST_14_DAYS to root.findViewById(R.id.filterLast14Days),
            OrderDateFilter.EARLIER to root.findViewById(R.id.filterEarlier)
        )

        filterViews.forEach { (filter, textView) ->
            textView.setOnClickListener {
                selectedFilter = filter
                updateFilterSelectionUi()
                renderOrders()
            }
        }

        updateFilterSelectionUi()
    }

    private fun updateFilterSelectionUi() {
        filterViews.forEach { (filter, textView) ->
            val isSelected = filter == selectedFilter
            textView.setBackgroundResource(
                if (isSelected) R.drawable.bg_orders_filter_chip_selected
                else R.drawable.bg_orders_filter_chip
            )
            textView.setTextColor(
                Color.parseColor(
                    if (isSelected) "#2E2018" else "#6E5A4D"
                )
            )
        }
    }

    private fun renderOrders() {
        val safeCustomerId = customerId ?: return

        renderJob?.cancel()
        renderJob = viewLifecycleOwner.lifecycleScope.launch {
            val visibleOrders = allOrders
                .filterNot { order ->
                    HiddenOrdersStore.getHiddenOrderIds(
                        requireContext().applicationContext,
                        safeCustomerId
                    ).contains(order.orderId)
                }
                .filter { order ->
                    matchesSelectedFilter(order.createdAt)
                }

            val detailsAndFeedback = withContext(Dispatchers.IO) {
                visibleOrders.associate { order ->
                    val details = db.orderItemDao().getDisplayItemsForOrder(order.orderId)
                    val feedbackItems = db.orderItemDao().getFeedbackItemsForOrder(order.orderId)
                    val reviewedCount = feedbackItems.count { it.feedbackId != null }

                    order.orderId to Pair(
                        details,
                        OrderFeedbackSummary(
                            eligibleItemCount = feedbackItems.size,
                            reviewedItemCount = reviewedCount
                        )
                    )
                }
            }

            val detailsByOrderId = detailsAndFeedback.mapValues { it.value.first }
            val feedbackSummaryByOrderId = detailsAndFeedback.mapValues { it.value.second }

            adapter.submitData(
                newItems = visibleOrders,
                newDetailsByOrderId = detailsByOrderId,
                newFeedbackSummaryByOrderId = feedbackSummaryByOrderId
            )

            val isEmpty = visibleOrders.isEmpty()
            tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            tvEmpty.text = if (selectedFilter == OrderDateFilter.ALL) {
                "No orders yet."
            } else {
                "No orders found for this period."
            }
        }
    }

    private fun hideOrder(orderId: Long) {
        val safeCustomerId = customerId ?: return
        HiddenOrdersStore.hideOrder(
            context = requireContext().applicationContext,
            customerId = safeCustomerId,
            orderId = orderId
        )
        renderOrders()
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
                val order = adapter.getOrderAt(position) ?: return
                hideOrder(order.orderId)
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

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B98C73")
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8F2")
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

        canvas.drawRoundRect(rect, dp(22f), dp(22f), backgroundPaint)

        val textY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("Hide", rect.centerX(), textY, textPaint)
    }

    private fun matchesSelectedFilter(createdAt: Long): Boolean {
        val startToday = startOfDay(0)
        val startYesterday = startOfDay(1)
        val startTwoDaysAgo = startOfDay(2)
        val startSevenDaysAgo = startOfDay(7)
        val startFourteenDaysAgo = startOfDay(14)

        return when (selectedFilter) {
            OrderDateFilter.ALL -> true
            OrderDateFilter.TODAY -> createdAt >= startToday
            OrderDateFilter.YESTERDAY -> createdAt in startYesterday until startToday
            OrderDateFilter.TWO_DAYS_AGO -> createdAt in startTwoDaysAgo until startYesterday
            OrderDateFilter.LAST_7_DAYS -> createdAt in startSevenDaysAgo until startTwoDaysAgo
            OrderDateFilter.LAST_14_DAYS -> createdAt in startFourteenDaysAgo until startSevenDaysAgo
            OrderDateFilter.EARLIER -> createdAt < startFourteenDaysAgo
        }
    }

    private fun startOfDay(daysAgo: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.timeInMillis
    }

    private fun dp(value: Float): Float = value * requireContext().resources.displayMetrics.density

    private enum class OrderDateFilter {
        ALL,
        TODAY,
        YESTERDAY,
        TWO_DAYS_AGO,
        LAST_7_DAYS,
        LAST_14_DAYS,
        EARLIER
    }
}