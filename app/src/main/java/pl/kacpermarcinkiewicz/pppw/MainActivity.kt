package pl.kacpermarcinkiewicz.pppw

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), PermissionsListener {

    companion object {
        var place = "1"
    }

    private var PRIVATE_MODE = 0
    private val PREF_NAME = "GPSPermissionDenied"

    private var permissionsManager: PermissionsManager = PermissionsManager(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar

        actionBar!!.title = ""

        checkPerm()

        place_1.setOnClickListener{
            place = "1"
            runMap()
        }
        place_2.setOnClickListener{
            place = "2"
            runMap()
        }
        place_3.setOnClickListener{
            place = "3"
            runMap()
        }
        map_btn.setOnClickListener {
            place = "all"
            runMap()
        }
        near_btn.setOnClickListener {
            place = "near"
            runMap()
        }

    }

    fun checkPerm() {


        if (ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
            if (sharedPref.getString(PREF_NAME, "") == "denied"){
                return
            }


            //Popup
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Uprawnienia do odczytu lokalizacji")
            builder.setMessage("Do wykorzystania wszystkich możliwości aplikacji wymagane jest przyznanie uprawnień do lokalizacji.\nCzy chcesz teraz przyznać uprawnienia?")

            builder.setPositiveButton("TAK"){dialog, which ->
                //ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),MapsActivity.LOCATION_PERMISSION_REQUEST_CODE)
                permissionsManager = PermissionsManager(this)
                permissionsManager.requestLocationPermissions(this)
            }
            builder.setNegativeButton("NIE"){dialog, which ->
                return@setNegativeButton
            }
            builder.setNeutralButton("Nie, zapamiętaj mój wybór"){dialog, which ->
                areUSure()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()

            return
        }
    }

    fun areUSure(){
        val sharedPref = getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("UWAGA!")
        builder.setMessage("Uwaga! Aby cofnąć tę opcję wymagane będzie ponowne zainstalowanie aplikacji!\n\nKontynuować? ")
        builder.setPositiveButton("TAK"){dialog, which ->
            val editor = sharedPref.edit()
            editor.putString(PREF_NAME, "denied")
            editor.apply()
            if(sharedPref.getString(PREF_NAME, "") == "denied") {
                Toast.makeText(applicationContext, "Zapamiętano", Toast.LENGTH_LONG).show()
            }
        }
        builder.setNegativeButton("Anuluj"){dialog, which ->
            Toast.makeText(applicationContext, "Anulowano", Toast.LENGTH_LONG).show()
            return@setNegativeButton
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun runMap(){
        var aktywnoscMapy = Intent(applicationContext, MapsActivity::class.java)
        startActivity(aktywnoscMapy)

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_locations, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.goto_place1){
            place = "1"
            runMap()
        }
        if (item?.itemId == R.id.goto_place2){
            place = "2"
            runMap()
        }
        if (item?.itemId == R.id.goto_place3){
            place = "3"
            runMap()
        }
        if (item?.itemId == R.id.goto_placeAll){
            place = "all"
            runMap()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Toast.makeText(this, "Pomyślnie przyznano uprawnienia", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Nie przyznano uprawnień!", Toast.LENGTH_LONG).show()
        }
    }
}
