package com.unreelnet.unnet.home.utils.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.avatarfirst.avatargenlib.AvatarConstants
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions

import com.unreelnet.unnet.databinding.ItemUserBinding
import com.unreelnet.unnet.home.models.UserModel
import com.unreelnet.unnet.home.profile.ViewProfileActivity

class UserRecyclerViewAdapter(private val context:Context?,options: FirebaseRecyclerOptions<UserModel>)
    : FirebaseRecyclerAdapter<UserModel, UserRecyclerViewAdapter.ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: UserModel) {
        holder.profileName.text = model.name
        holder.profileUsername.text = model.userId
        Glide.with(holder.itemView).load(model.profileImage).dontAnimate().into(holder.profileImage)
        holder.itemView.setOnClickListener {
            if (context!=null) {
                ViewProfileActivity.startProfileActivity(context,model)
            }
        }
    }

    inner class ViewHolder(binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        val profileImage = binding.userProfileImage
        val profileName = binding.userProfileName
        val profileUsername = binding.userProfileUsername
    }

}