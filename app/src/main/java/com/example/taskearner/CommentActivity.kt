package com.example.taskearner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskearner.databinding.ActivityCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class CommentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentBinding
    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var commentAdapter: CommentAdapter
    private val commentList = mutableListOf<Comment>()
    private var uploadId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        firebaseAuth = FirebaseAuth.getInstance()
        uploadId = intent.getStringExtra("uploadId") ?: ""

        setupRecyclerView()
        loadComments()

        binding.sendButton.setOnClickListener {
            val text = binding.commentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text)
            }
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(commentList)
        binding.commentRecycler.apply {
            layoutManager = LinearLayoutManager(this@CommentActivity)
            adapter = commentAdapter
        }
    }

    private fun loadComments() {
        database.child("Uploads").child(uploadId).child("comments")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    commentList.clear()
                    for (data in snapshot.children) {
                        val comment = data.getValue(Comment::class.java)

                        if (comment != null) {
                            commentList.add(comment)
                        }
                    }
                    commentList.sortBy { it.timestamp }
                    commentAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@CommentActivity,
                        "Failed to load comments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun postComment(text: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val commentId = database.push().key ?: return

        database.child("Users").child(userId).get().addOnSuccessListener { userSnap ->
            val userName = userSnap.child("name").getValue(String::class.java) ?: "User"
            val userProfile = userSnap.child("profileImage").getValue(String::class.java) ?: ""

            val comment = Comment(
                commentId = commentId,
                userId = userId,
                userName = userName,
                userProfileImage = userProfile,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            database.child("Uploads").child(uploadId).child("comments").child(commentId)
                .setValue(comment).addOnCompleteListener { commentTask ->
                    if (commentTask.isSuccessful) {
                        binding.commentInput.text?.clear()

                        database.child("Uploads").child(uploadId).child("numcomment")
                            .runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val currentCount = currentData.getValue(Int::class.java) ?: 0
                                    currentData.value = currentCount + 1
                                    return Transaction.success(currentData)
                                }

                                override fun onComplete(
                                    error: DatabaseError?,
                                    committed: Boolean,
                                    currentData: DataSnapshot?
                                ) {
                                    if (error != null) {
                                        Toast.makeText(
                                            this@CommentActivity,
                                            "Failed to update count",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this@CommentActivity,
                                            "Comment posted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            })
                    }
                }
        }
    }
}
