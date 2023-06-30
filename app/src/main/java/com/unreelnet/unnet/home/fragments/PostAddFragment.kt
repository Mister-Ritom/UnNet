package com.unreelnet.unnet.home.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.FragmentPostAddBinding
import com.unreelnet.unnet.post.MediaPostAddActivity
import com.unreelnet.unnet.post.PostAddActivity


class PostAddFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view  = inflater.inflate(R.layout.fragment_post_add, container, false)
        val binding = FragmentPostAddBinding.bind(view)
        binding.postAddText.setOnClickListener {
            startActivity(Intent(context, PostAddActivity::class.java))
        }
        binding.postAddImage.setOnClickListener {
            MediaPostAddActivity.startPostAddActivity(context,MediaPostAddActivity.Companion.MediaTypes.PHOTO)
        }
        binding.postAddVideo.setOnClickListener {
            MediaPostAddActivity.startPostAddActivity(context,MediaPostAddActivity.Companion.MediaTypes.VIDEO)
        }
        return view
    }
}