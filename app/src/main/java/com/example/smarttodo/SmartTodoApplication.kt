package com.example.smarttodo

import android.app.Application
import com.example.smarttodo.data.TaskDatabase
import com.example.smarttodo.data.TaskRepository

class SmartTodoApplication : Application() {

    // Database instance - lazy initialization
    val database by lazy { TaskDatabase.getDatabase(this) }

    // Repository instance - lazy initialization
    val repository by lazy { TaskRepository(database.taskDao()) }
}