package com.example.smarttodo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The Room database for this application.
 * This class defines the database configuration, lists the entities it contains,
 * and provides access to the Data Access Objects (DAOs).
 *
 * It uses a singleton pattern to ensure only one instance of the database is created.
 *
 * @property entities An array of entity classes that are part of this database. Currently [Task].
 * @property version The version number of the database schema.
 *                   Increment this version if you make schema changes.
 * @property exportSchema If set to true, Room exports the database schema into a folder specified
 *                        in the build file. Defaults to true. Here it's set to `false`, which means
 *                        the schema is not exported. For production apps where schema history is
 *                        important for migrations, consider setting this to true and specifying a
 *                        schema location.
 */
@Database(
    entities = [Task::class], // Defines the tables (entities) in the database.
    version = 1,              // Schema version. Must be incremented on schema changes.
    exportSchema = false      // Disables schema export to JSON files.
)
@TypeConverters(Converters::class) // Registers custom type converters (e.g., for Date objects).
abstract class TaskDatabase : RoomDatabase() {

    /**
     * Abstract method to get an instance of [TaskDao].
     * Room will generate the implementation for this method.
     *
     * @return An instance of [TaskDao] for interacting with the "tasks" table.
     */
    abstract fun taskDao(): TaskDao

    /**
     * Companion object to provide a singleton instance of the [TaskDatabase].
     * This ensures that only one database instance exists throughout the application's lifecycle,
     * preventing potential issues with multiple open database connections.
     */
    companion object {
        /**
         * The singleton instance of the [TaskDatabase].
         * Marked as `@Volatile` to ensure that changes to this variable are immediately visible
         * to all threads, preventing issues with cached values in a multi-threaded environment.
         */
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /**
         * Gets the singleton instance of the [TaskDatabase].
         * Uses a double-checked locking pattern to ensure thread-safe instantiation.
         *
         * @param context The application context, used to create the database instance.
         *                It's important to use the application context to avoid memory leaks.
         * @return The singleton [TaskDatabase] instance.
         */
        fun getDatabase(context: Context): TaskDatabase {
            // Return the existing instance if it's already created.
            return INSTANCE ?: synchronized(this) { // synchronized block to ensure only one thread creates the instance.
                // Re-check INSTANCE inside synchronized block to handle cases where multiple threads passed the first check.
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, // Use application context to avoid leaks.
                    TaskDatabase::class.java,   // The RoomDatabase class.
                    "task_database"        // The name of the database file.
                )
                    // Specifies a migration strategy: if a schema migration is needed and not provided,
                    // Room will clear all tables and recreate the database with the new schema.
                    // This is simple for development but means data loss on schema version changes.
                    // For production, provide explicit migration paths.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance // Assign the newly created instance.
                instance // Return the instance.
            }
        }
    }
}
