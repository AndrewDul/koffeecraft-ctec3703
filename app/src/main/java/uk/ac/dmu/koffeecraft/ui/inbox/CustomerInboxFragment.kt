package uk.ac.dmu.koffeecraft.ui.inbox

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

class CustomerInboxFragment : Fragment(R.layout.fragment_customer_inbox) {

    private lateinit var viewModel: CustomerInboxViewModel
    private lateinit var adapter: CustomerInboxAdapter
    private lateinit var tvEmpty: TextView

    private lateinit var tvFilterAll: TextView
    private lateinit var tvFilterRead: TextView
    private lateinit var tvFilterUnread: TextView
    private lateinit var tvFilterPromo: TextView
    private lateinit var tvFilterImportant: TextView
    private lateinit var tvFilterService: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val launchInboxMessageId = arguments?.getLong("launchInboxMessageId", -1L)?.takeIf { it > 0L }

        viewModel = ViewModelProvider(
            this,
            CustomerInboxViewModel.Factory(
                repository = appContainer.customerInboxRepository,
                sessionRepository = appContainer.sessionRepository
            )
        )[CustomerInboxViewModel::class.java]

        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerInbox)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        tvFilterAll = view.findViewById(R.id.tvFilterAll)
        tvFilterRead = view.findViewById(R.id.tvFilterRead)
        tvFilterUnread = view.findViewById(R.id.tvFilterUnread)
        tvFilterPromo = view.findViewById(R.id.tvFilterPromo)
        tvFilterImportant = view.findViewById(R.id.tvFilterImportant)
        tvFilterService = view.findViewById(R.id.tvFilterService)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(false)
        rv.clipToPadding = false

        adapter = CustomerInboxAdapter(
            items = emptyList(),
            onDelete = { item ->
                viewModel.deleteMessage(item.inboxMessageId)
            },
            onOpen = { item ->
                viewModel.openMessage(item)
            }
        )
        rv.adapter = adapter

        setupFilters()
        attachSwipeToDelete(rv)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitList(state.filteredItems)
                tvEmpty.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
                tvEmpty.text = state.emptyMessage
                updateFilterChipStyles(state.currentFilter)
            }
        }

        viewModel.start(launchInboxMessageId)
    }

    private fun setupFilters() {
        tvFilterAll.setOnClickListener {
            viewModel.setFilter(InboxFilter.ALL)
        }

        tvFilterRead.setOnClickListener {
            viewModel.setFilter(InboxFilter.READ)
        }

        tvFilterUnread.setOnClickListener {
            viewModel.setFilter(InboxFilter.UNREAD)
        }

        tvFilterPromo.setOnClickListener {
            viewModel.setFilter(InboxFilter.PROMO)
        }

        tvFilterImportant.setOnClickListener {
            viewModel.setFilter(InboxFilter.IMPORTANT)
        }

        tvFilterService.setOnClickListener {
            viewModel.setFilter(InboxFilter.SERVICE)
        }
    }

    private fun updateFilterChipStyles(currentFilter: InboxFilter) {
        styleFilterChip(tvFilterAll, currentFilter == InboxFilter.ALL)
        styleFilterChip(tvFilterRead, currentFilter == InboxFilter.READ)
        styleFilterChip(tvFilterUnread, currentFilter == InboxFilter.UNREAD)
        styleFilterChip(tvFilterPromo, currentFilter == InboxFilter.PROMO)
        styleFilterChip(tvFilterImportant, currentFilter == InboxFilter.IMPORTANT)
        styleFilterChip(tvFilterService, currentFilter == InboxFilter.SERVICE)
    }

    private fun styleFilterChip(view: TextView, selected: Boolean) {
        if (selected) {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip_selected)
            view.setTextColor(color(R.color.kc_text_primary))
        } else {
            view.setBackgroundResource(R.drawable.bg_orders_filter_chip)
            view.setTextColor(color(R.color.kc_text_secondary))
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
                viewModel.deleteMessage(item.inboxMessageId)
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