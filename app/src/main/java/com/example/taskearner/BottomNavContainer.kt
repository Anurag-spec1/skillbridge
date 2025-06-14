package com.example.taskearner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.taskearner.databinding.ActivityBottomNavContainerBinding

class BottomNavContainer : AppCompatActivity() {
    private lateinit var binding: ActivityBottomNavContainerBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBottomNavContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val home = HomeUploadsUsers()
        val profile = Profile()
        val upload = AddUpload()


        changefragment(home)

        binding.bottomNavBar.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> changefragment(home)
                R.id.account -> changefragment(profile)
                R.id.upload -> changefragment(upload)
            }
            true
        }

    }

    private fun changefragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.Frame, fragment)
            commit()
        }
    }

}