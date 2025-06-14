package com.example.taskearner


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.taskearner.databinding.ActivityInspectUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class InspectUser : AppCompatActivity() {

    private lateinit var binding: ActivityInspectUserBinding
    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userProfile: UserProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInspectUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        firebaseAuth = FirebaseAuth.getInstance()

        val uid = intent.getStringExtra("uid") ?: run {
            finish()
            return
        }

        val currentUserUid = firebaseAuth.currentUser?.uid ?: run {
            finish()
            return
        }

        binding.follow.setOnClickListener {
            followUser(currentUserUid, uid)
        }

        verifyProfileBeforeLoading(currentUserUid, uid)
    }

    private fun verifyProfileBeforeLoading(currentUserUid: String, profileUid: String) {
        database.child("Users").child(currentUserUid).child("hasEditedProfile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasEdited = snapshot.getValue(Boolean::class.java) ?: false

                    if (hasEdited) {
                        loadUserProfile(profileUid)
                    } else {
                        showProfileEditRequiredDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorAndFinish("Error verifying profile: ${error.message}")
                }
            })
    }

    fun followUser(currentUserId: String, targetUserId: String) {
        val followRef =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Follow")
        followRef.child(currentUserId).child("following").child(targetUserId).setValue(true)
        followRef.child(targetUserId).child("followers").child(currentUserId).setValue(true)
    }

    fun unfollowUser(currentUserId: String, targetUserId: String) {
        val followRef = FirebaseDatabase.getInstance().getReference("Follow")
        followRef.child(currentUserId).child("following").child(targetUserId).removeValue()
        followRef.child(targetUserId).child("followers").child(currentUserId).removeValue()
    }

    private fun loadUserProfile(uid: String) {
        database.child("EditedAccount").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showErrorAndFinish("User profile not found")
                        return
                    }

                    userProfile = UserProfile(
                        name = snapshot.child("name").getValue(String::class.java) ?: "User",
                        domain = snapshot.child("domain").getValue(String::class.java) ?: "",
                        email = snapshot.child("email").getValue(String::class.java) ?: "",
                        github = snapshot.child("github").getValue(String::class.java) ?: "",
                        linkedin = snapshot.child("linkedin").getValue(String::class.java) ?: "",
                        instagram = snapshot.child("instagram").getValue(String::class.java) ?: "",
                        achievements = snapshot.child("achievements").getValue(String::class.java)
                            ?: "",
                        orgname = snapshot.child("orgname").getValue(String::class.java) ?: "",
                        profileImage = snapshot.child("profileImage").getValue(String::class.java)
                            ?: "",
                        access = snapshot.child("Access").getValue(Boolean::class.java) ?: false,
                        skill = snapshot.child("skill").getValue(String::class.java) ?: ""
                    )

                    populateUserProfile()
                    setupClickListeners()
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorAndFinish("Error loading profile: ${error.message}")
                }
            })
    }

    private fun populateUserProfile() {
        with(binding) {

            userName.text = userProfile.name
            skill.text = userProfile.domain
            skillsreal.text = userProfile.skill



            organisation.text = userProfile.orgname

            if (userProfile.profileImage.isNotEmpty()) {
                Glide.with(this@InspectUser)
                    .load(userProfile.profileImage)
                    .placeholder(R.drawable.profile)
                    .into(mainImg)

                Glide.with(this@InspectUser)
                    .load(userProfile.profileImage)
                    .placeholder(R.drawable.profile)
                    .into(childImg)
            }
        }
    }

    private fun setupClickListeners() {

        binding.github.setOnClickListener {
            openWebLink("${userProfile.github}")
        }

        binding.linkedin.setOnClickListener {
            openWebLink("${userProfile.linkedin}")
        }

        binding.instagram.setOnClickListener {
            openWebLink("${userProfile.instagram}")
        }
        binding.gmail.setOnClickListener {
            openEmailClient(userProfile.email)
        }
    }

    private fun openEmailClient(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Only email apps should handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                putExtra(Intent.EXTRA_SUBJECT, "")
                putExtra(Intent.EXTRA_TEXT, "")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProfileEditRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profile Edit Required")
            .setMessage("You must complete your profile before you can view other profiles")
            .setPositiveButton("Edit Profile") { _, _ ->
                startActivity(Intent(this, EditAccount::class.java))
                finish()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}