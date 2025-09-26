package com.example.smarttodo

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.ui.TaskViewModel
import com.example.smarttodo.util.AlarmScheduler
import com.example.smarttodo.util.OperationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for the [TaskViewModel].
 */
@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Mocks
    private lateinit var application: Application
    private lateinit var taskRepository: TaskRepository
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var alarmScheduler: MockedStatic<AlarmScheduler>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        taskRepository = mock()
        // Mock static calls to AlarmScheduler as it depends on Android framework
        alarmScheduler = mockStatic(AlarmScheduler::class.java)

        taskViewModel = TaskViewModel(application, taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        // Close the static mock
        alarmScheduler.close()
    }

    @Test
    fun `insert task calls repository insert`() = runTest {
        val task = Task(title = "New Task", hasReminder = false) // Ensure reminder is not set so scheduler is not called
        whenever(taskRepository.insert(task)).thenReturn(OperationResult.Success(1L))
        whenever(taskRepository.getTaskByIdNonLiveData(1)).thenReturn(task.copy(id = 1))

        taskViewModel.insert(task)

        verify(taskRepository).insert(task)
    }

    @Test
    fun `update task calls repository update`() = runTest {
        val task = Task(id = 1, title = "Updated Task")
        whenever(taskRepository.update(task)).thenReturn(OperationResult.Success(Unit))

        taskViewModel.update(task)

        verify(taskRepository).update(task)
    }

    @Test
    fun `delete task calls repository delete`() = runTest {
        val task = Task(id = 1, title = "Delete Me")
        whenever(taskRepository.delete(task)).thenReturn(OperationResult.Success(Unit))

        taskViewModel.delete(task)

        verify(taskRepository).delete(task)
    }

    @Test
    fun `toggleTaskCompletion calls repository update`() = runTest {
        val task = Task(id = 1, title = "Toggle Me", isCompleted = false)
        val taskCaptor = ArgumentCaptor.forClass(Task::class.java)
        whenever(taskRepository.update(taskCaptor.capture())).thenReturn(OperationResult.Success(Unit))

        taskViewModel.toggleTaskCompletion(task)

        verify(taskRepository).update(any())
        assert(taskCaptor.value.isCompleted)
    }

    @Test
    fun `deleteCompletedTasks calls repository deleteCompletedTasks`() = runTest {
        whenever(taskRepository.getCompletedTaskIds()).thenReturn(listOf(1, 2))
        whenever(taskRepository.deleteCompletedTasks()).thenReturn(OperationResult.Success(2))

        taskViewModel.deleteCompletedTasks()

        verify(taskRepository).deleteCompletedTasks()
    }

    @Test
    fun `deleteAllTasks calls repository deleteAllTasks`() = runTest {
        whenever(taskRepository.getAllTaskIds()).thenReturn(listOf(1, 2, 3))
        whenever(taskRepository.deleteAllTasks()).thenReturn(OperationResult.Success(3))

        taskViewModel.deleteAllTasks()

        verify(taskRepository).deleteAllTasks()
    }
}
