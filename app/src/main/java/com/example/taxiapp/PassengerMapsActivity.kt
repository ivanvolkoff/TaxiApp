package com.example.taxiapp

import android.Manifest
import android.annotation.SuppressLint
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
import com.example.taxiapp.model.Driver
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class PassengerMapsActivity : AppCompatActivity(), OnMapReadyCallback {

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
    private lateinit var signOutButton: Button
    private lateinit var bookTaxiButton: Button
    private lateinit var pickUpLocation: LatLng
    private lateinit var auth: FirebaseAuth
    private lateinit var currentDriver: FirebaseUser
    var userID: String = ""
    private lateinit var driversGeoFire: DatabaseReference
    private lateinit var geoFire: GeoFire
    private var searchRadius: Double = 1.0
    private var isDriverFound: Boolean = false
    private var nearestDriverId: String = ""
    private lateinit var nearestDriverLocation: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_maps)

        auth = FirebaseAuth.getInstance()
        currentDriver = auth.currentUser!!
        userID = currentDriver.uid

        initView()
        initClicks()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        driversGeoFire = FirebaseDatabase.getInstance().reference.child("driversAvailable")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        buildLocationRequest()
        buildLocationCallBack()
        buildLocationSettingsRequest()
        startLocationUpdates()
    }

    private fun gettingNearestTaxi() {

        geoFire = GeoFire(driversGeoFire)
        val geoQuery: GeoQuery = geoFire.queryAtLocation(
            GeoLocation(
                currentLocation!!.latitude,
                currentLocation!!.longitude
            ), searchRadius
        )

        geoQuery.removeAllListeners()

        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                if (!isDriverFound) {
                    isDriverFound = true
                    nearestDriverId = key!!
                    Log.i("cause", "$nearestDriverId")
                    Toast.makeText(
                        this@PassengerMapsActivity,
                        "Taxi Cab Found$nearestDriverId",
                        Toast.LENGTH_LONG
                    ).show()
                    var driverRef =
                        FirebaseDatabase.getInstance().reference.child("Users").child("Drivers")
                            .child(nearestDriverId)
                    var customerId = auth.currentUser?.uid
                    var map = hashMapOf<String, Any?>()
                    map.put("customerRideId", customerId)
                    driverRef.updateChildren(map)

                    getNearestDriverLocation()
                }
            }

            override fun onKeyExited(key: String?) {
                TODO("Not yet implemented")
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {
                TODO("Not yet implemented")
            }

            override fun onGeoQueryReady() {
                if (!isDriverFound) {
                    searchRadius++
                    gettingNearestTaxi()
                }
            }

            override fun onGeoQueryError(error: DatabaseError?) {
                TODO("Not yet implemented")
            }

        })


    }

    private fun getNearestDriverLocation() {
        bookTaxiButton.text = "Getting your driver location"
        Log.i("cause", "on button spell getting your driver location")
        nearestDriverLocation =
            FirebaseDatabase.getInstance().reference.child("driversAvailable")
                .child(nearestDriverId)
                .child("l")
        Log.i("cause", "onearestdriverlocation get instance")

        nearestDriverLocation.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i("cause", "on data changed before snapshot")

                if (snapshot.exists()) {
                    Log.i("cause", "on data changed after snapshot")

                    var driverLocationParameters: List<Any> = snapshot.value as List<Any>


                    var latitude: Double = 0.0
                    var longitude: Double = 0.0

                    if (driverLocationParameters[0] != null) {
                        latitude = driverLocationParameters[0] as Double
                    }

                    if (driverLocationParameters.get(1) != null) {
                        longitude = driverLocationParameters[1]!! as Double
                    }
                    Log.i("cause", "$latitude,  $longitude  ")
                    var driverLatLng = LatLng(latitude, longitude)

                    val driverLoc = Location("")
                    driverLoc.latitude = latitude
                    driverLoc.longitude = longitude

                    val distanceToDriver: Float = driverLoc.distanceTo(currentLocation) / 1000
                    var distanceInKm: String = "%.2f".format(distanceToDriver)
                    bookTaxiButton.text = "Distance to driver $distanceInKm Km"
                    Log.i("cause", "Distance to driver $distanceInKm Km")

                    mMap.addMarker(
                        MarkerOptions().position(driverLatLng).title("Your driver is here")
                    )
                    Log.i("cause", "Your driver is here")
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

    }


    private fun signOutPassenger() {
        val customerRequestRef = FirebaseDatabase.getInstance().getReference("customerRequests")
        val geoFire: GeoFire = GeoFire(customerRequestRef)
        geoFire.removeLocation(auth.currentUser?.uid)

        auth.signOut()
        intent = Intent(this, PassengerSignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

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
//            mMap.clear()
//            mMap.addMarker(MarkerOptions().position(currentLatLng).title("Passenger Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest!!.interval = 1000
        locationRequest!!.fastestInterval = 1000
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
            if (grantResults.isEmpty()) {
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

    private fun initClicks() {
        signOutButton.setOnClickListener {
            signOutPassenger()
        }
        this.bookTaxiButton.setOnClickListener {
            var currentLatLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            val passengerID = FirebaseAuth.getInstance().currentUser?.uid
            val customerRequestRef = FirebaseDatabase.getInstance().getReference("customerRequests")
            val geoFire: GeoFire = GeoFire(customerRequestRef)
            geoFire.setLocation(
                passengerID,
                GeoLocation(currentLatLng.longitude, currentLatLng.latitude)
            )

            pickUpLocation = currentLatLng
            stopLocationUpdates()
            mMap.addMarker(MarkerOptions().position(pickUpLocation).title("Pick up here"))

            gettingNearestTaxi()

        }
    }

    private fun initView() {
        signOutButton = findViewById(R.id.sign_out_Button)
        bookTaxiButton = findViewById(R.id.bookTaxiButton)
    }

    override fun onDestroy() {
        super.onDestroy()
        signOutPassenger()
    }
}