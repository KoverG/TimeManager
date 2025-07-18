package app.utils;

import javafx.scene.control.Alert;
import javafx.scene.paint.Color;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class UIHelper {
    public static void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Ошибка", message);
    }

    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Успешно", message);
    }

    public static void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "Предупреждение", message);
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        try {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Ошибка при отображении диалога: " + e.getMessage());
        }
    }

    public static Color color(String hex) {
        try {
            return Color.web(hex);
        } catch (Exception e) {
            System.err.println("Ошибка преобразования цвета: " + hex);
            return Color.WHITE;
        }
    }

    public static String getProgressColor(double progress) {
        if (progress >= 0.99) return "#27ae60";
        if (progress >= 0.7) return "#f39c12";
        return "#e74c3c";
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static String getDayTypeColor(String dayType) {
        System.out.println("Day type received: " + dayType);
        switch (dayType) {
            case "holiday": return "#ffcdd2"; // Праздничный (красный)
            case "WEEKEND": return "#d0d0d0"; // Выходной (приглушенный серый)
            case "short":   return "#fff8e1"; // Сокращенный
            default:        return "#e0e0e0"; // Рабочий (темнее основного фона)
        }
    }
}
