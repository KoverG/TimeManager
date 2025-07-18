package app.utils;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class CalendarCellStyleManager {

    // Метод для получения цвета фона ячейки по типу дня
    public static String getBackgroundColor(String dayType, boolean isCurrentMonth) {
        String colorHex;

        switch (dayType) {
            case "HOLIDAY":
                colorHex = "#ffcdd2";  // Праздничный день (красный)
                break;
            case "WEEKEND":
                colorHex = "#d0d0d0";  // Выходной день (приглушённый серый)
                break;
            case "SHORT":
                colorHex = "#fff8e1";  // Сокращённый день (жёлтый)
                break;
            default:
                colorHex = "#e0e0e0";  // Рабочий день (по умолчанию)
        }

        if (!isCurrentMonth) {
            // Если это не текущий месяц, то цвет будет более тусклым
            return Color.web(colorHex).deriveColor(0, 1, 1, 0.6).toString();
        }
        return colorHex;
    }

    // Метод для получения цвета для текста (например, метка с номером дня)
    public static Color getTextColor(boolean isCurrentMonth) {
        return isCurrentMonth ? Color.web("#343a40") : Color.web("#a0a0a0"); // Тёмно-серый для текущего месяца, светлый для других
    }

    // Метод для получения цвета для обводки ячейки с учётом прогресса
    public static Color getBorderColor(double progress, String dayType, boolean isCurrentMonth) {
        if (progress > 0) {
            // Если прогресс больше 0, вычисляем цвет по прогрессу
            String progressColor = getProgressColor(progress);
            return Color.web(progressColor);  // Возвращаем соответствующий цвет
        } else {
            // Если прогресс 0, вычисляем цвет в зависимости от типа дня и месяца
            return getBorderColorForDayType(dayType, isCurrentMonth);
        }
    }

    // Метод для получения цвета для обводки ячейки по типу дня
    private static Color getBorderColorForDayType(String dayType, boolean isCurrentMonth) {
        if (!isCurrentMonth) {
            return Color.web("#d0d0d0");  // Цвет обводки для не текущего месяца
        }

        switch (dayType) {
            case "HOLIDAY":
                return Color.web("#ffcdd2"); // Праздничный (красный)
            case "WEEKEND":
                return Color.web("#d0d0d0"); // Выходной (серый)
            case "SHORT":
                return Color.web("#ffecb3"); // Сокращённый (жёлтый)
            default:
                return Color.web("#d0d0d0"); // Рабочий (серый)
        }
    }

    // Метод для получения цвета прогресса
    public static String getProgressColor(double progress) {
        if (progress >= 0.99) return "#27ae60";  // Зеленый
        if (progress >= 0.7) return "#f39c12";   // Желтый
        return "#e74c3c";  // Красный
    }

    // Метод для получения формы ячейки (круглая)
    public static double getArcWidth() {
        return 15.0;  // Радиус скругления
    }

    public static Rectangle createCellBackground(double width, double height, String bgColor) {
        Rectangle bg = new Rectangle(width, height);
        bg.setArcWidth(getArcWidth());  // Получаем радиус скругления из CalendarCellStyleManager
        bg.setArcHeight(getArcWidth());
        bg.setFill(Color.web(bgColor));  // Устанавливаем цвет фона
        return bg;
    }

}
