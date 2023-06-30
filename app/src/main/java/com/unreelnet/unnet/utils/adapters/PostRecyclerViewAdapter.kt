package com.unreelnet.unnet.utils.adapters

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.Toast
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


class PostRecyclerViewAdapter(private val context: Context?,private val posts:List<PostModel>)
    : RecyclerView.Adapter<PostRecyclerViewAdapter.ViewHolder>() {

    private val tag = "PostViewAdapter"
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = posts[position]

        if (model.text==null)holder.postText.visibility = View.GONE
        else holder.postText.visibility = View.VISIBLE
        if (model.imageUri==null)holder.postImage.visibility = View.GONE
        else{
            Glide.with(holder.itemView).load(model.imageUri).into(holder.postImage)
            holder.postImage.visibility = View.VISIBLE
        }
        if (model.videoUri==null)holder.postVideo.visibility = View.GONE
        else {
            holder.postVideo.visibility = View.VISIBLE
            holder.postVideo.setVideoURI(Uri.parse(model.videoUri))
            holder.postVideo.start()
            holder.postVideo.setOnClickListener {
                if (holder.postVideo.isPlaying)holder.postVideo.pause()
                else holder.postVideo.start()
            }
        }


        setupUser(model,holder)
        val currentUser = FirebaseAuth.getInstance().currentUser
        holder.postText.text = model.text
        holder.commentText.text = context?.getString(R.string.comments_text,model.comments.size.toString())
        holder.shareText.text = context?.getString(R.string.share_text,model.shares.size.toString())
        holder.likeText.text = context?.getString(R.string.likes_text,model.likes.size.toString())
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
                animate(holder.commentCard)
            }
            holder.reShareCard.setOnClickListener {
                model.shares.add(currentUserId)
                databaseReference.child("Posts").child(currentUserId).child(model.postId)
                    .setValue(model).addOnSuccessListener {
                        databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                            .child("shares").setValue(model.shares).addOnSuccessListener {
                                animate(holder.shareImage)
                                Toast.makeText(context,"Shared post",Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
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
    }

}