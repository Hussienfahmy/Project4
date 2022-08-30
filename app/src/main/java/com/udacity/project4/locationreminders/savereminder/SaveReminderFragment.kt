package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.Constants
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedStateViewModel()
    private lateinit var geofenceCLint: GeofencingClient
    private val TAG = "SaveReminderFragment"

    private val resolutionLauncher = registerForActivityResult(StartIntentSenderForResult()) {
        checkDeviceLocationSettingsAndStartGeofence()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofenceCLint = LocationServices.getGeofencingClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentSaveReminderBinding =
            FragmentSaveReminderBinding.inflate(inflater, container, false)

        with(binding) {
            viewModel = _viewModel
            lifecycleOwner = viewLifecycleOwner

            selectLocation.setOnClickListener {
                // Navigate to another fragment to get the user location
                _viewModel.navigationCommand.value =
                    NavigationCommand.To(
                        SaveReminderFragmentDirections.toSetLocation()
                    )
            }

            saveReminder.setOnClickListener {
                if (_viewModel.validateEnteredData()) {
                    if (!isFineLocationGranted()) {
                        _viewModel.showSnackBarInt.value = R.string.location_required_error
                        return@setOnClickListener
                    }

                    if (!isBackGroundLocationIsGranted()) {
                        _viewModel.showSnackBarInt.value = R.string.location_required_error
                        return@setOnClickListener
                    }

                    // add the geofence after validate the reminder to ensure there is no null values
                    checkDeviceLocationSettingsAndStartGeofence()
                }
            }
        }

        return binding.root
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    resolutionLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireView().rootView,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                _viewModel.saveReminder()
                // location is granted and location in ON
                addGeoFence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence() {
        val reminderDataItem = _viewModel.reminderDataItem

        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                Constants.REMINDER_LOCATION_CIRCLE_RADIUS
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(3000)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofenceCLint.addGeofences(
            geofenceRequest,
            broadcastGeofencePendingIntent
        ).addOnCompleteListener {
            Log.d("SaveReminder", "BroadCasted GeofenceAdded Successful: ${it.isSuccessful}")
        }

        geofenceCLint.addGeofences(
            geofenceRequest,
            serviceGeofencePendingIntent
        ).addOnCompleteListener {
            Log.d("SaveReminder", "Service GeofenceAdded Successful: ${it.isSuccessful}")
        }
    }

    private val broadcastGeofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val serviceGeofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceTransitionsService::class.java)
        intent.action = GeofenceTransitionsService.ACTION_GEOFENCE_EVENT
        PendingIntent.getService(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's NOT a single view model.
        _viewModel.onClear()
    }
}
