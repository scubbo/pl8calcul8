package com.scubbo.pl8calcul8.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Lift::class, Workout::class, Exercise::class, DraftExercise::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liftDao(): LiftDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun draftDao(): DraftDao

    companion object {
        private val SEED_LIFTS = listOf(
            "Squat",
            "Bench Press",
            "Deadlift",
            "Overhead Press",
            "Barbell Row",
            "Bicep Curl",
        )

        /** Populates a fresh database with a starter set of common lifts. */
        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                SEED_LIFTS.forEach { name ->
                    db.execSQL(
                        "INSERT INTO Lift (name, incrementLb) VALUES (?, 5.0)",
                        arrayOf(name),
                    )
                }
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pl8calcul8.db",
                )
                    .addCallback(seedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}
