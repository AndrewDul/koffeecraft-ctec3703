package uk.ac.dmu.koffeecraft.ui.rewards

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.cart.CartManager
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product
import uk.ac.dmu.koffeecraft.data.session.SessionManager

class CustomerRewardsFragment : Fragment(R.layout.fragment_customer_rewards) {

    private lateinit var tvBeansCount: TextView
    private lateinit var tvBeansSubtitle: TextView
    private lateinit var adapter: CustomerRewardsAdapter

    private val customerId: Long?
        get() = SessionManager.currentCustomerId

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvBeansCount = view.findViewById(R.id.tvBeansCount)
        tvBeansSubtitle = view.findViewById(R.id.tvBeansSubtitle)

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

                tvBeansCount.text = customer.beansBalance.toString()
                tvBeansSubtitle.text =
                    "Available now: $availableBeansForNewRewards • Reserved in cart: $reservedBeans"

                val rewardProductsByName = rewardProducts.associateBy { it.name }

                val items = mutableListOf<RewardUiModel>()

                items += RewardUiModel(
                    id = "BEAN_BOOSTER",
                    title = "5 Bean Booster",
                    description = "Claim 5 extra beans when you reach your next milestone.",
                    beansLabel = "Next: ${customer.nextBeansBonusThreshold}",
                    actionLabel = if (customer.beansBalance >= customer.nextBeansBonusThreshold) {
                        "Claim +5 beans"
                    } else {
                        "Need ${customer.nextBeansBonusThreshold} beans"
                    },
                    enabled = customer.beansBalance >= customer.nextBeansBonusThreshold
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

            if (customer.beansBalance < customer.nextBeansBonusThreshold) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "You have not reached the next bean milestone yet.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            db.customerDao().update(
                customer.copy(
                    beansBalance = customer.beansBalance + 5,
                    nextBeansBonusThreshold = customer.nextBeansBonusThreshold + 10
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

                val names = options.map { it.name }.toTypedArray()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(if (category == "COFFEE") "Choose your free coffee" else "Choose your free cake")
                    .setItems(names) { _, which ->
                        val selected = options[which]
                        CartManager.addReward(
                            sourceProduct = selected,
                            rewardType = rewardType,
                            beansCostPerUnit = beansCost
                        )
                        Toast.makeText(requireContext(), "Reward added to cart.", Toast.LENGTH_SHORT).show()
                        loadRewards()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
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

            CartManager.addReward(
                sourceProduct = selected,
                rewardType = "PHYSICAL_REWARD",
                beansCostPerUnit = beansCost
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Reward added to cart.", Toast.LENGTH_SHORT).show()
                loadRewards()
            }
        }
    }
}