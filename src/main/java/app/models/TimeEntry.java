package app.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimeEntry {

    private String task;
    private String hours;
    private String minutes;
    private String comment;
    private boolean completed;

    public TimeEntry() {
        this.task = "";
        this.hours = "0";
        this.minutes = "0";
        this.comment = "";
        this.completed = false;
    }

    // Переопределяем метод toString() для вывода содержимого
    @Override
    public String toString() {
        return "TimeEntry{" +
                "task='" + task + '\'' +
                ", hours='" + hours + '\'' +
                ", minutes='" + minutes + '\'' +
                ", comment='" + comment + '\'' +
                ", completed=" + completed +
                '}';
    }

    // Геттеры и сеттеры
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getHours() { return hours; }
    public void setHours(String hours) { this.hours = hours; }

    public String getMinutes() { return minutes; }
    public void setMinutes(String minutes) { this.minutes = minutes; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    @JsonIgnore
    public int getTotalMinutes() {
        return Integer.parseInt(hours) * 60 + Integer.parseInt(minutes);
    }
}
