package pl.kacpermarcinkiewicz.pppw

import android.location.LocationManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_scrolling.*



class ScrollingActivity : AppCompatActivity() {
    companion object {
        var ScrollMarker = "null"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(ScrollMarker == "Tesco") {
            setContentView(R.layout.activity_scrolling)

        }
        else if (ScrollMarker == "Szkoła"){
            setContentView(R.layout.activity_scrolling2)
        }
        else if (ScrollMarker == "Lewiatan"){
            setContentView(R.layout.activity_scrolling3)
        }
        //Ustawia tytul Activity na ScrollActivity (do zmiany)
        //setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            //check is GPS on
            var locationManager: LocationManager? = null
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager?
            if (!locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) && PermissionsManager.areLocationPermissionsGranted(this)) {
                //set snack bar max lines to 5 because without this text is cut and after that show snack bar
                val sb = Snackbar.make(view, "Aby uruchomić nawigację, należy uruchomić moduł GPS, wrócić do poprzedniego okna, a następnie ponownie zaznaczyć marker!", Snackbar.LENGTH_INDEFINITE)
                val sbView = sb.view
                val textView = sbView.findViewById<View>(android.support.design.R.id.snackbar_text) as TextView
                textView.maxLines = 5
                sb.show()
            }
            // Check is currentRoute initialized if you don't want app crash ;)
            else if (!MapsActivity.isRouteInitialized) Snackbar.make(view, "Trasa nie została załadowana!", Snackbar.LENGTH_LONG).show()
            else {
                val options = NavigationLauncherOptions.builder()
                    .directionsRoute(MapsActivity.currentRoute)
                    .shouldSimulateRoute(false)
                    .build()
                NavigationLauncher.startNavigation(this, options)
                Toast.makeText(this, "Uruchamiam nawigację...", Toast.LENGTH_LONG).show()
            }
        }
    }
    override fun finish(){
        super.finish()
        overridePendingTransition(R.anim.abc_slide_in_top, R.anim.abc_slide_out_top)
    }
}
