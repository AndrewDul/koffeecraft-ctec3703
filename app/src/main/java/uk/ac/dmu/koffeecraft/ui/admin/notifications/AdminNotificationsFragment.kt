package uk.ac.dmu.koffeecraft.ui.admin.notifications

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import java.util.Locale

class AdminNotificationsFragment : Fragment(R.layout.fragment_admin_notifications) {

    private lateinit var viewModel: AdminNotificationsViewModel
    private lateinit var adapter: AdminNotificationsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tvQueueSummary: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            AdminNotificationsViewModel.Factory(appContainer.adminNotificationsRepository)
        )[AdminNotificationsViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvQueueSummary = view.findViewById(R.id.tvQueueSummary)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = AdminNotificationsAdapter(
            items = emptyList(),
            onDelete = { item -> showDeleteDialog(item) },
            onNext = { item -> showAdvanceDialog(item) }
        )
        rv.adapter = adapter

        attachSwipeToDelete(rv)
        observeState()

        viewModel.start()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitList(state.items)
                tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
                tvQueueSummary.text = state.queueSummary
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is AdminNotificationsViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
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

                if (item.orderStatus != "COLLECTED") {
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        requireContext(),
                        "Only collected notifications can be removed.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                viewModel.deleteNotification(item)
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
            color = ContextCompat.getColor(requireContext(), R.color.kc_brand_button)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(requireContext(), R.color.kc_text_inverse)
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

    private fun showDeleteDialog(item: AppNotification) {
        if (item.orderStatus != "COLLECTED") {
            Toast.makeText(
                requireContext(),
                "Only collected notifications can be removed.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove notification?")
            .setMessage("This will clear the admin notification for order #${item.orderId}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                viewModel.deleteNotification(item)
            }
            .show()
    }

    private fun showAdvanceDialog(item: AppNotification) {
        val nextLabel = when (item.orderStatus?.uppercase(Locale.UK)) {
            "PLACED" -> "Preparing"
            "PREPARING" -> "Ready"
            "READY" -> "Collected"
            else -> null
        }

        if (nextLabel == null) {
            Toast.makeText(
                requireContext(),
                "This notification has no next admin action.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move order forward?")
            .setMessage(
                "Order #${item.orderId} will be moved from ${formatStatus(item.orderStatus)} to $nextLabel."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.advanceOrder(item)
            }
            .show()
    }

    private fun formatStatus(status: String?): String {
        if (status.isNullOrBlank()) return "Update"

        return status
            .lowercase(Locale.UK)
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { firstChar ->
                    firstChar.titlecase(Locale.UK)
                }
            }
    }

    private fun dp(value: Float): Float = value * requireContext().resources.displayMetrics.density
}