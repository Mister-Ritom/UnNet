package com.unreelnet.unnet.profile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ActivityViewProfileBinding
import com.unreelnet.unnet.models.UserModel

class ViewProfileActivity : AppCompatActivity() {
    private val tag = "ViewPostActivity"
    private val databaseReference  = FirebaseDatabase.getInstance().reference

    private var followingNumber:Int = 0
    private var followersNumber:Int = 0

    private lateinit var binding:ActivityViewProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (intent.extras!=null) {
            val user = intent.extras!!.getParcelable<UserModel>("user")
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (user!=null&&currentUser!=null) {
                binding.profileName.text = user.name
                Glide.with(this).load(user.profileImage).dontAnimate().into(binding.profileImage)
                Glide.with(this).load(user.profileImage).dontAnimate().into(binding.profileToolbarImage)
                binding.profileToolbar.title = getString(R.string.profile_string,user.name)
                getPosts(user.userId,binding.profilePostsNumber)


                if (user.userId == currentUser.uid) {
                    binding.profileButtons.visibility = View.GONE
                }
                else {
                    getFollowingStatus(currentUser.uid,user.userId)
                    binding.profileChatCard.setOnClickListener {
                        failure("Coming soon",null)
                    }
                }

            }
            else {
                Log.e(tag,"User is null")
                finish()
            }
        }
        else {
            Log.e(tag,"Intent extras are null")
            finish()
        }
    }


    private fun getPosts(userId: String,postsNumber:TextView) {
        databaseReference.child("Posts").child(userId)
            .addListenerForSingleValueEvent(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        postsNumber.text = snapshot.childrenCount.toString()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    failure("Couldn't get posts",error.toException())
                }

            })
    }

    private fun getFollowingStatus(currentUserId:String,userId:String) {
        databaseReference.child("Following").child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //TODO show user whom they are following
                    if (snapshot.exists()) {
                        followingNumber = snapshot.childrenCount.toInt()
                        for (child in snapshot.children) {
                            val id = child.getValue(String::class.java)
                            if (id!=null){
                                Log.e("WTF",id)
                                if (id==userId) {
                                    Log.e("WTF","Yes")
                                    following(currentUserId,userId)
                                    break
                                }
                                else {
                                    Log.e("WTF","No")
                                    follow(currentUserId,userId)
                                    continue
                                }
                            }
                        }
                    }
                    else {
                        follow(currentUserId,userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    failure("Couldn't get following status",error.toException())
                }
            })

        databaseReference.child("Followers").child(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //TODO show user who is following
                    if (snapshot.exists()) {
                        followersNumber = snapshot.childrenCount.toInt()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    failure("Couldn't get followers",error.toException())
                }
            })
    }

    private fun following(currentUserId: String,userId: String) {
        binding.profileFollowText.text = getString(R.string.following_text)
        binding.profileFollowCard.setOnClickListener {
            databaseReference.child("Followers").child(userId)
                .orderByValue().equalTo(currentUserId).ref.setValue(null).addOnSuccessListener {
                    databaseReference.child("Following").child(currentUserId)
                        .orderByValue().equalTo(userId).ref.setValue(null).addOnSuccessListener {
                            followersNumber--
                            binding.profileFollowText.text = getString(R.string.follow_text)
                            binding.profileFollowingsNumber.text = followersNumber.toString()
                            follow(currentUserId,userId)
                        }
                        .addOnFailureListener {
                            failure("Couldn't unfollow user",it)
                        }
                }
                .addOnFailureListener {
                    failure("Couldn't unfollow user",it)
                }
        }
    }

    private fun follow(currentUserId: String,userId: String) {
        binding.profileFollowText.text = getString(R.string.follow_text)
        binding.profileFollowCard.setOnClickListener {
            databaseReference.child("Followers").child(userId).push()
                .setValue(currentUserId).addOnSuccessListener {
                    databaseReference.child("Following").child(currentUserId).push()
                        .setValue(userId)
                        .addOnSuccessListener {
                            followersNumber++
                            binding.profileFollowText.text = getString(R.string.following_text)
                            binding.profileFollowingsNumber.text = followersNumber.toString()
                            following(currentUserId,userId)
                        }
                        .addOnFailureListener {
                            databaseReference.child("Followers").child(userId).orderByValue()
                                .equalTo(currentUserId).ref.removeValue()
                            failure("Couldn't follow user",it)
                        }
                }
                .addOnFailureListener {
                    failure("Couldn't follow user",it)
                }

        }
    }

    private fun failure(s:String,e:Exception?) {
        Log.e(tag,s,e)
        Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    }


    companion object {
        fun startProfileActivity(context: Context, user: UserModel) {
            val intent = Intent(context, ViewProfileActivity::class.java)
            intent.putExtra("user",user)
            context.startActivity(intent)
        }
    }

}