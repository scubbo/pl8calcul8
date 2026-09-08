package com.scubbo.pl8calcul8.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.AutoMigration
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Lift::class, Workout::class, Exercise::class, DraftExercise::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liftDao(): LiftDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun draftDao(): DraftDao

    companion object {
        /**
         * Before v4, every lift stored the default 5lb explicitly. Keep
         * non-default values as overrides and turn stored 5s into null.
         */
        val migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE Lift_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, incrementLb REAL)"
                )
                db.execSQL(
                    "INSERT INTO Lift_new (id, name, incrementLb) " +
                        "SELECT id, name, " +
                        "CASE WHEN incrementLb = 5.0 THEN NULL ELSE incrementLb END FROM Lift"
                )
                db.execSQL("DROP TABLE Lift")
                db.execSQL("ALTER TABLE Lift_new RENAME TO Lift")
                db.execSQL("CREATE UNIQUE INDEX index_Lift_name ON Lift (name)")
            }
        }

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
                        "INSERT INTO Lift (name) VALUES (?)",
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
                    .addMigrations(migration3To4)
                    .build()
                    .also { instance = it }
            }
    }
}
