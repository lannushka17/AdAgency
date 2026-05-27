package com.sab.adagency.controller;

import com.sab.adagency.persistence.contract.BillboardRepository;
import com.sab.adagency.persistence.contract.ClientRepository;
import com.sab.adagency.persistence.contract.RentalContractRepository;
import com.sab.adagency.persistence.entity.Billboard;
import com.sab.adagency.persistence.entity.Client;
import com.sab.adagency.persistence.entity.RentalContract;
import com.sab.adagency.persistence.implementation.BillboardRepositoryImpl;
import com.sab.adagency.persistence.implementation.ClientRepositoryImpl;
import com.sab.adagency.persistence.implementation.RentalContractRepositoryImpl;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for RentalContract CRUD operations.
 *
 * Uses an in-memory date Dictionary (Map<LocalDate, Integer>) to quickly check
 * whether a particular date is already occupied by an existing contract for the
 * currently selected billboard.
 */
public class RentalContractController implements Initializable {

    @FXML
    private TableView<RentalContract> contractTable;

    @FXML
    private TableColumn<RentalContract, Integer> idColumn;

    @FXML
    private TableColumn<RentalContract, String> billboardColumn;

    @FXML
    private TableColumn<RentalContract, String> clientColumn;

    @FXML
    private TableColumn<RentalContract, String> startDateColumn;

    @FXML
    private TableColumn<RentalContract, String> endDateColumn;

    @FXML
    private TableColumn<RentalContract, Long> daysColumn;

    @FXML
    private TableColumn<RentalContract, Double> totalColumn;

    @FXML
    private TableColumn<RentalContract, String> paidColumn;

    @FXML
    private ComboBox<Billboard> billboardComboBox;

    @FXML
    private ComboBox<Client> clientComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private CheckBox paidCheckBox;

    @FXML
    private Label totalAmountLabel;

    @FXML
    private Label availabilityLabel;

    @FXML
    private ComboBox<Billboard> billboardFilterComboBox;

    @FXML
    private ComboBox<String> paidFilterComboBox;

    @FXML
    private Button checkAvailabilityButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button updateButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button clearButton;

    private RentalContractRepository contractRepository;
    private BillboardRepository billboardRepository;
    private ClientRepository clientRepository;

    private ObservableList<RentalContract> contractData;
    private ObservableList<Billboard> billboardData;
    private ObservableList<Client> clientData;

    private RentalContract selectedContract;

    /**
     * Dictionary that maps an occupied date -> contract id occupying that date,
     * built for the currently selected billboard. Used for fast O(1) availability
     * lookups while the user picks a date range.
     */
    private final Map<LocalDate, Integer> occupancyDictionary = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        contractRepository = new RentalContractRepositoryImpl();
        billboardRepository = new BillboardRepositoryImpl();
        clientRepository = new ClientRepositoryImpl();

        contractData = FXCollections.observableArrayList();
        billboardData = FXCollections.observableArrayList();
        clientData = FXCollections.observableArrayList();

        setupTableColumns();
        setupTableSelection();
        setupButtons();
        setupComboBoxes();
        setupFilters();
        setupListeners();
        loadReferenceData();
        loadContracts();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        billboardColumn.setCellValueFactory(cellData -> {
            int billboardId = cellData.getValue().getBillboardId();
            String text = billboardRepository.findOneById(billboardId)
                    .map(b -> b.getCode() + " (" + b.getCity().getDisplayName() + ")")
                    .orElse("Невідомий");
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        clientColumn.setCellValueFactory(cellData -> {
            int clientId = cellData.getValue().getClientId();
            String text = clientRepository.findOneById(clientId)
                    .map(Client::getName)
                    .orElse("Невідомий");
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        startDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStartDate().toString()));
        endDateColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEndDate().toString()));

        daysColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().getDurationDays()));

        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        paidColumn.setCellValueFactory(cellData -> {
            boolean paid = cellData.getValue().isPaid();
            return new javafx.beans.property.SimpleStringProperty(paid ? "✔ Оплачено" : "✗ Неоплачено");
        });

        contractTable.setItems(contractData);
    }

    private void setupTableSelection() {
        contractTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedContract = newValue;
            if (selectedContract != null) {
                populateFields(selectedContract);
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
        billboardComboBox.setItems(billboardData);
        billboardComboBox.setCellFactory(listView -> new ListCell<Billboard>() {
            @Override
            protected void updateItem(Billboard item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getCode() + " — " + item.getAddress() + " (" + item.getCity().getDisplayName() + ")");
            }
        });
        billboardComboBox.setButtonCell(new ListCell<Billboard>() {
            @Override
            protected void updateItem(Billboard item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCode() + " (" + item.getCity().getDisplayName() + ")");
            }
        });

        clientComboBox.setItems(clientData);
        clientComboBox.setCellFactory(listView -> new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item.isVip() ? "★ " : "") + item.getName());
            }
        });
        clientComboBox.setButtonCell(new ListCell<Client>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void setupFilters() {
        billboardFilterComboBox.setItems(billboardData);
        billboardFilterComboBox.setCellFactory(listView -> new ListCell<Billboard>() {
            @Override
            protected void updateItem(Billboard item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCode());
            }
        });
        billboardFilterComboBox.setButtonCell(new ListCell<Billboard>() {
            @Override
            protected void updateItem(Billboard item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCode());
            }
        });

        ObservableList<String> paidOptions = FXCollections.observableArrayList("Всі", "Оплачені", "Неоплачені");
        paidFilterComboBox.setItems(paidOptions);
        paidFilterComboBox.setValue("Всі");

        billboardFilterComboBox.valueProperty().addListener((obs, o, n) -> applyFilters());
        paidFilterComboBox.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void setupListeners() {
        billboardComboBox.valueProperty().addListener((obs, oldB, newB) -> {
            rebuildOccupancyDictionary(newB);
            updateAvailabilityLabel();
            updateTotalAmount();
        });

        startDatePicker.valueProperty().addListener((obs, o, n) -> {
            updateAvailabilityLabel();
            updateTotalAmount();
        });
        endDatePicker.valueProperty().addListener((obs, o, n) -> {
            updateAvailabilityLabel();
            updateTotalAmount();
        });

        clientComboBox.valueProperty().addListener((obs, o, n) -> updateTotalAmount());
    }

    /**
     * Rebuilds the in-memory dictionary of occupied dates for the supplied billboard.
     * Each occupied date is mapped to the id of the contract that occupies it.
     */
    private void rebuildOccupancyDictionary(Billboard billboard) {
        occupancyDictionary.clear();
        if (billboard == null) {
            return;
        }
        List<RentalContract> existing = contractRepository.findByBillboardId(billboard.getId());
        for (RentalContract c : existing) {
            if (selectedContract != null && c.getId() == selectedContract.getId()) {
                continue;
            }
            LocalDate d = c.getStartDate();
            while (!d.isAfter(c.getEndDate())) {
                occupancyDictionary.put(d, c.getId());
                d = d.plusDays(1);
            }
        }
    }

    /**
     * Updates the availability hint label using the in-memory dictionary.
     */
    private void updateAvailabilityLabel() {
        Billboard billboard = billboardComboBox.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (billboard == null || start == null || end == null) {
            availabilityLabel.setText("—");
            availabilityLabel.setStyle("-fx-text-fill: #777;");
            return;
        }

        if (end.isBefore(start)) {
            availabilityLabel.setText("Дата завершення раніше дати початку!");
            availabilityLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
            return;
        }

        int conflicts = 0;
        LocalDate firstConflict = null;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (occupancyDictionary.containsKey(d)) {
                conflicts++;
                if (firstConflict == null) {
                    firstConflict = d;
                }
            }
            d = d.plusDays(1);
        }

        if (conflicts == 0) {
            availabilityLabel.setText("✔ Білборд вільний на обраний період");
            availabilityLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        } else {
            availabilityLabel.setText(String.format("✗ Зайнято %d дн. (перша колізія: %s)", conflicts, firstConflict));
            availabilityLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
        }
    }

    private void updateTotalAmount() {
        Billboard billboard = billboardComboBox.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        Client client = clientComboBox.getValue();

        if (billboard == null || start == null || end == null || end.isBefore(start)) {
            totalAmountLabel.setText("0.00 грн");
            return;
        }

        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        double total = days * billboard.getSize().getDailyPrice();
        if (client != null && client.isVip()) {
            total *= 0.9;
        }
        totalAmountLabel.setText(String.format("%.2f грн (%d дн.)", total, days));
    }

    private void applyFilters() {
        try {
            List<RentalContract> all = contractRepository.findAll();
            ObservableList<RentalContract> filtered = FXCollections.observableArrayList();

            Billboard billboardFilter = billboardFilterComboBox.getValue();
            String paidFilter = paidFilterComboBox.getValue();

            for (RentalContract c : all) {
                boolean matchesBillboard = billboardFilter == null || c.getBillboardId() == billboardFilter.getId();
                boolean matchesPaid = "Всі".equals(paidFilter)
                        || ("Оплачені".equals(paidFilter) && c.isPaid())
                        || ("Неоплачені".equals(paidFilter) && !c.isPaid());

                if (matchesBillboard && matchesPaid) {
                    filtered.add(c);
                }
            }

            contractData.clear();
            contractData.addAll(filtered);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося застосувати фільтри: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void populateFields(RentalContract contract) {
        billboardRepository.findOneById(contract.getBillboardId())
                .ifPresent(billboardComboBox::setValue);
        clientRepository.findOneById(contract.getClientId())
                .ifPresent(clientComboBox::setValue);
        startDatePicker.setValue(contract.getStartDate());
        endDatePicker.setValue(contract.getEndDate());
        paidCheckBox.setSelected(contract.isPaid());
        rebuildOccupancyDictionary(billboardComboBox.getValue());
        updateAvailabilityLabel();
        updateTotalAmount();
    }

    private void clearFields() {
        billboardComboBox.setValue(null);
        clientComboBox.setValue(null);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        paidCheckBox.setSelected(false);
        totalAmountLabel.setText("0.00 грн");
        availabilityLabel.setText("—");
        availabilityLabel.setStyle("-fx-text-fill: #777;");
        selectedContract = null;
        occupancyDictionary.clear();
    }

    private void loadReferenceData() {
        try {
            billboardData.clear();
            billboardData.addAll(billboardRepository.findAll());
            clientData.clear();
            clientData.addAll(clientRepository.findAll());
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося завантажити довідники: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadContracts() {
        try {
            List<RentalContract> contracts = contractRepository.findAll();
            contractData.clear();
            contractData.addAll(contracts);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося завантажити договори: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleCheckAvailability() {
        Billboard billboard = billboardComboBox.getValue();
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (billboard == null || start == null || end == null) {
            showAlert("Перевірка зайнятості", "Виберіть білборд і дати!", Alert.AlertType.WARNING);
            return;
        }

        rebuildOccupancyDictionary(billboard);
        updateAvailabilityLabel();

        StringBuilder report = new StringBuilder();
        report.append("Білборд: ").append(billboard.getCode()).append("\n");
        report.append("Період: ").append(start).append(" — ").append(end).append("\n\n");

        int free = 0;
        int busy = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (occupancyDictionary.containsKey(d)) {
                busy++;
            } else {
                free++;
            }
            d = d.plusDays(1);
        }
        report.append("Вільних днів: ").append(free).append("\n");
        report.append("Зайнятих днів: ").append(busy);

        Alert.AlertType type = busy == 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
        showAlert("Перевірка зайнятості дат", report.toString(), type);
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        try {
            Billboard billboard = billboardComboBox.getValue();
            Client client = clientComboBox.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (contractRepository.existsOverlap(billboard.getId(), start, end, 0)) {
                showAlert("Конфлікт дат",
                        "Білборд вже зайнятий на обраний період! Скористайтеся перевіркою зайнятості.",
                        Alert.AlertType.WARNING);
                return;
            }

            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            double total = days * billboard.getSize().getDailyPrice();
            if (client.isVip()) {
                total *= 0.9;
            }

            RentalContract contract = RentalContract.builder()
                    .billboardId(billboard.getId())
                    .clientId(client.getId())
                    .startDate(start)
                    .endDate(end)
                    .totalPrice(total)
                    .paid(paidCheckBox.isSelected())
                    .build();

            contractRepository.save(contract);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Договір оренди створено успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося створити договір: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedContract == null || !validateInput()) {
            return;
        }

        try {
            Billboard billboard = billboardComboBox.getValue();
            Client client = clientComboBox.getValue();
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();

            if (contractRepository.existsOverlap(billboard.getId(), start, end, selectedContract.getId())) {
                showAlert("Конфлікт дат",
                        "Білборд вже зайнятий іншим договором на обраний період!",
                        Alert.AlertType.WARNING);
                return;
            }

            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
            double total = days * billboard.getSize().getDailyPrice();
            if (client.isVip()) {
                total *= 0.9;
            }

            RentalContract updated = RentalContract.builder()
                    .id(selectedContract.getId())
                    .billboardId(billboard.getId())
                    .clientId(client.getId())
                    .startDate(start)
                    .endDate(end)
                    .totalPrice(total)
                    .paid(paidCheckBox.isSelected())
                    .build();

            contractRepository.save(updated);
            applyFilters();
            clearFields();
            showAlert("Успіх", "Договір оновлено успішно!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Помилка", "Не вдалося оновити договір: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedContract == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Розірвати договір");
        confirm.setContentText("Ви впевнені, що хочете видалити договір #" + selectedContract.getId() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                contractRepository.deleteById(selectedContract.getId());
                applyFilters();
                clearFields();
                showAlert("Успіх", "Договір видалено успішно!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Помилка", "Не вдалося видалити договір: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleClear() {
        clearFields();
        contractTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleRefresh() {
        loadReferenceData();
        applyFilters();
    }

    /**
     * Public method to refresh data - called when tab is switched.
     */
    public void refreshData() {
        loadReferenceData();
        loadContracts();
        contractTable.getSelectionModel().clearSelection();
        clearFields();
    }

    private boolean validateInput() {
        if (billboardComboBox.getValue() == null) {
            showAlert("Помилка валідації", "Білборд обов'язковий!", Alert.AlertType.WARNING);
            return false;
        }
        if (clientComboBox.getValue() == null) {
            showAlert("Помилка валідації", "Клієнт обов'язковий!", Alert.AlertType.WARNING);
            return false;
        }
        if (startDatePicker.getValue() == null) {
            showAlert("Помилка валідації", "Дата початку обов'язкова!", Alert.AlertType.WARNING);
            return false;
        }
        if (endDatePicker.getValue() == null) {
            showAlert("Помилка валідації", "Дата завершення обов'язкова!", Alert.AlertType.WARNING);
            return false;
        }
        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            showAlert("Помилка валідації", "Дата завершення не може бути раніше дати початку!", Alert.AlertType.WARNING);
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
