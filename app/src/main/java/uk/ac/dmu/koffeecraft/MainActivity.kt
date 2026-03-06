package uk.ac.dmu.koffeecraft

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.dmu.koffeecraft.data.db.KoffeeCraftDatabase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = KoffeeCraftDatabase.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            val admins = db.adminDao().countAdmins()
            val products = db.productDao().countProducts()
            Log.d("KoffeeCraftDB", "admins=$admins, products=$products")
        }
    }
}