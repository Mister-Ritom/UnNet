package com.unreelnet.unnet.utils.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
            }
            POST_TYPE_VIDEO -> {
                holder.postText.visibility = View.GONE
                holder.postImage.visibility = View.GONE
            }
        }

        return holder

    }

    override fun getItemCount(): Int {
        return posts.size
    }

    private fun setupUser(post: PostModel, holder: ViewHolder) {
       databaseReference.child("Users/${post.uploaderId}")
            .addListenerForSingleValueEvent(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel!=null) {
                        holder.userName.text = userModel.name
                        holder.userId.text = userModel.userId
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
        if (model.imageUri==null&&model.videoUri==null)return POST_TYPE_TEXT
        if (model.text==null) {
            if (model.videoUri==null)return POST_TYPE_IMAGE
            else if (model.imageUri==null) return POST_TYPE_VIDEO
        }
        else {
            if (model.videoUri==null)return POST_TYPE_IMAGE_TEXT
            else if (model.imageUri==null) return POST_TYPE_VIDEO_TEXT
        }
        return super.getItemViewType(position)
    }

    private fun setupImage(holder: ViewHolder, model:PostModel) {
        if (model.imageUri!=null) {
            Glide.with(holder.itemView).load(model.imageUri).listener(object:RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    a: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e(tag,"Couldn't load post ${model.postId}")
                    Toast.makeText(context, "Couldn't load post", Toast.LENGTH_SHORT).show()
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Handler(Looper.getMainLooper()).post {
                        holder.postImage.setImageDrawable(resource)
                        holder.postProgress.visibility = View.GONE
                    }
                    return true
                }

            }).submit()
        }
    }

    private fun setupVideo(holder: ViewHolder, model:PostModel) {
        if (model.videoUri!=null && context!=null) {
            ExoPlayer.Builder(context).build().also {
                holder.postVideo.player = it
                val mediaItem = MediaItem.fromUri(model.videoUri)
                it.setMediaItem(mediaItem)
                it.play()
                it.playWhenReady = true
            }

            holder.postVideo.visibility = View.VISIBLE
            holder.postVideo.player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        holder.postProgress.visibility = View.GONE
                    }
                    super.onIsPlayingChanged(isPlaying)
                }
            })
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
                        }
                }
                else {
                    model.likes.add(currentUserId)
                    databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                        .child("likes").setValue(model.likes).addOnSuccessListener {
                            animate(holder.likeImage)
                            holder.likeImage.setColorFilter(context?.resources!!.getColor(R.color.red))
                        }
                }
            }
            holder.commentCard.setOnClickListener {
                animate(holder.commentImage)
            }
            holder.reShareCard.setOnClickListener {
                val model1 = PostModel(model.postId,model.uploaderId,System.currentTimeMillis(),
                    model.text,model.imageUri,model.videoUri,model.likes,model.comments,
                    model.shares,currentUserId)
                model1.shares.add(currentUserId)
                databaseReference.child("Posts").child(currentUserId).child(model.postId)
                    .setValue(model1).addOnSuccessListener {
                        databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                            .child("shares").setValue(model1.shares).addOnSuccessListener {
                                animate(holder.shareImage)
                                Toast.makeText(context,"Shared post",Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = posts[position]
        holder.postProgress.visibility = View.VISIBLE

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

    private fun animate(view:View) {
        val animationSet = AnimatorSet()
        val accelerateInterpolator = AccelerateInterpolator()
        view.scaleX = 0.1F
        view.scaleY = 0.1F
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.1f, 1f)
        scaleDownY.duration = 300
        scaleDownY.interpolator = accelerateInterpolator
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.1f, 1f)
        scaleDownX.duration = 300
        scaleDownX.interpolator = accelerateInterpolator
        animationSet.playTogether(scaleDownX,scaleDownY)
        animationSet.start()
    }
    
    inner class ViewHolder(binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        val userName = binding.postUserName
        val userId = binding.postUserId
        val userImage = binding.postUserImage
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