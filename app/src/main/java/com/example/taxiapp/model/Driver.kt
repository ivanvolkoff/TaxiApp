package com.example.taxiapp.model

import android.location.Location
import com.firebase.geofire.GeoFire
import com.google.android.gms.maps.model.LatLng


class Driver(var firstName:String,var lastName: String,var car :String,var email:String,var geoFire: GeoFire?) {

}