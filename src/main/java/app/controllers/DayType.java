package app.controllers;

public enum DayType {
    WORKDAY("Рабочий день"),
    WEEKEND("Выходной"),
    SHORT("Сокращённый день"); // Добавлен новый тип для сокращённого дня

    private final String type;

    DayType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static DayType fromString(String dayTypeString) {
        switch (dayTypeString.trim()) {
            case "Рабочий день":
                return WORKDAY;
            case "Выходной":
                return WEEKEND;
            case "Сокращённый день":  // Обрабатываем строку для сокращённого дня
                return SHORT;
            default:
                return WORKDAY;  // По умолчанию рабочий день
        }
    }
}