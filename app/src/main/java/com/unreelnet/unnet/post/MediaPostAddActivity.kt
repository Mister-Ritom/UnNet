package com.unreelnet.unnet.post

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ActivityMediaPostAddBinding
import com.unreelnet.unnet.models.PostModel
import java.util.UUID

class MediaPostAddActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMediaPostAddBinding
    private val tag = "MediaPostAddActivity"

    private var user:FirebaseUser? = null
    private val database = FirebaseDatabase.getInstance().reference

    private var uri:Uri? = null
    private var mediaType:MediaTypes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaPostAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        user = FirebaseAuth.getInstance().currentUser
        if (user==null)finish()

        binding.postBack.setOnClickListener {
            finish()
        }
        binding.postProgressLayout.visibility = View.GONE
        if (intent.extras!=null) {
            mediaType = intent.extras!!.getSerializable("mediaType") as MediaTypes?
            if (mediaType!=null) {
                if (mediaType==MediaTypes.VIDEO) {
                    getContent.launch( "video/*")
                    binding.postImage.visibility = View.GONE
                }
                else {
                    getContent.launch( "image/*")
                    binding.postVideo.visibility = View.GONE
                }
            }
            else {
                finish()
                displayToast("Something went wrong")
            }
        } else finish()
    }

    private fun setupView() {
        if (mediaType!=null&&uri!=null && user!=null) {
            if (mediaType==MediaTypes.PHOTO) {
                Glide.with(this).load(uri).into(binding.postImage)
            }
            else {
                binding.postVideo.setVideoURI(uri)
                binding.postVideo.start()
            }
            binding.postSubmit.setOnClickListener {
                uploadPost()
            }
        } else {
            displayToast("Something went wrong")
            finish()
        }
    }

    private fun uploadPost() {
        binding.postProgressLayout.visibility = View.VISIBLE
        val postUid = UUID.randomUUID().toString()
        if (uri!=null&&mediaType!=null&&user!=null) {
            FirebaseStorage.getInstance().reference
                .child("Posts").child(user!!.uid).child(mediaType!!.name).child(postUid)
                .putFile(uri!!).addOnSuccessListener {
                    binding.postStatus.text = getString(R.string.uploaded_text)
                    it.storage.downloadUrl.addOnSuccessListener {
                        addToDatabase(postUid,uri)
                    }
                        .addOnFailureListener {
                            Log.e(tag,"Couldn't get download url",it)
                        }
                }
                .addOnFailureListener {
                    Log.e(tag,"Couldn't upload post type $mediaType",it)
                    displayToast("Failed to upload post")
                }
                .addOnProgressListener {
                    var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                    progress-=10
                    binding.progressText.text = progress.toString()
                    binding.postStatus.text = getString(R.string.uploading_text)
                }
        } else {
            displayToast("Something went wrong")
            finish()
        }
    }

    private fun addToDatabase(uid:String,storageUri:Uri?) {
        var text:String? = binding.postTitle.text.toString()
        if (text!=null && text.isBlank()) text = null
        binding.postStatus.text = getString(R.string.database_adding_text)
        if (storageUri!=null && mediaType!=null && user!=null) {
            val postModel = PostModel(uid,user!!.uid,System.currentTimeMillis(),text,
                if (mediaType == MediaTypes.VIDEO)null else storageUri.toString(),
                if (mediaType == MediaTypes.VIDEO) storageUri.toString() else null)
            database.child("Posts").child(user!!.uid).child(uid)
                .setValue(postModel).addOnSuccessListener {
                finish()
            }
                .addOnFailureListener {
                    displayToast("technical error")
                    FirebaseStorage.getInstance().reference
                        .child("Posts").child(user!!.uid).child(mediaType!!.name).child(uid)
                        .delete()
                    finish()
                }
        } else {
            displayToast("Something went wrong")
            finish()
        }
    }

    private fun displayToast(s:String) {
        Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        this.uri = uri
        setupView()
    }

    companion object {
        enum class MediaTypes {
            PHOTO,VIDEO;
        }
        fun startPostAddActivity(context:Context?,mediaType: MediaTypes) {
            val intent = Intent(context,MediaPostAddActivity::class.java)
            intent.putExtra("mediaType",mediaType)
            context?.startActivity(intent)
        }
    }

}