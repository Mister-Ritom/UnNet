package com.unreelnet.unnet.utils.dialogs

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.unreelnet.unnet.databinding.DialogCommentBinding
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.utils.adapters.CommentRecyclerViewAdapter

class CommentDialog(context:Context,post:PostModel) : BottomSheetDialog(context) {
    var binding = DialogCommentBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.addCommentComments.adapter = CommentRecyclerViewAdapter(context,post.comments)
    }
}