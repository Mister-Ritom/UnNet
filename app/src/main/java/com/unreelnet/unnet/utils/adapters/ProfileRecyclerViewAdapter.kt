package com.unreelnet.unnet.utils.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.unreelnet.unnet.databinding.ItemSmallPostBinding
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.models.UserModel
import com.unreelnet.unnet.post.ViewPostActivity


open class ProfileRecyclerViewAdapter(private val context: Context?, private val user:UserModel,options: FirebaseRecyclerOptions<PostModel>)
    : FirebaseRecyclerAdapter<PostModel, ProfileRecyclerViewAdapter.ViewHolder>(options) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProfileRecyclerViewAdapter.ViewHolder {
        return ViewHolder(
            ItemSmallPostBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ProfileRecyclerViewAdapter.ViewHolder,
        position: Int,
        model: PostModel
    ) {
        if (model.text==null) holder.postText.visibility = View.GONE
        else {
            holder.postText.visibility = View.VISIBLE
            holder.postText.text=model.text
        }
        if (model.imageUri==null)holder.postImageParent.visibility= View.GONE
        else {
            holder.postImageParent.visibility = View.VISIBLE
            Glide.with(context!!).asBitmap()
                .load(model.imageUri).into(holder.postImage)
        }
        holder.itemView.setOnClickListener {
            if (context != null) {
                ViewPostActivity.startViewPostActivity(context,user,model)
            }
        }
//        if (model.videoUri ==null) holder.postImageParent.visibility = View.GONE
//        else {
//            holder.postImageParent.visibility = View.VISIBLE
//            val interval: Long = 1 * 1000
//            val options = RequestOptions().frame(interval)
//            Glide.with(context!!).asBitmap()
//                .load(model.videoUri)
//                .apply(options)
//                .into(holder.postImage)
//        } TODO Videos
    }

    inner class ViewHolder(binding:ItemSmallPostBinding): RecyclerView.ViewHolder(binding.root) {
        val postImage = binding.postImage
        val postImageParent = binding.postParent
        val postText = binding.postText
    }

}