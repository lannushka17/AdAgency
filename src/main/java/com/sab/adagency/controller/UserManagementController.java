package com.sab.adagency.controller;

import com.sab.adagency.persistence.contract.AppUserRepository;
import com.sab.adagency.persistence.entity.AppUser;
import com.sab.adagency.persistence.entity.UserRole;
import com.sab.adagency.persistence.implementation.AppUserRepositoryImpl;
import com.sab.adagency.persistence.utility.PasswordUtil;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/** Управління користувачами системи. */
public class UserManagementController implements Initializable {

    @FXML
    private TableView<AppUser> userTable;

    @FXML
    private TableColumn<AppUser, Integer> idColumn;

    @FXML
    private TableColumn<AppUser, String> usernameColumn;

    @FXML
    private TableColumn<AppUser, String> fullNameColumn;

    @FXML
    private TableColumn<AppUser, String> roleColumn;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField fullNameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<UserRole> roleComboBox;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    private final AppUserRepository userRepository = new AppUserRepositoryImpl();
    private final ObservableList<AppUser> data = FXCollections.observableArrayList();

    private AppUser selected;
    private AppUser currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getRole().getDisplayName()));

        roleComboBox.getItems().setAll(UserRole.values());
        userTable.setItems(data);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            selected = n;
            if (n == null) {
                clearFields();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
            } else {
                usernameField.setText(n.getUsername());
                fullNameField.setText(n.getFullName() == null ? "" : n.getFullName());
                passwordField.clear();
                roleComboBox.setValue(n.getRole());
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
            }
        });

        updateButton.setDisable(true);
        deleteButton.setDisable(true);
        loadUsers();
    }

    public void setCurrentUser(AppUser currentUser) {
        this.currentUser = currentUser;
    }

    public void refreshData() {
        loadUsers();
    }

    private void loadUsers() {
        try {
            data.setAll(userRepository.findAll());
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Не вдалося завантажити користувачів: " + ex.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        if (!validate(true)) {
            return;
        }
        String username = usernameField.getText().trim();
        Optional<AppUser> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            alert(Alert.AlertType.WARNING, "Користувач з таким логіном вже існує.");
            return;
        }
        AppUser user = AppUser.builder()
                .username(username)
                .fullName(fullNameField.getText() == null ? "" : fullNameField.getText().trim())
                .passwordHash(PasswordUtil.sha256(passwordField.getText()))
                .role(roleComboBox.getValue())
                .build();
        userRepository.save(user);
        loadUsers();
        clearFields();
        alert(Alert.AlertType.INFORMATION, "Користувача створено.");
    }

    @FXML
    private void handleUpdate() {
        if (selected == null || !validate(false)) {
            return;
        }
        String username = usernameField.getText().trim();
        Optional<AppUser> existing = userRepository.findByUsername(username);
        if (existing.isPresent() && existing.get().getId() != selected.getId()) {
            alert(Alert.AlertType.WARNING, "Користувач з таким логіном вже існує.");
            return;
        }
        String passwordHash = selected.getPasswordHash();
        if (passwordField.getText() != null && !passwordField.getText().isBlank()) {
            passwordHash = PasswordUtil.sha256(passwordField.getText());
        }
        AppUser updated = AppUser.builder()
                .id(selected.getId())
                .username(username)
                .fullName(fullNameField.getText() == null ? "" : fullNameField.getText().trim())
                .passwordHash(passwordHash)
                .role(roleComboBox.getValue())
                .build();
        userRepository.save(updated);
        loadUsers();
        clearFields();
        alert(Alert.AlertType.INFORMATION, "Дані користувача оновлено.");
    }

    @FXML
    private void handleDelete() {
        if (selected == null) {
            return;
        }
        if (currentUser != null && currentUser.getId() == selected.getId()) {
            alert(Alert.AlertType.WARNING, "Не можна видалити власний обліковий запис.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Видалити користувача");
        confirm.setContentText("Видалити користувача '" + selected.getUsername() + "'?");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            userRepository.deleteById(selected.getId());
            loadUsers();
            clearFields();
            alert(Alert.AlertType.INFORMATION, "Користувача видалено.");
        }
    }

    @FXML
    private void handleClear() {
        userTable.getSelectionModel().clearSelection();
        clearFields();
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    private boolean validate(boolean createMode) {
        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Логін обов'язковий.");
            return false;
        }
        if (createMode && (passwordField.getText() == null || passwordField.getText().isBlank())) {
            alert(Alert.AlertType.WARNING, "Пароль обов'язковий для нового користувача.");
            return false;
        }
        if (roleComboBox.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Роль обов'язкова.");
            return false;
        }
        return true;
    }

    private void clearFields() {
        usernameField.clear();
        fullNameField.clear();
        passwordField.clear();
        roleComboBox.setValue(null);
        selected = null;
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
