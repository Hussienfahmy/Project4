package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.Constants
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceTransitionsService
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var geofenceCLint: GeofencingClient

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
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val location = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value

                val reminderDataItem =
                    ReminderDataItem(title, description, location, latitude, longitude)

                _viewModel.validateAndSaveReminder(reminderDataItem) {
                    // add the geofence after validate the reminder to ensure there is no null values
                    addGeoFence(reminderDataItem)
                }
            }
        }

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence(reminderDataItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(reminderDataItem.latitude!!, reminderDataItem.longitude!!, Constants.REMINDER_LOCATION_CIRCLE_RADIUS)
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
