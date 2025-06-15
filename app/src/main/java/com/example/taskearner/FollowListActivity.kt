package com.example.taskearner

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskearner.databinding.ActivityFollowListBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FollowListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowListBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: UserAdapter
    private val userList = mutableListOf<UserProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val access = intent.getStringExtra("hello") ?: ""


        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        val userId = intent.getStringExtra("userId") ?: run {
            finish()
            return
        }
        val showFollowers = intent.getBooleanExtra("showFollowers", true)

        binding.back.setOnClickListener {
            onBackPressed()
        }

        setupRecyclerView()
        setupText(showFollowers)

        if (showFollowers) {
            loadFollowers(userId)
        } else {
            loadFollowing(userId)
        }

    }

    private fun setupText(showFollowers: Boolean) {
        binding.constTxt.text = if (showFollowers) "Followers" else "Following"
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(userList) { user ->
            val intent = Intent(this, InspectUser::class.java).apply {
                putExtra("uid", user.uid)
            }
            startActivity(intent)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadFollowers(userId: String) {
        binding.progressBar.visibility = View.VISIBLE
        database.child("Follow").child(userId).child("followers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followerIds = snapshot.children.mapNotNull { it.key }
                    if (followerIds.isEmpty()) {
                        showEmptyState("No followers yet")
                    } else {
                        loadUserProfiles(followerIds)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@FollowListActivity,
                        "Error loading followers",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadFollowing(userId: String) {
        binding.progressBar.visibility = View.VISIBLE
        database.child("Follow").child(userId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followingIds = snapshot.children.mapNotNull { it.key }
                    if (followingIds.isEmpty()) {
                        showEmptyState("Not following anyone yet")
                    } else {
                        loadUserProfiles(followingIds)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@FollowListActivity,
                        "Error loading following",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun loadUserProfiles(userIds: List<String>) {
        userList.clear()
        val totalUsers = userIds.size
        var loadedUsers = 0

        if (userIds.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            return
        }

        userIds.forEach { userId ->
            database.child("EditedAccount").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(UserProfile::class.java)?.apply {
                            this.uid = userId
                        }
                        user?.let {
                            userList.add(it)
                        }

                        loadedUsers++
                        if (loadedUsers == totalUsers) {
                            binding.progressBar.visibility = View.GONE
                            if (userList.isEmpty()) {
                                showEmptyState("No users found")
                            } else {
                                binding.emptyState.visibility = View.GONE
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        loadedUsers++
                        if (loadedUsers == totalUsers) {
                            binding.progressBar.visibility = View.GONE
                            if (userList.isEmpty()) {
                                showEmptyState("No users found")
                            } else {
                                binding.emptyState.visibility = View.GONE
                                adapter.notifyDataSetChanged()
                            }
                        }
                        Log.e("FollowList", "Error loading user $userId", error.toException())
                    }
                })
        }
    }

    private fun showEmptyState(message: String) {
        binding.emptyState.text = message
        binding.emptyState.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }
}