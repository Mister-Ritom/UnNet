package com.unreelnet.unnet.utils.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unreelnet.unnet.databinding.ItemCommentBinding
import com.unreelnet.unnet.models.CommentModel
import com.unreelnet.unnet.models.UserModel

class CommentRecyclerViewAdapter(private val context: Context,private val comments:List<CommentModel>) :
    RecyclerView.Adapter<CommentRecyclerViewAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCommentBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.commentText.text = comment.comment
        setupUser(holder,comment.userId)
    }

    private fun setupUser(holder:ViewHolder,userId:String) {
        FirebaseDatabase.getInstance().reference
            .child("Users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel!=null) {
                        holder.userName.text = userModel.name
                        holder.userId.text = userModel.username
                        Glide.with(holder.itemView).load(userModel.profileImage)
                            .into(holder.userImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CommentRecyclerView","Couldn't get user",error.toException())
                }
            })
    }

    inner class ViewHolder(binding:ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        private val userBinding= binding.commentUser
        val userImage = userBinding.userProfileImage
        val userName = userBinding.userProfileName
        val userId = userBinding.userProfileUsername
        val commentText = binding.commentText
    }

}