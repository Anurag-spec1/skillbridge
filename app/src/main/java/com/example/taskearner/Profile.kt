package com.example.taskearner

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.taskearner.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class Profile : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app")

        fetchUserData()

        setupClickListeners()

        return binding.root
    }

    private fun fetchUserData() {
        val currentUser = firebaseAuth.currentUser
        currentUser?.uid?.let { userId ->
            database.getReference("Users").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {

                            val name = snapshot.child("name").getValue(String::class.java) ?: ""
                            val email = snapshot.child("email").getValue(String::class.java) ?: ""
                            val profileImageUrl =
                                snapshot.child("profileImage").getValue(String::class.java) ?: ""


                            binding.userName.text = name
                            binding.userMail.text = email

                            if (profileImageUrl.isNotEmpty()) {
                                Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.profile) // Your default profile image
                                    .circleCrop()
                                    .into(binding.userIcon)
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "User data not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load user data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } ?: run {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.linLay.setOnClickListener {
            val intent = Intent(requireContext(), EditAccount::class.java)
            startActivity(intent)
        }

        binding.linLay2.setOnClickListener {
            Toast.makeText(requireContext(), "Under Development", Toast.LENGTH_SHORT).show()
        }

        binding.linLay3.setOnClickListener {
            Toast.makeText(requireContext(), "Under Development", Toast.LENGTH_SHORT).show()
        }

        binding.logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}