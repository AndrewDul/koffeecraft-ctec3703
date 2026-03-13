package uk.ac.dmu.koffeecraft.ui.admin.notifications

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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

class AdminNotificationsFragment : Fragment(R.layout.fragment_admin_notifications) {

    private lateinit var adapter: AdminNotificationsAdapter
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminNotifications)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())

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
            }
        }
    }

    private fun attachSwipeToDelete(rv: RecyclerView, db: KoffeeCraftDatabase) {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
        }

        ItemTouchHelper(callback).attachToRecyclerView(rv)
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
            .setTitle("Delete notification?")
            .setMessage("This will remove the notification for order #${item.orderId}.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.notificationDao().deleteById(item.notificationId)
                }
            }
            .show()
    }

    private fun advanceOrder(db: KoffeeCraftDatabase, item: AppNotification) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Move order to next status?")
            .setMessage("Order #${item.orderId} will be moved from ${item.orderStatus} to the next status.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Next") { _, _ ->
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
}