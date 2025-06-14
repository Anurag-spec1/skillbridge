package com.example.taskearner

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.taskearner.databinding.FragmentHomeUploadsUsersBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeUploadsUsers : Fragment() {

    private var _binding: FragmentHomeUploadsUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var uploadsAdapter: UploadsAdapter
    private val uploadsList = mutableListOf<UploadItem>()
    private val originalUploadsList = mutableListOf<UploadItem>()
    private var isSearching = false
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeUploadsUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeFirebase()
        checkProfileEditStatus()
    }

    private fun initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(
            "https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app"
        ).reference
    }

    private fun checkProfileEditStatus() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        database.child("Users").child(uid).child("hasEditedProfile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE

                    val hasEdited = snapshot.getValue(Boolean::class.java) ?: false

                    if (hasEdited) {
                        setupRecyclerView()
                        loadUserData()
                        loadUploads()
                        setupSearch()
                    } else {
                        showProfileEditRequiredDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showErrorMessage("Error checking profile status")
                }
            })
    }

    private fun showProfileEditRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Edit Required")
            .setMessage("You must complete your profile before you can view other profiles")
            .setPositiveButton("Edit Profile") { _, _ ->
                startActivity(Intent(requireContext(), EditAccount::class.java))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupRecyclerView() {
        uploadsAdapter = UploadsAdapter(
            uploadsList,
            onLikeClick = { item ->
                updateLikeCount(item)
            },
            onCommentClick = { item ->
                openComments(item)
            },
            onShareClick = { item ->
                shareContent(item)
            },
            onLinkClick = { item ->
                openLink(item)
            },
            onProfileClick = { item ->
                openOtherProfile(item)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = uploadsAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    clearSearch()
                } else {
                    performSearch()
                }
            }
        })

        binding.searchEditText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = (v as android.widget.EditText).compoundDrawables[2]
                if (drawableEnd != null && event.rawX >= (v.right - drawableEnd.bounds.width())) {
                    binding.searchEditText.text.clear()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(300)

            val query = binding.searchEditText.text.toString().trim().lowercase()
            if (query.isEmpty()) {
                clearSearch()
                return@launch
            }

            val filteredList = originalUploadsList.filter {
                it.userName.lowercase().contains(query) ||
                        it.content.lowercase().contains(query) ||
                        it.domain.lowercase().contains(query)
            }

            uploadsList.clear()
            uploadsList.addAll(filteredList)
            isSearching = true
            uploadsAdapter.notifyDataSetChanged()

            if (filteredList.isEmpty()) {
                showErrorMessage("No results found for '$query'")
            }
        }
    }

    private fun clearSearch() {
        uploadsList.clear()
        uploadsList.addAll(originalUploadsList)
        isSearching = false
        uploadsAdapter.notifyDataSetChanged()
    }

    private fun loadUserData() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        database.child("Users").child(uid).get().addOnCompleteListener { task ->
            binding.progressBar.visibility = View.GONE

            if (task.isSuccessful) {
                val name = task.result?.child("name")?.getValue(String::class.java) ?: "User"
                val profileImage = task.result?.child("profileImage")?.getValue(String::class.java)

                binding.UserNameHome.text = "Hi, $name"
                loadProfileImage(profileImage)
            } else {
                showErrorMessage("Failed to load user data")
            }
        }
    }

    private fun loadProfileImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.profile)
                .circleCrop()
                .into(binding.profileHome)
        }
    }

    private fun loadUploads() {
        binding.progressBar.visibility = View.VISIBLE

        database.child("Uploads")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    originalUploadsList.clear()
                    uploadsList.clear()

                    for (data in snapshot.children) {
                        val upload = data.getValue(UploadItem::class.java)
                        if (upload != null) {
                            upload.uploadId = data.key ?: ""
                            originalUploadsList.add(upload)
                        }
                    }

                    originalUploadsList.reverse()

                    if (!isSearching) {
                        uploadsList.addAll(originalUploadsList)
                        uploadsAdapter.notifyDataSetChanged()
                    }

                    binding.progressBar.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showErrorMessage("Failed to load uploads: ${error.message}")
                }
            })
    }

    private fun updateLikeCount(item: UploadItem) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val uploadRef = database.child("Uploads").child(item.uploadId)
        val userLikeRef = uploadRef.child("likedBy").child(userId)

        userLikeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    uploadRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val currentLikes = currentData.child("likes").getValue(Int::class.java) ?: 0
                            currentData.child("likes").value = currentLikes + 1
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                            if (committed) {
                                userLikeRef.setValue(true)
                            }
                        }
                    })
                } else {
                    Toast.makeText(requireContext(), "You already liked this post", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showErrorMessage("Error liking post: ${error.message}")
            }
        })
    }

    private fun openOtherProfile(item: UploadItem) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        database.child("Users").child(uid).child("hasEditedProfile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hasEdited = snapshot.getValue(Boolean::class.java) ?: false

                    if (hasEdited) {
                        val intent = Intent(requireContext(), InspectUser::class.java)
                        intent.putExtra("uid", item.userId)
                        startActivity(intent)
                    } else {
                        showProfileEditRequiredDialog()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorMessage("Error verifying profile status")
                }
            })
    }

    private fun openComments(item: UploadItem) {
        val intent = Intent(requireContext(), CommentActivity::class.java)
        intent.putExtra("uploadId", item.uploadId)
        startActivity(intent)
    }

    private fun openLink(item: UploadItem) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
        } catch (e: Exception) {
            showErrorMessage("Invalid link")
        }
    }

    private fun shareContent(item: UploadItem) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${item.content}\n\n${item.link}")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share using"))
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}