package com.example.taxiapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class PassengerSignInActivity : AppCompatActivity() {
    private lateinit var emailInput: TextInputLayout
    private lateinit var nameInput: TextInputLayout
    private lateinit var passwordInput: TextInputLayout
    private lateinit var passwordConfirmInput: TextInputLayout
    private lateinit var logInSignInButton: Button
    private lateinit var toggleLoginSignUp: TextView
    private var isLoginModeActive = false
    private lateinit var activityHeader: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passanger_sign_in)
        auth = FirebaseAuth.getInstance()
        initViews()

    }


    @SuppressLint("SetTextI18n")
    fun toggleLoginSignUp(view: View) {
        if (isLoginModeActive) {
            isLoginModeActive = false
            logInSignInButton.text = "Sign Up"
            toggleLoginSignUp.text = "Or,Sign In"
            passwordConfirmInput.visibility = View.VISIBLE
            activityHeader.text = "Passenger sign up"
        } else {
            isLoginModeActive = true
            logInSignInButton.text = "Sign in"
            toggleLoginSignUp.text = "Or,Sign up"
            passwordConfirmInput.visibility = View.INVISIBLE
            activityHeader.text = "Passenger sign in"

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
        val nameInputString: String = nameInput.editText?.text.toString().trim()


        return when {
            nameInputString.isEmpty() -> {
                nameInput.error = "name"
                false
            }
            nameInputString.length > 15 -> {
                nameInput.error = "Name length have to be less than 15 symbols"
                false
            }
            else -> {
                nameInput.error = ""
                true
            }
        }
    }

    private fun isPasswordValid(): Boolean {
        val passwordInputSring: String = passwordInput.editText?.text.toString().trim()

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
        val passwordInputSring: String = passwordInput.editText?.text.toString().trim()
        val passwordConfirmInputSring: String =
            passwordConfirmInput.editText?.text.toString().trim()
        return if (passwordInputSring != passwordConfirmInputSring) {
            passwordInput.error = "Passwords didn't match"
            passwordConfirmInput.error = "Passwords didn't match"
            false
        } else {
            passwordInput.error = ""
            passwordConfirmInput.error = ""
            true
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
                        auth.currentUser
                        startActivity(
                            Intent(this@PassengerSignInActivity ,PassengerMapsActivity::class.java)
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
                            val userID = auth.currentUser?.uid.toString()
                            val currentUserDB: DatabaseReference =
                                FirebaseDatabase.getInstance().reference.child("Users")
                                    .child("Customers").child(userID)
                            currentUserDB.setValue(true)
                            startActivity(
                                Intent(this@PassengerSignInActivity ,PassengerMapsActivity::class.java)
                            )
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(this, "sign up failed", Toast.LENGTH_LONG).show()
                            Toast.makeText(
                                this@PassengerSignInActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // updateUI(null)
                        }
                    })

        }
    }

    private fun initViews() {
        emailInput = findViewById(R.id.passenger_input_email)
        nameInput = findViewById(R.id.passenger_name_input)
        passwordInput = findViewById(R.id.passenger_passwordInput)
        passwordConfirmInput = findViewById(R.id.passenger_password_confirmation_input)
        logInSignInButton = findViewById(R.id.passenger_login_button)
        toggleLoginSignUp = findViewById(R.id.passenger_toggle_login_signup_textView)
        activityHeader = findViewById(R.id.passengerSignInSignUp)
    }

}