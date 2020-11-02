package com.example.taxiapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.taxiapp.model.Driver

class DriverProfileSetings : AppCompatActivity() {



    private lateinit var firstNameTV: TextView
    private lateinit var lastNameTv: TextView
    private lateinit var car: TextView
    private lateinit var  driver: Driver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile_setings)

        firstNameTV = findViewById(R.id.firstNameTV)
        lastNameTv = findViewById(R.id.lastNameTV)
        car = findViewById(R.id.driver_car_model_input)














    }
}


