package app.controllers;

import app.models.DayData;
import app.services.JsonService;
import app.services.ProductionCalendarService;
import app.utils.CalendarCellStyleManager;
import app.utils.DateHelper;
import app.utils.UIHelper;
import app.views.custom.RoundedCalendarCell;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.shape.Rectangle;

public class CalendarController {
    @FXML
    private Label monthYearLabel;
    @FXML
    private GridPane calendarGrid;
    @FXML
    private VBox mainContainer;
    @FXML
    private ComboBox<String> monthCombo;
    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button updateCalendarButton;
    @FXML
    private ProgressIndicator updateProgress;
    @FXML
    private Label yearWarningIcon;
    @FXML
    private StackPane statusContainer;
    @FXML
    private Label successIcon;

    private LocalDate currentDate = LocalDate.now();
    private final AtomicReference<Map<String, DayData>> dataRef =
            new AtomicReference<>(new HashMap<>());

    private boolean updatingCombo = false;

    @FXML
    public void initialize() {
        try {
            dataRef.set(JsonService.loadData());
        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            dataRef.set(new HashMap<>());
        }

        monthCombo.getItems().addAll(
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        );
        monthCombo.setVisibleRowCount(12);

        int currentYear = currentDate.getYear();
        yearCombo.getItems().addAll(
                currentYear - 2,
                currentYear - 1,
                currentYear,
                currentYear + 1
        );

        monthCombo.getSelectionModel().select(currentDate.getMonthValue() - 1);
        yearCombo.getSelectionModel().select((Integer) currentYear);
        yearCombo.setValue(currentYear);

        updateNavigationButtons();

        updateCalendarButton.setText("Загрузить календарь");
        updateCalendarButton.setTooltip(new Tooltip("Загрузить производственный календарь за выбранный год"));
        updateProgress.setVisible(false);
        successIcon.setVisible(false);
        statusContainer.setVisible(false);

        yearWarningIcon.setVisible(false);
        Tooltip warningTooltip = new Tooltip("Данные за год могут быть неполными.\nПроизводственный календарь будет основан на стандартных выходных.");
        warningTooltip.setShowDelay(Duration.ZERO);
        warningTooltip.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #000000; -fx-font-weight: normal;");
        yearWarningIcon.setTooltip(warningTooltip);

        monthCombo.setOnAction(e -> updateCalendarFromCombo());
        yearCombo.setOnAction(e -> {
            if (updatingCombo) return;
            Integer selectedYear = yearCombo.getValue();
            if (selectedYear != null) {
                boolean isLoaded = ProductionCalendarService.isCalendarLoaded(selectedYear);
                yearWarningIcon.setVisible(!isLoaded);
                updateCalendarButton.setVisible(!isLoaded);
            }
            updateCalendarFromCombo();
        });

        updateCalendar();
        updateWarningAndButtonVisibility();
    }

    private void showDayView(LocalDate date, DayData dayData) {
        URL fxmlUrl = Thread.currentThread()
                .getContextClassLoader()
                .getResource("fxml/day_view_v2.fxml");
        if (fxmlUrl == null) {
            UIHelper.showError("FXML файл не найден: /fxml/day_view_v2.fxml");
            return;
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        try {
            VBox dayView = loader.load();

            DayViewController controller = loader.getController();

            DayType dayType = dayData.getDayType();
            int calculatedWorkHours = dayData.getWorkDayHours(9);

            controller.setDayData(date, dayData, calculatedWorkHours, dayType);
            controller.setMainContainer(mainContainer);

            mainContainer.setUserData(this);
            mainContainer.getChildren().setAll(dayView);

        } catch (Exception ex) {
            UIHelper.showError("Ошибка загрузки экрана дня:\n" + ex.getMessage());
        }
    }

    private void updateWarningAndButtonVisibility() {
        Integer selectedYear = yearCombo.getValue();
        if (selectedYear != null) {
            boolean isLoaded = ProductionCalendarService.isCalendarLoaded(selectedYear);
            yearWarningIcon.setVisible(!isLoaded);
            updateCalendarButton.setVisible(!isLoaded);
        }
    }

    private void updateNavigationButtons() {
        LocalDate prevMonth = currentDate.minusMonths(1);
        LocalDate nextMonth = currentDate.plusMonths(1);

        previousButton.setText("◀ " + DateHelper.getMonthName(prevMonth.getMonthValue()));
        nextButton.setText(DateHelper.getMonthName(nextMonth.getMonthValue()) + " ▶");

        ObservableList<Integer> years = yearCombo.getItems();
        if (!years.isEmpty()) {
            int minYear = Collections.min(years);
            int maxYear = Collections.max(years);

            previousButton.setVisible(!(currentDate.getYear() == minYear && currentDate.getMonthValue() == 1));
            nextButton.setVisible(!(currentDate.getYear() == maxYear && currentDate.getMonthValue() == 12));
        }
    }

    @FXML
    private void updateProductionCalendar() {
        Integer selectedYear = yearCombo.getValue();
        if (selectedYear == null) {
            UIHelper.showWarning("Пожалуйста, выберите год для загрузки");
            return;
        }

        updateCalendarButton.setDisable(true);
        statusContainer.setVisible(true);
        updateProgress.setVisible(true);
        successIcon.setVisible(false);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    ProductionCalendarService.loadCalendarForYear(selectedYear);
                    return true;
                } catch (Exception e) {
                    throw new IOException("Ошибка загрузки данных: " + e.getMessage());
                }
            }
        };

        task.setOnSucceeded(e -> {
            updateCalendarButton.setDisable(false);
            updateProgress.setVisible(false);
            successIcon.setVisible(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), successIcon);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            PauseTransition pause = new PauseTransition(Duration.seconds(2));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), successIcon);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(event -> {
                statusContainer.setVisible(false);
                yearWarningIcon.setVisible(false);
                updateCalendarButton.setVisible(false);
            });

            SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
            sequence.play();

            updateCalendar();
        });

        task.setOnFailed(e -> {
            updateCalendarButton.setDisable(false);
            updateProgress.setVisible(false);
            statusContainer.setVisible(false);

            String errorMessage = "Не удалось загрузить календарь:\n";
            Throwable exception = task.getException();

            if (exception != null) {
                errorMessage += exception.getMessage();

                if (exception.getMessage().contains("пустой")) {
                    errorMessage += "\n\nПопробуйте загрузить данные повторно или проверьте доступность сервера";
                } else if (exception.getMessage().contains("формат")) {
                    errorMessage += "\n\nСервер изменил формат данных. Сообщите разработчику";
                } else if (exception.getMessage().contains("404")) {
                    errorMessage += "\n\nДанные за этот год еще не доступны на сервере";
                } else if (exception.getMessage().contains("код ошибки")) {
                    errorMessage += "\n\nПроверьте подключение к интернету";
                }
            } else {
                errorMessage += "Неизвестная ошибка";
            }

            UIHelper.showError(errorMessage);
        });

        new Thread(task).start();
    }

    private void updateCalendarFromCombo() {
        if (updatingCombo) return;

        int monthIndex = monthCombo.getSelectionModel().getSelectedIndex();
        Integer year = yearCombo.getValue();

        if (monthIndex < 0 || year == null) return;

        currentDate = LocalDate.of(year, monthIndex + 1, 1);
        updateNavigationButtons();
        updateCalendar();
    }

    @FXML
    private void previousMonth() {
        currentDate = currentDate.minusMonths(1);
        updateNavigationButtons();
        updateCalendar();
        updateWarningAndButtonVisibility();
    }

    @FXML
    private void nextMonth() {
        currentDate = currentDate.plusMonths(1);
        updateNavigationButtons();
        updateCalendar();
        updateWarningAndButtonVisibility();
    }

    private void updateCalendar() {
        updatingCombo = true;
        try {
            monthYearLabel.setText(DateHelper.getMonthName(currentDate.getMonthValue()) + " " + currentDate.getYear());

            monthCombo.getSelectionModel().select(currentDate.getMonthValue() - 1);
            yearCombo.getSelectionModel().select(currentDate.getYear());
            yearCombo.setValue(currentDate.getYear());

            calendarGrid.getChildren().removeIf(node ->
                    GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) > 0
            );

            YearMonth yearMonth = YearMonth.from(currentDate);
            LocalDate firstDay = currentDate.withDayOfMonth(1);
            int daysInMonth = yearMonth.lengthOfMonth();
            int startDayOfWeek = firstDay.getDayOfWeek().getValue();
            int currentMonth = currentDate.getMonthValue();

            int row = 1;
            int col = startDayOfWeek - 1;

            if (col > 0) {
                LocalDate prevMonth = currentDate.minusMonths(1);
                int daysInPrevMonth = YearMonth.from(prevMonth).lengthOfMonth();
                int prevStartDay = daysInPrevMonth - (col - 1);

                for (int i = 0; i < col; i++) {
                    int day = prevStartDay + i;
                    LocalDate date = prevMonth.withDayOfMonth(day);
                    addCalendarCell(date, currentMonth, row, i);
                }
            }

            for (int day = 1; day <= daysInMonth; day++) {
                if (col > 6) {
                    col = 0;
                    row++;
                }
                LocalDate date = currentDate.withDayOfMonth(day);
                addCalendarCell(date, currentMonth, row, col);
                col++;
            }
        } finally {
            updatingCombo = false;
        }
    }

    private void addCalendarCell(LocalDate date, int currentMonth, int row, int col) {
        Map<String, DayData> data = dataRef.get();
        if (data == null) {
            data = new HashMap<>();
            dataRef.set(data);
        }

        String dateStr = DateHelper.formatDate(date);
        AtomicReference<DayData> dayDataRef = new AtomicReference<>(data.get(dateStr));

        DayData dayData = dayDataRef.get();
        if (dayData == null) {
            dayData = new DayData();
            dayDataRef.set(dayData);
        }

        if (data.containsKey(dateStr)) {
            dayData = data.get(dateStr);
        } else {
            List<String> holidays = ProductionCalendarService.loadHolidays(String.valueOf(date.getYear()));
            List<String> shortDays = ProductionCalendarService.loadShortDays(String.valueOf(date.getYear()));

            if (holidays.contains(dateStr)) {
                if (shortDays.contains(dateStr)) {
                    dayData.setDayType(DayType.SHORT);
                } else {
                    dayData.setDayType(DayType.WEEKEND);
                }
            } else if (shortDays.contains(dateStr)) {
                dayData.setDayType(DayType.SHORT);
            } else {
                if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                    dayData.setDayType(DayType.WEEKEND);
                } else {
                    dayData.setDayType(DayType.WORKDAY);
                }
            }
        }

        int workHours = dayData.getWorkDayHours(9);

        double progress = dayData.getProgress(workHours);

        RoundedCalendarCell cell = new RoundedCalendarCell(date, currentMonth, progress, dayData.getDayType().toString());

        String bgColor = CalendarCellStyleManager.getBackgroundColor(dayData.getDayType().toString(), date.getMonthValue() == currentMonth);

        Rectangle bg = CalendarCellStyleManager.createCellBackground(90, 75, bgColor);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(bg);

        if (progress > 0) {
            Rectangle progressBar = CalendarCellStyleManager.createProgressBar(90 * progress, 25, 15);
            progressBar.setFill(Color.web(CalendarCellStyleManager.getProgressColor(progress)));
            StackPane.setAlignment(progressBar, Pos.BOTTOM_LEFT);
            stackPane.getChildren().add(progressBar);
            bg.setStroke(Color.web(CalendarCellStyleManager.getProgressColor(progress)));
            cell.getChildren().addAll(bg, progressBar);
        } else {
            cell.getChildren().add(bg);
        }

        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        dayLabel.setTextFill(CalendarCellStyleManager.getTextColor(date.getMonthValue() == currentMonth));

        StackPane.setAlignment(dayLabel, Pos.CENTER);
        stackPane.getChildren().add(dayLabel);

        cell.getChildren().add(dayLabel);

        cell.setOnMouseClicked(e -> showDayView(date, dayDataRef.get()));
        calendarGrid.add(cell, col, row);
    }

    public void showCalendar() {
        updateCalendar();
        try {
            Parent root = FXMLLoader.load(
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResource("fxml/calendar.fxml")
            );
            mainContainer.getChildren().clear();
            mainContainer.getChildren().add(root);
        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки календаря: " + e.getMessage());
        }
    }
}
