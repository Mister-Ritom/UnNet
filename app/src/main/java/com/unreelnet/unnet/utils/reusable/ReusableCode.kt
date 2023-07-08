package com.unreelnet.unnet.utils.reusable

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.R
import com.unreelnet.unnet.models.CommentModel
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.utils.dialogs.CommentDialog

class ReusableCode {
    companion object {
        fun setupCommentDialog(currentUserId:String,context: Context,post:PostModel,callback: Callback<*>? = null) {
            val dialog = CommentDialog(context,post)
            dialog.binding.addCommentSend.setOnClickListener {
                val comment = dialog.binding.addCommentInput.text.toString()
                if (comment.isNotBlank()) {
                    val commentModel = CommentModel(currentUserId,comment)
                    FirebaseDatabase.getInstance().reference
                        .child("Posts")
                        .child(post.uploaderId).child(post.postId).child("comments")
                        .child(post.comments.size.toString())
                        .setValue(commentModel).addOnSuccessListener {
                            post.comments.add(commentModel)
                            callback?.onCall(null)
                            dialog.binding.addCommentComments.adapter?.notifyItemInserted(post.comments.size-1)
                            dialog.binding.addCommentInput.text?.clear()
                            dialog.binding.addCommentInput.clearFocus()
                        }
                }
            }
            dialog.show()
        }

        fun setupExoPlayer(context: Context,playerView:PlayerView,uri:Uri):ExoPlayer {
            return ExoPlayer.Builder(context).build().also {
                playerView.player = it
                val mediaItem = MediaItem.fromUri(uri)
                it.setMediaItem(mediaItem)
                it.prepare()
                it.play()
                it.playWhenReady = true
            }
        }

        fun setBackground(resource:Bitmap,background:View) {
            Palette.Builder(resource).generate { p->
                val dominantColorInt = p?.getDominantColor(Resources.getSystem().getColor(R.color.crimson))
                val mutedColorInt = p?.getMutedColor(Resources.getSystem().getColor(R.color.crimson))
                if (dominantColorInt != null && mutedColorInt != null) {
                    val colorArray: IntArray = intArrayOf(dominantColorInt, mutedColorInt)
                    val drawable = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        colorArray
                    )
                    Handler(Looper.getMainLooper()).post {
                        background.setBackgroundDrawable(drawable)
                    }
                }
            }
        }

        fun loadPostImage(context:Context,imageview:ImageView,progress:ProgressBar?,uri:Uri) {
            Glide.with(context).load(uri).listener(object:
                RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    a: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e("ReusableCode","Couldn't load post. uri: $uri",e)
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
                        imageview.setImageDrawable(resource)
                        progress?.visibility = View.GONE
                    }
                    return true
                }

            }).submit()
        }

    }
}