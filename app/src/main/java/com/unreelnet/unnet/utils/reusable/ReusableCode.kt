package com.unreelnet.unnet.utils.reusable

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.models.CommentModel
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.utils.dialogs.CommentDialog

class ReusableCode {
    companion object {
        fun setupCommentDialog(currentUserId:String,context: Context,post:PostModel,callback: Callback? = null) {
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
                            callback?.onCall()
                            dialog.binding.addCommentComments.adapter?.notifyItemInserted(post.comments.size-1)
                            dialog.binding.addCommentInput.text?.clear()
                            dialog.binding.addCommentInput.clearFocus()
                        }
                }
            }
            dialog.show()
        }
    }
}