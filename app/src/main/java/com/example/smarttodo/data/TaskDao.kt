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
 * This interface defines all the database interactions (queries, inserts, updates, deletes)
 * related to tasks. Room will generate the necessary implementation for these methods.
 */
@Dao
interface TaskDao {

    /**
     * Retrieves a list of tasks, optionally filtered by completion status and a search query.
     * The results are ordered by priority (descending) and then by creation date (ascending).
     *
     * The search query matches against the task's title or description.
     * If `isCompleted` is null, tasks are not filtered by completion status.
     *
     * @param searchQuery The text to search for in task titles or descriptions. An empty string matches all.
     * @param isCompleted An optional Boolean to filter tasks by their completion status.
     *                    If true, only completed tasks are returned.
     *                    If false, only incomplete tasks are returned.
     *                    If null, tasks are not filtered by completion status.
     * @return A [LiveData] list of [Task] objects that match the criteria.
     *         LiveData allows observing data changes and updating the UI automatically.
     */
    @Query("SELECT * FROM tasks WHERE (:isCompleted IS NULL OR isCompleted = :isCompleted) AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') ORDER BY priority DESC, createdAt ASC")
    fun getTasks(searchQuery: String, isCompleted: Boolean?): LiveData<List<Task>>

    /**
     * Retrieves a single task by its unique ID.
     * This is a suspend function, meaning it should be called from a coroutine or another suspend function
     * to avoid blocking the main thread.
     *
     * @param taskId The ID of the task to retrieve.
     * @return The [Task] object if found, otherwise null.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    /**
     * Inserts a new task into the database.
     * If a task with the same primary key already exists, it will be replaced due to the
     * `OnConflictStrategy.REPLACE` strategy.
     * This is a suspend function.
     *
     * @param task The [Task] object to insert.
     * @return The row ID of the newly inserted task as a [Long].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /**
     * Updates an existing task in the database.
     * The task is identified by its primary key (`id`).
     * This is a suspend function.
     *
     * @param task The [Task] object to update.
     */
    @Update
    suspend fun update(task: Task)

    /**
     * Deletes a specific task from the database.
     * This is a suspend function.
     *
     * @param task The [Task] object to delete.
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Deletes all tasks that are marked as completed (where `isCompleted` is true).
     * This is a suspend function.
     */
    @Query("DELETE FROM tasks WHERE isCompleted = 1") // In SQLite, boolean true is often stored as 1
    suspend fun deleteCompletedTasks()

    /**
     * Deletes all tasks from the "tasks" table. Use with caution.
     * This is a suspend function.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    /**
     * Gets the total number of tasks currently in the database.
     * This is a suspend function.
     *
     * @return The total count of tasks as an [Int].
     */
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    /**
     * Gets the number of tasks that are marked as completed.
     * This is a suspend function.
     *
     * @return The count of completed tasks as an [Int].
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1") // In SQLite, boolean true is often stored as 1
    suspend fun getCompletedTaskCount(): Int
}
