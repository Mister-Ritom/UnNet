package com.unreelnet.unnet.utils.adapters

import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ItemPostBinding
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.models.UserModel
import com.unreelnet.unnet.post.ViewPostActivity
import com.unreelnet.unnet.profile.ViewProfileActivity
import com.unreelnet.unnet.utils.reusable.ReusableAnimator
import com.unreelnet.unnet.utils.reusable.ReusableCode


class PostRecyclerViewAdapter(private val context: Context?,var posts:List<PostModel>)
    : RecyclerView.Adapter<PostRecyclerViewAdapter.ViewHolder>() {

    companion object {
        private const val POST_TYPE_TEXT = 0
        private const val POST_TYPE_IMAGE = 1
        private const val POST_TYPE_VIDEO = 2
        private const val POST_TYPE_VIDEO_TEXT = 3
        private const val POST_TYPE_IMAGE_TEXT = 4
    }

    private val tag = "PostViewAdapter"
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val holder = ViewHolder(
            ItemPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        when(viewType) {
            POST_TYPE_TEXT -> holder.postParent.visibility = View.GONE
            POST_TYPE_IMAGE_TEXT -> holder.postVideo.visibility = View.GONE
            POST_TYPE_VIDEO_TEXT -> holder.postImage.visibility = View.GONE
            POST_TYPE_IMAGE -> {
                holder.postText.visibility = View.GONE
                holder.postVideo.visibility = View.GONE
                holder.postImage.visibility = View.VISIBLE
            }
            POST_TYPE_VIDEO -> {
                holder.postText.visibility = View.GONE
                holder.postImage.visibility = View.GONE
                holder.postVideo.visibility = View.VISIBLE
            }
        }

        return holder

    }

    override fun getItemCount(): Int {
        return posts.size
    }
    //Use this instead of firebase user.
    // firebase doesn't have the username.
    private fun setupUser(post: PostModel, holder: ViewHolder) {

       databaseReference.child("Users/${post.uploaderId}")
            .addListenerForSingleValueEvent(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel!=null) {
                        holder.userName.text = userModel.name
                        holder.userId.text = userModel.username
                        Glide.with(holder.itemView).load(userModel.profileImage).into(holder.userImage)
                        if (context!=null) {
                            holder.userImage.setOnClickListener {
                                holder.postVideo.player?.release()
                                ViewProfileActivity.startProfileActivity(context,userModel)
                            }
                            holder.itemView.setOnClickListener {
                                holder.postVideo.player?.release()
                                ViewPostActivity.startViewPostActivity(context,userModel,post)
                            }
                        }
                    }
                    else {
                        Log.e(tag,"User Model is null")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get user",error.toException())
                }

            })
    }

    private fun setupSharedBy(sharedBy: String, holder: ViewHolder) {

        databaseReference.child("Users/$sharedBy")
            .addListenerForSingleValueEvent(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel!=null) {
                        holder.postSharedByText.text =
                            context?.getString(R.string.shared_by_text,userModel.name) ?: userModel.name
                        Glide.with(holder.itemView).load(userModel.profileImage).into(holder.postShardByImage)
                        holder.postShardByImage.setOnClickListener {
                            if (context!=null) {
                                ViewProfileActivity.startProfileActivity(context,userModel)
                            }
                        }
                    }
                    else {
                        Log.e(tag,"User Model is null")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get user",error.toException())
                }

            })
    }

    override fun getItemViewType(position: Int): Int {
        val model = posts[position]
        if (model.media==null || model.media.mediaType==PostModel.MediaType.UNDEFINED)return POST_TYPE_TEXT
        if (model.text==null) {
            if (model.media.mediaType==PostModel.MediaType.PHOTO)return POST_TYPE_IMAGE
            else if (model.media.mediaType==PostModel.MediaType.VIDEO) return POST_TYPE_VIDEO
        }
        else {
            if (model.media.mediaType==PostModel.MediaType.PHOTO)return POST_TYPE_IMAGE_TEXT
            else if (model.media.mediaType==PostModel.MediaType.VIDEO) return POST_TYPE_VIDEO_TEXT
        }
        return super.getItemViewType(position)
    }

    private fun setupImage(holder: ViewHolder, model:PostModel) {
        if (context!=null&&model.media!=null&&model.media.mediaType==PostModel.MediaType.PHOTO) {
            ReusableCode.loadPostImage(context,holder.postImage,holder.postProgress,Uri.parse(model.media.uri))
        }
    }

    private fun setupVideo(holder: ViewHolder, model:PostModel) {
        if (model.media!=null && model.media.mediaType==PostModel.MediaType.VIDEO && context!=null) {
            ReusableCode.setupExoPlayer(context,holder.postVideo, Uri.parse(model.media.uri))
                .also {
                    it.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == ExoPlayer.STATE_READY) {
                                holder.postProgress.visibility = View.GONE
                            }
                            super.onPlaybackStateChanged(playbackState)
                        }
                    })
                }
        }
    }

    @Suppress("deprecation")
    private fun setupEngagementButtons(holder: ViewHolder, model:PostModel) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser!=null) {
            val currentUserId = currentUser.uid
            if (model.likes.contains(currentUserId))holder.likeImage.setColorFilter(context?.resources!!.getColor(R.color.red))
            holder.likeCard.setOnClickListener {
                if (model.likes.contains(currentUserId)) {
                    model.likes.remove(currentUserId)
                    databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                        .child("likes").setValue(model.likes).addOnSuccessListener {
                            holder.likeImage.setColorFilter(context?.resources!!.getColor(R.color.gray),PorterDuff.Mode.SRC_ATOP)
                            notifyItemChanged(holder.bindingAdapterPosition)
                        }
                }
                else {
                    model.likes.add(currentUserId)
                    databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                        .child("likes").setValue(model.likes).addOnSuccessListener {
                            ReusableAnimator.animate(holder.likeImage)
                            holder.likeImage.setColorFilter(context?.resources!!.getColor(R.color.red))
                            notifyItemChanged(holder.bindingAdapterPosition)
                        }
                }
            }
            holder.commentCard.setOnClickListener {
                if (context != null) {
                    ReusableCode.setupCommentDialog(currentUserId,context,model) {
                        notifyItemChanged(holder.bindingAdapterPosition)
                    }
                }
                ReusableAnimator.animate(holder.commentImage)
            }
            holder.reShareCard.setOnClickListener {
                val model1 = PostModel(model.postId,model.uploaderId,System.currentTimeMillis(),
                    model.text,model.media,model.visibility,model.likes,model.comments,
                    model.shares,currentUserId)
                model1.shares.add(currentUserId)
                databaseReference.child("Posts").child(currentUserId).child(model.postId)
                    .setValue(model1).addOnSuccessListener {
                        databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                            .child("shares").setValue(model1.shares).addOnSuccessListener {
                                ReusableAnimator.animate(holder.shareImage)
                                Toast.makeText(context,"Shared post",Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.postVideo.player?.release()
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = posts[position]
        holder.postProgress.visibility = View.VISIBLE
        holder.postVideo.player?.release()

        setupImage(holder, model)
        setupVideo(holder, model)
        setupEngagementButtons(holder, model)

        if (model.sharedBy!=null){
            holder.sharedByParent.visibility = View.VISIBLE
            setupSharedBy(model.sharedBy,holder)
        }
        else holder.sharedByParent.visibility = View.GONE

        setupUser(model,holder)
        holder.postText.text = model.text
        holder.commentText.text = context?.getString(R.string.comments_text,model.comments.size.toString())
        holder.shareText.text = context?.getString(R.string.share_text,model.shares.size.toString())
        holder.likeText.text = context?.getString(R.string.likes_text,model.likes.size.toString())
    }
    
    inner class ViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        private val userBinding = binding.postUserLayout
        val userName = userBinding.userProfileName
        val userId = userBinding.userProfileUsername
        val userImage = userBinding.userProfileImage
        val likeCard = binding.postLikeCard
        val likeImage = binding.postHeart
        val likeText = binding.postLikeText
        val commentCard = binding.postCommentCard
        val commentImage = binding.postComment
        val commentText = binding.postCommentText
        val reShareCard = binding.postShareCard
        val shareImage = binding.postShare
        val shareText = binding.postShareText
        val postText = binding.postText
        val postImage = binding.postImage
        val postVideo = binding.postVideo
        val postParent = binding.postParent
        val postSharedByText = binding.postSharedByName
        val postShardByImage = binding.postSharedByImage
        val postProgress = binding.postLoadProgress
        val sharedByParent = binding.postShardByParent
    }

}