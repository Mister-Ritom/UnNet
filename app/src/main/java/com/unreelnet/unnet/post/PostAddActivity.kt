package com.unreelnet.unnet.post

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ActivityAddPostBinding
import com.unreelnet.unnet.home.HomeActivity
import com.unreelnet.unnet.models.PostModel
import java.util.UUID

class PostAddActivity : AppCompatActivity() {
    private val tag = "PostAddActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val user = FirebaseAuth.getInstance().currentUser
        val databaseReference = FirebaseDatabase.getInstance().reference
        if (user!=null) {
            binding.postAdd.setOnClickListener {
                val text = binding.postAddText.text.toString()
                if (text.isNotEmpty()) {
                    val time = System.currentTimeMillis()
                    val userId = user.uid
                    val postId = UUID.randomUUID().toString()
                    val postModel = PostModel(postId,userId,time,text,null,null)
                    databaseReference.child("Posts").child(userId).child(postId).setValue(postModel)
                        .addOnSuccessListener {
                            displayPost(R.string.posted_successfully_text)
                            Log.d(tag,"Post $postId posted successfully")
                            finish()
                    }
                        .addOnFailureListener {
                            displayPost(R.string.posting_failed_text)
                            Log.e(tag,"Posting failed",it)
                            finish()
                        }
                }
            }
        }
        else {
            startActivity(Intent(this,HomeActivity::class.java))
            finish()
        }
    }

    private fun displayPost(i:Int) {
        Toast.makeText(this@PostAddActivity,i,Toast.LENGTH_LONG).show()
    }

}