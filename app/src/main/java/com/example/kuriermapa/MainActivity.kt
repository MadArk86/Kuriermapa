package com.example.kuriermapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import java.io.InputStream

data class Miejsce(
    val nazwa: String,
    val lat: Double,
    val lng: Double
)

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MiejscaAdapter
    private lateinit var btnNavigate: Button
    private lateinit var btnToggleList: Button

    private val listaMiejsc = mutableListOf<Miejsce>()
    private val markery = mutableListOf<Marker>()

    private val LOCATION_PERMISSION_REQUEST = 1000
    private var listaVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.listaMiejsc)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnNavigate = findViewById(R.id.btnNavigate)
        btnToggleList = findViewById(R.id.btnToggleList)

        wczytajMiejscaZJsona()

        adapter = MiejscaAdapter(
            listaMiejsc.toMutableList(),
            onCheckChanged = { miejsce, isChecked ->
                if (isChecked) {
                    if (!markery.any { it.title == miejsce.nazwa }) {
                        val marker = mMap.addMarker(
                            MarkerOptions().position(LatLng(miejsce.lat, miejsce.lng)).title(miejsce.nazwa)
                        )
                        if (marker != null) markery.add(marker)
                    }
                } else {
                    val marker = markery.find { it.title == miejsce.nazwa }
                    marker?.remove()
                    markery.remove(marker)
                }
            },
            onDeliveredClicked = { miejsce ->
                // Usuń marker z mapy
                val marker = markery.find { it.title == miejsce.nazwa }
                marker?.remove()
                markery.remove(marker)

                // Usuń punkt z listy adaptera
                adapter.removeItem(miejsce)

                // Opcjonalnie: pokaż Toast
                Toast.makeText(this, "Dostarczono przesyłkę: ${miejsce.nazwa}", Toast.LENGTH_SHORT).show()
            }
        )


        recyclerView.adapter = adapter

        // Przesuwanie elementów listy
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                adapter.swapItems(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Nawigacja
        btnNavigate.setOnClickListener {
            val selectedPlaces = adapter.getSelectedPlaces()
            if (selectedPlaces.isNotEmpty()) {
                startNavigationWithCurrentLocation(selectedPlaces)
            }
        }

        // Chowanie/pokazywanie listy
        btnToggleList.setOnClickListener {
            listaVisible = !listaVisible
            recyclerView.visibility = if (listaVisible) View.VISIBLE else View.GONE

            val mapParams = mapFragment.view?.layoutParams
            mapParams?.height = if (listaVisible) 300 else RecyclerView.LayoutParams.MATCH_PARENT
            mapFragment.view?.layoutParams = mapParams
            mapFragment.view?.requestLayout() // konieczne do przeliczenia układu
        }
    }

    private fun wczytajMiejscaZJsona() {
        try {
            val inputStream: InputStream = assets.open("miejsca.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val nazwa = obj.getString("nazwa")
                val lat = obj.getDouble("lat")
                val lng = obj.getDouble("lng")
                listaMiejsc.add(Miejsce(nazwa, lat, lng))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(52.2297, 21.0122), 12f))
    }

    private fun startNavigationWithCurrentLocation(points: List<Miejsce>) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val origin = LatLng(location.latitude, location.longitude)
                val destination = points.last()
                val waypoints = points.dropLast(1)
                val waypointsStr = waypoints.joinToString("|") { "${it.lat},${it.lng}" }

                val uri = if (waypoints.isEmpty()) {
                    "https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}&destination=${destination.lat},${destination.lng}"
                } else {
                    "https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}&destination=${destination.lat},${destination.lng}&waypoints=$waypointsStr"
                }

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            }
        }
    }
}
