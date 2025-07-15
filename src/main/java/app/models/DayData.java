package app.models;

import java.util.HashMap;
import java.util.Map;

public class DayData {
    private int workDayHours = 8;
    private String dayType = "workday";
    private String notes1;
    private String notes2;
    private String notesTime;
    private Map<String, TimeEntry> tasks = new HashMap<>();

    public double getProgress(int defaultWorkHours) {
        int totalMinutes = tasks.values().stream()
                .mapToInt(TimeEntry::getTotalMinutes)
                .sum();
        int workHours = workDayHours > 0 ? workDayHours : defaultWorkHours;
        return Math.min((double) totalMinutes / (workHours * 60), 1.0);
    }

    // Геттеры и сеттеры
    public int getWorkDayHours() { return workDayHours; }
    public void setWorkDayHours(int workDayHours) { this.workDayHours = workDayHours; }
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    public String getNotes1() { return notes1; }
    public void setNotes1(String notes1) { this.notes1 = notes1; }
    public String getNotes2() { return notes2; }
    public void setNotes2(String notes2) { this.notes2 = notes2; }
    public String getNotesTime() { return notesTime; }
    public void setNotesTime(String notesTime) { this.notesTime = notesTime; }
    public Map<String, TimeEntry> getTasks() { return tasks; }
    public void setTasks(Map<String, TimeEntry> tasks) { this.tasks = tasks; }
}
