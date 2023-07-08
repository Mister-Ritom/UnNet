package com.unreelnet.unnet.post

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ActivityViewPostBinding
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.models.UserModel
import com.unreelnet.unnet.utils.adapters.CommentRecyclerViewAdapter
import com.unreelnet.unnet.utils.reusable.Callback
import com.unreelnet.unnet.utils.reusable.ReusableAnimator
import com.unreelnet.unnet.utils.reusable.ReusableCode
import kotlin.random.Random


@Suppress("deprecation")
class ViewPostActivity : AppCompatActivity() {
    
    private val tag = "ViewPostActivity"
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var binding:ActivityViewPostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.extras==null) finish()
        val post = intent.extras?.getParcelable<PostModel>("post")
        val user = intent.extras?.getParcelable<UserModel>("user")
        if (post==null||user==null)finish()
        else {
            setupEngagementButtons(post)
            binding.postBack.setOnClickListener { finish() }
            binding.postComments.adapter = CommentRecyclerViewAdapter(this,post.comments)

            if (post.media==null){
                binding.postParent.visibility = View.GONE
            }
            else{
                binding.postParent.visibility = View.VISIBLE
                if (post.media.mediaType==PostModel.MediaType.PHOTO) {
                    binding.postImage.visibility = View.VISIBLE
                    setupImage(post)
                }

                else {
                    binding.postVideo.visibility = View.VISIBLE
                    setupVideo(Uri.parse(post.media.uri))
                }
            }

            if (post.text==null)binding.postText.visibility = View.GONE
            else binding.postText.text = post.text

            binding.postToolbarText.text = getString(R.string.post_by_text,user.name)
            Glide.with(this).load(user.profileImage).into(binding.postUserLayout.userProfileImage)
            binding.postUserLayout.userProfileUsername.text = user.username
            binding.postUserLayout.userProfileName.text = user.name
        }
    }

    private fun setupImage(post:PostModel) {
        Glide.with(this).asBitmap().load(post.media?.uri).listener(object:
            RequestListener<Bitmap> {
            override fun onLoadFailed(
                e: GlideException?,
                a: Any?,
                target: Target<Bitmap>?,
                isFirstResource: Boolean
            ): Boolean {
                Log.e(tag,"Couldn't load post ${post.postId}")
                Toast.makeText(this@ViewPostActivity, "Couldn't load post", Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onResourceReady(
                resource: Bitmap,
                post: Any?,
                target: Target<Bitmap>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                ReusableCode.setBackground(resource,binding.postBackground)
                runOnUiThread {
                    binding.postLoadProgress.visibility = View.GONE
                    binding.postImage.setImageBitmap(resource)
                }
                return true
            }
        }).submit()
    }

    private fun setupVideo(uri:Uri) {
        ReusableCode.setupExoPlayer(this,binding.postVideo,uri).also {
            it.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == ExoPlayer.STATE_READY) {
                        binding.postLoadProgress.visibility = View.GONE
                        BackgroundSetter(it.duration,uri) { resource ->
                            ReusableCode.setBackground(resource,binding.postBackground)
                        }.execute()
                    }
                    super.onPlaybackStateChanged(playbackState)
                }
            })
        }
    }

    @Suppress("deprecation")
    private fun setupEngagementButtons(model:PostModel) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        binding.postCommentText.text =getString(R.string.comments_text,model.comments.size.toString())
        binding.postShareText.text = getString(R.string.share_text,model.shares.size.toString())
        binding.postLikeText.text = getString(R.string.likes_text,model.likes.size.toString())
        if (currentUser!=null) {
            val currentUserId = currentUser.uid
            if (model.likes.contains(currentUserId))binding.postHeart.setColorFilter(this.resources!!.getColor(R.color.red))
            binding.postLikeCard.setOnClickListener {
                if (model.likes.contains(currentUserId)) {
                    model.likes.remove(currentUserId)
                    databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                        .child("likes").setValue(model.likes).addOnSuccessListener {
                            binding.postHeart.setColorFilter(this.resources!!.getColor(R.color.gray),
                                PorterDuff.Mode.SRC_ATOP)
                        }
                }
                else {
                    model.likes.add(currentUserId)
                    databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                        .child("likes").setValue(model.likes).addOnSuccessListener {
                            ReusableAnimator.animate(binding.postHeart)
                            binding.postHeart.setColorFilter(this.resources!!.getColor(R.color.red))
                        }
                }
            }
            binding.postCommentCard.setOnClickListener {
                ReusableCode.setupCommentDialog(currentUserId,this@ViewPostActivity,model)
                ReusableAnimator.animate(binding.postComment)
            }
            binding.postShareCard.setOnClickListener {
                val model1 = PostModel(model.postId,model.uploaderId,System.currentTimeMillis(),
                    model.text,model.media,model.visibility,model.likes,model.comments,
                    model.shares,currentUserId)
                model1.shares.add(currentUserId)
                databaseReference.child("Posts").child(currentUserId).child(model.postId)
                    .setValue(model1).addOnSuccessListener {
                        databaseReference.child("Posts/${model.uploaderId}/${model.postId}")
                            .child("shares").setValue(model1.shares).addOnSuccessListener {
                                ReusableAnimator.animate(binding.postShare)
                                Toast.makeText(this,"Shared post",Toast.LENGTH_LONG).show()
                            }
                    }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        binding.postVideo.player?.release()
    }

    class BackgroundSetter(private val duration:Long,private val uri:Uri,private val callback: Callback<Bitmap>) : AsyncTask<Unit,Unit,Unit>(){
        private val tag = "BackgroundSetter"
        @Deprecated("Deprecated in Java")
        private fun getVideoFrame(s:String):Bitmap? {
            var bitmap:Bitmap? = null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(s, emptyMap())
                bitmap = run {
                    val time = Random.nextLong(1000,duration)
                    retriever.getFrameAtTime(time)
                }
            } catch (e:RuntimeException) {
                Log.e(tag,"Something went wrong",e)

            } finally {

                try {
                    retriever.release()
                } catch (e:RuntimeException) {
                    Log.e(tag,"Something went wrong",e)
                }
            }

            return bitmap

        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg time: Unit?) {
            val bitmap = getVideoFrame(uri.toString())
            if (bitmap!=null) {
                callback.onCall(bitmap)
            }
        }
    }

    companion object {
        fun startViewPostActivity(context:Context, user:UserModel, post:PostModel) {
            val intent = Intent(context,ViewPostActivity::class.java)
            intent.putExtra("post",post)
            intent.putExtra("user",user)
            context.startActivity(intent)
        }
    }
    
}