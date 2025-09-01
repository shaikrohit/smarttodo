package com.example.smarttodo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.ui.TaskFilter
import com.example.smarttodo.ui.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@ExperimentalCoroutinesApi
class TaskViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var taskRepository: TaskRepository
    private lateinit var taskViewModel: TaskViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = mock()

        // Mock the getTasks method
        whenever(taskRepository.getTasks(any(), any())).thenReturn(MutableLiveData())

        taskViewModel = TaskViewModel(taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `insert task calls repository insert and updates isLoading`() = runTest {
        val task = Task(title = "New Task", description = "", priority = 1, isCompleted = false, createdAt = Date())
        taskViewModel.insert(task)

        assertTrue(taskViewModel.isLoading.value == false) // Should be false after completion
        verify(taskRepository).insert(task)
    }

    @Test
    fun `update task calls repository update`() = runTest {
        val task = Task(id = 1, title = "Updated Task", description = "", priority = 1, isCompleted = false, createdAt = Date())
        taskViewModel.update(task)
        verify(taskRepository).update(task)
    }

    @Test
    fun `delete task calls repository delete`() = runTest {
        val task = Task(id = 1, title = "Delete Me", description = "", priority = 1, isCompleted = false, createdAt = Date())
        taskViewModel.delete(task)
        verify(taskRepository).delete(task)
    }

    @Test
    fun `toggleTaskCompletion calls repository toggleTaskCompletion`() = runTest {
        val task = Task(id = 1, title = "Toggle Me", description = "", priority = 1, isCompleted = false, createdAt = Date())
        taskViewModel.toggleTaskCompletion(task)
        verify(taskRepository).toggleTaskCompletion(task.id)
    }

    @Test
    fun `deleteCompletedTasks calls repository deleteCompletedTasks`() = runTest {
        taskViewModel.deleteCompletedTasks()
        verify(taskRepository).deleteCompletedTasks()
    }

    @Test
    fun `deleteAllTasks calls repository deleteAllTasks and updates isLoading`() = runTest {
        taskViewModel.deleteAllTasks()
        assertFalse(taskViewModel.isLoading.value ?: false)
        verify(taskRepository).deleteAllTasks()
    }

    @Test
    fun `setFilter updates tasks LiveData`() = runTest {
        val tasks = listOf(
            Task(id = 1, title = "Completed Task", description = "", priority = 1, isCompleted = true, createdAt = Date()),
            Task(id = 2, title = "Incomplete Task", description = "", priority = 1, isCompleted = false, createdAt = Date())
        )
        val liveData = MutableLiveData(tasks.filter { it.isCompleted })
        whenever(taskRepository.getTasks("", true)).thenReturn(liveData)

        taskViewModel.setFilter(TaskFilter.COMPLETED)

        // Observe the tasks to trigger the switchMap
        taskViewModel.tasks.observeForever { }

        val filteredTasks = taskViewModel.tasks.value
        assertEquals(1, filteredTasks?.size)
        assertEquals("Completed Task", filteredTasks?.get(0)?.title)
    }

    @Test
    fun `setSearchQuery updates tasks LiveData`() = runTest {
        val tasks = listOf(
            Task(id = 1, title = "Search Me", description = "", priority = 1, isCompleted = false, createdAt = Date()),
            Task(id = 2, title = "Ignore Me", description = "", priority = 1, isCompleted = false, createdAt = Date())
        )
        val liveData = MutableLiveData(tasks.filter { it.title.contains("Search") })
        whenever(taskRepository.getTasks("Search", null)).thenReturn(liveData)

        taskViewModel.setSearchQuery("Search")

        // Observe the tasks to trigger the switchMap
        taskViewModel.tasks.observeForever { }

        val filteredTasks = taskViewModel.tasks.value
        assertEquals(1, filteredTasks?.size)
        assertEquals("Search Me", filteredTasks?.get(0)?.title)
    }
}