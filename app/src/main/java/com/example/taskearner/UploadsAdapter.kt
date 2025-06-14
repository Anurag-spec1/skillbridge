package com.example.taskearner

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.taskearner.databinding.ItemUploadsBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class UploadsAdapter(
    private val uploads: List<UploadItem>,
    private val onLikeClick: (UploadItem) -> Unit,
    private val onCommentClick: (UploadItem) -> Unit,
    private val onShareClick: (UploadItem) -> Unit,
    private val onLinkClick: (UploadItem) -> Unit,
    private val onProfileClick: (UploadItem) -> Unit
) : RecyclerView.Adapter<UploadsAdapter.UploadViewHolder>() {

    inner class UploadViewHolder(private val binding: ItemUploadsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UploadItem) {
            with(binding) {
                userName.text = item.userName
                domain.text = item.domain
                dateTime.text = formatDate(item.timestamp)

                if (item.userProfileImage.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(item.userProfileImage)
                        .circleCrop()
                        .placeholder(R.drawable.profile)
                        .into(UserImage)
                } else {
                    UserImage.setImageResource(R.drawable.profile)
                }

                description.text = item.content

                if (item.imageUrl.isNotEmpty()) {
                    prototypeImg.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(item.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.profile)
                        .into(prototypeImg)
                } else {
                    prototypeImg.visibility = View.GONE
                }

                if (item.link.isNotEmpty()) {
                    link.visibility = View.VISIBLE
                    link.text = item.link
                } else {
                    link.visibility = View.GONE
                }

                likecount.text = item.likes.toString()
                comcount.text = item.numcomment.toString()


                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val isLiked = currentUserId != null && item.likedBy.containsKey(currentUserId)

                if (isLiked) {
                    like.setImageResource(R.drawable.likefill)
                } else {
                    like.setImageResource(R.drawable.like)
                }

                like.setOnClickListener {
                    Log.d("Adapter", "Like clicked for: ${item.uploadId}")
                    onLikeClick(item)
                }
                comment.setOnClickListener { onCommentClick(item) }
                share.setOnClickListener { onShareClick(item) }
                link.setOnClickListener { onLinkClick(item) }
                UserImage.setOnClickListener { onProfileClick(item) }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
            return format.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val binding = ItemUploadsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UploadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        holder.bind(uploads[position])
    }

    override fun getItemCount(): Int = uploads.size
}
