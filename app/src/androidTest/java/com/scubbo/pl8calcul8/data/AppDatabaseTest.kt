package com.scubbo.pl8calcul8.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertedLiftIsReadable() = runBlocking {
        db.liftDao().insert(Lift(name = "Bench Press", incrementLb = 5.0))
        val lifts = db.liftDao().all().first()
        assertEquals(1, lifts.size)
        assertEquals("Bench Press", lifts[0].name)
        assertEquals(5.0, lifts[0].incrementLb, 1e-9)
    }

    @Test
    fun mostRecentExerciseIsNullWithNoHistory() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Squat"))
        assertNull(db.workoutDao().mostRecentExerciseForLift(liftId))
    }

    @Test
    fun mostRecentExerciseComesFromLatestWorkout() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Squat"))
        val olderWorkout = db.workoutDao().insert(Workout(date = 1_000L))
        val newerWorkout = db.workoutDao().insert(Workout(date = 2_000L))
        db.workoutDao().insert(
            Exercise(
                workoutId = olderWorkout, liftId = liftId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 200.0, rpe = 8.0,
            )
        )
        db.workoutDao().insert(
            Exercise(
                workoutId = newerWorkout, liftId = liftId,
                assignedReps = 4, assignedRpe = 7.0, sets = 3,
                weightLb = 205.0, rpe = 7.5,
            )
        )

        val mostRecent = db.workoutDao().mostRecentExerciseForLift(liftId)!!
        assertEquals(205.0, mostRecent.weightLb, 1e-9)
        assertEquals(7.5, mostRecent.rpe, 1e-9)
    }

    @Test
    fun historyIsOrderedNewestFirstWithDates() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Deadlift"))
        val otherLiftId = db.liftDao().insert(Lift(name = "Squat"))
        val w1 = db.workoutDao().insert(Workout(date = 1_000L))
        val w2 = db.workoutDao().insert(Workout(date = 2_000L))
        db.workoutDao().insert(
            Exercise(
                workoutId = w1, liftId = liftId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 300.0, rpe = 8.5, notes = "grip slipped",
            )
        )
        db.workoutDao().insert(
            Exercise(
                workoutId = w2, liftId = liftId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 305.0, rpe = 8.0,
            )
        )
        db.workoutDao().insert(
            Exercise(
                workoutId = w2, liftId = otherLiftId,
                assignedReps = 8, assignedRpe = 7.0, sets = 3,
                weightLb = 225.0, rpe = 7.0,
            )
        )

        val history = db.workoutDao().historyForLift(liftId)
        assertEquals(2, history.size)
        assertEquals(2_000L, history[0].date)
        assertEquals(305.0, history[0].weightLb, 1e-9)
        assertEquals(1_000L, history[1].date)
        assertEquals("grip slipped", history[1].notes)
    }

    @Test
    fun incrementUpdatePersists() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Squat"))

        db.liftDao().updateIncrement(liftId, 10.0)

        val lift = db.liftDao().all().first().single()
        assertEquals(10.0, lift.incrementLb, 1e-9)
    }

    @Test
    fun dumpReturnsAllRowsAndWipeClearsThem() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Squat", incrementLb = 10.0))
        val workoutId = db.workoutDao().insert(Workout(date = 1_000L))
        db.workoutDao().insert(
            Exercise(
                workoutId = workoutId, liftId = liftId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 225.0, rpe = 8.5, notes = "hard",
            )
        )

        assertEquals(1, db.liftDao().dump().size)
        assertEquals(1, db.workoutDao().dumpWorkouts().size)
        assertEquals(1, db.workoutDao().dumpExercises().size)

        db.workoutDao().deleteAllExercises()
        db.workoutDao().deleteAllWorkouts()
        db.liftDao().deleteAll()

        assertEquals(0, db.liftDao().dump().size)
        assertEquals(0, db.workoutDao().dumpWorkouts().size)
        assertEquals(0, db.workoutDao().dumpExercises().size)
    }

    @Test
    fun insertPreservesExplicitIds() = runBlocking {
        val liftId = db.liftDao().insert(Lift(id = 42, name = "Squat"))
        assertEquals(42L, liftId)
        val workoutId = db.workoutDao().insert(Workout(id = 7, date = 1_000L))
        assertEquals(7L, workoutId)
    }

    @Test
    fun workoutLogGroupsExercisesByWorkoutNewestFirst() = runBlocking {
        val benchId = db.liftDao().insert(Lift(name = "Bench Press"))
        val squatId = db.liftDao().insert(Lift(name = "Squat"))
        val w1 = db.workoutDao().insert(Workout(date = 1_000L))
        val w2 = db.workoutDao().insert(Workout(date = 2_000L))
        db.workoutDao().insert(
            Exercise(
                workoutId = w1, liftId = benchId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 185.0, rpe = 8.0, notes = "solid",
            )
        )
        db.workoutDao().insert(
            Exercise(
                workoutId = w2, liftId = squatId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 245.0, rpe = 8.0,
            )
        )

        val rows = db.workoutDao().workoutLog()

        assertEquals(2, rows.size)
        assertEquals(2_000L, rows[0].date)
        assertEquals("Squat", rows[0].liftName)
        assertEquals(1_000L, rows[1].date)
        assertEquals("solid", rows[1].notes)
    }

    @Test
    fun draftRoundTripAndClear() = runBlocking {
        val liftId = db.liftDao().insert(Lift(name = "Squat"))
        val drafts = listOf(
            DraftExercise(
                position = 0, liftId = liftId, reps = 5, rpe = 8.0, sets = 3,
                advisedWeightLb = 225.0, resultWeightLb = 225.0, resultRpe = 8.5,
                resultNotes = "hard",
            ),
            DraftExercise(
                position = 1, liftId = liftId, reps = 4, rpe = 7.0, sets = 3,
                advisedWeightLb = null, resultWeightLb = null, resultRpe = null,
                resultNotes = null,
            ),
        )

        db.draftDao().replaceAll(drafts)

        val loaded = db.draftDao().load()
        assertEquals(2, loaded.size)
        assertEquals(225.0, loaded[0].resultWeightLb!!, 1e-9)
        assertEquals("hard", loaded[0].resultNotes)
        assertNull(loaded[1].advisedWeightLb)

        db.draftDao().replaceAll(listOf(drafts[1]))
        assertEquals(1, db.draftDao().load().size)

        db.draftDao().clear()
        assertEquals(0, db.draftDao().load().size)
    }

    @Test
    fun seedCallbackPopulatesDefaultLifts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val seeded = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(AppDatabase.seedCallback)
            .build()
        try {
            val lifts = seeded.liftDao().all().first()
            val names = lifts.map { it.name }
            assertTrue("expected seeded lifts, got $names", names.containsAll(
                listOf("Squat", "Bench Press", "Deadlift", "Overhead Press", "Barbell Row")
            ))
            assertTrue(lifts.all { it.incrementLb == 5.0 })
        } finally {
            seeded.close()
        }
    }
}
