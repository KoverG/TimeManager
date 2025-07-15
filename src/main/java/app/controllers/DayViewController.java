package app.controllers;

import app.models.DayData;
import app.models.TimeEntry;
import app.services.JsonService;
import app.utils.DateHelper;
import app.utils.UIHelper;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DayViewController {
    @FXML private Label dateLabel;
    @FXML private ComboBox<String> dayTypeCombo;
    @FXML private TextField workHoursField;
    @FXML private VBox timeSlotsContainer;
    @FXML private VBox mainContainer;
    @FXML private Button saveButton;
    @FXML private StackPane saveStatusContainer;

    private LocalDate currentDate;
    private DayData dayData;
    private Map<String, DayData> allData;
    private int defaultWorkHours;
    private Map<String, TimeEntry> timeEntries;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    public void setMainContainer(VBox mainContainer) {
        this.mainContainer = mainContainer;
    }

    @FXML
    public void initialize() {
        saveStatusContainer.setVisible(false);
        saveButton.disableProperty().bind(isDirty.not());
        dayTypeCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((o, ov, nv) -> markDirty());
    }

    public void setDayData(LocalDate date, DayData data, int workHours) {
        this.currentDate = date;
        this.dayData = (data != null) ? data : new DayData();
        this.defaultWorkHours = workHours;
        try {
            this.allData = JsonService.getData();
        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            this.allData = new HashMap<>();
        }

        TreeMap<String, TimeEntry> sortedEntries = new TreeMap<>();
        LocalTime t = LocalTime.of(8, 0), end = LocalTime.of(17, 0);
        while (!t.isAfter(end)) {
            String key = t.toString();
            TimeEntry entry = dayData.getTasks().getOrDefault(key, new TimeEntry());
            sortedEntries.put(key, entry);
            t = t.plusMinutes(30);
        }
        dayData.getTasks().forEach((time, entry) -> sortedEntries.putIfAbsent(time, entry));
        this.timeEntries = sortedEntries;

        dateLabel.setText(DateHelper.formatDisplayDate(date));
        dayTypeCombo.setItems(FXCollections.observableArrayList("Рабочий день", "Выходной день"));
        dayTypeCombo.getSelectionModel().select(
                "workday".equals(dayData.getDayType()) ? 0 : 1
        );
        workHoursField.setText(String.valueOf(
                dayData.getWorkDayHours() > 0 ? dayData.getWorkDayHours() : defaultWorkHours
        ));

        timeSlotsContainer.getChildren().clear();
        timeSlotsContainer.setStyle("-fx-alignment: CENTER;");
        timeEntries.forEach((timeKey, entry) ->
                timeSlotsContainer.getChildren().add(createTimeSlotRow(timeKey, entry))
        );
        isDirty.set(false);
    }

    private void markDirty() {
        isDirty.set(true);
    }

    private HBox createTimeSlotRow(String time, TimeEntry entry) {
        HBox row = new HBox(5);
        row.getStyleClass().add("main-banner-day-string");
        row.setMaxWidth(950);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-slot-label");
        timeLabel.setPrefWidth(40);

        TextField taskField = new TextField(entry.getTask());
        taskField.setPromptText("Задача");
        taskField.getStyleClass().add("custom-combo-mod");
        taskField.setPrefSize(320, 25);

        ComboBox<Integer> hoursCombo = new ComboBox<>(FXCollections.observableArrayList(0,1,2,3,4,5,6,7,8));
        hoursCombo.getStyleClass().add("combo-container-withoutRadius");
        hoursCombo.setStyle("-fx-background-radius:12 0 0 12; -fx-border-radius:12 0 0 12;");
        hoursCombo.setPrefSize(65,30);

        ComboBox<Integer> minutesCombo = new ComboBox<>(FXCollections.observableArrayList(0,10,20,30,40,50));
        minutesCombo.getStyleClass().add("combo-container-withoutRadius");
        minutesCombo.setStyle("-fx-background-radius:0 12 12 0; -fx-border-radius:0 12 12 0;");
        minutesCombo.setPrefSize(65,30);

        TextField commentField = new TextField(entry.getComment());
        commentField.setPromptText("Комментарий");
        commentField.getStyleClass().add("custom-combo-mod");
        commentField.setPrefSize(370,25);

        CheckBox completedCheck = new CheckBox();
        completedCheck.setSelected(entry.isCompleted());
        completedCheck.getStyleClass().add("jira-checkbox");

        row.getChildren().addAll(timeLabel, taskField, hoursCombo, minutesCombo, commentField, completedCheck);

        try {
            Integer hVal = Integer.valueOf(entry.getHours());
            hoursCombo.getSelectionModel().select(hoursCombo.getItems().contains(hVal) ? hVal : 0);
        } catch (Exception ex) {
            hoursCombo.getSelectionModel().select(0);
        }
        try {
            Integer mVal = Integer.valueOf(entry.getMinutes());
            minutesCombo.getSelectionModel().select(
                    minutesCombo.getItems().contains(mVal) ? minutesCombo.getItems().indexOf(mVal) : 0
            );
        } catch (Exception ex) {
            minutesCombo.getSelectionModel().select(0);
        }

        hoursCombo.valueProperty().addListener((o, ov, nv) -> {
            int oldH = ov != null ? ov : 0;
            int oldM = minutesCombo.getValue() != null ? minutesCombo.getValue() : 0;
            entry.setHours(nv != null ? nv.toString() : "0");
            adjustTimeSlotRow(time, oldH, oldM, nv != null ? nv : 0, oldM);
            markDirty();
        });
        minutesCombo.valueProperty().addListener((o, ov, nv) -> {
            int oldH = hoursCombo.getValue() != null ? hoursCombo.getValue() : 0;
            int oldM = ov != null ? ov : 0;
            entry.setMinutes(nv != null ? nv.toString() : "0");
            adjustTimeSlotRow(time, oldH, oldM, oldH, nv != null ? nv : 0);
            markDirty();
        });
        taskField.textProperty().addListener((o, ov, nv) -> { entry.setTask(nv); markDirty(); });
        commentField.textProperty().addListener((o, ov, nv) -> { entry.setComment(nv); markDirty(); });
        completedCheck.selectedProperty().addListener((o, ov, nv) -> { entry.setCompleted(nv); markDirty(); });

        return row;
    }

    private void adjustTimeSlotRow(String baseTime, int oldH, int oldM, int newH, int newM) {
        try {
            LocalTime t0 = LocalTime.parse(baseTime);
            LocalTime oldTime = t0.plusHours(oldH).plusMinutes(oldM);
            LocalTime newTime = t0.plusHours(newH).plusMinutes(newM);
            String oldKey = oldTime.toString();
            String newKey = newTime.toString();

            if (newH == 0 && newM == 0) {
                if (!oldKey.equals(baseTime) && timeEntries.containsKey(oldKey)) {
                    timeEntries.remove(oldKey);
                    timeSlotsContainer.getChildren().removeIf(node -> {
                        Label lbl = (Label)((HBox) node).getChildren().get(0);
                        return lbl.getText().equals(oldKey);
                    });
                }
                return;
            }

            if (!timeEntries.containsKey(newKey)) {
                TimeEntry ne = new TimeEntry();
                timeEntries.put(newKey, ne);
                int idx = 0;
                for (int i = 0; i < timeSlotsContainer.getChildren().size(); i++) {
                    Label lbl = (Label)((HBox) timeSlotsContainer.getChildren().get(i)).getChildren().get(0);
                    if (LocalTime.parse(lbl.getText()).isBefore(newTime)) idx = i + 1;
                    else break;
                }
                timeSlotsContainer.getChildren().add(idx, createTimeSlotRow(newKey, ne));
            }

            if ((oldH != 0 || oldM != 0) && timeEntries.containsKey(oldKey) && !oldKey.equals(baseTime)) {
                timeEntries.remove(oldKey);
                timeSlotsContainer.getChildren().removeIf(node -> {
                    Label lbl = (Label)((HBox) node).getChildren().get(0);
                    return lbl.getText().equals(oldKey);
                });
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении слота: " + e.getMessage());
        }
    }

    @FXML
    private void previousDay() {
        setDayData(
                currentDate.minusDays(1),
                allData.get(DateHelper.formatDate(currentDate.minusDays(1))),
                defaultWorkHours
        );
    }

    @FXML
    private void nextDay() {
        setDayData(
                currentDate.plusDays(1),
                allData.get(DateHelper.formatDate(currentDate.plusDays(1))),
                defaultWorkHours
        );
    }

    @FXML
    private void saveDay() {
        try {
            int wh = Integer.parseInt(workHoursField.getText());
            if (wh < 1 || wh > 24) throw new NumberFormatException();
            dayData.setWorkDayHours(wh);
            dayData.setDayType(
                    dayTypeCombo.getSelectionModel().getSelectedIndex() == 0 ? "workday" : "weekend"
            );
            dayData.setTasks(timeEntries);
            allData.put(DateHelper.formatDate(currentDate), dayData);
            JsonService.saveData(allData);

            // Показываем иконку успешного сохранения на переднем плане
            saveStatusContainer.toFront();
            saveStatusContainer.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), saveStatusContainer);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(e -> {
                PauseTransition pause = new PauseTransition(Duration.seconds(2));
                pause.setOnFinished(evt -> saveStatusContainer.setVisible(false));
                pause.play();
            });
            fadeIn.play();

            isDirty.set(false);

        } catch (NumberFormatException e) {
            UIHelper.showError("Некорректное количество рабочих часов.\nВведите число от 1 до 24.");
        } catch (IOException e) {
            UIHelper.showError("Ошибка сохранения: " + e.getMessage());
        }
    }

    @FXML
    private void clearDay() {
        workHoursField.setText(String.valueOf(defaultWorkHours));
        dayTypeCombo.getSelectionModel().select(0);
        timeEntries.clear();
        setDayData(currentDate, new DayData(), defaultWorkHours);
        markDirty();
    }

    @FXML
    private void backToCalendar() {
        CalendarController ctrl = (CalendarController) mainContainer.getUserData();
        if (ctrl != null) ctrl.showCalendar();
    }
}
