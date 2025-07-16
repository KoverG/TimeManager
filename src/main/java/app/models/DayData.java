package app.models;

import java.util.HashMap;
import java.util.Map;

public class DayData {
    private int workDayHours = 8;
    private String dayType = "workday";
    private Map<String, TimeEntry> tasks = new HashMap<>();

    // Переопределяем метод toString()
    @Override
    public String toString() {
        return "DayData{" +
                "workDayHours=" + workDayHours +
                ", dayType='" + dayType + '\'' +
                ", tasks=" + tasks +
                '}';
    }

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
    public Map<String, TimeEntry> getTasks() { return tasks; }
    public void setTasks(Map<String, TimeEntry> tasks) { this.tasks = tasks; }
}