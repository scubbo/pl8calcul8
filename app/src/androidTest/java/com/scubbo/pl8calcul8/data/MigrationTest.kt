package com.scubbo.pl8calcul8.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DB_NAME = "migration-test.db"

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsData() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO Lift (name, incrementLb) VALUES ('Squat', 5.0)")
            execSQL("INSERT INTO Workout (id, date) VALUES (1, 1000)")
            execSQL(
                "INSERT INTO Exercise (workoutId, liftId, assignedReps, assignedRpe," +
                    " sets, weightLb, rpe, notes) VALUES (1, 1, 5, 8.0, 3, 225.0, 8.5, 'hard')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 2, true)

        db.query("SELECT count(*) FROM Workout").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT count(*) FROM DraftExercise").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate2To3KeepsDrafts() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL("INSERT INTO Lift (id, name, incrementLb) VALUES (1, 'Squat', 5.0)")
            execSQL(
                "INSERT INTO DraftExercise (position, liftId, reps, rpe, sets," +
                    " advisedWeightLb, resultWeightLb, resultRpe, resultNotes)" +
                    " VALUES (0, 1, 5, 8.0, 3, 225.0, NULL, NULL, NULL)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true)

        db.query("SELECT sessionDate FROM DraftExercise").use { cursor ->
            cursor.moveToFirst()
            assertEquals(true, cursor.isNull(0))
        }
    }
}
