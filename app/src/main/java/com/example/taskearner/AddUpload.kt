package com.example.taskearner

import android.app.Activity
import androidx.fragment.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.taskearner.databinding.FragmentAddUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddUpload : Fragment() {

    private var _binding: FragmentAddUploadBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    private lateinit var database: DatabaseReference
    private var isUploading = false
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddUploadBinding.inflate(inflater, container, false)
        retainInstance = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK && isUploading) {
                Toast.makeText(
                    requireContext(),
                    "Upload in progress. Please wait...",
                    Toast.LENGTH_SHORT
                ).show()
                true
            } else {
                false
            }
        }

        firebaseAuth = FirebaseAuth.getInstance()
        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        initCloudinary()
        setupViews()
    }

    private fun setupViews() {
        binding.apply {
            profileHome.setOnClickListener {
                if (!isUploading) {
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(intent, 101)
                }
            }

            post.setOnClickListener {
                if (!isUploading) {
                    val contentText = content.text.toString().trim()
                    val linkText = link.text.toString().trim()

                    when {
                        selectedImageUri == null -> showToast("Please select an image")
                        contentText.isEmpty() && linkText.isEmpty() -> showToast("Please enter content or link")
                        else -> {
                            showLoading()
                            uploadImageToCloudinary(selectedImageUri!!, contentText, linkText)
                        }
                    }
                }
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
            MediaManager.init(requireContext(), config)
        } catch (e: Exception) {
            Log.e("Cloudinary", "Initialization error", e)
        }
    }

    private fun uploadImageToCloudinary(uri: Uri, content: String, link: String) {
        if (!isAdded) return

        isUploading = true
        showToast("Uploading image...")

        MediaManager.get().upload(uri).callback(object : UploadCallback {
            override fun onStart(requestId: String) {
                Log.d("Upload", "Upload started")
            }

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                val progress = (bytes * 100 / totalBytes).toInt()
                Log.d("Upload", "Progress: $progress%")
            }

            override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                val imageUrl = resultData["secure_url"]?.toString()
                if (imageUrl != null) {
                    saveToDatabase(content, imageUrl, link)
                } else {
                    handleUploadError("Image upload failed - no URL returned")
                }
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                handleUploadError("Upload error: ${error.description}")
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) {
                Log.d("Upload", "Rescheduled: ${error.description}")
            }
        }).dispatch()
    }

    private fun saveToDatabase(content: String, imageUrl: String, link: String) {
        if (!isAdded || isDetached) return

        val currentUser = firebaseAuth.currentUser
        val userId = currentUser?.uid ?: run {
            handleUploadError("User not authenticated")
            return
        }

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        database.child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    if (!isAdded) return

                    val userName =
                        userSnapshot.child("name").getValue(String::class.java) ?: "Anonymous"
                    val domain =
                        userSnapshot.child("domain").getValue(String::class.java) ?: "Anonymous"
                    val userProfileImage =
                        userSnapshot.child("profileImage").getValue(String::class.java) ?: ""

                    val uploadsRef = database.child("Uploads")
                    val key = uploadsRef.push().key ?: run {
                        handleUploadError("Could not generate key")
                        return
                    }

                    val data = hashMapOf(
                        "uploadId" to key,
                        "content" to content,
                        "imageUrl" to imageUrl,
                        "link" to link,
                        "timestamp" to ServerValue.TIMESTAMP,
                        "dateTimeString" to currentDateTime,
                        "userId" to userId,
                        "userName" to userName,
                        "userProfileImage" to userProfileImage,
                        "likes" to 0,
                        "comments" to 0,
                        "domain" to domain,
                        "likedBy" to emptyMap<String, Boolean>()
                    )

                    uploadsRef.child(key).setValue(data)
                        .addOnSuccessListener {
                            if (isAdded) {
                                showToast("Uploaded successfully")
                                resetForm()
                            }
                        }
                        .addOnFailureListener { e ->
                            if (isAdded) handleUploadError("Database error: ${e.message}")
                        }
                        .addOnCompleteListener {
                            isUploading = false
                            hideLoading()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) {
                        handleUploadError("Failed to fetch user data: ${error.message}")
                        isUploading = false
                        hideLoading()
                    }
                }
            })
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

    private fun resetForm() {
        binding.apply {
            content.setText("")
            link.setText("")
            profileHome.setImageResource(R.drawable.profile)
            selectedImageUri = null
        }
    }

    private fun handleUploadError(message: String) {
        if (isAdded) {
            showToast(message)
            hideLoading()
            isUploading = false
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                binding.profileHome.setImageURI(uri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AddUpload()
    }
}

