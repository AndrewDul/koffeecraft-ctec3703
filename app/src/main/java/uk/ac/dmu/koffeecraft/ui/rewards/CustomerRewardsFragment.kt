package uk.ac.dmu.koffeecraft.ui.rewards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.session.SessionManager
import uk.ac.dmu.koffeecraft.util.rewards.BeansBoosterManager




class CustomerRewardsFragment : Fragment(R.layout.fragment_customer_rewards) {

    private lateinit var tvBeansCount: TextView
    private lateinit var tvBeansSubtitle: TextView
    private lateinit var progressBeansBooster: LinearProgressIndicator
    private lateinit var tvBeansProgress: TextView
    private lateinit var adapter: CustomerRewardsAdapter

    private val customerId: Long?
        get() = SessionManager.currentCustomerId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvBeansCount = view.findViewById(R.id.tvBeansCount)
        tvBeansSubtitle = view.findViewById(R.id.tvBeansSubtitle)
        progressBeansBooster = view.findViewById(R.id.progressBeansBooster)
        tvBeansProgress = view.findViewById(R.id.tvBeansProgress)

        val rvRewards = view.findViewById<RecyclerView>(R.id.rvRewards)
        rvRewards.layoutManager = LinearLayoutManager(requireContext())

        adapter = CustomerRewardsAdapter(emptyList()) { reward ->
            handleRewardAction(reward)
        }
        rvRewards.adapter = adapter

        loadRewards()
    }

    override fun onResume() {
        super.onResume()
        loadRewards()
    }

    private fun loadRewards() {
        val cid = customerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(cid)
            val rewardProducts = db.productDao().getRewardProducts()

            withContext(Dispatchers.Main) {
                if (!isAdded || customer == null) return@withContext

                val reservedBeans = CartManager.beansToSpend()
                val availableBeansForNewRewards = (customer.beansBalance - reservedBeans).coerceAtLeast(0)

                val progress = customer.beansBoosterProgress.coerceIn(0, BeansBoosterManager.BOOSTER_STEP - 1)
                val pendingBoosters = customer.pendingBeansBoosters.coerceAtLeast(0)

                tvBeansCount.text = customer.beansBalance.toString()
                tvBeansSubtitle.text =
                    "Available now: $availableBeansForNewRewards • Reserved in cart: $reservedBeans"

                progressBeansBooster.max = BeansBoosterManager.BOOSTER_STEP
                progressBeansBooster.progress = progress
                tvBeansProgress.text = BeansBoosterManager.progressStatusText(progress, pendingBoosters)

                val rewardProductsByName = rewardProducts.associateBy { it.name }

                val items = mutableListOf<RewardUiModel>()

                items += RewardUiModel(
                    id = "BEAN_BOOSTER",
                    title = "5 Bean Booster",
                    description = "Every 10 earned beans unlock a +5 bean booster.",
                    beansLabel = BeansBoosterManager.rewardMetaLine(progress, pendingBoosters),
                    actionLabel = if (pendingBoosters > 0) "Claim +5 beans" else "Keep collecting beans",
                    enabled = pendingBoosters > 0
                )

                items += RewardUiModel(
                    id = "FREE_COFFEE",
                    title = "Free Coffee",
                    description = "Choose one crafted coffee reward.",
                    beansLabel = "15 beans",
                    actionLabel = if (availableBeansForNewRewards >= 15) "Choose reward" else "Need more beans",
                    enabled = availableBeansForNewRewards >= 15
                )

                items += RewardUiModel(
                    id = "FREE_CAKE",
                    title = "Free Cake",
                    description = "Choose one crafted cake reward.",
                    beansLabel = "18 beans",
                    actionLabel = if (availableBeansForNewRewards >= 18) "Choose reward" else "Need more beans",
                    enabled = availableBeansForNewRewards >= 18
                )

                rewardProductsByName["KoffeeCraft Mug"]?.let {
                    items += RewardUiModel(
                        id = "MUG",
                        title = "KoffeeCraft Mug",
                        description = "Premium crafted mug with KoffeeCraft branding.",
                        beansLabel = "125 beans",
                        actionLabel = if (availableBeansForNewRewards >= 125) "Add to cart" else "Need more beans",
                        enabled = availableBeansForNewRewards >= 125
                    )
                }

                rewardProductsByName["KoffeeCraft Teddy Bear"]?.let {
                    items += RewardUiModel(
                        id = "TEDDY",
                        title = "KoffeeCraft Teddy Bear",
                        description = "Soft teddy bear with KoffeeCraft branding.",
                        beansLabel = "250 beans",
                        actionLabel = if (availableBeansForNewRewards >= 250) "Add to cart" else "Need more beans",
                        enabled = availableBeansForNewRewards >= 250
                    )
                }

                rewardProductsByName["1kg Crafted Coffee Beans"]?.let {
                    items += RewardUiModel(
                        id = "BEANS_1KG",
                        title = "1kg Crafted Coffee Beans",
                        description = "One kilogram of crafted KoffeeCraft coffee beans.",
                        beansLabel = "370 beans",
                        actionLabel = if (availableBeansForNewRewards >= 370) "Add to cart" else "Need more beans",
                        enabled = availableBeansForNewRewards >= 370
                    )
                }

                adapter.submitList(items)
            }
        }
    }

    private fun handleRewardAction(reward: RewardUiModel) {
        when (reward.id) {
            "BEAN_BOOSTER" -> claimBeanBooster()
            "FREE_COFFEE" -> openRewardChoiceDialog(category = "COFFEE", rewardType = "FREE_COFFEE", beansCost = 15)
            "FREE_CAKE" -> openRewardChoiceDialog(category = "CAKE", rewardType = "FREE_CAKE", beansCost = 18)
            "MUG" -> addPhysicalReward("KoffeeCraft Mug", 125)
            "TEDDY" -> addPhysicalReward("KoffeeCraft Teddy Bear", 250)
            "BEANS_1KG" -> addPhysicalReward("1kg Crafted Coffee Beans", 370)
        }
    }

    private fun claimBeanBooster() {
        val cid = customerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(cid) ?: return@launch

            if (customer.pendingBeansBoosters <= 0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "You do not have a bean booster ready yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }

            db.customerDao().update(
                customer.copy(
                    beansBalance = customer.beansBalance + BeansBoosterManager.BOOSTER_REWARD,
                    pendingBeansBoosters = (customer.pendingBeansBoosters - 1).coerceAtLeast(0)
                )
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "You claimed +5 beans.", Toast.LENGTH_SHORT).show()
                loadRewards()
            }
        }
    }

    private fun openRewardChoiceDialog(category: String, rewardType: String, beansCost: Int) {
        val cid = customerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(cid) ?: return@launch
            val availableBeans = (customer.beansBalance - CartManager.beansToSpend()).coerceAtLeast(0)

            if (availableBeans < beansCost) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "You do not have enough beans for this reward.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val options = db.productDao().getAvailableByCategory(category)

            withContext(Dispatchers.Main) {
                if (options.isEmpty()) {
                    Toast.makeText(requireContext(), "No products are available in this category right now.", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                RewardProductPickerBottomSheet.newInstance(
                    category = category,
                    rewardType = rewardType,
                    beansCost = beansCost
                ).show(parentFragmentManager, "reward_picker")
            }
        }
    }

    private fun addPhysicalReward(productName: String, beansCost: Int) {
        val cid = customerId ?: return
        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val customer = db.customerDao().getById(cid) ?: return@launch
            val availableBeans = (customer.beansBalance - CartManager.beansToSpend()).coerceAtLeast(0)

            if (availableBeans < beansCost) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "You do not have enough beans for this reward.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val rewards = db.productDao().getRewardProducts()
            val selected = rewards.firstOrNull { it.name == productName }

            if (selected == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Reward product not found.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val added = CartManager.addReward(
                sourceProduct = selected,
                rewardType = "PHYSICAL_REWARD",
                beansCostPerUnit = beansCost
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    if (added) "Reward added to cart." else "That reward is already in your cart.",
                    Toast.LENGTH_SHORT
                ).show()
                loadRewards()
            }
        }
    }
}