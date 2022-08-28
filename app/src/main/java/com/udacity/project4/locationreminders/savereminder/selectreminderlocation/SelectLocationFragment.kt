package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Criteria
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.Constants
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.isPermissionGranted
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = "SelectLocationFragment"
    private lateinit var map: GoogleMap
    private var activeMark: Marker? = null
    private var activeCircle: Circle? = null
    private val requestLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onPermissionResult(it)
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding =
            FragmentSelectLocationBinding.inflate(inflater, container, false)

        with(binding) {
            viewModel = _viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).run {
            this.getMapAsync(this@SelectLocationFragment)
        }

        binding.confirmBtn.setOnClickListener {
            if (activeMark == null) {
                _viewModel.showToast.value = getString(R.string.select_location)
            } else {
                setLocationData()
            }
        }

        return binding.root
    }

    private fun setLocationData() {
        _viewModel.run {
            reminderSelectedLocationStr.value = activeMark?.snippet
            latitude.value =  activeMark?.position?.latitude
            longitude .value = activeMark?.position?.longitude

            navigationCommand.value = NavigationCommand.Back
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.map_options, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.normal_map -> {
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.hybrid_map -> {
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            R.id.satellite_map -> {
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            R.id.terrain_map -> {
                map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            else -> false
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setOnMapLongClickListener(googleMap)
        setMapStyle(googleMap)
        setPOIClick(googleMap)

        enableMyLocationBtn(googleMap)
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationBtn(googleMap: GoogleMap) {
        val granted = requireContext().isPermissionGranted(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (granted) {
            googleMap.isMyLocationEnabled = true
            zoomToUserLocation()
        } else {
            requestLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun zoomToUserLocation() {
        val locationManager = requireContext().getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        locationManager.requestLocationUpdates(
            locationManager.getBestProvider(Criteria(), true) ?: LocationManager.GPS_PROVIDER,
            2000L,
            20f
        ) { location ->
            Log.d(TAG, "Location Update Provided -> Lat: ${location.latitude}, Long: ${location.longitude}")

            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder()
                        .target(LatLng(location.latitude, location.longitude))
                        .zoom(16f)
                        .build()
                )
            )
        }
    }

    private fun onPermissionResult(granted: Boolean?) {
        if (granted == true) enableMyLocationBtn(map)
    }


    private fun drawRadiusAroundTheMark(googleMap: GoogleMap) {
        activeMark?.let {
            activeCircle?.remove()
            activeCircle = googleMap.addCircle(
                CircleOptions()
                    .center(it.position)
                    .radius(Constants.REMINDER_LOCATION_CIRCLE_RADIUS.toDouble())
                    .strokeColor(Color.MAGENTA)
            )
        }
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        googleMap.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )
    }

    private fun setOnMapLongClickListener(googleMap: GoogleMap) {
        googleMap.setOnMapLongClickListener { latLang ->
            val snippets = String.format(
                "Lat: %1.2f, Long: %2.2f",
                latLang.latitude, latLang.longitude
            )

            activeMark?.remove()
            activeMark = googleMap.addMarker(
                MarkerOptions().position(latLang).snippet(snippets)
            )
            drawRadiusAroundTheMark(googleMap)
        }
    }

    private fun setPOIClick(googleMap: GoogleMap) {
        googleMap.setOnPoiClickListener { poi ->
            activeMark?.remove()
            activeMark = googleMap.addMarker(
                MarkerOptions().position(poi.latLng).title(poi.name)
            )
            activeMark?.showInfoWindow()
            drawRadiusAroundTheMark(googleMap)
        }
    }
}
