package com.example.taskearner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taskearner.databinding.ActivityEditAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditAccount : AppCompatActivity() {

    private lateinit var binding: ActivityEditAccountBinding
    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        firebaseAuth = FirebaseAuth.getInstance()

        binding.save.setOnClickListener {
            saveProfileData()
        }

        loadExistingData()
    }

    private fun loadExistingData() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("EditedAccount").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.github.setText(
                        snapshot.child("github").getValue(String::class.java) ?: ""
                    )
                    binding.linkedin.setText(
                        snapshot.child("linkedin").getValue(String::class.java) ?: ""
                    )
                    binding.instagram.setText(
                        snapshot.child("instagram").getValue(String::class.java) ?: ""
                    )
                    binding.skill.setText(
                        snapshot.child("skill").getValue(String::class.java) ?: ""
                    )
                    binding.achievement.setText(
                        snapshot.child("achievements").getValue(String::class.java) ?: ""
                    )
                    binding.organisation.setText(
                        snapshot.child("orgname").getValue(String::class.java) ?: ""
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@EditAccount,
                        "Error loading existing data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveProfileData() {
        val github = binding.github.text.toString().trim()
        val linkedin = binding.linkedin.text.toString().trim()
        val instagram = binding.instagram.text.toString().trim()
        val skill = binding.skill.text.toString().trim()
        val achievements = binding.achievement.text.toString().trim()
        val orgname = binding.organisation.text.toString().trim()
        val bool: Boolean = true

        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java) ?: ""
                    val domain = snapshot.child("domain").getValue(String::class.java) ?: ""
                    val profileImage =
                        snapshot.child("profileImage").getValue(String::class.java) ?: ""

                    val updates = mapOf(
                        "github" to github,
                        "linkedin" to linkedin,
                        "instagram" to instagram,
                        "skill" to skill,
                        "achievements" to achievements,
                        "orgname" to orgname,
                        "name" to name,
                        "email" to email,
                        "domain" to domain,
                        "profileImage" to profileImage,
                        "Access" to bool,
                        "hasEditedProfile" to true
                    )


                    val updatesMap = mapOf(
                        "EditedAccount/$uid" to updates,
                        "Users/$uid/hasEditedProfile" to true
                    )

                    database.updateChildren(updatesMap)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    this@EditAccount,
                                    "Profile updated successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            } else {
                                Toast.makeText(
                                    this@EditAccount,
                                    "Failed to update details",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditAccount, "Error: ${error.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }
}