package com.example.taskearner


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
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
    private var currentUserUid: String = ""
    private var profileUid: String = ""
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInspectUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        firebaseAuth = FirebaseAuth.getInstance()

        profileUid = intent.getStringExtra("uid") ?: run {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserUid = firebaseAuth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        userProfile = UserProfile(uid = profileUid)

        setupButtons()
        verifyProfileBeforeLoading()
    }

    private fun setupButtons() {
        if (currentUserUid == profileUid) {
            binding.follow.visibility = View.GONE
            binding.unfollow.visibility = View.GONE
        } else {
            checkFollowingStatus()
        }

        binding.follow.setOnClickListener { followUser() }
        binding.unfollow.setOnClickListener { unfollowUser() }
        binding.followersCount.setOnClickListener { showFollowList(true) }
        binding.followingCount.setOnClickListener { showFollowList(false) }

    }

    private fun checkFollowingStatus() {
        database.child("Follow").child(currentUserUid).child("following").child(profileUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isFollowing = snapshot.exists()
                    updateFollowButtonVisibility()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@InspectUser,
                        "Error checking follow status",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateFollowButtonVisibility() {
        binding.follow.visibility = if (isFollowing) View.GONE else View.VISIBLE
        binding.unfollow.visibility = if (isFollowing) View.VISIBLE else View.GONE
    }

    private fun followUser() {
        val updates = hashMapOf<String, Any>(
            "Follow/$currentUserUid/following/$profileUid" to true,
            "Follow/$profileUid/followers/$currentUserUid" to true
        )

        database.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isFollowing = true
                updateFollowButtonVisibility()
                updateFollowCounts(true)  // Updated this line
                Toast.makeText(this, "Followed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unfollowUser() {
        val updates = hashMapOf<String, Any?>(
            "Follow/$currentUserUid/following/$profileUid" to null,
            "Follow/$profileUid/followers/$currentUserUid" to null
        )

        database.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                isFollowing = false
                updateFollowButtonVisibility()
                updateFollowCounts(false)  // Updated this line
                Toast.makeText(this, "Unfollowed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFollowCounts(isFollow: Boolean) {
        val increment = if (isFollow) 1 else -1

        database.child("Users").child(profileUid).child("followersCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentFollowers = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentFollowers + increment
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("FollowCount", "Error updating followers count", error.toException())
                    }
                }
            })

        database.child("Users").child(currentUserUid).child("followingCount")
            .runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentFollowing = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = currentFollowing + increment
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("FollowCount", "Error updating following count", error.toException())
                    }
                }
            })
    }

    private fun loadFollowCounts() {
        database.child("Users").child(profileUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followers = snapshot.child("followersCount").getValue(Int::class.java) ?: 0
                    val following = snapshot.child("followingCount").getValue(Int::class.java) ?: 0

                    binding.followersCount.text = "$followers Followers"
                    binding.followingCount.text = "$following Following"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FollowCount", "Error loading follow counts", error.toException())
                }
            })
    }

    private fun showFollowList(showFollowers: Boolean) {
        val intent = Intent(this, FollowListActivity::class.java).apply {
            putExtra("userId", profileUid)
            putExtra("showFollowers", showFollowers)
        }
        startActivity(intent)
    }

    private fun verifyProfileBeforeLoading() {
        database.child("Users").child(currentUserUid).child("hasEditedProfile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.getValue(Boolean::class.java) == true) {
                        loadUserProfile()
                    } else {
                        showProfileEditRequiredDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorAndFinish("Error verifying profile: ${error.message}")
                }
            })
    }

    private fun loadUserProfile() {
        database.child("EditedAccount").child(profileUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        showErrorAndFinish("User profile not found")
                        return
                    }

                    userProfile = snapshot.getValue(UserProfile::class.java)?.apply {
                        uid = profileUid
                    } ?: UserProfile(uid = profileUid)

                    populateUserProfile()
                    setupClickListeners()
                    loadFollowCounts()
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
            openWebLink(userProfile.github)
        }

        binding.linkedin.setOnClickListener {
            openWebLink(userProfile.linkedin)
        }

        binding.instagram.setOnClickListener {
            openWebLink(userProfile.instagram)
        }

        binding.gmail.setOnClickListener {
            openEmailClient(userProfile.email)
        }
    }

    private fun openEmailClient(email: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, "")
                putExtra(Intent.EXTRA_TEXT, "")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWebLink(url: String) {
        if (url.isEmpty()) {
            Toast.makeText(this, "No link available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Uri.parse("https://$url")
            } else {
                Uri.parse(url)
            }

            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
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