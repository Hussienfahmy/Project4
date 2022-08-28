package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecuteRule = InstantTaskExecutorRule()

    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource

    @Before
    fun setUp() {
        stopKoin()
        val context = ApplicationProvider.getApplicationContext<Application>()
        dataSource = FakeDataSource()

        remindersListViewModel = RemindersListViewModel(
            context,
            dataSource
        )
    }

    @Test
    fun loadReminders_Empty() = runTest {
        dataSource.deleteAllReminders()
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.remindersList.value, equalTo(emptyList()))
    }


    @Test
    fun loadReminders_NotNull() = runTest {
        dataSource.saveReminder(ReminderDTO("Title", "Desc", "loc", null, null))
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.remindersList.getOrAwaitValue(), not(equalTo(null)))
    }

    @Test
    fun shouldReturnError() {
        dataSource.makeErrorWhileGetReminders = true

        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), not(isEmptyOrNullString()))
    }

    @Test
    fun checkLoading() {
        remindersListViewModel.loadReminders()

        // after loading the reminders the loading indicator should be false
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), equalTo(false))
    }
}