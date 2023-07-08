package com.unreelnet.unnet.home.fragments

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.unreelnet.unnet.R
import com.unreelnet.unnet.models.PostModel
import com.unreelnet.unnet.utils.adapters.PostRecyclerViewAdapter


class PostViewFragment : Fragment() {

    private val posts:MutableList<PostModel> = ArrayList()
    private val postIds:MutableList<String> = ArrayList()
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var postAdapter: PostRecyclerViewAdapter
    private val thread = BackgroundPostAdder{
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser!=null) {
            getFollowingAndPosts(currentUser.uid)
        }
    }

    private val tag = "PostViewFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_post_view, container, false)
        if (view is RecyclerView) {
            postAdapter = PostRecyclerViewAdapter(context,posts)
            with(view) {
                adapter = postAdapter
            }
        }

        thread.execute()

        return view
    }

    override fun onDestroy() {
        thread.cancel(false)
        super.onDestroy()
    }

    private fun getFollowingAndPosts(currentUserId: String) {
        databaseReference.child("Following").child(currentUserId)
            .addValueEventListener(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (idSnapshot in snapshot.children) {
                            val id = idSnapshot.getValue(String::class.java)
                            if (id!=null) {
                                getPosts(id)
                            }
                            else {
                                Log.e(tag,"Snapshot $idSnapshot value is null")
                            }
                        }
                    }
                    else {
                        Log.d(tag,"User isn't following anyone trying backup method")
                    }
                    getSponsors()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get followings")
                }

            })
    }

    private fun getSponsors() {
        databaseReference.child("Sponsors")
            .addValueEventListener(object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (idSnapshot in snapshot.children) {
                        val id = idSnapshot.getValue(String::class.java)
                        if (id!=null && !postIds.contains(id)) {
                            postIds.add(id)
                            getPosts(id)
                        }
                        else {
                            Log.e(tag,"Snapshot $idSnapshot value is null")
                        }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get followings")
                }

            })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getPosts(userId:String) {
        databaseReference.child("Posts").child(userId)
            .addValueEventListener(object  : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (postSnapshot in snapshot.children) {
                            val postModel = postSnapshot.getValue(PostModel::class.java)
                            if (postModel!=null&&postModel.visibility == PostModel.PostVisibility.VISIBLE) {
                                posts.add(postModel)
                            }
                        }
                    }
                    else {
                        Log.d(tag,"User $userId doesn't have any posts")
                    }
                    postAdapter.posts = posts.sortedByDescending { it.uploadTime }.toMutableList()
                    postAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get posts of $userId")
                }

            })

    }

    class BackgroundPostAdder(private val run:Runnable) : AsyncTask<Unit, Unit, Unit>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg p0: Unit?) {
            run.run()
        }

    }

}