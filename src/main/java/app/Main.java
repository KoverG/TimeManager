// src/main/java/app/Main.java
package app;

import app.services.JsonService;
import app.utils.UIHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.net.URL;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleUncaughtException(throwable);
        });

        try {
            // NEW: Предварительная загрузка данных
            try {
                JsonService.loadData();
            } catch (Exception e) {
                System.err.println("Предварительная загрузка данных: " + e.getMessage());
            }

            URL fxmlResource = getClass().getResource("/fxml/calendar.fxml");
            if (fxmlResource == null) {
                throw new RuntimeException("FXML file not found: /fxml/calendar.fxml");
            }

            Parent root = FXMLLoader.load(fxmlResource);
            Scene scene = new Scene(root, 1200, 750);

            URL cssResource = getClass().getResource("/styles/main.css");
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                System.out.println("CSS file not found, continuing without styles");
            }

            primaryStage.setTitle("Time Manager");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (Exception e) {
            handleUncaughtException(e);
        }
    }

    private void handleUncaughtException(Throwable throwable) {
        System.err.println("Необработанное исключение: " + throwable.getMessage());
        throwable.printStackTrace();

        try (PrintWriter writer = new PrintWriter("error.log")) {
            throwable.printStackTrace(writer);
        } catch (Exception ex) {
            System.err.println("Failed to log error: " + ex.getMessage());
        }

        // Показать сообщение об ошибке пользователю
        Label errorLabel = new Label("Критическая ошибка:\n" + throwable.getMessage());
        Scene errorScene = new Scene(errorLabel, 600, 400);
        Stage errorStage = new Stage();
        errorStage.setTitle("Ошибка");
        errorStage.setScene(errorScene);
        errorStage.show();
    }

    @Override
    public void stop() {
        try {
            JsonService.saveData(JsonService.getData());
            System.out.println("Data saved successfully");
        } catch (Exception e) {
            System.err.println("Error saving data on exit: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
