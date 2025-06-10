package com.example.taskearner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.taskearner.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var selectedImageUri: Uri? = null
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        initCloudinary()

        // Setup Domain AutoComplete
        val domainOptions = listOf(
            "Android Developer",
            "Web Developer",
            "Graphic Designer",
            "ML Developer",
            "HR",
            "Manager",
            "Anonymous"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, domainOptions)
        binding.domainET.setAdapter(adapter)
        binding.domainET.setOnClickListener { binding.domainET.showDropDown() }

        binding.togglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            binding.passwordET.inputType =
                if (isPasswordVisible)
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            binding.togglePassword.setImageResource(
                if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
            )
            binding.passwordET.setSelection(binding.passwordET.text.length)
        }

        binding.signUpImg.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        binding.continueBtn.setOnClickListener {
            val name = binding.nameET.text.toString().trim()
            val email = binding.emailET.text.toString().trim()
            val pass = binding.passwordET.text.toString().trim()
            val domain = binding.domainET.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && domain.isNotEmpty()) {
                if (selectedImageUri != null) {
                    showLoading()
                    uploadToCloudinary(selectedImageUri!!, name, email, pass, domain)
                } else {
                    Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initCloudinary() {
        try {
            val config = HashMap<String, String>().apply {
                put("cloud_name", "daq5eddi4")
                put("api_key", "811288581511616")
                put("api_secret", "AktCLC3S34tNUEqNgY_iG74e3j8")
            }
            MediaManager.init(this, config)
        } catch (e: Exception) {
            Log.e("CloudinaryInit", "Cloudinary already initialized or failed", e)
        }
    }

    private fun uploadToCloudinary(
        uri: Uri,
        name: String,
        email: String,
        pass: String,
        domain: String
    ) {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
        showLoading()
        MediaManager.get().upload(uri)
            .option("resource_type", "image")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Upload", "Started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    Log.d("Upload", "Progress: $progress%")
                }

                override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                    hideLoading()
                    val imageUrl = resultData["secure_url"]?.toString()
                    if (imageUrl != null) {
                        createUser(name, email, pass, domain, imageUrl)
                    } else {
                        Toast.makeText(this@SignUp, "Failed to get image URL", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    hideLoading()
                    Log.e("Upload", "Error: ${error.description}")
                    Toast.makeText(
                        this@SignUp,
                        "Upload failed: ${error.description}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.w("Upload", "Reschedule: ${error.description}")
                }
            }).dispatch()
    }

    private fun createUser(
        name: String,
        email: String,
        pass: String,
        domain: String,
        imageUrl: String
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = mapOf(
                        "name" to name,
                        "email" to email,
                        "Password" to pass,
                        "Points" to 0,
                        "domain" to domain,
                        "profileImage" to imageUrl
                    )
                    database.child("Users").child(uid).setValue(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, BottomNavContainer::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Database error: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Auth error: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.signUpImg.setImageURI(selectedImageUri)
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

