package com.example.taxiapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taxiapp.model.Driver
import com.firebase.geofire.GeoFire
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class DriverSignInActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputLayout
    private lateinit var nameInput: TextInputLayout
    private lateinit var lastName : TextInputLayout
    private lateinit var passwordInput: TextInputLayout
    private lateinit var passwordConfirmInput: TextInputLayout
    private lateinit var carTV : TextInputLayout
    private lateinit var logInSignInButton: Button
    private lateinit var toggleLoginSignUp: TextView
    private var isLoginModeActive = false
    private lateinit var activityHeader: TextView
    private lateinit var auth: FirebaseAuth
    private val TAG: String = "log record"
    private var database: FirebaseDatabase? = null
    private lateinit var DriversDatabaseReference : DatabaseReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_sign_in)



        initViews()
        auth = FirebaseAuth.getInstance()
    }

    fun toggleLoginSignUp(view: View) {
        if (isLoginModeActive) {
            isLoginModeActive = false
            logInSignInButton.text = "Sign Up"
            toggleLoginSignUp.text = "Or,Sign In"
            passwordConfirmInput.visibility = View.VISIBLE
            activityHeader.text = "Driver sign up"
        } else {
            isLoginModeActive = true
            logInSignInButton.text = "Sign in"
            toggleLoginSignUp.text = "Or,Sign up"
            passwordConfirmInput.visibility = View.INVISIBLE
            activityHeader.text = "Driver sign in"

        }
    }

    private fun isEmailValid(): Boolean {
        val email = emailInput.editText?.text.toString().trim()
        return if (email.isEmpty()) {
            emailInput.error = "Please Input your email"
            false
        } else {
            emailInput.error = ""
            true
        }
    }

    private fun isNameValid(): Boolean {
        var nameInputSring: String = nameInput.editText?.text.toString().trim()

        if (nameInputSring.isEmpty()) {
            nameInput.error = "name"
            return false
        } else if (nameInputSring.length > 15) {
            nameInput.error = "Name lenght have to be less than 15 symbols"
            return false
        } else {
            nameInput.error = ""
            return true
        }
    }

    private fun isPasswordValid(): Boolean {
        var passwordInputSring: String = passwordInput.editText?.text.toString().trim()

        return when {
            passwordInputSring.isEmpty() -> {
                passwordInput.error = "Please input your password"
                false
            }
            passwordInputSring.length < 8 -> {
                passwordInput.error = "Password must be at least 8 symbols"
                false
            }
            else -> {
                passwordInput.error = ""
                passwordConfirmInput.error = ""
                true
            }
        }
    }

    private fun isPasswordConfirmValid(): Boolean {
        var passwordInputSring: String = passwordInput.editText?.text.toString().trim()
        var passwordConfirmInputSring: String =
            passwordConfirmInput.editText?.text.toString().trim()
        if (!passwordInputSring.equals(passwordConfirmInputSring)) {
            passwordInput.error = "Passwords didn't match"
            passwordConfirmInput.error = "Passwords didn't match"
            return false
        } else {
            passwordInput.error = ""
            passwordConfirmInput.error = ""
            return true
        }
    }

    fun loginSignUpUser(view: View) {
        if (!isEmailValid() or !isNameValid() or !isPasswordValid()) {
            return
        }
        if (isLoginModeActive) {
            auth.signInWithEmailAndPassword(
                emailInput.editText?.text.toString().trim(),
                passwordInput.editText?.text.toString().trim()
            )
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "log in success", Toast.LENGTH_LONG).show()
                        val user = auth.currentUser
                        startActivity(
                            Intent(this@DriverSignInActivity, DriverMapsActivity::class.java)
                        )
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "log in failed", Toast.LENGTH_LONG).show()
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        //updateUI(null)
                        // ...
                    }
                }
        } else if (!isLoginModeActive) {
            if (!isEmailValid() or !isNameValid() or !isPasswordValid() or !isPasswordConfirmValid()) {
                return
            }

            auth.createUserWithEmailAndPassword(
                emailInput.editText?.text.toString().trim(),
                passwordInput.editText?.text.toString().trim()
            )
                .addOnCompleteListener(this,
                    OnCompleteListener<AuthResult?> { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Toast.makeText(this, "sign up success", Toast.LENGTH_LONG).show()
                            var userID = auth.currentUser?.uid.toString()
                            var currentUserDB: DatabaseReference =
                                FirebaseDatabase.getInstance().reference.child("Users")
                                    .child("Drivers").child(userID)
                            currentUserDB.setValue(createDriver())
                            startActivity(
                                Intent(this@DriverSignInActivity, DriverMapsActivity::class.java)
                            )
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(this, "sign up failed", Toast.LENGTH_LONG).show()
                            Toast.makeText(
                                this@DriverSignInActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // updateUI(null)
                        }
                    })

        }
    }

    private fun initViews() {
        emailInput = findViewById(R.id.driver_input_email)
        nameInput = findViewById(R.id.driver_name_input)
        passwordInput = findViewById(R.id.driver_password_input)
        passwordConfirmInput = findViewById(R.id.driver_password_confirmation_input)
        logInSignInButton = findViewById(R.id.passenger_login_button)
        toggleLoginSignUp = findViewById(R.id.passenger_toggle_login_signup_textView)
        activityHeader = findViewById(R.id.driverSignInSignUp)
        lastName= findViewById(R.id.driver_last_name_input)
        carTV = findViewById(R.id.driver_car_model_input)
    }

    private fun createDriver():Driver{
        var driver : Driver = Driver(
            nameInput.editText?.text.toString(),
            lastName.editText?.text.toString(),
            carTV.editText?.text.toString(),
            emailInput.editText?.text.toString(),
            geoFire = null
        )
        return driver
    }
}
