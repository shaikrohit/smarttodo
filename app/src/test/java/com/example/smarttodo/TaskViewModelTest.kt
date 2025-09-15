package com.example.smarttodo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.smarttodo.data.Task
import com.example.smarttodo.data.TaskRepository
import com.example.smarttodo.ui.TaskFilter // Ensure correct import for TaskFilter
import com.example.smarttodo.ui.TaskViewModel
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
// No need for import java.util.* if not directly used for new Date() in tests.

/**
 * Unit tests for the [TaskViewModel].
 * This class verifies that the ViewModel interacts correctly with its [TaskRepository] dependency
 * and that its LiveData objects are updated as expected based on user actions and repository responses.
 *
 * It uses JUnit 4, Mockito-Kotlin for mocking, and kotlinx-coroutines-test for managing coroutines.
 */
@ExperimentalCoroutinesApi // Indicates use of experimental coroutine testing APIs.
class TaskViewModelTest {

    /**
     * JUnit rule that swaps the background executor used by Android Architecture Components
     * with a different one which executes each task synchronously.
     * This is crucial for testing LiveData, ensuring that changes are observed immediately.
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /**
     * A [TestDispatcher] that executes tasks immediately in the current thread.
     * Used here to control the execution of coroutines launched by the ViewModel.
     */
    private val testDispatcher = UnconfinedTestDispatcher()

    // Mock instance of TaskRepository to control its behavior during tests.
    private lateinit var taskRepository: TaskRepository
    // The instance of TaskViewModel being tested.
    private lateinit var taskViewModel: TaskViewModel

    /**
     * Sets up the test environment before each test case.
     * This involves:
     * 1. Setting the main coroutine dispatcher to [testDispatcher] for predictable coroutine execution.
     * 2. Creating a mock instance of [TaskRepository].
     * 3. Initializing the [TaskViewModel] with the mock repository.
     */
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher) // Replace the main dispatcher with the test dispatcher.
        taskRepository = mock() // Create a mock TaskRepository.
        taskViewModel = TaskViewModel(taskRepository) // Initialize ViewModel with the mock.
    }

    /**
     * Cleans up the test environment after each test case.
     * This involves resetting the main coroutine dispatcher to its original state.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain() // Reset the main dispatcher.
    }

    /**
     * Tests that calling [TaskViewModel.insert] correctly invokes [TaskRepository.insert]
     * with the same task.
     */
    @Test
    fun `insert task calls repository insert`() = runTest {
        val task = Task(title = "New Task") // Create a sample task.
        taskViewModel.insert(task) // Call the ViewModel method.
        // Verify that the repository's insert method was called once with the specified task.
        verify(taskRepository).insert(task)
    }

    /**
     * Tests that calling [TaskViewModel.update] correctly invokes [TaskRepository.update]
     * with the same task.
     */
    @Test
    fun `update task calls repository update`() = runTest {
        val task = Task(id = 1, title = "Updated Task")
        taskViewModel.update(task)
        verify(taskRepository).update(task)
    }

    /**
     * Tests that calling [TaskViewModel.delete] correctly invokes [TaskRepository.delete]
     * with the same task.
     */
    @Test
    fun `delete task calls repository delete`() = runTest {
        val task = Task(id = 1, title = "Delete Me")
        taskViewModel.delete(task)
        verify(taskRepository).delete(task)
    }

    /**
     * Tests that calling [TaskViewModel.toggleTaskCompletion] correctly invokes
     * [TaskRepository.toggleTaskCompletion] with the ID of the given task.
     */
    @Test
    fun `toggleTaskCompletion calls repository toggleTaskCompletion`() = runTest {
        val task = Task(id = 1, title = "Toggle Me")
        taskViewModel.toggleTaskCompletion(task)
        verify(taskRepository).toggleTaskCompletion(task.id)
    }

    /**
     * Tests that calling [TaskViewModel.deleteCompletedTasks] correctly invokes
     * [TaskRepository.deleteCompletedTasks].
     */
    @Test
    fun `deleteCompletedTasks calls repository deleteCompletedTasks`() = runTest {
        taskViewModel.deleteCompletedTasks()
        verify(taskRepository).deleteCompletedTasks()
    }

    /**
     * Tests that calling [TaskViewModel.deleteAllTasks] correctly invokes
     * [TaskRepository.deleteAllTasks].
     */
    @Test
    fun `deleteAllTasks calls repository deleteAllTasks`() = runTest {
        taskViewModel.deleteAllTasks()
        verify(taskRepository).deleteAllTasks()
    }

    /**
     * Tests that setting different filters via [TaskViewModel.setFilter]
     * correctly updates the `tasks` LiveData by fetching the appropriate data
     * from the [TaskRepository].
     */
    @Test
    fun `setFilter updates tasks LiveData`() = runTest {
        // Define sample task lists for different filter states.
        val allTasks = listOf(Task(id = 1, title = "All Task"))
        val completedTasks = listOf(Task(id = 2, title = "Completed Task", isCompleted = true))
        val incompleteTasks = listOf(Task(id = 3, title = "Incomplete Task")) // isCompleted defaults to false

        // Mock repository responses for different filter parameters.
        // The search query is empty ("") for these filter tests.
        whenever(taskRepository.getTasks("", null)).thenReturn(MutableLiveData(allTasks))
        whenever(taskRepository.getTasks("", true)).thenReturn(MutableLiveData(completedTasks))
        whenever(taskRepository.getTasks("", false)).thenReturn(MutableLiveData(incompleteTasks))

        // Create a mock observer to verify LiveData emissions.
        @Suppress("UNCHECKED_CAST") // Mockito type inference can sometimes require this for generic observers.
        val observer = mock<Observer<List<Task>>>()
        taskViewModel.tasks.observeForever(observer) // Start observing the tasks LiveData.

        // Test ALL filter.
        taskViewModel.setFilter(TaskFilter.ALL)
        // Verify that the observer received the `allTasks` list.
        verify(observer).onChanged(allTasks)

        // Test COMPLETED filter.
        taskViewModel.setFilter(TaskFilter.COMPLETED)
        // Verify that the observer received the `completedTasks` list.
        verify(observer).onChanged(completedTasks)

        // Test INCOMPLETE filter.
        taskViewModel.setFilter(TaskFilter.INCOMPLETE)
        // Verify that the observer received the `incompleteTasks` list.
        verify(observer).onChanged(incompleteTasks)

        // Clean up: remove the observer to prevent leaks and interference with other tests.
        taskViewModel.tasks.removeObserver(observer)
    }
}
