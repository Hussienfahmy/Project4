package com.udacity.project4.locationreminders.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var dispatcher: TestDispatcher
    private lateinit var scope: TestScope
    
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var remindersDaoMock: RemindersDao

    private val reminderDTO = ReminderDTO(
        "Title",
        "Desc",
        "Loc",
        0.0,
        0.0
    )

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        scope = TestScope(dispatcher)

        remindersDaoMock = mock(RemindersDao::class.java)

        remindersLocalRepository = RemindersLocalRepository(
            remindersDaoMock,
            dispatcher
        )
    }

    @Test
    fun getReminders_Success() = scope.runTest {
        `when`(remindersDaoMock.getReminders()).thenReturn(listOf())

        val result = remindersLocalRepository.getReminders()

        assertThat(result, `is`(instanceOf(Result.Success::class.java)))
    }

    @Test
    fun getReminders_Error() = scope.runTest {
        `when`(remindersDaoMock.getReminders()).thenAnswer{ throw IOException() }

        val result = remindersLocalRepository.getReminders()

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    // ----------------------------------------//

    @Test
    fun getReminder_Success() = scope.runTest {
        `when`(remindersDaoMock.getReminderById(anyString())).thenReturn(reminderDTO)

        val result = remindersLocalRepository.getReminder(reminderDTO.id)

        assertThat(result, `is`(instanceOf(Result.Success::class.java)))
    }

    @Test
    fun getReminder_NotFound() = scope.runTest {
        `when`(remindersDaoMock.getReminderById(anyString())).thenReturn(null)

        val result = remindersLocalRepository.getReminder("")

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    @Test
    fun getReminder_Error() = scope.runTest {
        `when`(remindersDaoMock.getReminderById(anyString())).thenAnswer{ throw IOException() }

        val result = remindersLocalRepository.getReminder("")

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    // ------------------------------------------//

    @Test
    fun saveReminder() = scope.runTest {
        remindersLocalRepository.saveReminder(reminderDTO)

        verify(remindersDaoMock).saveReminder(reminderDTO)
    }

    @Test
    fun deleteReminders() = scope.runTest {
        remindersLocalRepository.deleteAllReminders()

        verify(remindersDaoMock).deleteAllReminders()
    }
}