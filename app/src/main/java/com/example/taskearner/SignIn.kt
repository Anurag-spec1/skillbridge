package com.example.taskearner

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskearner.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth

class SignIn : AppCompatActivity() {
    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var isPasswordVisible = false

        binding.togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {

                binding.passwordET.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.togglePassword.setImageResource(R.drawable.ic_visibility) // use visible icon
            } else {

                binding.passwordET.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.togglePassword.setImageResource(R.drawable.ic_visibility_off) // use hidden icon
            }
            binding.passwordET.setSelection(binding.passwordET.text.length)
        }

        firebaseAuth = FirebaseAuth.getInstance()

        binding.continueBtn.setOnClickListener {
            showLoading()
            val email = binding.emailET.text.toString().trim()
            val pass = binding.passwordET.text.toString().trim()

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {
                    if (it.isSuccessful) {
                        hideLoading()
                        startActivity(Intent(this, BottomNavContainer::class.java))
                        finish()
                    } else {
                        hideLoading()
                        Toast.makeText(this, "Create your account first", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                hideLoading()
                Toast.makeText(this, "Fields can not be empty", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun showLoading() {
        binding.apply {
            spinKit.visibility = View.VISIBLE
            spinKit.isIndeterminate = true
            card.visibility = View.VISIBLE
            root.isEnabled = false
        }
    }

    private fun hideLoading() {
        binding.apply {
            spinKit.visibility = View.GONE
            spinKit.isIndeterminate = false
            card.visibility = View.GONE
            root.isEnabled = true
        }
    }
}