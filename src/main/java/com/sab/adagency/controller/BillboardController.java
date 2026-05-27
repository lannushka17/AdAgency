package com.sab.adagency.controller;

import com.sab.adagency.persistence.contract.BillboardRepository;
import com.sab.adagency.persistence.entity.Billboard;
import com.sab.adagency.persistence.entity.BillboardSize;
import com.sab.adagency.persistence.entity.City;
import com.sab.adagency.persistence.implementation.BillboardRepositoryImpl;
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
 * Controller for Billboard CRUD operations.
 */
public class BillboardController implements Initializable {

    @FXML
    private TableView<Billboard> billboardTable;

    @FXML
    private TableColumn<Billboard, Integer> idColumn;

    @FXML
    private TableColumn<Billboard, String> codeColumn;

    @FXML
    private TableColumn<Billboard, String> addressColumn;

    @FXML
    private TableColumn<Billboard, String> cityColumn;

    @FXML
    private TableColumn<Billboard, String> sizeColumn;

    @FXML
    private TableColumn<Billboard, String> availableColumn;

    @FXML
    private TextField codeField;

    @FXML
    private TextField addressField;

    @FXML
    private ComboBox<City> cityComboBox;

    @FXML
    private ComboBox<BillboardSize> sizeComboBox;

    @FXML
    private CheckBox availableCheckBox;

    @FXML
    private ComboBox<String> cityFilterComboBox;

    @FXML
    private Button saveButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    private BillboardRepository billboardRepository;
    private ObservableList<Billboard> billboardData;
    private Billboard selectedBillboard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        billboardRepository = new BillboardRepositoryImpl();
        billboardData = FXCollections.observableArrayList();

        setupTableColumns();
        setupTableSelection();
        setupButtons();
        setupComboBoxes();
        setupFilters();
        loadBillboards();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));

        cityColumn.setCellValueFactory(cellData -> {
            City c = cellData.getValue().getCity();
            return new javafx.beans.property.SimpleStringProperty(c.getDisplayName());
        });

        sizeColumn.setCellValueFactory(cellData -> {
            BillboardSize s = cellData.getValue().getSize();
            return new javafx.beans.property.SimpleStringProperty(s.getDisplayName());
        });

        availableColumn.setCellValueFactory(cellData -> {
            boolean a = cellData.getValue().isAvailable();
            return new javafx.beans.property.SimpleStringProperty(a ? "✔ Вільний" : "✗ Зайнятий");
        });

        billboardTable.setItems(billboardData);
    }

    private void setupTableSelection() {
        billboardTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedBillboard = newValue;
            if (selectedBillboard != null) {
                populateFields(selectedBillboard);
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

    private void setupComboBoxes() {
        cityComboBox.getItems().addAll(City.values());
        sizeComboBox.getItems().addAll(BillboardSize.values());
    }

    private void setupFilters() {
        ObservableList<String> filterOptions = FXCollections.observableArrayList();
        filterOptions.add("Всі міста");
        for (City city : City.values()) {
            filterOptions.add(city.getDisplayName());
        }
        cityFilterComboBox.setItems(filterOptions);
        cityFilterComboBox.setValue("Всі міста");
        cityFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void applyFilters() {
        try {
            List<Billboard> all = billboardRepository.findAll();
            ObservableList<Billboard> filtered = FXCollections.observableArrayList();

            String cityFilter = cityFilterComboBox.getValue();
            for (Billboard b : all) {
                boolean matches = "Всі міста".equals(cityFilter)
                        || b.getCity().getDisplayName().equals(cityFilter);
                if (matches) {
                    filtered.add(b);
                }
            }

            billboardData.clear();
            billboardData.addAll(filtered);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося застосувати фільтри: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void populateFields(Billboard billboard) {
        codeField.setText(billboard.getCode());
        addressField.setText(billboard.getAddress());
        cityComboBox.setValue(billboard.getCity());
        sizeComboBox.setValue(billboard.getSize());
        availableCheckBox.setSelected(billboard.isAvailable());
    }

    private void clearFields() {
        codeField.clear();
        addressField.clear();
        cityComboBox.setValue(null);
        sizeComboBox.setValue(null);
        availableCheckBox.setSelected(true);
        selectedBillboard = null;
    }

    private void loadBillboards() {
        try {
            List<Billboard> billboards = billboardRepository.findAll();
            billboardData.clear();
            billboardData.addAll(billboards);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося завантажити білборди: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Billboard billboard = Billboard.builder()
                    .code(codeField.getText().trim())
                    .address(addressField.getText().trim())
                    .city(cityComboBox.getValue())
                    .size(sizeComboBox.getValue())
                    .available(availableCheckBox.isSelected())
                    .build();

            billboardRepository.save(billboard);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Білборд додано успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося зберегти білборд: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedBillboard == null || !validateInput()) {
            return;
        }

        try {
            Billboard updated = Billboard.builder()
                    .id(selectedBillboard.getId())
                    .code(codeField.getText().trim())
                    .address(addressField.getText().trim())
                    .city(cityComboBox.getValue())
                    .size(sizeComboBox.getValue())
                    .available(availableCheckBox.isSelected())
                    .build();

            billboardRepository.save(updated);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Білборд оновлено успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося оновити білборд: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedBillboard == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Видалити білборд");
        confirm.setContentText("Ви впевнені, що хочете видалити білборд '" + selectedBillboard.getCode() + "'?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                billboardRepository.deleteById(selectedBillboard.getId());
                applyFilters();
                clearFields();
                showAlert("Успіх", "Білборд видалено успішно!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Помилка", "Не вдалося видалити білборд: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleClear() {
        clearFields();
        billboardTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh() {
        applyFilters();
    }

    /**
     * Public method to refresh data - called when tab is switched.
     */
    public void refreshData() {
        loadBillboards();
        billboardTable.getSelectionModel().clearSelection();
        clearFields();
    }

    private boolean validateInput() {
        if (codeField.getText().trim().isEmpty()) {
            showAlert("Помилка валідації", "Код білборду обов'язковий!", Alert.AlertType.WARNING);
            return false;
        }
        if (addressField.getText().trim().isEmpty()) {
            showAlert("Помилка валідації", "Адреса обов'язкова!", Alert.AlertType.WARNING);
            return false;
        }
        if (cityComboBox.getValue() == null) {
            showAlert("Помилка валідації", "Місто обов'язкове!", Alert.AlertType.WARNING);
            return false;
        }
        if (sizeComboBox.getValue() == null) {
            showAlert("Помилка валідації", "Формат білборду обов'язковий!", Alert.AlertType.WARNING);
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
