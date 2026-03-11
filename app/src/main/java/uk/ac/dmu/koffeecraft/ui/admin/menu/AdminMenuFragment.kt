package uk.ac.dmu.koffeecraft.ui.admin.menu

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.dmu.koffeecraft.R
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase
import uk.ac.dmu.koffeecraft.data.entities.Product

class AdminMenuFragment : Fragment(R.layout.fragment_admin_menu) {

    private lateinit var adapter: AdminProductsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rvAdminProducts)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val fab = view.findViewById<FloatingActionButton>(R.id.fabAddProduct)

        val db = KoffeeCraftDatabase.getInstance(requireContext().applicationContext)

        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = AdminProductsAdapter(
            items = emptyList(),
            onToggle = { p ->
                // I toggle active status (disable/enable) without deleting the product.
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.productDao().setActive(p.productId, !p.isActive)
                }
            },
            onEdit = { p ->
                // I will implement edit screen later; for now I show a placeholder.
                Toast.makeText(requireContext(), "Edit: ${p.name} (TODO)", Toast.LENGTH_SHORT).show()
            },
            onDelete = { p ->
                // I hard-delete the product from DB.
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.productDao().deleteById(p.productId)
                }
            }
        )
        rv.adapter = adapter

        fab.setOnClickListener {
            // I will implement add-product screen later; for now I insert a dummy product to test UI.
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val newProduct = Product(
                    name = "New Product",
                    category = "COFFEE",
                    description = "Added from admin (placeholder).",
                    price = 1.99,
                    isActive = true,
                    isNew = true,
                    imageKey = null
                )
                db.productDao().insert(newProduct)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Inserted test product", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // I observe all products for admin management.
        viewLifecycleOwner.lifecycleScope.launch {
            db.productDao().observeAll().collect { products ->
                adapter.submitList(products)
                tvEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}