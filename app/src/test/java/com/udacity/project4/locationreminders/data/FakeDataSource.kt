package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private val reminders = mutableListOf<ReminderDTO>()

    var makeErrorWhileGetReminders = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (makeErrorWhileGetReminders) {
            Result.Error("Error")
        } else Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        reminders.firstOrNull { it.id == id }.also {
            return when(it) {
                null -> Result.Error("Not Found")
                else -> Result.Success(it)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}