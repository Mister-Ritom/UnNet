package com.unreelnet.unnet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostModel(val postId:String, val uploaderId:String, val uploadTime:Long,
                     var text:String? = null,
                     val media:Media? = null,
                     var visibility: PostVisibility = PostVisibility.VISIBLE,
                     val likes:MutableList<String> = emptyList<String>().toMutableList(),
                     val comments:MutableList<CommentModel> = emptyList<CommentModel>().toMutableList(),
                     val shares:MutableList<String> = emptyList<String>().toMutableList(), val sharedBy:String? = null
) : Parcelable {
    constructor():this("","",-1,null,null)

    @Parcelize
    enum class PostVisibility : Parcelable {
        VISIBLE,LINK_ONLY,PRIVATE
    }

    @Parcelize
    data class Media(var uri:String,var mediaType: MediaType) : Parcelable {
        constructor():this("",MediaType.UNDEFINED)
    }

    @Parcelize
    enum class MediaType : Parcelable {
        PHOTO,VIDEO,UNDEFINED
    }

}