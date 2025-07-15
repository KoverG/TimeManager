package app.views.custom;

import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

public class RoundedButton extends Button {
    public RoundedButton(String text) {
        super(text);
        setStyle("-fx-background-radius: 15; -fx-padding: 5px 15px;");
    }

    public void setColor(String hexColor) {
        setBackground(new Background(new BackgroundFill(
                Color.web(hexColor),
                new CornerRadii(15),
                null
        )));
        setTextFill(Color.WHITE);
    }

    public void setHoverColor(String hexColor) {
        setOnMouseEntered(e -> setBackground(new Background(new BackgroundFill(
                Color.web(hexColor),
                new CornerRadii(15),
                null
        ))));

        setOnMouseExited(e -> setBackground(new Background(new BackgroundFill(
                Color.web("#e74c3c"),
                new CornerRadii(15),
                null
        ))));
    }
}
