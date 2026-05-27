package com.sab.adagency;

import com.sab.adagency.controller.LoginController;
import com.sab.adagency.controller.MainController;
import com.sab.adagency.persistence.entity.AppUser;
import com.sab.adagency.persistence.utility.PersistenceConfig;
import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        PersistenceConfig.initSchema();
        showLoginScene(stage);
    }

    private void showLoginScene(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/login-view.fxml"));
        Parent root = loader.load();
        LoginController controller = loader.getController();
        controller.setLoginSuccessHandler(user -> openMainScene(stage, user));

        Scene scene = new Scene(root, 660, 460);
        scene.getStylesheets().add(Main.class.getResource("/style/app.css").toExternalForm());

        stage.setTitle("AdAgency — Авторизація");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.show();
    }

    private void openMainScene(Stage stage, AppUser user) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/main-view.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setAuthenticatedUser(user);

            Scene scene = new Scene(root, 1240, 780);
            scene.getStylesheets().add(Main.class.getResource("/style/app.css").toExternalForm());

            stage.setTitle("AdAgency — управління рекламним агентством");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(660);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Не вдалося відкрити головне вікно", e);
        }
    }
}
