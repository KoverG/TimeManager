package app.views.custom;

import app.utils.CalendarCellStyleManager;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;

public class RoundedCalendarCell extends StackPane {
    private static final int CELL_WIDTH = 90;
    private static final int CELL_HEIGHT = 75;

    public RoundedCalendarCell(LocalDate date, int currentMonth, double progress, String dayType) {
        this.getStyleClass().clear();
        this.setStyle("");

        Rectangle bg = new Rectangle(CELL_WIDTH, CELL_HEIGHT);
        bg.setArcWidth(CalendarCellStyleManager.getArcWidth());
        bg.setArcHeight(CalendarCellStyleManager.getArcWidth());

        boolean isCurrentMonth = date.getMonthValue() == currentMonth;
        boolean isHoliday = "HOLIDAY".equals(dayType);
        boolean isWeekend = "WEEKEND".equals(dayType);
        boolean isShortDay = "SHORT".equals(dayType);

        // Установка цвета фона с использованием нового класса
        String bgColor = CalendarCellStyleManager.getBackgroundColor(dayType, isCurrentMonth);
        bg.setFill(Color.web(bgColor));

        // Установка цвета обводки
        bg.setStroke(CalendarCellStyleManager.getBorderColor(progress, dayType, isCurrentMonth));

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        dayLabel.setTextFill(CalendarCellStyleManager.getTextColor(isCurrentMonth));

        if (progress > 0 && isCurrentMonth) {
            Rectangle progressBar = new Rectangle(CELL_WIDTH * progress, 7);
            progressBar.setFill(Color.web(CalendarCellStyleManager.getProgressColor(progress)));

            double yPosition = CELL_HEIGHT - 7 - 2;
            progressBar.setY(yPosition);

            getChildren().addAll(bg, progressBar, dayLabel);
        } else {
            getChildren().addAll(bg, dayLabel);
        }

        setOnMouseClicked(e -> {
            if (e.getClickCount() == 1 && isCurrentMonth) {
                fireEvent(new DayCellClickEvent(date));
            }
        });
    }

    public static class DayCellClickEvent extends Event {
        public static final EventType<DayCellClickEvent> DAY_CLICKED =
                new EventType<>(Event.ANY, "DAY_CLICKED");

        private final LocalDate date;

        public DayCellClickEvent(LocalDate date) {
            super(DAY_CLICKED);
            this.date = date;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}
