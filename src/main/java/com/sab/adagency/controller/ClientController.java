package com.sab.adagency.controller;

import com.sab.adagency.persistence.contract.ClientRepository;
import com.sab.adagency.persistence.contract.RentalContractRepository;
import com.sab.adagency.persistence.entity.Client;
import com.sab.adagency.persistence.entity.RentalContract;
import com.sab.adagency.persistence.implementation.ClientRepositoryImpl;
import com.sab.adagency.persistence.implementation.RentalContractRepositoryImpl;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for Client CRUD operations.
 */
public class ClientController implements Initializable {

    @FXML
    private TableView<Client> clientTable;

    @FXML
    private TableColumn<Client, Integer> idColumn;

    @FXML
    private TableColumn<Client, String> nameColumn;

    @FXML
    private TableColumn<Client, String> phoneColumn;

    @FXML
    private TableColumn<Client, String> emailColumn;

    @FXML
    private TableColumn<Client, String> vipColumn;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private CheckBox vipCheckBox;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> vipFilterComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    private ClientRepository clientRepository;
    private RentalContractRepository rentalContractRepository;
    private ObservableList<Client> clientData;
    private Client selectedClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clientRepository = new ClientRepositoryImpl();
        rentalContractRepository = new RentalContractRepositoryImpl();
        clientData = FXCollections.observableArrayList();

        setupTableColumns();
        setupTableSelection();
        setupButtons();
        setupFilters();
        loadClients();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));

        vipColumn.setCellValueFactory(cellData -> {
            boolean isVip = cellData.getValue().isVip();
            return new javafx.beans.property.SimpleStringProperty(isVip ? "★ VIP" : "—");
        });

        clientTable.setItems(clientData);
    }

    private void setupTableSelection() {
        clientTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedClient = newValue;
            if (selectedClient != null) {
                populateFields(selectedClient);
                updateButton.setDisable(false);
                deleteButton.setDisable(false);
            } else {
                clearFields();
                updateButton.setDisable(true);
                deleteButton.setDisable(true);
            }
        });
    }

    private void setupButtons() {
        updateButton.setDisable(true);
        deleteButton.setDisable(true);
    }

    private void setupFilters() {
        ObservableList<String> vipFilterOptions = FXCollections.observableArrayList();
        vipFilterOptions.addAll("Всі", "Тільки VIP", "Тільки звичайні");
        vipFilterComboBox.setItems(vipFilterOptions);
        vipFilterComboBox.setValue("Всі");

        vipFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        try {
            List<Client> all = clientRepository.findAll();
            ObservableList<Client> filtered = FXCollections.observableArrayList();

            String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            String vipFilter = vipFilterComboBox.getValue();

            for (Client c : all) {
                boolean matchesSearch = search.isEmpty()
                        || c.getName().toLowerCase().contains(search)
                        || (c.getPhone() != null && c.getPhone().toLowerCase().contains(search));
                boolean matchesVip = "Всі".equals(vipFilter)
                        || ("Тільки VIP".equals(vipFilter) && c.isVip())
                        || ("Тільки звичайні".equals(vipFilter) && !c.isVip());

                if (matchesSearch && matchesVip) {
                    filtered.add(c);
                }
            }

            clientData.clear();
            clientData.addAll(filtered);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося застосувати фільтри: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void populateFields(Client client) {
        nameField.setText(client.getName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
        vipCheckBox.setSelected(client.isVip());
    }

    private void clearFields() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        vipCheckBox.setSelected(false);
        selectedClient = null;
    }

    private void loadClients() {
        try {
            List<Client> clients = clientRepository.findAll();
            clientData.clear();
            clientData.addAll(clients);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося завантажити клієнтів: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Client client = Client.builder()
                    .name(nameField.getText().trim())
                    .phone(phoneField.getText().trim())
                    .email(emailField.getText().trim())
                    .vip(vipCheckBox.isSelected())
                    .build();

            clientRepository.save(client);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Клієнта додано успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося зберегти клієнта: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedClient == null || !validateInput()) {
            return;
        }

        try {
            Client updated = Client.builder()
                    .id(selectedClient.getId())
                    .name(nameField.getText().trim())
                    .phone(phoneField.getText().trim())
                    .email(emailField.getText().trim())
                    .vip(vipCheckBox.isSelected())
                    .build();

            clientRepository.save(updated);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Клієнта оновлено успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося оновити клієнта: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedClient == null) {
            return;
        }

        List<RentalContract> linkedContracts = rentalContractRepository.findByClientId(selectedClient.getId());
        if (!linkedContracts.isEmpty()) {
            showAlert(
                    "Видалення неможливе",
                    "У клієнта «" + selectedClient.getName() + "» є " + linkedContracts.size()
                            + " пов'язаний(их) договір(ів) оренди. Спочатку видаліть або перепризначте ці договори "
                            + "на вкладці «Договори оренди», а потім повторіть видалення клієнта.",
                    Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Видалити клієнта");
        confirm.setContentText("Ви впевнені, що хочете видалити клієнта '" + selectedClient.getName() + "'?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                clientRepository.deleteById(selectedClient.getId());
                applyFilters();
                clearFields();
                showAlert("Успіх", "Клієнта видалено успішно!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Помилка", "Не вдалося видалити клієнта: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleClear() {
        clearFields();
        clientTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh() {
        applyFilters();
    }

    /**
     * Public method to refresh data - called when tab is switched.
     */
    public void refreshData() {
        loadClients();
        clientTable.getSelectionModel().clearSelection();
        clearFields();
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showAlert("Помилка валідації", "Ім'я клієнта обов'язкове!", Alert.AlertType.WARNING);
            return false;
        }
        if (phoneField.getText().trim().isEmpty()) {
            showAlert("Помилка валідації", "Телефон обов'язковий!", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
