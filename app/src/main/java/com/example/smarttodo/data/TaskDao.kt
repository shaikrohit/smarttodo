package com.example.smarttodo.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for the [Task] entity.
 * Provides methods to interact with the 'tasks' table in the database.
 * All methods that modify the database are `suspend` functions to ensure they are called
 * from a coroutine, and Room handles running them on a background thread.
 * Methods returning [LiveData] are observed by the UI and automatically update on data changes.
 */
@Dao
interface TaskDao {

    /**
     * Selects all tasks from the tasks table, filtered by completion status and a search query,
     * ordered by priority (descending) and then by creation date (ascending).
     *
     * @param searchQuery The string to search for in task titles or descriptions.
     * @param isCompleted Null to fetch all tasks, true for only completed tasks, false for only incomplete tasks.
     * @return A [LiveData] list of [Task] objects matching the criteria.
     */
    @Query("SELECT * FROM tasks WHERE (:isCompleted IS NULL OR isCompleted = :isCompleted) AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') ORDER BY priority DESC, createdAt ASC")
    fun getTasks(searchQuery: String, isCompleted: Boolean?): LiveData<List<Task>>

    /**
     * Selects a single task by its ID.
     *
     * @param taskId The ID of the task to retrieve.
     * @return A [Task] object if found, otherwise null. This is a suspend function.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    /**
     * Selects a single task by its ID. This is a synchronous variant.
     * Note: While the DAO method is synchronous, the [TaskRepository] should ensure it's called on a background thread.
     *
     * @param taskId The ID of the task to retrieve.
     * @return A [Task] object if found, otherwise null.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdNonLiveData(taskId: Int): Task? // Called from Repository within withContext(Dispatchers.IO)

    /**
     * Inserts a task into the table. If the task already exists, it replaces it.
     *
     * @param task The task to be inserted.
     * @return The new row ID for the inserted item. This is a suspend function.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /**
     * Updates an existing task in the table.
     *
     * @param task The task to be updated.
     * This is a suspend function.
     */
    @Update
    suspend fun update(task: Task)

    /**
     * Deletes a task from the table.
     *
     * @param task The task to be deleted.
     * This is a suspend function.
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Deletes all tasks that are marked as completed from the table.
     * @return The number of rows deleted. This is a suspend function.
     */
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks(): Int

    /**
     * Selects all completed tasks. This is a synchronous variant.
     * Note: While the DAO method is synchronous, the [TaskRepository] should ensure it's called on a background thread.
     *
     * @return A list of completed [Task] objects.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasksNonLiveData(): List<Task> // Called from Repository within withContext(Dispatchers.IO)

    /**
     * Deletes all tasks from the table.
     * @return The number of rows deleted. This is a suspend function.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks(): Int

    /**
     * Selects all tasks. This is a synchronous variant.
     * Note: While the DAO method is synchronous, the [TaskRepository] should ensure it's called on a background thread.
     *
     * @return A list of all [Task] objects.
     */
    @Query("SELECT * FROM tasks")
    fun getAllTasksNonLiveData(): List<Task> // Called from Repository within withContext(Dispatchers.IO)

    /**
     * Gets the total count of tasks in the table.
     * @return The total number of tasks. This is a suspend function.
     */
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    /**
     * Gets the count of tasks that are marked as completed.
     * @return The number of completed tasks. This is a suspend function.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
}
