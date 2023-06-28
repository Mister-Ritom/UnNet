package com.unreelnet.unnet.home.models

data class PostModel(val postId:String,val uploaderId:String,val uploadTime:Long,var text:String? = null,
                     var imageUri:String? = null,
                     val likes:MutableList<String> = emptyList<String>().toMutableList(),
                     val comments:MutableList<String> = emptyList<String>().toMutableList(),
                    val shares:MutableList<String> = emptyList<String>().toMutableList()
) {
    constructor():this("","",-1,null,null)
}