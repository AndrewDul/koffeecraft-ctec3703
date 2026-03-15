package uk.ac.dmu.koffeecraft.ui.notifications

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.dao.OrderDisplayItem
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerNotificationsFragment : Fragment(R.layout.fragment_customer_notifications) {

    private lateinit var adapter: CustomerNotificationsAdapter
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId ?: return

        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = CustomerNotificationsAdapter(
            items = emptyList(),
            detailsByOrderId = emptyMap(),
            onDelete = { item ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.notificationDao().deleteById(item.notificationId)
                }
            }
        )
        rv.adapter = adapter

        attachSwipeToDelete(rv, db)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.notificationDao().markAllCustomerAsRead(customerId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.notificationDao().observeCustomerNotifications(customerId).collect { items ->
                val detailsByOrderId: Map<Long, List<OrderDisplayItem>> = withContext(Dispatchers.IO) {
                    items.mapNotNull { it.orderId }
                        .distinct()
                        .associateWith { orderId ->
                            db.orderItemDao().getDisplayItemsForOrder(orderId)
                        }
                }

                adapter.submitData(items, detailsByOrderId)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun attachSwipeToDelete(rv: RecyclerView, db: KoffeeCraftDatabase) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItemAt(viewHolder.bindingAdapterPosition)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.notificationDao().deleteById(item.notificationId)
                }
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

        ItemTouchHelper(callback).attachToRecyclerView(rv)
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
        canvas.drawText("Remove", rect.centerX(), textY, textPaint)
    }

    private fun dp(value: Float): Float = value * requireContext().resources.displayMetrics.density
}