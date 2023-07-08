package com.unreelnet.unnet.home.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.FragmentPostBinding
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.utils.reusable.Callback
import com.unreelnet.unnet.utils.reusable.ReusableCode


class PostAddFragment(private val postId:String,private val callback: Callback<PostModel.Media>) : Fragment() {

    private lateinit var binding:FragmentPostBinding
    private var media: PostModel.Media? = null
    private val databaseReference = FirebaseDatabase.getInstance().reference
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view  = inflater.inflate(R.layout.fragment_post, container, false)
        binding = FragmentPostBinding.bind(view)
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.postAddVideo.setOnClickListener {
            getContentVideo.launch("video/*")
        }
        binding.postAddPhoto.setOnClickListener {
            getContentImage.launch("image/*")
        }
        if (currentUser!=null) {
            binding.postAddUpload.setOnClickListener {
                Toast.makeText(context, "Uploading😍", Toast.LENGTH_SHORT).show()
                var text:String? = binding.postAddText.text.toString()
                if (text.isNullOrBlank())text = null
                val postModel = PostModel(postId,currentUser.uid,System.currentTimeMillis(),text)
                if (media!=null)postModel.visibility = PostModel.PostVisibility.PRIVATE
                databaseReference.child("Posts").child(currentUser.uid).child(postId)
                    .setValue(postModel)
                    .addOnSuccessListener {
                        if (media!=null) {
                            callback.onCall(media) //Upload files to storage and set the database values
                        }
                    }
            }
        }
        return binding.root
    }

    private val getContentImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it!=null) {
            media = PostModel.Media(it.toString(),PostModel.MediaType.PHOTO)
            binding.addPostParent.visibility = View.VISIBLE
            binding.addPostImage.visibility = View.VISIBLE
            binding.addPostVideo.visibility = View.GONE
            ReusableCode.loadPostImage(requireContext(),binding.addPostImage,null,it)
        }
    }

    private val getContentVideo = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it!=null) {
            media = PostModel.Media(it.toString(),PostModel.MediaType.VIDEO)
            binding.addPostParent.visibility = View.VISIBLE
            binding.addPostVideo.visibility = View.VISIBLE
            binding.addPostImage.visibility = View.GONE
            ReusableCode.setupExoPlayer(requireContext(),binding.addPostVideo,it)
        }
    }

}