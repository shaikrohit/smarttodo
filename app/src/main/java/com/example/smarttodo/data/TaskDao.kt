package com.example.smarttodo.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {

    @Suppress("unused")
    @Query("SELECT * FROM tasks WHERE (:isCompleted IS NULL OR isCompleted = :isCompleted) ORDER BY priority DESC, createdAt ASC")
    fun getAllTasks(isCompleted: Boolean?): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE (:isCompleted IS NULL OR isCompleted = :isCompleted) AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') ORDER BY priority DESC, createdAt ASC")
    fun getTasks(searchQuery: String, isCompleted: Boolean?): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdNonLiveData(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks(): Int

    @Query("SELECT * FROM tasks WHERE isCompleted = 1")
    fun getCompletedTasksNonLiveData(): List<Task>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks(): Int

    @Query("SELECT * FROM tasks")
    fun getAllTasksNonLiveData(): List<Task>

    @Query("SELECT id FROM tasks WHERE isCompleted = 1")
    fun getCompletedTaskIds(): List<Int>

    @Query("SELECT id FROM tasks")
    fun getAllTaskIds(): List<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
}
