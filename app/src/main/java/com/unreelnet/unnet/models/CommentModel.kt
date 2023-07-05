package com.unreelnet.unnet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CommentModel(val userId:String, var comment:String) : Parcelable {
    constructor():this("","")
}