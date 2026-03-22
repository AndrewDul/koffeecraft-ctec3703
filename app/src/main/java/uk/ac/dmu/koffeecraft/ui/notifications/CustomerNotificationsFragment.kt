package uk.ac.dmu.koffeecraft.ui.notifications

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerNotificationsFragment : Fragment(R.layout.fragment_customer_notifications) {

    private lateinit var viewModel: CustomerNotificationsViewModel
    private lateinit var adapter: CustomerNotificationsAdapter
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId ?: return

        viewModel = ViewModelProvider(
            this,
            CustomerNotificationsViewModel.Factory(appContainer.customerNotificationsRepository)
        )[CustomerNotificationsViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = CustomerNotificationsAdapter(
            items = emptyList(),
            detailsByOrderId = emptyMap(),
            onDelete = { item ->
                viewModel.deleteNotification(item.notificationId)
            },
            onOpen = { item ->
                viewModel.openNotification(item)
            }
        )
        rv.adapter = adapter

        attachSwipeToDelete(rv)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitData(state.items, state.detailsByOrderId)
                tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
            }
        }

        viewModel.start(customerId)
    }

    private fun attachSwipeToDelete(rv: RecyclerView) {
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
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val item = adapter.getItemAt(position)
                viewModel.deleteNotification(item.notificationId)
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

        canvas.drawRoundRect(rect, dp(22f), dp(22f), backgroundPaint)

        val textY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("Remove", rect.centerX(), textY, textPaint)
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }

    private fun dp(value: Float): Float = value * requireContext().resources.displayMetrics.density
}