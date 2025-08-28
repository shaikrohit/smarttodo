package com.example.smarttodo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.ui.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

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

        // Mock the repository's LiveData properties
        whenever(taskRepository.allTasks).thenReturn(MutableLiveData())
        whenever(taskRepository.incompleteTasks).thenReturn(MutableLiveData())
        whenever(taskRepository.completedTasks).thenReturn(MutableLiveData())

        taskViewModel = TaskViewModel(taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `insert task calls repository insert`() = runTest {
        val task = Task(title = "New Task")
        taskViewModel.insert(task)
        verify(taskRepository).insert(task)
    }

    @Test
    fun `update task calls repository update`() = runTest {
        val task = Task(id = 1, title = "Updated Task")
        taskViewModel.update(task)
        verify(taskRepository).update(task)
    }

    @Test
    fun `delete task calls repository delete`() = runTest {
        val task = Task(id = 1, title = "Delete Me")
        taskViewModel.delete(task)
        verify(taskRepository).delete(task)
    }

    @Test
    fun `toggleTaskCompletion calls repository toggleTaskCompletion`() = runTest {
        val task = Task(id = 1, title = "Toggle Me")
        taskViewModel.toggleTaskCompletion(task)
        verify(taskRepository).toggleTaskCompletion(task.id)
    }

    @Test
    fun `deleteCompletedTasks calls repository deleteCompletedTasks`() = runTest {
        taskViewModel.deleteCompletedTasks()
        verify(taskRepository).deleteCompletedTasks()
    }

    @Test
    fun `deleteAllTasks calls repository deleteAllTasks`() = runTest {
        taskViewModel.deleteAllTasks()
        verify(taskRepository).deleteAllTasks()
    }

    @Test
    fun `setFilter updates currentFilter LiveData`() {
        taskViewModel.setFilter(com.example.smarttodo.ui.TaskFilter.COMPLETED)
        assertEquals(com.example.smarttodo.ui.TaskFilter.COMPLETED, taskViewModel.currentFilter.value)
    }
}