package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecuteRule = InstantTaskExecutorRule()

    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource

    private val validReminder = ReminderDataItem(
        "Title", "Desc", "Loc", 0.0, 0.0
    )

    private val invalidReminder = ReminderDataItem(
        "", "Desc", null, 0.0, 0.0
    )

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Application>()
        dataSource = FakeDataSource()

        saveReminderViewModel = SaveReminderViewModel(
            context,
            dataSource
        )
    }

    @Test
    fun onClear() {
        saveReminderViewModel.onClear()

        with(saveReminderViewModel) {
            assertThat(reminderTitle.getOrAwaitValue(), equalTo(null))
            assertThat(reminderDescription.getOrAwaitValue(), equalTo(null))
            assertThat(reminderSelectedLocationStr.getOrAwaitValue(), equalTo(null))
            assertThat(latitude.getOrAwaitValue(), equalTo(null))
            assertThat(longitude.getOrAwaitValue(), equalTo(null))
        }
    }

    @Test
    fun validateAndSaveReminder_Valid() = runTest {
        saveReminderViewModel.validateAndSaveReminder(validReminder)

        assertThat(dataSource.getReminder(validReminder.id), `is`(instanceOf(Result.Success::class.java)))
        assertThat(saveReminderViewModel.navigationCommand.getOrAwaitValue(), equalTo(NavigationCommand.Back))
    }

    @Test
    fun validateAndSaveReminder_InValid() = runTest {
        saveReminderViewModel.validateAndSaveReminder(invalidReminder)

        assertThat(dataSource.getReminder(invalidReminder.id), `is`(instanceOf(Result.Error::class.java)))
    }

    @Test
    fun shouldReturnError() {
        saveReminderViewModel.validateAndSaveReminder(invalidReminder)

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), not(equalTo(null)))
    }

    @Test
    fun checkLoading() {
        saveReminderViewModel.validateAndSaveReminder(validReminder)

        // after loading the reminders the loading indicator should be false
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), equalTo(false))
    }
}