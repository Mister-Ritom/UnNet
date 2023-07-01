package com.unreelnet.unnet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostModel(val postId:String,val uploaderId:String,val uploadTime:Long,var text:String? = null,
                     val imageUri:String? = null,val videoUri:String?,
                     val likes:MutableList<String> = emptyList<String>().toMutableList(),
                     val comments:MutableList<String> = emptyList<String>().toMutableList(),
                    val shares:MutableList<String> = emptyList<String>().toMutableList(),val sharedBy:String? = null
) : Parcelable {
    constructor():this("","",-1,null,null,null)
}