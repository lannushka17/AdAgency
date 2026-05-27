package com.sab.adagency.controller;

import com.sab.adagency.persistence.contract.AppUserRepository;
import com.sab.adagency.persistence.entity.AppUser;
import com.sab.adagency.persistence.implementation.AppUserRepositoryImpl;
import com.sab.adagency.persistence.utility.PasswordUtil;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/** Контролер екрану авторизації. */
public class LoginController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private AppUserRepository userRepository;
    private Consumer<AppUser> loginSuccessHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userRepository = new AppUserRepositoryImpl();
    }

    public void setLoginSuccessHandler(Consumer<AppUser> handler) {
        this.loginSuccessHandler = handler;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Введіть логін і пароль.");
            return;
        }

        Optional<AppUser> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty() || !PasswordUtil.matches(password, userOpt.get().getPasswordHash())) {
            alert(Alert.AlertType.ERROR, "Невірний логін або пароль.");
            return;
        }

        if (loginSuccessHandler != null) {
            loginSuccessHandler.accept(userOpt.get());
        }
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
