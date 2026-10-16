package com.focusday.app

import java.time.LocalTime

data class DayTask(val start: String, val end: String, val title: String, val detail: String) {
    fun startMinute() = LocalTime.parse(start).hour * 60 + LocalTime.parse(start).minute
    fun endMinute() = LocalTime.parse(end).hour * 60 + LocalTime.parse(end).minute
}

object TaskData {
    // Times are local device time. Edit these defaults or add tasks in the app later.
    val tasks = listOf(
        DayTask("14:00", "15:15", "German university application", "Finish the German university application or list missing requirements."),
        DayTask("15:15", "15:40", "Lunch", "Eat properly and step away from your screen."),
        DayTask("15:40", "16:00", "Tea break", "Tea and a proper reset."),
        DayTask("16:00", "17:15", "Canadian supervisor research", "Prioritise UNBC, then shortlist supervisors at one other university."),
        DayTask("17:15", "18:00", "German A1", "Revise old material and learn 10–15 new words."),
        DayTask("18:00", "19:30", "Freelance website", "Work on one defined feature and test the change."),
        DayTask("19:30", "20:15", "Dinner and rest", "Eat and take a genuine break."),
        DayTask("20:15", "21:00", "Architecture portfolio research", "Compare Framer and alternatives; identify one animation to prototype."),
        DayTask("21:00", "21:25", "Job search", "Save three relevant jobs or submit one good application."),
        DayTask("21:25", "21:45", "Python DSA", "Solve one manageable problem or learn one pattern.")
    )
}
