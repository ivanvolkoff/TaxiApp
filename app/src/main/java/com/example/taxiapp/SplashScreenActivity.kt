package com.example.taxiapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen_main)

        val thread: Thread = object : Thread() {
            override fun run() = try {
                sleep(2000)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                startActivity(
                    Intent(this@SplashScreenActivity, ChoseModeActivity::class.java)
                )
            }
        }
        thread.start()

    }

    override fun onPause() {
        super.onPause()
        finish()
    }
}