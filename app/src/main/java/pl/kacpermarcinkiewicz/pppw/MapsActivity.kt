@file:Suppress("DEPRECATION")

package pl.kacpermarcinkiewicz.pppw

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationProvider
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.maps.LocationSource
//import com.google.android.gms.maps.model.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
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
import  com.mapbox.geojson.*
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.coroutineContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback , PermissionsListener, LocationSource.OnLocationChangedListener{
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private lateinit var mapboxMap: MapboxMap

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    private lateinit var pd: ProgressDialog


    private var PRIVATE_MODE = 0
    private val PREF_NAME = "GPSPermissionDenied"

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var startNaviButton: Button
    private lateinit var originLocation: Location
    private lateinit var originPosition: Point
    private lateinit var destinationPosition: Point
    private  lateinit var currentRoute: DirectionsRoute

    private var locationLayerPlugin: LocationLayerPlugin? = null
    private var navigationMapRoute: NavigationMapRoute? = null


    private val zste = LatLng(49.97307239745034, 19.836487258575385)
    private val tesco = LatLng(49.97331729856471, 19.830607184950175)
    private val lewiatan = LatLng(49.97468931541608, 19.83366504433204)

    private var goToLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        pd = ProgressDialog(this)

        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext,getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.mapView)
        startNaviButton = findViewById(R.id.navi_btn)
        mapView?.onCreate(savedInstanceState)

        // check is remember permission denied == true
        val sharedPref: SharedPreferences = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(sharedPref.getString(PREF_NAME, "") == "denied"){
                Toast.makeText(applicationContext, "Brak uprawnień do odczytania lokalizacji!", Toast.LENGTH_LONG).show()
                //return
            }
            //else ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST_CODE)

            //return
        }

        // go back button enable and set tittle to " "
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val actionBar = supportActionBar
        actionBar!!.title = ""

        mapView.getMapAsync(this)

        startNaviButton.setOnClickListener{
            // Check is currentRoute initialized if you don't want app crash ;)
            if (!this::currentRoute.isInitialized) Snackbar.make(ConstraintLayout, "Spróbuj ponownie za chwilę", Snackbar.LENGTH_LONG).show()
            else {
                val options = NavigationLauncherOptions.builder()
                    .directionsRoute(currentRoute)
                    .shouldSimulateRoute(false)
                    .build()
                NavigationLauncher.startNavigation(this, options)
            }
        }
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

    @SuppressLint("MissingPermission")
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

        //set gps button visibility
        if(PermissionsManager.areLocationPermissionsGranted(this) && locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            gps_btn.visibility = View.VISIBLE
        }
        gps_btn.setOnClickListener{
            if(PermissionsManager.areLocationPermissionsGranted(this)) {
                val locationComponent = mapboxMap.locationComponent
                locationComponent.cameraMode = CameraMode.TRACKING
            }
        }

        //when camera move check is GPS now on
        var isNowGPSOn = false
        mapboxMap.addOnCameraMoveListener {
            if(PermissionsManager.areLocationPermissionsGranted(this) && locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) && !isNowGPSOn){
                gps_btn.visibility = View.VISIBLE
                isNowGPSOn = true
            }
        }

        mapboxMap.addOnMapClickListener { point: LatLng ->
            if (navigationMapRoute != null){
                navigationMapRoute?.removeRoute()
            }

            startNaviButton.isEnabled = false
            startNaviButton.setBackgroundResource(R.color.mapboxGrayLight)

            return@addOnMapClickListener true
        }

        if(MainActivity.place == "1"){
            mapboxMap.addMarker(MarkerOptions().position(zste).setTitle("Szkoła"))
        }
        else if(MainActivity.place == "2"){
            mapboxMap.addMarker(MarkerOptions().position(tesco).setTitle("Tesco"))
        }
        else if(MainActivity.place == "3"){
            mapboxMap.addMarker(MarkerOptions().position(lewiatan).setTitle("Lewiatan"))
        }
        else if(MainActivity.place == "all"){
            mapboxMap.addMarker(MarkerOptions().position(zste).setTitle("Szkoła"))
            mapboxMap.addMarker(MarkerOptions().position(tesco).setTitle("Tesco"))
            mapboxMap.addMarker(MarkerOptions().position(lewiatan).setTitle("Lewiatan"))
        }
        else if(MainActivity.place == "near") {
            mapboxMap.addMarker(MarkerOptions().position(zste).setTitle("Szkoła"))
            mapboxMap.addMarker(MarkerOptions().position(tesco).setTitle("Tesco"))
            mapboxMap.addMarker(MarkerOptions().position(lewiatan).setTitle("Lewiatan"))

            goToLocation = true

            //load style before showing user location
            mapboxMap.setStyle(Style.DARK) {

                Timber.i("Załadowano style")
                // Map is set up and the style has loaded. Now you can add data or make other map adjustments
                enableLocationComponent(it)
            }
        }

        mapboxMap.setOnMarkerClickListener{marker: Marker ->
            var locationManager: LocationManager? = null
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
            if (locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) && PermissionsManager.areLocationPermissionsGranted(this)){
                val locationComponent = mapboxMap.locationComponent

                if (locationComponent.lastKnownLocation == null) Snackbar.make(ConstraintLayout, "Spróbuj ponownie za chwilę", Snackbar.LENGTH_LONG).show()
                else {
                    val markerPos: LatLng = marker.position
                    destinationPosition = Point.fromLngLat(markerPos.longitude, markerPos.latitude)
                    originPosition = Point.fromLngLat(
                        locationComponent.lastKnownLocation!!.longitude,
                        locationComponent.lastKnownLocation!!.latitude
                    )

                    pd.setTitle("Ładowanie")
                    pd.setMessage("Trwa szukanie najlepszej trasy...")
                    pd.setInverseBackgroundForced(true)
                    pd.show()

                    getRoute(originPosition, destinationPosition)

                    startNaviButton.isEnabled = true
                    startNaviButton.setBackgroundResource(R.color.mapboxBlue)
                }

            }
            else{
                val builder = AlertDialog.Builder(this@MapsActivity)
                builder.setTitle("Wyznaczanie trasy do punktu.")
                builder.setMessage("Aby wyznaczyć trasę do punktu, wymagane jest włączenie modułu GPS oraz przyznanie aplikacji uprawnień do lokalizacji!")
                builder.setPositiveButton("OK"){dialog, which ->
                    //
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

            return@setOnMarkerClickListener true
        }
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            originLocation = location
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
            val lastLocation = locationComponent?.lastKnownLocation
            if (lastLocation != null) {
                originLocation = lastLocation
                originPosition = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
            }

            // Activate the component
            locationComponent.activateLocationComponent(this, loadedMapStyle)

            // Apply the options to the LocationComponent
            locationComponent.applyStyle(options)

            // Enable to make component visible
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode

            if(goToLocation) locationComponent.cameraMode = CameraMode.TRACKING // go to user location only when enabled
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

    private fun getRoute(origin: Point, destination: Point){
        this.map = mapboxMap
        NavigationRoute.builder(applicationContext)
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object: Callback<DirectionsResponse>{
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    pd.dismiss()

                    val routeResponse = response?: return
                    val body = routeResponse.body() ?: return
                    if (body.routes().count() == 0){
                        Log.e("Map", "No route found")
                        Toast.makeText(applicationContext, R.string.noRouteFound, Toast.LENGTH_LONG).show()
                        return
                    }

                    if (navigationMapRoute != null){
                        navigationMapRoute?.removeRoute()
                    }
                    else{
                        navigationMapRoute = NavigationMapRoute(null, mapView, map)
                    }
                    navigationMapRoute?.addRoute(body.routes().first())

                    currentRoute = response.body()!!.routes().first()
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    pd.dismiss()
                    Log.e("Map", "Error: ${t?.message}")
                    Toast.makeText(applicationContext, "Error: ${t?.message}", Toast.LENGTH_LONG).show()

                    startNaviButton.isEnabled = false
                    startNaviButton.setBackgroundResource(R.color.mapboxGrayLight)
                }
            })
    }

    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
