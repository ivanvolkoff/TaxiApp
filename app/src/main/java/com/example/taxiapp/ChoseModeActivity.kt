package com.example.taxiapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase

class ChoseModeActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chose_mode)

        auth = FirebaseAuth.getInstance()


    }

    fun goToPassangerSignIn(view: View) {
        startActivity(
            Intent(this@ChoseModeActivity, PassengerSignInActivity::class.java))
    }
    fun goToDriverSignIn(view: View) {
        startActivity(
            Intent(this@ChoseModeActivity, DriverSignInActivity::class.java))
    }
}