package com.example.taskearner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskearner.databinding.UserItemBinding

//class UserAdapter(
//    private val users: List<UserProfile>,
//    private val onItemClick: (UserProfile) -> Unit
//) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
//
//    class UserViewHolder(val binding: UserItemBinding) : RecyclerView.ViewHolder(binding.root)
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
//        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return UserViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
//        val user = users[position]
//        with(holder.binding) {
//            userName.text = user.name
//            userSkill.text = user.domain.takeIf { it.isNotEmpty() } ?: user.domain
//            userdomain.text=user.skill.takeIf { it.isNotEmpty() } ?: user.skill
//
//            if (user.profileImage.isNotEmpty()) {
//                Glide.with(userImage.context)
//                    .load(user.profileImage)
//                    .circleCrop()
//                    .placeholder(R.drawable.profile)
//                    .into(userImage)
//            } else {
//                userImage.setImageResource(R.drawable.profile)
//            }
//
//            root.setOnClickListener { onItemClick(user) }
//        }
//    }
//
//    override fun getItemCount() = users.size
//}

class UserAdapter(
    private val users: List<UserProfile>,
    private val onItemClick: (UserProfile) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(val binding: UserItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        with(holder.binding) {
            userName.text = user.name
            userSkill.text = user.domain.ifEmpty { user.domain }
            userdomain.text=user.skill.ifEmpty { user.skill }


            if (user.profileImage.isNotEmpty()) {
                Glide.with(userImage.context)
                    .load(user.profileImage)
                    .circleCrop()
                    .placeholder(R.drawable.profile)
                    .into(userImage)
            } else {
                userImage.setImageResource(R.drawable.profile)
            }

            root.setOnClickListener { onItemClick(user) }
        }
    }

    override fun getItemCount() = users.size
}