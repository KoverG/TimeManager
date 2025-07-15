package app.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TimeEntry {
    private String time;
    private String task;
    private String hours;
    private String minutes;
    private String comment;
    private boolean completed;
    private boolean highlighted;

    public TimeEntry() {
        this.time = "";
        this.task = "";
        this.hours = "0";
        this.minutes = "0";
        this.comment = "";
        this.completed = false;
        this.highlighted = false;
    }

    @JsonIgnore
    public int getTotalMinutes() {
        try {
            return Integer.parseInt(hours) * 60 + Integer.parseInt(minutes);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Геттеры и сеттеры
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
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
    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }
}
