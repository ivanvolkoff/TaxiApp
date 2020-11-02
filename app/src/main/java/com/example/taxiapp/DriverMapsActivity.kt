package com.example.taxiapp

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class DriverMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var CHECK_SETTINGS_CODE = 111
    private var REQUEST_LOCATION_PERMISSION = 111


    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var settingsClient: SettingsClient? = null
    private var locationRequest: LocationRequest? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var locationCallback: LocationCallback? = null
    private var currentLocation: Location? = null
    private var isLocationUpdatesActive = false
    private lateinit var settingsButton: Button
    private lateinit var sign_outButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_maps)

        settingsButton = findViewById(R.id.settings_button)
        sign_outButton = findViewById(R.id.sign_out_Button)
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!


        sign_outButton.setOnClickListener {
            auth.signOut()
            signOutDriver()
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices
            .getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)


        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()
        startLocationUpdates()

        settingsButton.setOnClickListener {
            var intent : Intent = Intent(this,DriverProfileSetings::class.java)
            intent.putExtra("driverName", currentUser)
            startActivity(intent)
        }
    }

    private fun signOutDriver() {

        var driverUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
        var driversGeoFire: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("driversGeoFire")

        var geoFire = GeoFire(driversGeoFire)
        geoFire.removeLocation(driverUserId)

        intent = Intent(this,DriverSignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
//        if (currentLocation != null) {
//            mMap.clear()
//            val driverLocation = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
//            mMap.addMarker(MarkerOptions().position(driverLocation).title("Driver Locaiton"))
//            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation))
//
//        }
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) {
            return
        }
        fusedLocationClient!!.removeLocationUpdates(locationCallback)
            .addOnCompleteListener(this) {
                isLocationUpdatesActive = false

            }
    }

    private fun startLocationUpdates() {
        isLocationUpdatesActive = true

        settingsClient!!.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this,
                OnSuccessListener {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) !=
                        PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat
                            .checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return@OnSuccessListener
                    }
                    fusedLocationClient!!.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.myLooper()
                    )
                    updateLocationUi()
                })
            .addOnFailureListener(this) { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException = e as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            this,
                            CHECK_SETTINGS_CODE
                        )
                    } catch (sie: IntentSender.SendIntentException) {
                        sie.printStackTrace()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val message = "Adjust location settings on your device"
                        Toast.makeText(
                            this, message,
                            Toast.LENGTH_LONG
                        ).show()
                        isLocationUpdatesActive = false

                    }
                }
                updateLocationUi()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_SETTINGS_CODE -> when (resultCode) {
                RESULT_OK -> {
                    Log.d(
                        "MainActivity", "User has agreed to change location" +
                                "settings"
                    )
                    startLocationUpdates()
                }
                RESULT_CANCELED -> {
                    Log.d(
                        "MainActivity", "User has not agreed to change location" +
                                "settings"
                    )
                    isLocationUpdatesActive = false

                    updateLocationUi()
                }
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest!!)
        locationSettingsRequest = builder.build()
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                currentLocation = locationResult.lastLocation
                updateLocationUi()
            }
        }
    }

    private fun updateLocationUi() {
        if (currentLocation != null) {

            var currentLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Driver Locaiton"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
//            mMap.animateCamera(CameraUpdateFactory.zoomTo(12f))

            var driverUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
            var driversGeoFire: DatabaseReference =
                FirebaseDatabase.getInstance().reference.child("driversGeoFire")


            var geoFire = GeoFire(driversGeoFire)
            geoFire.setLocation(
                driverUserId, GeoLocation(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                )
            )

        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest!!.interval = 10000
        locationRequest!!.fastestInterval = 3000
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (isLocationUpdatesActive && checkLocationPermission()) {
            startLocationUpdates()
        } else if (!checkLocationPermission()) {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            showSnackBar(
                "Location permission is needed for " +
                        "app functionality",
                "OK"
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(
        mainText: String,
        action: String,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mainText,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(
                action,
                listener
            )
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size <= 0) {
                Log.d(
                    "onRequestPermissions",
                    "Request was cancelled"
                )
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdatesActive) {
                    startLocationUpdates()
                }
            } else {
                showSnackBar(
                    "Turn on location on settings",
                    "Settings"
                ) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }


}