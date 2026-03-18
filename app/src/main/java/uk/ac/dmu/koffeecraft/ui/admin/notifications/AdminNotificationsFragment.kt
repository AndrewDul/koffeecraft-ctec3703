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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.AppNotification
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import java.util.Locale

class AdminNotificationsFragment : Fragment(R.layout.fragment_admin_notifications) {

    private lateinit var adapter: AdminNotificationsAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tvQueueSummary: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvQueueSummary = view.findViewById(R.id.tvQueueSummary)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = AdminNotificationsAdapter(
            items = emptyList(),
            onDelete = { item -> showDeleteDialog(db, item) },
            onNext = { item -> advanceOrder(db, item) }
        )
        rv.adapter = adapter

        attachSwipeToDelete(rv, db)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.notificationDao().markAllAdminAsRead()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.notificationDao().observeAdminNotifications().collect { items ->
                adapter.submitList(items)
                tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                tvQueueSummary.text = buildQueueSummary(items)
            }
        }
    }

    private fun buildQueueSummary(items: List<AppNotification>): String {
        if (items.isEmpty()) return "No admin notifications right now."

        val actionNeeded = items.count {
            it.notificationType == "ADMIN_ORDER_ACTION" &&
                    (it.orderStatus == "PLACED" || it.orderStatus == "PREPARING" || it.orderStatus == "READY")
        }

        val removable = items.count { it.orderStatus == "COLLECTED" }

        return buildString {
            append("${items.size} notification")
            if (items.size != 1) append("s")
            append(" in queue")

            if (actionNeeded > 0) {
                append(" • ")
                append("$actionNeeded need action")
            }

            if (removable > 0) {
                append(" • ")
                append("$removable ready to clear")
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

    private fun showDeleteDialog(db: KoffeeCraftDatabase, item: AppNotification) {
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
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.notificationDao().deleteById(item.notificationId)
                }
            }
            .show()
    }

    private fun advanceOrder(db: KoffeeCraftDatabase, item: AppNotification) {
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
            .setMessage("Order #${item.orderId} will be moved from ${formatStatus(item.orderStatus)} to $nextLabel.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    AdminNotificationManager.advanceOrderFromNotification(
                        context = requireContext(),
                        db = db,
                        notification = item
                    )
                }
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