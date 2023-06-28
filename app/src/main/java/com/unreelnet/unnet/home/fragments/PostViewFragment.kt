package com.unreelnet.unnet.home.fragments

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
import com.unreelnet.unnet.home.models.PostModel
import com.unreelnet.unnet.home.utils.adapters.PostRecyclerViewAdapter


class PostViewFragment : Fragment() {

    private val posts:MutableList<PostModel> = ArrayList()
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var postAdapter:PostRecyclerViewAdapter

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
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser!=null) {
            getFollowingAndPosts(currentUser.uid)
        }

        return view
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
                        if (id!=null) {
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

    private fun getPosts(userId:String) {
        databaseReference.child("Posts").child(userId)
            .addValueEventListener(object  : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (postSnapshot in snapshot.children) {
                            val postModel = postSnapshot.getValue(PostModel::class.java)
                            if (postModel!=null) {
                                posts.add(postModel)
                                postAdapter.notifyItemInserted(posts.size-1)
                            }
                        }
                    }
                    else {
                        Log.d(tag,"User $userId doesn't have any posts")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get posts of $userId")
                }

            })
    }

}