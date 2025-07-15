// src/main/java/app/Main.java
package app;

import app.services.JsonService;
import app.utils.UIHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Перехват любых необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> handleUncaughtException(throwable));

        try {
            // Предварительная загрузка данных
            try {
                JsonService.loadData();
            } catch (Exception e) {
                System.err.println("Ошибка при предварительной загрузке данных: " + e.getMessage());
            }

            // Загрузка FXML
            URL fxmlResource = getClass().getResource("/fxml/calendar.fxml");
            if (fxmlResource == null) {
                throw new RuntimeException("FXML не найден: /fxml/calendar.fxml");
            }
            Parent root = FXMLLoader.load(fxmlResource);

            // Подготовка сцены
            Scene scene = new Scene(root, 1200, 750);
            URL cssResource = getClass().getResource("/styles/main.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.out.println("CSS не найден, продолжаем без стилей");
            }

            primaryStage.setTitle("Time Manager");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            handleUncaughtException(e);
        }
    }

    private void handleUncaughtException(Throwable throwable) {
        // Логируем в консоль и файл
        System.err.println("Необработанное исключение: " + throwable.getMessage());
        throwable.printStackTrace();
        try (PrintWriter writer = new PrintWriter("error.log")) {
            throwable.printStackTrace(writer);
        } catch (Exception ex) {
            System.err.println("Не удалось записать лог: " + ex.getMessage());
        }
        // Показ диалога об ошибке в JavaFX‑потоке
        Platform.runLater(() ->
                UIHelper.showError("Критическая ошибка:\n" + throwable.getMessage())
        );
    }

    @Override
    public void stop() {
        try {
            JsonService.saveData(JsonService.getData());
            System.out.println("Данные успешно сохранены");
        } catch (Exception e) {
            System.err.println("Ошибка сохранения данных при выходе: " + e.getMessage());
            Platform.runLater(() ->
                    UIHelper.showError("Ошибка при сохранении данных: " + e.getMessage())
            );
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
