package com.example.mplab_navigation

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale


/**
 * MainActivity class represents the main activity of the application.
 * It includes map functionality, location tracking, route drawing, and other features.
 */
class MainActivity : Activity() {

    // MapView to display the map
    private lateinit var map: MapView

    // Overlay for the user's location
    private lateinit var mLocationOverlay: MyLocationNewOverlay

    // Target location for routing
    private lateinit var targetLocation: GeoPoint

    // Overlay for drawing the route
    private lateinit var routeOverlay: Polyline

    // ImageView for compass functionality to rotate the map
    private lateinit var compassImageView: ImageView

    /**
     * onCreate method is called when the activity is first created.
     * It initializes the map, location overlay, and other UI components.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load configuration settings for the map

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        // Allow network access on the main thread
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        setContentView(R.layout.activity_main)

        // Initialize the map
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        // Set initial map zoom level
        val mapController = map.controller
        mapController.setZoom(18)

        // Initialize location overlay for tracking user's location
        this.mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this.applicationContext), map)
        this.mLocationOverlay.enableMyLocation()
        map.overlays.add(this.mLocationOverlay)

        // Enable pinch-to-zoom gesture
        // Todo Der Nutzer soll mit den Fingern in die Karte hinein-/herauszoomen
        map.setMultiTouchControls(true)

        // Add a scale bar overlay to the map
        val scaleBarOverlay = ScaleBarOverlay(map)
        map.overlays.add(scaleBarOverlay)

        // Set an initial center point for the map
        val startPoint = GeoPoint(49.12, 9.21)
        map.controller.setCenter(startPoint)

        // Initialize ImageView for compass functionality
        // TODO rotate the map
        compassImageView = findViewById(R.id.compassImageView)

        // Set click listener on the compassImageView to rotate the map
        compassImageView.setOnClickListener {
            // Rotate the map by 90 degrees (clockwise)
            val currentBearing = map.mapOrientation
            map.setMapOrientation((currentBearing + 90) % 360)
        }


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

    /**
     * getGeoPointFromString method converts a location name to a GeoPoint using geocoding.
     * @param locationName The name of the location to be converted.
     * @return GeoPoint representing the location on the map.
     */
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
    /**
     * moveCameraToLocation method moves the map camera to the target location.
     */
    private fun moveCameraToLocation(){
        this.mLocationOverlay.disableFollowLocation()
        map.controller.animateTo(targetLocation)
    }

    /**
     * drawLine method draws a polyline on the map connecting the user's location to the target location.
     */
    private fun drawLine() {
        val path = Polyline()
        path.setColor(0xFF0000FF.toInt())
        path.setWidth(5.0f)
        path.setPoints(listOf(mLocationOverlay.myLocation, targetLocation))
        map.overlays.add(path)
        routeOverlay = path

        map.invalidate()
    }

    /**
     * drawRoute method draws a route on the map using the OSRM road manager.
     */
    private fun drawRoute() {
        val roadManager: RoadManager = OSRMRoadManager(this, "Manager")
        val waypoints = ArrayList<GeoPoint>()
        waypoints.add(mLocationOverlay.myLocation)
        waypoints.add(targetLocation)

        val road = roadManager.getRoad(waypoints)

        // TODO Remove all overlays from the map before drawing the new route
        map.overlays.clear()

        if (road != null) {
            routeOverlay = RoadManager.buildRoadOverlay(road)
            routeOverlay.width = 15.0f
            map.overlays.add(routeOverlay)


            //for (i in 0 until road.mNodes.size -1 ) {
            for (i in 0 until road.mNodes.size -1 ) {
                val currentNode = road.mNodes[i]
                val instructions = currentNode.mInstructions

                val marker = Marker(map) // makers that will represent the instructions of the nodes
                val title: String
                if  (instructions ==  null) { // The nodes instructions return null when the direction is straight, so i hard coded "Continue straight" to the route
                    title = "Step $i: \n \nContinue straigh"
                    marker.title = title
                    marker.image=  ContextCompat.getDrawable(this, R.drawable.righticon)
                }else{

                     title = instructions
                    marker.title = "Step $i: \n \n $title"
                    marker.image=  ContextCompat.getDrawable(this, R.drawable.righticon)
                }
                marker.position = currentNode.mLocation
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
            }

        } else {
            Log.e("checkThis", "Road is null")
        }


        map.overlays.add(routeOverlay)

        // Add the scale bar overlay after adding the route and markers
        val scaleBarOverlay = ScaleBarOverlay(map)
        map.overlays.add(scaleBarOverlay)
        map.invalidate()

    }


}

