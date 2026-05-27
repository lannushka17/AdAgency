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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Контролер модуля звітності з фільтром за періодом та експортом у CSV. */
public class ReportController implements Initializable {

    private static final String REPORT_BILLBOARDS = "Білборди за містами";
    private static final String REPORT_REVENUE = "Дохід і борг по договорах";
    private static final String REPORT_VIP = "VIP-клієнти та активність";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private ComboBox<String> reportTypeCombo;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button exportButton;

    @FXML
    private TableView<ObservableList<String>> reportTable;

    @FXML
    private Label summaryLabel;

    private final BillboardRepository billboards = new BillboardRepositoryImpl();
    private final ClientRepository clients = new ClientRepositoryImpl();
    private final RentalContractRepository contracts = new RentalContractRepositoryImpl();

    private final List<String> currentHeaders = new ArrayList<>();
    private final ObservableList<ObservableList<String>> currentRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reportTypeCombo.getItems().setAll(REPORT_BILLBOARDS, REPORT_REVENUE, REPORT_VIP);
        reportTypeCombo.setValue(REPORT_BILLBOARDS);

        LocalDate today = LocalDate.now();
        endDatePicker.setValue(today.plusMonths(3));
        startDatePicker.setValue(today.minusMonths(1));

        reportTable.setItems(currentRows);
        exportButton.setDisable(true);
    }

    public void refreshData() {
        handleGenerate();
    }

    @FXML
    private void handleGenerate() {
        if (!validateRange()) {
            return;
        }
        LocalDate from = startDatePicker.getValue();
        LocalDate to = endDatePicker.getValue();
        String type = reportTypeCombo.getValue();
        switch (type) {
            case REPORT_BILLBOARDS -> billboardsByCityReport();
            case REPORT_REVENUE -> contractRevenueReport(from, to);
            case REPORT_VIP -> vipClientsReport(from, to);
            default -> {
            }
        }
        exportButton.setDisable(currentRows.isEmpty());
    }

    @FXML
    private void handleExportCsv() {
        if (currentRows.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Спочатку сформуйте звіт.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Зберегти звіт у CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName(defaultFileName());
        Window owner = reportTable.getScene() != null ? reportTable.getScene().getWindow() : null;
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        try (PrintWriter pw = new PrintWriter(target, StandardCharsets.UTF_8)) {
            pw.write('\ufeff');
            pw.println(joinCsv(currentHeaders));
            for (ObservableList<String> row : currentRows) {
                pw.println(joinCsv(row));
            }
            alert(Alert.AlertType.INFORMATION, "CSV збережено: " + target.getAbsolutePath());
        } catch (IOException ex) {
            alert(Alert.AlertType.ERROR, "Не вдалося зберегти файл: " + ex.getMessage());
        }
    }

    private void billboardsByCityReport() {
        Map<String, Integer> totalByCity = new HashMap<>();
        Map<String, Integer> availableByCity = new HashMap<>();
        Map<String, Integer> rentedByCity = new HashMap<>();

        Set<Integer> rentedBillboardIds = new HashSet<>();
        LocalDate today = LocalDate.now();
        for (RentalContract c : contracts.findAll()) {
            if (!c.getEndDate().isBefore(today) && !c.getStartDate().isAfter(today.plusMonths(6))) {
                rentedBillboardIds.add(c.getBillboardId());
            }
        }

        for (Billboard b : billboards.findAll()) {
            String city = b.getCity() == null ? "—" : b.getCity().getDisplayName();
            totalByCity.merge(city, 1, Integer::sum);
            if (b.isAvailable() && !rentedBillboardIds.contains(b.getId())) {
                availableByCity.merge(city, 1, Integer::sum);
            } else {
                rentedByCity.merge(city, 1, Integer::sum);
            }
        }

        List<List<String>> result = new ArrayList<>();
        List<String> keys = new ArrayList<>(totalByCity.keySet());
        keys.sort(Comparator.naturalOrder());
        int totalAll = 0;
        int availAll = 0;
        for (String city : keys) {
            int total = totalByCity.getOrDefault(city, 0);
            int avail = availableByCity.getOrDefault(city, 0);
            int rented = rentedByCity.getOrDefault(city, 0);
            double load = total == 0 ? 0 : rented * 100.0 / total;
            result.add(List.of(
                    city,
                    String.valueOf(total),
                    String.valueOf(avail),
                    String.valueOf(rented),
                    String.format("%.1f%%", load)));
            totalAll += total;
            availAll += avail;
        }

        setReportData(
                List.of("Місто", "Усього білбордів", "Вільних", "Зайнятих", "Завантаженість"),
                result);
        summaryLabel.setText(String.format("Усього білбордів: %d · Вільних: %d", totalAll, availAll));
    }

    private void contractRevenueReport(LocalDate from, LocalDate to) {
        Map<Integer, Billboard> billboardMap = new HashMap<>();
        for (Billboard b : billboards.findAll()) {
            billboardMap.put(b.getId(), b);
        }
        Map<Integer, Client> clientMap = new HashMap<>();
        for (Client c : clients.findAll()) {
            clientMap.put(c.getId(), c);
        }

        List<List<String>> result = new ArrayList<>();
        double grandTotal = 0;
        double grandPaid = 0;
        for (RentalContract c : contracts.findAll()) {
            if (c.getEndDate().isBefore(from) || c.getStartDate().isAfter(to)) {
                continue;
            }
            Billboard b = billboardMap.get(c.getBillboardId());
            Client cl = clientMap.get(c.getClientId());
            String bbCode = b == null ? "—" : b.getCode();
            String clientName = cl == null ? "—" : cl.getName();
            grandTotal += c.getTotalPrice();
            if (c.isPaid()) {
                grandPaid += c.getTotalPrice();
            }
            double debt = c.isPaid() ? 0 : c.getTotalPrice();
            result.add(List.of(
                    bbCode,
                    clientName,
                    c.getStartDate().format(DATE_FMT),
                    c.getEndDate().format(DATE_FMT),
                    String.format("%.2f грн", c.getTotalPrice()),
                    c.isPaid() ? "Оплачено" : "Борг",
                    String.format("%.2f грн", debt)));
        }
        result.sort(Comparator.comparing(r -> r.get(0)));

        setReportData(
                List.of("Білборд", "Клієнт", "Початок", "Кінець", "Сума", "Статус", "Борг"),
                result);
        summaryLabel.setText(String.format(
                "Договорів: %d · Усього: %.2f грн · Оплачено: %.2f грн · Борг: %.2f грн",
                result.size(), grandTotal, grandPaid, grandTotal - grandPaid));
    }

    private void vipClientsReport(LocalDate from, LocalDate to) {
        Map<Integer, List<RentalContract>> byClient = new HashMap<>();
        for (RentalContract c : contracts.findAll()) {
            if (c.getEndDate().isBefore(from) || c.getStartDate().isAfter(to)) {
                continue;
            }
            byClient.computeIfAbsent(c.getClientId(), k -> new ArrayList<>()).add(c);
        }

        List<List<String>> result = new ArrayList<>();
        int vipCount = 0;
        int activeVip = 0;
        for (Client c : clients.findAll()) {
            List<RentalContract> list = byClient.getOrDefault(c.getId(), List.of());
            double sum = list.stream().mapToDouble(RentalContract::getTotalPrice).sum();
            long unpaid = list.stream().filter(ct -> !ct.isPaid()).count();
            String risk = detectRisk(c, unpaid, list.size());
            if (c.isVip()) {
                vipCount++;
                if (!list.isEmpty()) {
                    activeVip++;
                }
            }
            if (!c.isVip() && list.isEmpty()) {
                continue;
            }
            result.add(List.of(
                    c.getName(),
                    c.isVip() ? "VIP" : "Звичайний",
                    c.getPhone(),
                    String.valueOf(list.size()),
                    String.format("%.2f грн", sum),
                    String.valueOf(unpaid),
                    risk));
        }
        result.sort(Comparator.comparing(r -> r.get(0)));

        setReportData(
                List.of("Клієнт", "Статус", "Телефон", "Договорів", "Сума", "Несплачених", "Ризик"),
                result);
        summaryLabel.setText(String.format(
                "У вибірці: %d клієнтів · VIP: %d · Активних VIP: %d", result.size(), vipCount, activeVip));
    }

    private String detectRisk(Client c, long unpaid, int contractCount) {
        if (unpaid > 0) {
            return "Високий (є борг)";
        }
        if (c.isVip() && contractCount == 0) {
            return "Середній (VIP без договорів)";
        }
        if (contractCount == 0) {
            return "Низький";
        }
        return "Низький";
    }

    private void setReportData(List<String> headers, List<List<String>> rows) {
        currentHeaders.clear();
        currentHeaders.addAll(headers);
        reportTable.getColumns().clear();
        for (int i = 0; i < headers.size(); i++) {
            final int columnIndex = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(cell -> {
                ObservableList<String> values = cell.getValue();
                return new ReadOnlyStringWrapper(columnIndex < values.size() ? values.get(columnIndex) : "");
            });
            col.setSortable(false);
            reportTable.getColumns().add(col);
        }
        currentRows.clear();
        for (List<String> row : rows) {
            currentRows.add(FXCollections.observableArrayList(row));
        }
    }

    private boolean validateRange() {
        if (REPORT_BILLBOARDS.equals(reportTypeCombo.getValue())) {
            return true;
        }
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Оберіть період.");
            return false;
        }
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            alert(Alert.AlertType.WARNING, "Дата початку має бути не пізніше дати кінця.");
            return false;
        }
        return true;
    }

    private String defaultFileName() {
        String type = reportTypeCombo.getValue() == null ? "report" : reportTypeCombo.getValue();
        String sanitized = type.toLowerCase().replace(' ', '-').replaceAll("[^a-z0-9а-яіїєґ\\-]", "");
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            return String.format("%s_%s_%s.csv", sanitized, startDatePicker.getValue(), endDatePicker.getValue());
        }
        return sanitized + ".csv";
    }

    private String joinCsv(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeCsv(values.get(i)));
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private void alert(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
