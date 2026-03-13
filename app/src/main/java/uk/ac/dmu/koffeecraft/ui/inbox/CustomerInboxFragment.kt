package uk.ac.dmu.koffeecraft.ui.inbox

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
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.InboxMessage
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerInboxFragment : Fragment(R.layout.fragment_customer_inbox) {

    private lateinit var adapter: CustomerInboxAdapter
    private lateinit var tvEmpty: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val customerId = SessionManager.currentCustomerId ?: return

        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerInbox)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())

        adapter = CustomerInboxAdapter(
            items = emptyList(),
            onDelete = { item ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.inboxMessageDao().deleteById(item.inboxMessageId)
                }
            }
        )
        rv.adapter = adapter

        attachSwipeToDelete(rv, db)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.inboxMessageDao().markAllAsRead(customerId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.inboxMessageDao().observeInboxForCustomer(customerId).collect { items ->
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
                val item = adapter.getItemAt(viewHolder.bindingAdapterPosition)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.inboxMessageDao().deleteById(item.inboxMessageId)
                }
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(rv)
    }
}