package com.example.taskearner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskearner.databinding.ActivityAllUsersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class AllUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllUsersBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: UserAdapter
    private val allUsersList = mutableListOf<UserProfile>()
    private val filteredUsersList = mutableListOf<UserProfile>()
    private var searchText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database =
            FirebaseDatabase.getInstance("https://task-earner-2bedd-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        auth = FirebaseAuth.getInstance()

        setupSearchEditText()
        setupRecyclerView()
        loadAllUsers()
    }


    private fun setupSearchEditText() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s.toString()
                filterUsers(searchText)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    private fun filterUsers(query: String) {
        filteredUsersList.clear()
        if (query.isEmpty()) {
            filteredUsersList.addAll(allUsersList)
        } else {
            val searchQuery = query.lowercase(Locale.getDefault())
            allUsersList.forEach { user ->
                if (user.name.lowercase(Locale.getDefault()).contains(searchQuery)) {
                    filteredUsersList.add(user)
                }
            }
        }
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(filteredUsersList) { user ->
            if (auth.currentUser?.uid != user.uid) {
                val intent = Intent(this, InspectUser::class.java).apply {
                    putExtra("uid", user.uid)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "This is your profile", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AllUsersActivity)
            adapter = this@AllUsersActivity.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun loadAllUsers() {
        binding.progressBar.visibility = View.VISIBLE
        database.child("EditedAccount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsersList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(UserProfile::class.java)?.apply {
                        uid = userSnapshot.key ?: ""
                    }
                    user?.let { allUsersList.add(it) }
                }

                filteredUsersList.clear()
                filteredUsersList.addAll(allUsersList)
                adapter.notifyDataSetChanged()
                updateEmptyState()
                binding.progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AllUsersActivity, "Error loading users", Toast.LENGTH_SHORT)
                    .show()
                updateEmptyState()
            }
        })
    }

    private fun updateEmptyState() {
        if (filteredUsersList.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            if (searchText.isNotEmpty()) {
                binding.emptyState.text = "No users found for '$searchText'"
            } else {
                binding.emptyState.text = "No users found"
            }
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
}