package com.unreelnet.unnet.home

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.avatarfirst.avatargenlib.AvatarGenerator
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.navigation.NavigationBarView.OnItemSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.unreelnet.unnet.R
import com.unreelnet.unnet.databinding.ActivityHomeBinding
import com.unreelnet.unnet.home.fragments.PostAddFragment
import com.unreelnet.unnet.home.fragments.PostViewFragment
import com.unreelnet.unnet.home.fragments.SearchFragment
import com.unreelnet.unnet.models.UserModel
import com.unreelnet.unnet.profile.ViewProfileActivity
import java.io.ByteArrayOutputStream
import java.util.UUID

class HomeActivity : AppCompatActivity(),OnItemSelectedListener {
    private val tag = "HomeActivity"
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var binding: ActivityHomeBinding
    private lateinit var signInIntent:Intent
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.AnonymousBuilder().build(),
        )
        signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setLogo(R.mipmap.ic_launcher)
            .setTheme(R.style.Theme_UnNet)
            .setAvailableProviders(providers)
            .setTosAndPrivacyPolicyUrls("https://www.anons.cloud/terms","https://www.anons.cloud/privacy")
            .build()



        binding.homeBottomNavigation.setOnItemSelectedListener (this)
        binding.homeBottomNavigation.selectedItemId = R.id.post

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser==null) {
            signInLauncher.launch(signInIntent)
        }
        else {
            getCurrentUser(currentUser.uid)
        }
    }

    private fun getCurrentUser(userId:String) {
        databaseReference.child("Users").child(userId).addListenerForSingleValueEvent(
            object:ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userModel = snapshot.getValue(UserModel::class.java)
                    if (userModel!=null) {
                        setupView(userModel)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(tag,"Couldn't get current user",error.toException())
                    Toast.makeText(this@HomeActivity,"Couldn't get current user",Toast.LENGTH_LONG).show()
                    AuthUI.getInstance().signOut(this@HomeActivity).addOnSuccessListener {
                        signInLauncher.launch(signInIntent)
                    }
                }

            }
        )
    }

    private fun setupView(user: UserModel) {
        Glide.with(this).load(user.profileImage).dontAnimate().into(binding.homeProfileImage)
        binding.homeProfileImage.setOnClickListener {
            val popup = PopupMenu(this@HomeActivity,binding.homeProfileImage)
            popup.menuInflater.inflate(R.menu.home_profile_menu,popup.menu)
            popup.setOnMenuItemClickListener {
                when(it.itemId) {
                    R.id.home_profile -> {
                        ViewProfileActivity.startProfileActivity(this@HomeActivity,user)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.home_sign_out -> {
                        AuthUI.getInstance().signOut(this@HomeActivity).addOnSuccessListener {
                            signInLauncher.launch(signInIntent)
                        }
                        return@setOnMenuItemClickListener true
                    }
                    else ->
                        return@setOnMenuItemClickListener false
                }
            }
            popup.show()
        }
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (response!=null && response.isNewUser && user!=null) {
                var photoUrl: String
                val name = if (user.displayName == null) "Anonymous" else user.displayName!!
                if (user.photoUrl!=null) {
                    photoUrl = user.photoUrl.toString()
                    uploadUser(user,name,photoUrl)
                }
                else {
                    val bitmap = AvatarGenerator.AvatarBuilder(this@HomeActivity)
                        .setLabel(name)
                        .setAvatarSize(60)
                        .setTextSize(15)
                        .toCircle()
                        .build().bitmap
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    FirebaseStorage.getInstance()
                        .getReference("Users/${user.uid}/ProfileImages")
                        .child(UUID.randomUUID().toString()).putBytes(data).addOnSuccessListener {
                            it.storage.downloadUrl.addOnSuccessListener {uri->
                                photoUrl = uri.toString()
                                uploadUser(user,name,photoUrl)
                            }
                        }
                        .addOnFailureListener {
                            Log.e(tag,"Failed to upload user profile image",it)
                            photoUrl = "https://firebasestorage.googleapis.com/v0/b/unnet-unreel.appspot.com/o/" +
                                    "Assets%2FGeneric%20User.jpeg?alt=media&token=f8d2530c-48ad-4b67-a276-d1ae16a661be"
                            uploadUser(user,name,photoUrl)
                        }
                }

            }
        } else {
            if (response == null) {
                showToast(R.string.sign_in_cancelled)
                return
            }

            if (response.error?.errorCode == ErrorCodes.NO_NETWORK) {
                showToast(R.string.no_internet_connection)
                return
            }

            showToast(R.string.unknown_error)
            Log.e(tag, "Sign-in error: ", response.error)
        }
    }

    private fun uploadUser(user: FirebaseUser, name:String, photoUrl:String) {
        val userModel = UserModel(user.uid,user.email,user.phoneNumber
            ,name,photoUrl)
        databaseReference.child("Users").child(user.uid).setValue(userModel).addOnSuccessListener {
            setupView(userModel)
        }
            .addOnFailureListener {
                Toast.makeText(this@HomeActivity,"Failed to upload user.try again",Toast.LENGTH_LONG).show()
                Log.e(tag,"Failed to upload user",it)
                AuthUI.getInstance().signOut(this@HomeActivity).addOnSuccessListener {
                    signInLauncher.launch(signInIntent)
                }
            }
    }

    private fun showToast(i:Int) {
        Toast.makeText(this,i,Toast.LENGTH_LONG).show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment:Fragment  = PostViewFragment()
        var title = getString(R.string.home_text)
        var logo = R.drawable.baseline_home_48
        when(item.itemId) {
            R.id.post -> {
                fragment = PostViewFragment()
                title = getString(R.string.home_text)
                logo = R.drawable.baseline_home_48
            }
            R.id.add_post -> {
                fragment = PostAddFragment()
                title = getString(R.string.add_post_text)
                logo = R.drawable.baseline_add_48
            }
            R.id.search -> {
                fragment = SearchFragment()
                title = getString(R.string.search_text)
                logo = R.drawable.baseline_search_48
            }
        }
        binding.homeToolbar.title = title
        binding.homeToolbar.setLogo(logo)
        supportFragmentManager.beginTransaction().replace(R.id.home_frame,fragment).commit()
        return true
    }

}