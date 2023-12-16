package com.example.mplab_navigation

import android.app.Activity
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale


class MainActivity : Activity() {
    private lateinit var map : MapView
    private lateinit var mLocationOverlay: MyLocationNewOverlay
    private lateinit var targetLocation: GeoPoint
    private lateinit var routeOverlay: Polyline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

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
            if(input.isNotEmpty()){
                getGeoPointFromString(input)?.let { target ->
                    targetLocation = target
                    moveCameraToLocation()
                }
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
            val input = findViewById<EditText>(R.id.searchEditText).text.toString()
            if(input.isNotEmpty()){
                getGeoPointFromString(input)?.let { target ->
                    targetLocation = target
                    moveCameraToLocation()

                    //drawLine()
                    drawRoute()
                }
            } else {
                Toast.makeText(this.applicationContext,"Input can not be empty!",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getGeoPointFromString(locationName: String): GeoPoint? {
        try {
            val geocoder = Geocoder(this.applicationContext, Locale.getDefault())
            val geoResults: MutableList<Address>? = geocoder.getFromLocationName(locationName, 1)
            if (geoResults != null) {
                if (geoResults.isNotEmpty()) {
                    val address = geoResults[0]
                    return GeoPoint(address.latitude, address.longitude)
                } else {
                    Toast.makeText(this.applicationContext,"Location Not Found",Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: java.lang.Exception) {
            print(e.message)
        }
        return null
    }

    private fun moveCameraToLocation(){
        this.mLocationOverlay.disableFollowLocation()
        map.controller.animateTo(targetLocation)
    }

    private fun drawLine() {
        val path = Polyline()
        path.setColor(0xFF0000FF.toInt())
        path.setWidth(5.0f)
        path.setPoints(listOf(mLocationOverlay.myLocation, targetLocation))
        map.overlays.add(path)
        routeOverlay = path

        map.invalidate()
    }

    private fun drawRoute() {
        val roadManager: RoadManager = OSRMRoadManager(this, "Manager")
        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(mLocationOverlay.myLocation)
        waypoints.add(targetLocation)

        val road = roadManager.getRoad(waypoints)
        routeOverlay = RoadManager.buildRoadOverlay(road)

        routeOverlay.width = 15.0f

        map.overlays.add(routeOverlay)

        map.invalidate()
    }
}