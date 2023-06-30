package com.unreelnet.unnet.home.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.FragmentUserListBinding
import com.unreelnet.unnet.models.UserModel
import com.unreelnet.unnet.utils.adapters.UserRecyclerViewAdapter

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)
        val binding  = FragmentUserListBinding.bind(view)
        val query = FirebaseDatabase.getInstance()
            .reference
            .child("Users")
        val options = FirebaseRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        val userAdapter = UserRecyclerViewAdapter(context,options)
        binding.list.adapter = userAdapter
        userAdapter.startListening()

        binding.userSearch.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence, p1: Int, p2: Int, p3: Int) {
                userAdapter.stopListening()
                val newQuery = FirebaseDatabase.getInstance()
                    .reference
                    .child("Users")
                    .orderByChild("name")
                    .startAt(s.toString())
                    .endAt(s.toString() + "\uf8ff")
                val newOptions = FirebaseRecyclerOptions.Builder<UserModel>()
                    .setQuery(newQuery, UserModel::class.java)
                    .build()
                val newAdapter = UserRecyclerViewAdapter(context,newOptions)
                binding.list.adapter = newAdapter
                newAdapter.startListening()
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        return view
    }
}