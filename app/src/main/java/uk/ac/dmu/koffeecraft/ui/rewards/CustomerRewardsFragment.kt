package uk.ac.dmu.koffeecraft.ui.rewards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.core.di.appContainer
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager

class CustomerRewardsFragment : Fragment(R.layout.fragment_customer_rewards) {

    private lateinit var viewModel: CustomerRewardsViewModel

    private lateinit var tvBeansCount: TextView
    private lateinit var tvBeansSubtitle: TextView
    private lateinit var progressBeansBooster: LinearProgressIndicator
    private lateinit var tvBeansProgress: TextView
    private lateinit var adapter: CustomerRewardsAdapter

    private val customerId: Long?
        get() = SessionManager.currentCustomerId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            CustomerRewardsViewModel.Factory(appContainer.customerRewardsRepository)
        )[CustomerRewardsViewModel::class.java]

        tvBeansCount = view.findViewById(R.id.tvBeansCount)
        tvBeansSubtitle = view.findViewById(R.id.tvBeansSubtitle)
        progressBeansBooster = view.findViewById(R.id.progressBeansBooster)
        tvBeansProgress = view.findViewById(R.id.tvBeansProgress)

        val rvRewards = view.findViewById<RecyclerView>(R.id.rvRewards)
        rvRewards.layoutManager = LinearLayoutManager(requireContext())

        adapter = CustomerRewardsAdapter(emptyList()) { reward ->
            viewModel.handleRewardAction(reward)
        }
        rvRewards.adapter = adapter

        observeState()

        val safeCustomerId = customerId
        if (safeCustomerId == null) {
            Toast.makeText(requireContext(), "Please sign in first.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBeansBooster.max = BeansBoosterManager.BOOSTER_STEP
        viewModel.start(safeCustomerId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshRewards()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvBeansCount.text = state.beansCountValue
                tvBeansSubtitle.text = state.beansSubtitle
                progressBeansBooster.progress = state.boosterProgress
                tvBeansProgress.text = state.boosterProgressText
                adapter.submitList(state.rewards)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is CustomerRewardsViewModel.UiEffect.ShowMessage -> {
                        Toast.makeText(requireContext(), effect.message, Toast.LENGTH_SHORT).show()
                    }

                    is CustomerRewardsViewModel.UiEffect.OpenRewardPicker -> {
                        RewardProductPickerBottomSheet.newInstance(
                            category = effect.category,
                            rewardType = effect.rewardType,
                            beansCost = effect.beansCost
                        ).show(parentFragmentManager, "reward_picker")
                    }
                }
            }
        }
    }
}