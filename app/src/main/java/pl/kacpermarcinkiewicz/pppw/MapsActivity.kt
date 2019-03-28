package pl.kacpermarcinkiewicz.pppw

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.model.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import kotlinx.android.synthetic.main.activity_maps.*
import timber.log.Timber


class MapsActivity : AppCompatActivity(), OnMapReadyCallback , PermissionsListener{


    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "GPSPermissionDenied"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap

    private var locationEngine: LocationEngine? = null
    private var locationLayerPlugin: LocationLayerPlugin? = null

    val zste = LatLng(49.97307239745034, 19.836487258575385)
    val tesco = LatLng(49.97331729856471, 19.830607184950175)
    val lewiatan = LatLng(49.97468931541608, 19.83366504433204)

    val zoomLevel = 15.0f //This goes up to 21

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext,getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)


        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(sharedPref.getString(PREF_NAME, "") == "denied"){
                Toast.makeText(applicationContext, "Brak uprawnień do odczytania lokalizacji!", Toast.LENGTH_LONG).show()
                //return
            }
            //else ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST_CODE)

            //return
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val actionBar = supportActionBar

        actionBar!!.title = ""

        mapView.getMapAsync(this)
    }


    override fun onStart() {
        super.onStart()
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            locationLayerPlugin?.onStart()
        }
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        locationLayerPlugin?.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (outState != null){
            mapView.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_locations, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when {
            item?.itemId == R.id.goto_place1 -> {
                MainActivity.place = "1"
                runMap()
            }
            item?.itemId == R.id.goto_place2 -> {
                MainActivity.place = "2"
                runMap()
            }
            item?.itemId == R.id.goto_place3 -> {
                MainActivity.place = "3"
                runMap()
            }
            item?.itemId == R.id.goto_placeAll -> {
                MainActivity.place = "all"
                runMap()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun runMap(){
        val aktywnoscMapy = Intent(applicationContext, MapsActivity::class.java)
        startActivity(aktywnoscMapy)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.DARK) {

            Timber.i("Załadowano style")
            // Map is set up and the style has loaded. Now you can add data or make other map adjustments
            enableLocationComponent(it)
        }

        //check is GPS on
        var locationManager: LocationManager? = null
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?

        if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) && PermissionsManager.areLocationPermissionsGranted(this)){
            val builder = AlertDialog.Builder(this@MapsActivity)
            builder.setTitle("Moduł GPS")
            builder.setMessage("Aby mapa mogła wskazać twoją pozycję, musisz włączyć moduł GPS.")
            builder.setPositiveButton("OK"){dialog, which ->
                //
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
        else if (!PermissionsManager.areLocationPermissionsGranted(this)){
            Toast.makeText(this, "Aplikacja nie posiada uprawnień do pokazania twojej lokalizacji.", Toast.LENGTH_LONG).show()
        }

        if(PermissionsManager.areLocationPermissionsGranted(this)) gps_btn.visibility = View.VISIBLE
        gps_btn.setOnClickListener{
            if(PermissionsManager.areLocationPermissionsGranted(this)) {
                val locationComponent = mapboxMap.locationComponent
                locationComponent.cameraMode = CameraMode.TRACKING
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val options = LocationComponentOptions.builder(this)
                .trackingGesturesManagement(true)
                .accuracyColor(ContextCompat.getColor(this, R.color.mapboxBlue))
                .build()

            // Get an instance of the component
            val locationComponent = mapboxMap.locationComponent

            // Activate the component
            locationComponent.activateLocationComponent(this, loadedMapStyle)

            // Apply the options to the LocationComponent
            locationComponent.applyStyle(options)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS


        } else {
            //permissionsManager = PermissionsManager(this)
            //permissionsManager.requestLocationPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //Toast.makeText(this, "Aplikacja wymaga podniesienia uprawnień do funkcjonowania", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapboxMap.style!!)
        } else {
            Toast.makeText(this, "Nie przyznano uprawnień!", Toast.LENGTH_LONG).show()
        }
    }

    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
