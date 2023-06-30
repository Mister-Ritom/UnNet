package com.unreelnet.unnet.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserModel(val userId:String, var email:String?,var phone:String?,
                     var name:String, var profileImage:String) : Parcelable {
    constructor():this("",null,null,"","")
}