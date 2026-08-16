package com.scubbo.pl8calcul8.ui.settings

import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.Lift
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    private val liftDao = FakeLiftDao()
    private val vm = SettingsViewModel(liftDao)

    @Test
    fun `adding a lift stores it with the default increment`() = runTest {
        val created = vm.addLift(" Incline Bench ")

        assertEquals("Incline Bench", created.name)
        assertEquals(5.0, created.incrementLb, 1e-9)
        assertEquals(created, liftDao.lifts.value.single())
    }

    @Test
    fun `setting an increment updates the lift`() = runTest {
        val lift = Lift(id = 0, name = "Deadlift")
        val id = liftDao.insert(lift)

        vm.setIncrement(liftDao.lifts.value.single(), 10.0)

        assertEquals(10.0, liftDao.lifts.value.single { it.id == id }.incrementLb, 1e-9)
    }
}
