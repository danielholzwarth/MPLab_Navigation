package com.example.mplab_navigation

import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale


class MainActivity : Activity() {
    private lateinit var map : MapView
    private lateinit var mLocationOverlay: MyLocationNewOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val mapController = map.controller
        mapController.setZoom(18)

        this.mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this.applicationContext), map)
        this.mLocationOverlay.enableMyLocation()
        map.overlays.add(this.mLocationOverlay)

        val startPoint = GeoPoint(49.12, 9.21)
        map.controller.setCenter(startPoint)

        val searchBtn = findViewById<Button>(R.id.searchButton)
        searchBtn.setOnClickListener {
            val input = findViewById<EditText>(R.id.searchEditText).text.toString()
            if(input != ""){
                moveCameraToLocation(input)
            } else{
                Toast.makeText(this.applicationContext,"Input can not be empty!",Toast.LENGTH_LONG).show()
            }
        }

        val centerBtn = findViewById<Button>(R.id.centerButton)
        centerBtn.setOnClickListener {
            this.mLocationOverlay.enableFollowLocation()
        }

        val startRouteBtn = findViewById<Button>(R.id.startRouteButton)
        startRouteBtn.setOnClickListener {
            Toast.makeText(this.applicationContext,"Not implemented yet.", Toast.LENGTH_LONG).show()
        }
    }

    private fun moveCameraToLocation(locationName: String){
        try {
            val geocoder = Geocoder(this.applicationContext, Locale.getDefault())
            val geoResults: MutableList<Address>? = geocoder.getFromLocationName(locationName, 1)
            if (geoResults != null) {
                if (geoResults.isNotEmpty()) {
                    val address = geoResults[0]
                    val location = GeoPoint(address.latitude, address.longitude)
                    this.mLocationOverlay.disableFollowLocation()
                    map.controller.animateTo(location)
                }else{
                    Toast.makeText(this.applicationContext,"Location Not Found",Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: java.lang.Exception) {
            print(e.message)
        }
    }
}