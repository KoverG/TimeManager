package app.views.custom;

import app.utils.UIHelper;
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
        bg.setArcWidth(15);
        bg.setArcHeight(15);

        boolean isCurrentMonth = date.getMonthValue() == currentMonth;
        boolean isHoliday = "holiday".equals(dayType);
        boolean isWeekend = "weekend".equals(dayType);
        boolean isShortDay = "short".equals(dayType);

        // Установка цвета фона с прозрачностью для не текущего месяца
        String colorHex = UIHelper.getDayTypeColor(dayType);
        if (isCurrentMonth) {
            bg.setFill(UIHelper.color(colorHex));
        } else {
            bg.setFill(UIHelper.color(colorHex).deriveColor(0, 1, 1, 0.6));
        }

        if (progress > 0) {
            bg.setStroke(UIHelper.color(UIHelper.getProgressColor(progress)));
        } else {
            if (isCurrentMonth) {
                if (isHoliday) {
                    bg.setStroke(UIHelper.color("#ffcdd2"));
                } else if (isWeekend) {
                    bg.setStroke(UIHelper.color("#d0d0d0"));
                } else if (isShortDay) {
                    bg.setStroke(UIHelper.color("#ffecb3"));
                } else {
                    bg.setStroke(UIHelper.color("#d0d0d0"));
                }
            } else {
                bg.setStroke(UIHelper.color("#d0d0d0"));
            }
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        // Изменение цвета текста для не текущего месяца
        dayLabel.setTextFill(isCurrentMonth ?
                UIHelper.color("#343a40") :
                UIHelper.color("#a0a0a0")); // Приглушенный серый

        if (progress > 0 && isCurrentMonth) {
            Rectangle progressBar = new Rectangle(CELL_WIDTH * progress, 7);
            progressBar.setFill(UIHelper.color(UIHelper.getProgressColor(progress)));

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
