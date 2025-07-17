package app.models;


import java.util.HashMap;
import java.util.Map;
import app.controllers.DayType;  // Импортируем DayType из контроллера

public class DayData {
    private int workDayHours = 8;
    private DayType dayType = DayType.WORKDAY;  // Используем DayType из app.controllers
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
    public DayType getDayType() { return dayType; }  // Теперь возвращаем DayType
    public void setDayType(DayType dayType) { this.dayType = dayType; }  // Теперь принимаем DayType
    public Map<String, TimeEntry> getTasks() { return tasks; }
    public void setTasks(Map<String, TimeEntry> tasks) { this.tasks = tasks; }
}