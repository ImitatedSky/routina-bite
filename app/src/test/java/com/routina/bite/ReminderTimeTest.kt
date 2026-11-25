package com.routina.bite

import com.routina.bite.data.defaultMeal
import com.routina.bite.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTimeTest {

    private val now = LocalDateTime.of(2026, 11, 25, 17, 40)

    @Test
    fun nextFireIsTodayUntilTheMinutePasses() {
        assertEquals(now.withHour(18).withMinute(0), nextFire(LocalTime.of(18, 0), now))
        // 剛好是現在也排明天，不然會在同一分鐘連響兩次
        assertEquals(now.plusDays(1), nextFire(LocalTime.of(17, 40), now))
        assertEquals(now.plusDays(1).withHour(8).withMinute(0), nextFire(LocalTime.of(8, 0), now))
    }

    @Test
    fun mealSlotBoundaries() {
        assertEquals(Meal.BREAKFAST, defaultMeal(LocalTime.of(4, 0)))
        assertEquals(Meal.BREAKFAST, defaultMeal(LocalTime.of(10, 29)))
        assertEquals(Meal.LUNCH, defaultMeal(LocalTime.of(10, 30)))
        assertEquals(Meal.SNACK, defaultMeal(LocalTime.of(14, 30)))
        assertEquals(Meal.DINNER, defaultMeal(LocalTime.of(17, 0)))
        assertEquals(Meal.SNACK, defaultMeal(LocalTime.of(21, 30)))
    }

    @Test
    fun nightRemindersHaveNoMeal() {
        assertEquals(Meal.DINNER, reminderMeal(LocalTime.of(21, 29)))
        assertNull(reminderMeal(LocalTime.of(21, 30)))
        assertNull(reminderMeal(LocalTime.of(0, 0)))
        assertNull(reminderMeal(LocalTime.of(3, 59)))
        assertEquals(Meal.BREAKFAST, reminderMeal(LocalTime.of(4, 0)))
        assertEquals(Meal.SNACK, reminderMeal(LocalTime.of(15, 0)))
    }
}
