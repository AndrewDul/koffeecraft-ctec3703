package uk.ac.dmu.koffeecraft

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // I connect the BottomNavigationView with the Admin NavHost to handle tab navigation.
        val navHost = supportFragmentManager.findFragmentById(R.id.adminNavHost) as NavHostFragment
        val bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNav)
        bottomNav.setupWithNavController(navHost.navController)
    }
}