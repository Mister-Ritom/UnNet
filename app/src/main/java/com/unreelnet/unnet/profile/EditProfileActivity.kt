package com.unreelnet.unnet.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.unreelnet.unnet.databinding.ActivityEditProfileBinding
import com.unreelnet.unnet.models.UserModel

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding:ActivityEditProfileBinding
    private val tag = "EditProfileActivity"

    private var photoUri:Uri? = null
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setResult(RESULT_CANCELED)
        if(intent.extras==null) finish()
        else {
            val userModel = intent.extras?.getParcelable<UserModel>("user")
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (userModel==null || currentUser == null) finish()
            else {
                binding.profileImage.setOnClickListener {
                    launchForImage.launch("image/*")
                }
                binding.profileNameEdit.setText(userModel.name)
                binding.profileUsernameEdit.setText(userModel.username)
                Glide.with(this@EditProfileActivity).load(userModel.profileImage)
                    .into(binding.profileImage)


                binding.profileEditSubmit.setOnClickListener {
                    displayToast("Updating ui")
                    var changeRequest = false
                    val userId = binding.profileUsernameEdit.text.toString()
                    val name = binding.profileNameEdit.text.toString()
                    val changeRequestBuilder = UserProfileChangeRequest.Builder()
                    if (name.isNotBlank() && userId.isNotBlank()) {
                        if (photoUri != null) {
                            FirebaseStorage.getInstance().reference
                                .child("Users").child(currentUser.uid)
                                .child("ProfileImages").putFile(photoUri!!)
                                .addOnSuccessListener {
                                    it.storage.downloadUrl
                                        .addOnSuccessListener { uri ->
                                            databaseReference.child("Users")
                                                .child(currentUser.uid)
                                                .child("profileImage")
                                                .setValue(uri.toString()).addOnSuccessListener {
                                                    displayToast("Profile photo edited")
                                                    changeRequestBuilder.photoUri = uri
                                                    changeRequest = true
                                                }
                                                .addOnFailureListener {e->
                                                    Log.e(tag, "Database Error:Failed to change Profile photo ", e)
                                                    displayToast("Failed to change Profile photo")
                                                }
                                        }
                                }
                                .addOnFailureListener {
                                    displayToast("Failed to change profile photo")
                                }
                        }

                        if (name != userModel.name) {
                            changeRequestBuilder.displayName = name
                            changeRequest = true
                        }
                        if (userId != currentUser.uid) {
                            updateUserId(userId, currentUser.uid)
                        }

                        if (changeRequest) {
                            currentUser.updateProfile(changeRequestBuilder.build())
                                .addOnSuccessListener {
                                    if (name != userModel.name) {
                                        databaseReference.child("Users")
                                            .child(currentUser.uid)
                                            .child("name").setValue(name)
                                            .addOnSuccessListener {
                                                finishAndResult(userModel)
                                                displayToast("Name edited")
                                            }
                                            .addOnFailureListener {
                                                Log.e(tag, "Database Error:Failed to change name ", it)
                                                displayToast("Failed to change name")
                                            }
                                    }
                                    else {
                                        finishAndResult(userModel)
                                    }

                                }.addOnFailureListener {
                                    Log.e(tag, "Change request Error:Failed to change user info ", it)
                                    displayToast("Failed to edit profile")
                                }
                        }
                        else {
                            finishAndResult(userModel)
                        }
                    }
                }
            }

        }

    }

    private fun updateUserId(username:String,currentUserId:String) {
        databaseReference.child("Users")
            .orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Log.e("Value of snapshot", snapshot.toString())
                        displayToast("Username already exists")
                    } else {
                        databaseReference.child("Users")
                            .child(currentUserId)
                            .child("username").setValue(username)
                            .addOnSuccessListener {
                                displayToast("username edited")
                            }
                            .addOnFailureListener {
                                Log.e(tag, "Failed to change username ", it)
                                displayToast("Failed to change username")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag, "Couldn't't get info about userId")

                }

            })
    }

    private fun finishAndResult(userModel: UserModel) {
        val intent = Intent()
        intent.putExtra("user",userModel)
        setResult(RESULT_OK,intent)
        finish()
    }

    private fun displayToast(s:String) {
        Toast.makeText(this@EditProfileActivity, s, Toast.LENGTH_SHORT).show()
    }

    private val launchForImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it!=null) {
            Glide.with(this).load(it).into(binding.profileImage)
            photoUri = it
        }
    }

}