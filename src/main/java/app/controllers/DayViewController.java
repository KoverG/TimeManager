package app.controllers;

import app.models.DayData;
import app.models.TimeEntry;
import app.services.JsonService;
import app.services.ProductionCalendarService;
import app.utils.CalendarCellStyleManager;
import app.utils.DateHelper;
import app.utils.TimeZoneManager;
import app.utils.UIHelper;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.util.Pair;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import app.controllers.DayType;
import javafx.geometry.Pos;
import javafx.geometry.Insets;


public class DayViewController {

    public DayViewController() {
        System.out.println(">>> КОНСТРУКТОР DayViewController вызван");
    }

    private static final Logger logger = Logger.getLogger(DayViewController.class.getName());  // Создаем логгер

    @FXML private StackPane hoursRemainingContainer;
    @FXML
    private Label hoursRemainingLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private ComboBox<String> dayTypeCombo;
    @FXML
    private TextField workHoursField;
    @FXML
    private VBox timeSlotsContainer;
    @FXML
    private VBox mainContainer;
    @FXML
    private Button saveButton;
    @FXML
    private Label saveSuccessIcon; // Галочка
    @FXML
    private ProgressIndicator saveProgress;
    @FXML private Label currentTimeLabel;
    @FXML private ComboBox<TimeZoneManager.ZoneItem> timeZoneCombo;

    @FXML private HBox tzBox;
    @FXML private Region progressSpacer;

    @FXML private StackPane globalProgressContainer;
    @FXML private Region    globalProgressBg;
    @FXML private Region    globalProgressFill;
    @FXML private HBox      slotsWrapper;
    @FXML private ScrollPane scrollPane;
    @FXML private StackPane  slotsRoot;
    @FXML private StackPane saveStatusContainer;


    private LocalDate currentDate;
    private DayData dayData;
    private Map<String, DayData> allData;
    private int defaultWorkHours;
    private Map<String, TimeEntry> timeEntries;
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private boolean isClearButtonPressed = false;  // Флаг для отслеживания нажатия кнопки "Очистить"
    private int originalDefaultWorkHours;
    // Храним все строки временных слотов
    private final List<TimeSlotRow> slotRows = new ArrayList<>();
    private final TimeZoneManager tzManager = new TimeZoneManager();
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private Timeline clock;

    private final DoubleProperty dayProgress = new SimpleDoubleProperty(0);
    // Кешируем границы дня и шаг
    private LocalTime dayStart;
    private LocalTime dayEnd;
    private int       slotStepMinutes;

    public void setMainContainer(VBox mainContainer) {
        this.mainContainer = mainContainer;
    }

    private void bindGlobalProgressBar() {
        globalProgressBg.prefHeightProperty().bind(timeSlotsContainer.heightProperty());
        // Новый подход (spacer сверху, fill снизу)
        progressSpacer.prefHeightProperty().bind(globalProgressBg.heightProperty().multiply(1.0 - dayProgress.get()));
        globalProgressFill.prefHeightProperty().bind(globalProgressBg.heightProperty().multiply(dayProgress.get()));

        // Для обновления при изменении dayProgress:
        dayProgress.addListener((obs, oldV, newV) -> {
            progressSpacer.prefHeightProperty().bind(globalProgressBg.heightProperty().multiply(1.0 - newV.doubleValue()));
            globalProgressFill.prefHeightProperty().bind(globalProgressBg.heightProperty().multiply(newV.doubleValue()));
        });
    }

    @FXML
    public void initialize() {
        System.err.println("=== initialize ===");
        System.err.println("globalProgressContainer = " + globalProgressContainer);
        System.err.println("globalProgressBg = " + globalProgressBg);
        System.err.println("globalProgressFill = " + globalProgressFill);
        System.err.println("timeSlotsContainer = " + timeSlotsContainer);
        System.out.println(">>> DayViewController.initialize() called");
        saveStatusContainer.setVisible(false);
        saveSuccessIcon.setVisible(false); // Галочка скрыта изначально
        saveButton.disableProperty().bind(isDirty.not());
        dayTypeCombo.valueProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((o, ov, nv) -> markDirty());
        workHoursField.textProperty().addListener((obs, oldValue, newValue) -> updateHoursRemaining());
        dayTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateHoursRemaining());


        timeZoneCombo.setItems(tzManager.getZoneItems());

        // Автодобавление системной зоны, если её нет
        ZoneId sys = ZoneId.systemDefault();
        TimeZoneManager.ZoneItem def = tzManager.ensureZone(sys);
        timeZoneCombo.getSelectionModel().select(def);

        // Компактное отображение: только код
        timeZoneCombo.setVisibleRowCount(5);
        timeZoneCombo.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(TimeZoneManager.ZoneItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code());
            }
        });


        timeZoneCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(TimeZoneManager.ZoneItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.code());
            }
        });

        // Высота комбо = высоте часов
        timeZoneCombo.prefHeightProperty().bind(currentTimeLabel.heightProperty());
        timeZoneCombo.minHeightProperty().bind(currentTimeLabel.heightProperty());
        timeZoneCombo.maxHeightProperty().bind(currentTimeLabel.heightProperty());

        // Обновление часов при смене TZ
        timeZoneCombo.valueProperty().addListener((obs, o, n) -> updateClock());

        // Таймер
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            logger.info("Timer tick → updateClock()");             // ⭐ новый лог
            updateClock();
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        updateClock();
        attachHideArrow();

        // Привязка глобального прогресс-бара
        bindGlobalProgressBar();
        StackPane.setAlignment(globalProgressFill, Pos.TOP_CENTER);

        timeSlotsContainer.getChildren().addListener((ListChangeListener<Node>) c -> {
            recalcDayBounds();
            updateGlobalProgress();
        });

        // ЛОГ: слушаем изменения dayProgress
        dayProgress.addListener((obs, oldV, newV) ->
                logger.info(String.format(
                        "dayProgress listener → old=%.4f, new=%.4f",
                        oldV.doubleValue(), newV.doubleValue()
                ))
        );

        // ЛОГ: слушаем получение высоты контейнера фона
        globalProgressBg.heightProperty().addListener((obs, oldH, newH) ->
                logger.info(String.format(
                        "globalProgressBg.heightProperty → old=%.1fpx, new=%.1fpx",
                        oldH.doubleValue(), newH.doubleValue()
                ))
        );

        // Центрирование и фиксация ширины слотов
        scrollPane.viewportBoundsProperty().addListener((obs, o, vb) ->
                slotsRoot.setPrefWidth(vb.getWidth()));

        Platform.runLater(() -> {
            double w = slotsWrapper.prefWidth(-1);
            slotsWrapper.setPrefWidth(w);
            slotsWrapper.setMinWidth(w);
            slotsWrapper.setMaxWidth(w);
        });
    }

    private void recalcDayBounds() {
        if (slotRows.isEmpty()) {
            dayStart = LocalTime.MIN;
            dayEnd   = LocalTime.MAX;
            slotStepMinutes = 10; // запасной вариант
            return;
        }

        dayStart = slotRows.get(0).getTime(); // или твой геттер времени слота
        LocalTime second = slotRows.size() > 1 ? slotRows.get(1).getTime() : dayStart.plusMinutes(10);
        slotStepMinutes = (int) Math.max(1, java.time.Duration.between(dayStart, second).toMinutes());
        dayEnd = slotRows.get(slotRows.size() - 1).getTime().plusMinutes(slotStepMinutes);
    }

    private void updateGlobalProgress() {
        logger.info("updateGlobalProgress → entering");            // ⭐ ещё более ранний лог
        ZoneId zone = getCurrentZoneId();
        LocalTime now = LocalTime.now(zone);

        logger.info(String.format(
                "  [pre-calc] now=%s, dayStart=%s, dayEnd=%s, rows=%d",
                now, dayStart, dayEnd, slotRows.size()
        ));

        if (slotRows.isEmpty()) {
            logger.info("  slotRows empty → dayProgress=0");
            dayProgress.set(0);
            return;
        }

        long totalMs = java.time.Duration.between(dayStart, dayEnd).toMillis();
        long passedMs = now.isBefore(dayStart)
                ? 0
                : now.isBefore(dayEnd)
                ? java.time.Duration.between(dayStart, now).toMillis()
                : totalMs;

        logger.info(String.format("  Durations → totalMs=%d, passedMs=%d", totalMs, passedMs));

        double frac = totalMs > 0 ? (double)passedMs / totalMs : 0;
        logger.info(String.format("  Computed frac=%.4f", frac));

        dayProgress.set(frac);

        double bgH = globalProgressBg.getHeight();
        double fillH = globalProgressFill.getHeight();
        double calcH = bgH * frac;
        logger.info(String.format(
                "  [post-bind] bgH=%.1fpx, fillH=%.1fpx, expected=%.1fpx",
                bgH, fillH, calcH
        ));
    }




    private ZoneId getCurrentZoneId() {
        TimeZoneManager.ZoneItem item = timeZoneCombo.getValue();
        return item != null ? item.zoneId() : ZoneId.systemDefault();
    }


    private void attachHideArrow() {
        // 1-й заход — после построения сцены
        Platform.runLater(() -> tryHideArrow(timeZoneCombo, "Platform.runLater"));

        // На случай пересоздания скина
        timeZoneCombo.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            logger.info("[TZ] skin changed -> tryHideArrow");
            Platform.runLater(() -> tryHideArrow(timeZoneCombo, "skinListener"));
        });
    }

    private void tryHideArrow(ComboBox<?> combo, String from) {
        Region arrowBtn = (Region) combo.lookup(".arrow-button");
        if (arrowBtn == null) {
            logger.warning("[TZ] (" + from + ") .arrow-button NOT found");
            return;
        }

        logger.info("[TZ] (" + from + ") hide arrow. before: vis=" + arrowBtn.isVisible()
                + " managed=" + arrowBtn.isManaged() + " w=" + arrowBtn.getWidth());

        arrowBtn.setVisible(false);
        arrowBtn.setManaged(false);
        arrowBtn.setMouseTransparent(true);
        arrowBtn.setPrefSize(0, 0);
        arrowBtn.setMinSize(0, 0);
        arrowBtn.setMaxSize(0, 0);

        Region arrowShape = (Region) arrowBtn.lookup(".arrow");
        if (arrowShape != null) {
            arrowShape.setVisible(false);
            arrowShape.setManaged(false);
        }

        logger.info("[TZ] (" + from + ") after: vis=" + arrowBtn.isVisible()
                + " managed=" + arrowBtn.isManaged());
    }


    private void dumpChildren(Parent root, int level) {
        String indent = "  ".repeat(level);
        logger.info(indent + root.getClass().getSimpleName() +
                " id=" + root.getId() +
                " styleClass=" + root.getStyleClass());
        for (Node n : root.getChildrenUnmodifiable()) {
            if (n instanceof Parent) {
                dumpChildren((Parent) n, level + 1);
            } else {
                logger.info(indent + "  " + n.getClass().getSimpleName() +
                        " id=" + n.getId() +
                        " styleClass=" + n.getStyleClass());
            }
        }
    }


    private void updateClock() {
        logger.info("updateClock → entering");                     // ⭐ новый лог
        ZoneId zone = getCurrentZoneId();
        currentTimeLabel.setText(LocalTime.now(zone).format(TIME_FMT));
        updateGlobalProgress();
    }

    private void markDirty() {
        isDirty.set(true);
    }

    private void updateViewForEmptyDay(DayData resetDayData, int workHours, DayType type) {
        // 1) Обновляем модель
        this.dayData = resetDayData;
        // 2) Обновляем контр. UI
        dayTypeCombo.getSelectionModel().select(type.getType());
        workHoursField.setText(String.valueOf(workHours));
        // 3) Пересоздаем слоты по новому dayData
        this.timeEntries = createSortedTimeEntries();
        slotRows.clear();
        timeSlotsContainer.getChildren().clear();
        timeEntries.forEach((t, e) -> timeSlotsContainer.getChildren().add(createTimeSlotRow(t, e)));
        // 4) Разблокируем кнопку «Сохранить»
        markDirty();
        recalcHighlights();
        recalcDayBounds();
        updateGlobalProgress();
    }

    // Инициализация dataRef
    private final AtomicReference<Map<String, DayData>> dataRef = new AtomicReference<>(new HashMap<>());

    public void previousDay() {
        logger.info("Attempting to go to the previous day. Current date: " + currentDate);

        LocalDate previousDate = currentDate.minusDays(1);
        logger.info("Getting day type for previous day: " + previousDate);

        DayData previousDayData = getDayDataForDate(previousDate);
        DayType previousDayType = getDayTypeForDate(previousDate);

        // Получаем стандартные рабочие часы для нового типа дня
        int defaultWorkHoursForNewDay = previousDayData.calculateWorkHoursByDayType(previousDayType, 9);

        // Устанавливаем данные для предыдущего дня с правильным типом и рабочими часами
        setDayData(previousDate, previousDayData, defaultWorkHoursForNewDay, previousDayType);
    }

    public void nextDay() {
        logger.info("Attempting to go to the next day. Current date: " + currentDate);

        LocalDate nextDate = currentDate.plusDays(1);
        logger.info("Getting day type for next day: " + nextDate);

        DayData nextDayData = getDayDataForDate(nextDate);
        DayType nextDayType = getDayTypeForDate(nextDate);

        // Получаем стандартные рабочие часы для нового типа дня
        int defaultWorkHoursForNewDay = nextDayData.calculateWorkHoursByDayType(nextDayType, 9);

        // Устанавливаем данные для следующего дня с правильным типом и рабочими часами
        setDayData(nextDate, nextDayData, defaultWorkHoursForNewDay, nextDayType);
    }



    public void setDayData(LocalDate date, DayData data, int workHours, DayType defaultDayType) {
        logger.info("setDayData called for date: " + date);

        this.currentDate = date;
        this.dayData = (data != null) ? data : new DayData();
        this.defaultWorkHours = workHours;
        this.originalDefaultWorkHours = workHours;

        logger.info("Loaded DayData for date: " + date + ", dayType: " + this.dayData.getDayType());
        logger.info("Set work hours: " + workHours);
        logger.info("Set original work hours: " + workHours);

        loadDayDataFromJson();  // Загружаем данные из JSON

        // Если данные по дате пусты, задаем тип дня по умолчанию, полученный из календаря
        if (this.dayData == null || this.dayData.getDayType() == null) {
            this.dayData.setDayType(defaultDayType);  // Используем тип дня из календаря
            logger.info("Day type is null, setting default: " + defaultDayType);
        }

        logger.info("Day type before setting work hours: " + this.dayData.getDayType());

        // Проверяем, если рабочие часы уже установлены в данных, используем их
        logger.info("Work day hours before checking: " + this.dayData.getWorkDayHours());
        if (this.dayData.getWorkDayHours() < 0) {
            // Если в данных нет рабочих часов, вычисляем их в зависимости от типа дня
            logger.info("Calling calculateWorkHoursByDayType for dayType: " + this.dayData.getDayType() + " and defaultWorkHours: " + defaultWorkHours);
            int workHoursForDay = this.dayData.calculateWorkHoursByDayType(this.dayData.getDayType(), defaultWorkHours);
            this.dayData.setWorkDayHours(workHoursForDay);  // Сохраняем вычисленные рабочие часы в объект
            workHoursField.setText(String.valueOf(workHoursForDay));  // Устанавливаем вычисленные рабочие часы в UI
            logger.info("Calculated work hours for day: " + workHoursForDay);
        } else {
            // Используем уже загруженные рабочие часы из данных
            workHoursField.setText(String.valueOf(this.dayData.getWorkDayHours()));
            logger.info("Work hours set from existing data: " + this.dayData.getWorkDayHours());
        }

        // Сохраняем изменения рабочего времени обратно в DayData
        dayData.setWorkDayHours(Integer.parseInt(workHoursField.getText()));  // Сохраняем рабочие часы
        logger.info("Work hours set to: " + workHoursField.getText());

        timeEntries = createSortedTimeEntries(); // Создание стандартных временных слотов
        updateUIWithData(); // Обновление UI после загрузки данных

        logger.info("Day data successfully set for date: " + date);
        logger.info("Work hours field value after UI update: " + workHoursField.getText());

        dayTypeCombo.valueProperty().addListener((obs, oldType, newType) -> {
            logger.info("DayType changed from " + oldType + " to " + newType);

            DayType selectedType = getDayTypeFromString(newType);
            logger.info("Selected DayType: " + selectedType);


            logger.info("Проверка на то что originalDefaultWorkHours == 0: " + originalDefaultWorkHours);
                        // Если originalDefaultWorkHours == 0, установим стандартное значение (например, 9)
            if (originalDefaultWorkHours >= 0) {
                originalDefaultWorkHours = 9;
            }
            logger.info("Проверка что originalDefaultWorkHours поменялся на 9: " + originalDefaultWorkHours);

            // Пересчитываем рабочие часы для нового типа дня
            int recalculatedWorkHours = dayData.calculateWorkHoursByDayType(selectedType, originalDefaultWorkHours);
            logger.info("Recalculated work hours: " + recalculatedWorkHours);

            workHoursField.setText(String.valueOf(recalculatedWorkHours));  // Обновляем поле с рабочими часами
            logger.info("Work hours field updated with recalculated value: " + recalculatedWorkHours);

            // Обновляем тип дня и рабочие часы в dayData
            dayData.setDayType(selectedType);
            dayData.setWorkDayHours(recalculatedWorkHours); // Сохраняем новые рабочие часы

            markDirty();  // Устанавливаем флаг изменения
        });
    }




    private void loadDayDataFromJson() {
        logger.info("Attempting to load data from JSON.");
        try {
            // Загружаем все данные из JSON (в первую очередь смотрим в time_manager_data.json)
            this.allData = JsonService.getData();

            // Если данных нет в time_manager_data.json, пробуем загрузить из calendar_текущий год.json
            if (allData.isEmpty()) {
                logger.warning("allData is empty. No data available in time_manager_data.json.");
                String currentYear = String.valueOf(currentDate.getYear());
                this.allData = JsonService.loadCalendarData(currentYear);  // Загружаем данные из calendar_текущий год.json
            }

            // Логируем полученные данные
            for (Map.Entry<String, DayData> entry : allData.entrySet()) {
                logger.info("Loaded DayData: " + entry.getKey() + " -> " + entry.getValue().getDayType());
            }

        } catch (IOException e) {
            UIHelper.showError("Ошибка загрузки данных: " + e.getMessage());
            this.allData = new HashMap<>();
        }
    }


    private Map<String, DayData> loadCalendarData(String year) {
        // Формируем имя файла calendar_текущий год.json
        String calendarFileName = "data/calendar_" + year + ".json";
        Path calendarPath = Paths.get(calendarFileName);

        // Проверка существования файла
        if (!Files.exists(calendarPath)) {
            logger.warning("File " + calendarPath.toAbsolutePath() + " not found. Returning empty data.");
            return new HashMap<>();  // Если файл не существует, возвращаем пустую карту
        }

        // Проверка, если файл пустой
        try {
            if (Files.size(calendarPath) == 0) {
                logger.warning("Calendar file is empty, creating new: " + calendarPath.toAbsolutePath());
                return new HashMap<>();  // Если файл пустой, возвращаем пустую карту
            }
        } catch (IOException e) {
            logger.severe("Error checking the size of the calendar file: " + calendarPath.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если произошла ошибка
        }

        // Логируем путь к файлу перед загрузкой
        logger.info("Attempting to load data from calendar file: " + calendarPath.toAbsolutePath());

        // Попытка десериализации данных из файла
        try {
            Map<String, DayData> data = JsonService.MAPPER.readValue(calendarPath.toFile(), JsonService.TYPE_REF);

            // Логируем результат десериализации
            if (data == null || data.isEmpty()) {
                logger.warning("The deserialized data from the calendar is empty.");
                return new HashMap<>();  // Если десериализация пустая, возвращаем пустую карту
            }

            // Убираем null значения
            data.entrySet().removeIf(entry -> entry.getValue() == null);
            logger.info("Final data after removing null values from the calendar: " + data);

            return data;
        } catch (IOException e) {
            logger.severe("Error reading or deserializing the calendar file: " + calendarPath.toAbsolutePath());
            return new HashMap<>();  // Возвращаем пустую карту, если произошла ошибка
        }
    }



    private TreeMap<String, TimeEntry> createSortedTimeEntries() {
        TreeMap<String, TimeEntry> sortedEntries = new TreeMap<>();
        LocalTime t = LocalTime.of(8, 0), end = LocalTime.of(17, 0);

        // Создаем стандартные временные слоты с шагом 10 минут
        while (!t.isAfter(end)) {
            String key = t.toString();
            TimeEntry entry = new TimeEntry();  // По умолчанию создаем пустой объект
            sortedEntries.put(key, entry);
            t = t.plusMinutes(10); // Шаг 10 минут
        }

        // Заполняем стандартные слоты данными, если они есть
        if (dayData != null && dayData.getTasks() != null) {
            dayData.getTasks().forEach((timeKey, entry) -> {
                // Обновляем данные для конкретного временного слота
                if (sortedEntries.containsKey(timeKey)) {
                    sortedEntries.put(timeKey, entry);
                }
            });
        }

        return sortedEntries;
    }

    private List<Integer> generateTimeValues() {
        List<Integer> timeValues = new ArrayList<>();
        for (int i = 0; i < 60; i += 10) {  // Шаг 10 минут
            timeValues.add(i);
        }
        return timeValues;
    }

    private void updateUIWithData() {
        logger.info("Updating UI with data...");
        logger.info("Current work hours field value before UI update: " + workHoursField.getText());  // Логируем значение перед обновлением UI

        // Обновляем метку с текущей датой
        dateLabel.setText(DateHelper.formatDisplayDate(currentDate));

        // Получаем тип дня
        DayType dayTypeEnum = dayData.getDayType();  // Это теперь DayType, а не строка
        System.out.println("Loaded DayType for the day: " + dayTypeEnum);

        // Обновляем комбобокс с типами дней, используя DayType
        dayTypeCombo.setItems(FXCollections.observableArrayList(
                DayType.WORKDAY.getType(),  // Рабочий день
                DayType.WEEKEND.getType(),  // Выходной день
                DayType.SHORT.getType()  // Сокращенный день
        ));

// Устанавливаем правильное значение комбобокса в зависимости от типа дня
        if (DayType.WORKDAY.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WORKDAY.getType());  // Рабочий день
        } else if (DayType.WEEKEND.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        } else if (DayType.SHORT.equals(dayTypeEnum)) {
            dayTypeCombo.getSelectionModel().select(DayType.SHORT.getType());  // Сокращённый день
        } else {
            // Если тип дня не совпадает, ставим выходной по умолчанию
            dayTypeCombo.getSelectionModel().select(DayType.WEEKEND.getType());  // Выходной день
        }

        // Логируем значение рабочих часов из dayData перед установкой
        logger.info("Work day hours from DayData before setting: " + dayData.getWorkDayHours());

        int workHoursToDisplay = dayData.getWorkDayHours() > 0 ? dayData.getWorkDayHours() : defaultWorkHours;
        workHoursField.setText(String.valueOf(workHoursToDisplay));

        logger.info("Work hours field value after setting: " + workHoursField.getText());

        slotRows.clear();
        timeSlotsContainer.getChildren().clear();

        timeEntries.forEach((timeKey, entry) -> timeSlotsContainer.getChildren().add(createTimeSlotRow(timeKey, entry)));

        isDirty.set(false);
        updateHoursRemaining();
        recalcHighlights();

        // Вот тут уже можно пересчитывать прогресс!
        recalcDayBounds();
        updateGlobalProgress();
        System.err.println("[updateUIWithData] slotRows.size=" + slotRows.size() +
                " dayStart=" + dayStart + " dayEnd=" + dayEnd);
    }





    private HBox createTimeSlotRow(String time, TimeEntry entry) {
        // NEW: распарсили базовое время слота
        LocalTime slotTime = LocalTime.parse(
                time, DateTimeFormatter.ofPattern("HH:mm")
        );

        HBox row = new HBox(5);
        row.getStyleClass().add("main-banner-day-string");
        row.setMaxWidth(950);
        row.setPadding(new Insets(0, 10, 0, 10));
        row.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(time);
        timeLabel.getStyleClass().add("time-slot-label");
        timeLabel.setPrefWidth(45);
        timeLabel.setMinWidth(45);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);

        TextField taskField = new TextField(entry.getTask());
        taskField.setPromptText("Задача");
        taskField.getStyleClass().add("custom-combo-mod");
        taskField.setPrefSize(320, 25);

        ComboBox<Integer> hoursCombo = new ComboBox<>(FXCollections.observableArrayList(0, 1, 2, 3, 4, 5, 6, 7, 8));
        hoursCombo.getStyleClass().add("combo-container-withoutRadius");
        hoursCombo.setStyle("-fx-background-radius:12 0 0 12; -fx-border-radius:12 0 0 12;");
        hoursCombo.setPrefSize(65, 30);

        ComboBox<Integer> minutesCombo = new ComboBox<>();
        minutesCombo.getStyleClass().add("combo-container-withoutRadius");
        minutesCombo.setStyle("-fx-background-radius:0 12 12 0; -fx-border-radius:0 12 12 0;");
        minutesCombo.setPrefSize(65, 30);

        // Генерация значений для minutesCombo с шагом 5 минут от 0 до 55
        minutesCombo.setItems(FXCollections.observableArrayList(generateTimeValues()));

        TextField commentField = new TextField(entry.getComment());
        commentField.setPromptText("Комментарий");
        commentField.getStyleClass().add("custom-combo-mod");
        commentField.setPrefSize(370, 25);

        CheckBox completedCheck = new CheckBox();
        completedCheck.setSelected(entry.isCompleted());
        completedCheck.getStyleClass().add("jira-checkbox");

        row.getChildren().addAll(timeLabel, taskField, hoursCombo, minutesCombo, commentField, completedCheck);

        slotRows.add(new TimeSlotRow(
                slotTime, row, hoursCombo, minutesCombo
        ));



        // Устанавливаем значения для комбобоксов
        try {
            Integer hVal = Integer.valueOf(entry.getHours());
            hoursCombo.getSelectionModel().select(hoursCombo.getItems().contains(hVal) ? hVal : 0);
        } catch (Exception ex) {
            hoursCombo.getSelectionModel().select(0);  // Если значение не найдено, выберем 0
        }

        try {
            Integer mVal = Integer.valueOf(entry.getMinutes());
            minutesCombo.getSelectionModel().select(
                    minutesCombo.getItems().contains(mVal) ? minutesCombo.getItems().indexOf(mVal) : 0
            );
        } catch (Exception ex) {
            minutesCombo.getSelectionModel().select(0);  // Если значение не найдено, выберем 0
        }

        // Обработчики изменений
        taskField.textProperty().addListener((o, ov, nv) -> {
            entry.setTask(nv);
            markDirty();
        });

        commentField.textProperty().addListener((o, ov, nv) -> {
            entry.setComment(nv);
            markDirty();
        });

        completedCheck.selectedProperty().addListener((o, ov, nv) -> {
            entry.setCompleted(nv);
            markDirty();
        });

        // Обработчики изменений с подсветкой диапазона
        hoursCombo.valueProperty().addListener((o, ov, nv) -> {
            entry.setHours(nv != null ? nv.toString() : "0");
            markDirty();
            updateHoursRemaining();
            // NEW: обновляем подсветку
            recalcHighlights();
        });

        minutesCombo.valueProperty().addListener((o, ov, nv) -> {
            entry.setMinutes(nv != null ? nv.toString() : "0");
            markDirty();
            updateHoursRemaining();
            // NEW: обновляем подсветку
            recalcHighlights();
        });


        return row;
    }

    private void highlightRange(LocalTime startTime, int addedH, int addedM) {
        // 1) Сняли со всех
        slotRows.forEach(r -> r.row.getStyleClass().remove("range-selected"));

        // 2) Если ноль — выходим
        if (addedH == 0 && addedM == 0) return;

        // 3) Вычисляем конец диапазона
        LocalTime endTime = startTime.plusHours(addedH).plusMinutes(addedM);

        // 4) Проходим по всем строкам и добавляем класс тем, кто в диапазоне
        for (TimeSlotRow r : slotRows) {
            if (!r.time.isBefore(startTime) && r.time.isBefore(endTime)) {
                r.row.getStyleClass().add("range-selected");
            }
        }
    }

    private void recalcHighlights() {
        // 1) Снимаем на всякий случай старый highlight
        slotRows.forEach(r ->
                r.row.getStyleClass().remove("range-selected")
        );

        // 2) Собираем все диапазоны
        List<Pair<LocalTime, LocalTime>> ranges = new ArrayList<>();
        for (TimeSlotRow tsr : slotRows) {
            Integer h = tsr.hoursCombo.getValue();
            Integer m = tsr.minutesCombo.getValue();
            if (h != null && m != null && (h != 0 || m != 0)) {
                LocalTime start = tsr.time;
                LocalTime end = start.plusHours(h).plusMinutes(m);
                ranges.add(new Pair<>(start, end));
            }
        }

        // 3) Для каждой строки — проверить, попадает ли её время хотя бы в один диапазон
        for (TimeSlotRow tsr : slotRows) {
            for (Pair<LocalTime, LocalTime> rg : ranges) {
                if (!tsr.time.isBefore(rg.getKey()) && tsr.time.isBefore(rg.getValue())) {
                    tsr.row.getStyleClass().add("range-selected");
                    break; // больше не нужно проверять остальные диапазоны
                }
            }
        }
    }


    private DayType getDayTypeForDate(LocalDate date) {
        // Сначала проверяем, является ли день сокращенным
        if (ProductionCalendarService.isShortDay(date)) {
            return DayType.SHORT;  // Сокращенный рабочий день (даже если он в holidays)
        }
        // Затем проверяем, является ли день праздником/выходным
        if (ProductionCalendarService.isHoliday(date)) {
            return DayType.WEEKEND;  // Праздник — выходной день
        }
        // Если не сокращенный и не праздник, проверяем субботу/воскресенье
        if (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;  // Выходной
        }
        // В остальных случаях — рабочий день
        return DayType.WORKDAY;
    }

    private DayData getDayDataForDate(LocalDate date) {
        logger.info("Searching for DayData with dateKey: " + date);
        // Преобразуем дату в строку для поиска в allData
        String dateKey = DateHelper.formatDate(date);

        // Логируем ключ для поиска
        logger.info("Searching for DayData with dateKey: " + dateKey);

        // Проверяем, есть ли данные для этой даты в allData
        DayData data = allData.get(dateKey);

        if (data == null) {
            // Логируем, если данных для данной даты нет
            logger.warning("No DayData found for date: " + date + ", creating new DayData.");

            // Проверяем тип дня из календаря (если он не найден, устанавливаем тип по умолчанию)
            DayType dayType = getDayTypeForDate(date);

            // Создаем новый объект DayData и задаем правильный тип дня
            data = new DayData();
            data.setDayType(dayType);
        }

        return data;
    }



    @FXML
    private void saveDay() {
        // Показываем индикатор загрузки, скрываем галочку
        saveProgress.setVisible(true);
        saveSuccessIcon.setVisible(false);
        saveProgress.setStyle("-fx-progress-color: #FF3C00;");

        try {
            // Проверяем и парсим количество рабочих часов
            int wh = Integer.parseInt(workHoursField.getText());
            if (wh < 0 || wh > 24) throw new NumberFormatException();

            // Устанавливаем данные
            dayData.setWorkDayHours(wh);

            // Получаем выбранный тип дня из комбобокса и безопасно преобразуем его в DayType
            String selectedDayTypeString = dayTypeCombo.getSelectionModel().getSelectedItem();
            System.out.println("Selected DayType from ComboBox before save: " + selectedDayTypeString);  // Логируем выбранный тип дня из комбобокса

            DayType selectedDayType = getDayTypeFromString(selectedDayTypeString);
            System.out.println("Converted DayType: " + selectedDayType);  // Логируем, какой DayType был преобразован

            // Логируем значение типа дня перед сохранением
            System.out.println("Setting DayType in DayData: " + selectedDayType);
            dayData.setDayType(selectedDayType);  // Устанавливаем DayType

            // Логируем день после установки типа
            System.out.println("DayType after setting in dayData: " + dayData.getDayType());

            // Устанавливаем задачи
            dayData.setTasks(timeEntries);

            // Обновляем данные в allData
            allData.put(DateHelper.formatDate(currentDate), dayData);

            // Имитация задержки для демонстрации индикатора загрузки
            PauseTransition fakeLoad = createFakeLoad();
            fakeLoad.setOnFinished(ev -> {
                try {
                    // Прежде чем сохранить, убедимся, что данные корректны
                    if (allData.isEmpty()) {
                        saveProgress.setVisible(false);
                        UIHelper.showError("Нет данных для сохранения.");
                        return;
                    }

                    // Логируем данные перед сохранением
                    for (Map.Entry<String, DayData> entry : allData.entrySet()) {
                        System.out.println("DayData before save: " + entry.getKey() + " -> " + entry.getValue().getDayType());
                    }

                    if (isClearButtonPressed) {
                        // Удаляем только объект за текущий день
                        String key = DateHelper.formatDate(currentDate);
                        allData.remove(key);
                        JsonService.saveData(allData);
                        isClearButtonPressed = false;
                        logger.info("Данные за день " + key + " удалены из time_manager_data.json.");
                    } else {
                        JsonService.saveData(allData);
                    }


                    handleSaveSuccess(); // Обрабатываем успешное сохранение
                } catch (IOException e) {
                    saveProgress.setVisible(false);
                    UIHelper.showError("Ошибка при сохранении данных: " + e.getMessage());
                }
            });
            fakeLoad.play();

        } catch (NumberFormatException e) {
            saveProgress.setVisible(false);
            UIHelper.showError("Некорректное количество рабочих часов.\nВведите число от 1 до 24.");
        }
    }



    // Метод для безопасного преобразования строки в DayType
    private DayType getDayTypeFromString(String dayTypeString) {
        if (dayTypeString == null) {
            return DayType.WORKDAY;  // По умолчанию рабочий день
        }

        // Убираем возможные пробелы и приводим к нужному регистру
        return DayType.fromString(dayTypeString.trim());
    }



    private void handleSaveSuccess() {
        saveProgress.setVisible(false);
        saveSuccessIcon.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), saveSuccessIcon);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(evt -> saveSuccessIcon.setVisible(false));
            pause.play();
        });
        fadeIn.play();

        isDirty.set(false);
    }

    private PauseTransition createFakeLoad() {
        return new PauseTransition(Duration.millis(500));
    }

    @FXML
    private void clearDay() {
        logger.info("Нажата кнопка 'Очистить' для даты: " + currentDate);

        // 1. Создаем абсолютно новый DayData
        DayData reset = new DayData();

        // 2. Вычисляем тип дня и часы чисто по календарю
        DayType t = getDayTypeForDate(currentDate);
        int hrs = reset.calculateWorkHoursByDayType(t, originalDefaultWorkHours);
        logger.info("Часы для UI после очистки: " + hrs);

        // 3. Обновляем только UI (не трогая defaultWorkHours и слушатели)
        updateViewForEmptyDay(reset, hrs, t);
        logger.info("Интерфейс для пустого дня обновлен");

        // 4. Ставим флаг, чтобы saveDay() почистил JSON
        isClearButtonPressed = true;
        logger.info("isClearButtonPressed = true");
    }



    // Новый метод для форматирования времени
    private String formatTime(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    // Добавьте новый метод для обновления оставшихся часов
    private void updateHoursRemaining() {
        try {
            int totalWorkMinutes = Integer.parseInt(workHoursField.getText()) * 60;
            int enteredMinutes   = calculateEnteredMinutes();
            int remaining        = Math.max(0, totalWorkMinutes - enteredMinutes);

            // Обновляем текст
            hoursRemainingLabel.setText("Осталось времени: " + formatTime(remaining));

            // Вычисляем прогресс [0…1]
            double progress = (double)(totalWorkMinutes - remaining) / totalWorkMinutes;
            progress = Math.max(0, Math.min(progress, 1));

            if (progress <= 0) {
                hoursRemainingLabel.setText("Осталось времени: " + formatTime(remaining));
                hoursRemainingContainer.setStyle(
                        "-fx-border-width: 2px; " +
                                "-fx-border-radius: 12px; " +
                                "-fx-border-color: #ced4da;"
                );
                return;
            }

            // Вычисляем ширину перехода
            double transitionWidth = Math.min(0.1, 1 - progress);
            int pct            = (int)Math.round(progress * 100);           // точка перехода
            int transitionPct  = (int)Math.round(transitionWidth * 100);    // ширина перехода
            int endPct         = Math.min(100, pct + transitionPct);        // конец перехода

            // Цвета
            String usedColor    = CalendarCellStyleManager.getProgressColor(progress);
            String defaultColor = "#ced4da";

            // Градиент:
            //  - от 0% до pct% — сплошной usedColor
            //  - от pct% до endPct% — плавный переход usedColor→defaultColor
            //  - от endPct% до 100% — сплошной defaultColor
            hoursRemainingContainer.setStyle(String.format(
                    "-fx-border-width: 2px; " +
                            "-fx-border-radius: 12px; " +
                            "-fx-border-color: linear-gradient(" +
                            "from 0%% 0%% to 100%% 0%%, " +
                            "%s 0%%, " +   // usedColor at 0%
                            "%s %d%%, " +  // usedColor at pct%
                            "%s %d%%, " +  // defaultColor at endPct%
                            "%s 100%%" +   // defaultColor at 100%
                            ");",
                    usedColor,
                    usedColor,    pct,
                    defaultColor, endPct,
                    defaultColor
            ));

        } catch (NumberFormatException e) {
            hoursRemainingLabel.setText("Осталось времени: --:--");
            hoursRemainingContainer.setStyle("");
        }
    }


    // Метод для расчета введенных часов
    private int calculateEnteredMinutes() {
        int total = 0;
        if (timeEntries != null) {
            for (TimeEntry entry : timeEntries.values()) {
                try {
                    int hours = Integer.parseInt(entry.getHours());
                    int minutes = Integer.parseInt(entry.getMinutes());
                    total += hours * 60 + minutes;
                } catch (NumberFormatException e) {
                    // Игнорируем невалидные значения
                }
            }
        }
        return total;
    }

    @FXML
    private void backToCalendar() {
        CalendarController ctrl = (CalendarController) mainContainer.getUserData();
        if (ctrl != null) ctrl.showCalendar();
    }

    private static class TimeSlotRow {
        LocalTime time;
        HBox row;
        ComboBox<Integer> hoursCombo;
        ComboBox<Integer> minutesCombo;
        TimeSlotRow(LocalTime t, HBox r, ComboBox<Integer> h, ComboBox<Integer> m) {
            time = t; row = r; hoursCombo = h; minutesCombo = m;
        }
        LocalTime getTime() { return time; }
    }

}