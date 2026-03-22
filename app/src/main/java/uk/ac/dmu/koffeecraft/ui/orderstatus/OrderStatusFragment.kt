package uk.ac.dmu.koffeecraft.ui.orderstatus

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.util.notifications.AdminNotificationManager
import uk.ac.dmu.koffeecraft.util.orders.OrderSimulationManager

class OrderStatusFragment : Fragment(R.layout.fragment_order_status) {

    private lateinit var vm: OrderStatusViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvHeroIcon = view.findViewById<TextView>(R.id.tvHeroIcon)
        val tvHeroTitle = view.findViewById<TextView>(R.id.tvHeroTitle)
        val tvHeroSubtitle = view.findViewById<TextView>(R.id.tvHeroSubtitle)

        val tvStatusChip = view.findViewById<TextView>(R.id.tvStatusChip)
        val tvOrderId = view.findViewById<TextView>(R.id.tvOrderId)

        val tvItemsOrderedValue = view.findViewById<TextView>(R.id.tvItemsOrderedValue)
        val tvTotalPaidValue = view.findViewById<TextView>(R.id.tvTotalPaidValue)
        val tvPaymentTypeValue = view.findViewById<TextView>(R.id.tvPaymentTypeValue)
        val tvEtaValue = view.findViewById<TextView>(R.id.tvEtaValue)

        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvStatusHelper = view.findViewById<TextView>(R.id.tvStatusHelper)
        val tvFeedbackHelper = view.findViewById<TextView>(R.id.tvFeedbackHelper)

        val tvStepPlaced = view.findViewById<TextView>(R.id.tvStepPlaced)
        val tvStepPreparing = view.findViewById<TextView>(R.id.tvStepPreparing)
        val tvStepReady = view.findViewById<TextView>(R.id.tvStepReady)
        val tvStepCollected = view.findViewById<TextView>(R.id.tvStepCollected)

        val btnBackToHome = view.findViewById<MaterialButton>(R.id.btnBackToHome)
        val btnViewOrders = view.findViewById<MaterialButton>(R.id.btnViewOrders)
        val btnFeedback = view.findViewById<MaterialButton>(R.id.btnFeedback)

        val orderId = requireArguments().getLong("orderId")
        val simulate = requireArguments().getBoolean("simulate", false)
        val fromCheckout = requireArguments().getBoolean("fromCheckout", false)

        val db = appContainer.database

        vm = ViewModelProvider(
            this,
            OrderStatusViewModelFactory(appContainer.customerOrdersRepository)
        )[OrderStatusViewModel::class.java]

        tvOrderId.text = "Order #$orderId"

        btnBackToHome.setOnClickListener {
            findNavController().navigate(R.id.customerHomeFragment)
        }

        btnViewOrders.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment)
        }

        btnFeedback.setOnClickListener {
            if (!btnFeedback.isEnabled) return@setOnClickListener

            findNavController().navigate(
                R.id.feedbackFragment,
                bundleOf("orderId" to orderId)
            )
        }

        if (simulate) {
            OrderSimulationManager.startIfNeeded(requireContext().applicationContext, orderId)
        }

        vm.start(orderId = orderId, fromCheckout = fromCheckout)

        var lastSyncedSignature: String? = null

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collect { state ->
                if (state.orderId == 0L) return@collect

                tvOrderId.text = "Order #${state.orderId}"

                tvStatusChip.text = state.statusLabel
                tvStatus.text = state.statusLabel
                bindStatusChip(tvStatusChip, state.statusRaw)

                tvItemsOrderedValue.text = state.itemsOrderedLabel
                tvTotalPaidValue.text = state.totalPaidLabel
                tvPaymentTypeValue.text = state.paymentTypeLabel
                tvEtaValue.text = state.etaLabel
                tvStatusHelper.text = state.statusHelper
                tvFeedbackHelper.text = state.feedbackHelper

                bindHero(
                    iconView = tvHeroIcon,
                    titleView = tvHeroTitle,
                    subtitleView = tvHeroSubtitle,
                    tone = state.heroTone,
                    icon = state.heroIcon,
                    title = state.heroTitle,
                    subtitle = state.heroSubtitle
                )

                setJourneyStep(tvStepPlaced, state.stepPlacedActive)
                setJourneyStep(tvStepPreparing, state.stepPreparingActive)
                setJourneyStep(tvStepReady, state.stepReadyActive)
                setJourneyStep(tvStepCollected, state.stepCollectedActive)

                btnFeedback.visibility = View.VISIBLE
                btnFeedback.isEnabled = state.feedbackEnabled
                btnFeedback.alpha = if (state.feedbackEnabled) 1f else 0.45f
                btnFeedback.text = state.feedbackButtonLabel

                if (!simulate) {
                    val signature = "${state.orderId}:${state.statusRaw}:${state.createdAt}"
                    if (signature != lastSyncedSignature) {
                        lastSyncedSignature = signature

                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            AdminNotificationManager.syncAdminOrderActionNotification(
                                context = requireContext(),
                                db = db,
                                orderId = state.orderId,
                                orderCreatedAt = state.createdAt,
                                orderStatus = state.statusRaw
                            )
                        }
                    }
                }
            }
        }
    }

    private fun bindHero(
        iconView: TextView,
        titleView: TextView,
        subtitleView: TextView,
        tone: HeroTone,
        icon: String,
        title: String,
        subtitle: String
    ) {
        iconView.text = icon
        titleView.text = title
        subtitleView.text = subtitle

        iconView.setTextColor(
            when (tone) {
                HeroTone.SUCCESS -> color(R.color.kc_success_text)
                HeroTone.WARM -> color(R.color.kc_warning_text)
                HeroTone.NEUTRAL -> color(R.color.kc_info_text)
                HeroTone.DANGER -> color(R.color.kc_danger_text)
            }
        )
    }

    private fun setJourneyStep(view: TextView, isActive: Boolean) {
        view.setBackgroundResource(
            if (isActive) R.drawable.bg_orders_filter_chip_selected
            else R.drawable.bg_orders_filter_chip
        )
        view.setTextColor(
            color(
                if (isActive) R.color.kc_text_primary else R.color.kc_text_secondary
            )
        )
        view.alpha = if (isActive) 1f else 0.9f
    }

    private fun bindStatusChip(view: TextView, status: String) {
        val background = view.background.mutate() as GradientDrawable

        val (backgroundColorRes, textColorRes) = when (status.uppercase()) {
            "READY" -> R.color.kc_validation_valid_bg to R.color.kc_success_text
            "PREPARING" -> R.color.kc_surface_warning to R.color.kc_warning_text
            "PLACED" -> R.color.kc_surface_info to R.color.kc_info_text
            "COLLECTED" -> R.color.kc_surface_success to R.color.kc_success_text
            "COMPLETED" -> R.color.kc_surface_success to R.color.kc_success_text
            "CANCELLED" -> R.color.kc_surface_error to R.color.kc_danger_text
            else -> R.color.kc_status_neutral_bg to R.color.kc_neutral_text
        }

        background.setColor(color(backgroundColorRes))
        view.setTextColor(color(textColorRes))
    }

    private fun color(colorResId: Int): Int {
        return ContextCompat.getColor(requireContext(), colorResId)
    }
}