package com.sab.adagency.controller;

import com.sab.adagency.persistence.entity.AppUser;
import com.sab.adagency.persistence.entity.UserRole;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Головне вікно з анімованим вертикальним меню та плавним перемиканням розділів. */
public class MainController implements Initializable {

    private static final double SIDEBAR_EXPANDED = 240.0;
    private static final double SIDEBAR_COLLAPSED = 76.0;
    private static final Duration SIDEBAR_DURATION = Duration.millis(260);
    private static final Duration INDICATOR_DURATION = Duration.millis(220);
    private static final Duration FADE_DURATION = Duration.millis(180);

    @FXML
    private VBox sidebar;

    @FXML
    private Pane indicatorLayer;

    @FXML
    private Region menuIndicator;

    @FXML
    private StackPane contentArea;

    @FXML
    private ScrollPane homeContent;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label currentRoleLabel;

    @FXML
    private ToggleButton homeNav;

    @FXML
    private ToggleButton billboardNav;

    @FXML
    private ToggleButton clientNav;

    @FXML
    private ToggleButton contractNav;

    @FXML
    private ToggleButton usersNav;

    @FXML
    private ToggleButton reportsNav;

    private final ToggleGroup navGroup = new ToggleGroup();
    private final Map<ToggleButton, Node> sections = new LinkedHashMap<>();
    private Node currentSection;
    private boolean collapsed = false;

    private AppUser authenticatedUser;

    private BillboardController billboardController;
    private ClientController clientController;
    private RentalContractController contractController;
    private UserManagementController userManagementController;
    private ReportController reportController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupWelcomeText();
        loadSections();
        bindNavToggles();
        applyRolePermissions(UserRole.MANAGER);
        setupIndicator();
        selectInitial();
    }

    public void setAuthenticatedUser(AppUser user) {
        this.authenticatedUser = user;
        if (user != null) {
            currentUserLabel.setText("Користувач: "
                    + (user.getFullName() == null || user.getFullName().isBlank()
                            ? user.getUsername()
                            : user.getFullName() + " (" + user.getUsername() + ")"));
            applyRolePermissions(user.getRole());
            setupWelcomeText();
            if (userManagementController != null) {
                userManagementController.setCurrentUser(user);
            }
        }
    }

    private void loadSections() {
        try {
            FXMLLoader bb = new FXMLLoader(getClass().getResource("/view/billboard-management.fxml"));
            Parent billboardRoot = bb.load();
            billboardController = bb.getController();

            FXMLLoader cl = new FXMLLoader(getClass().getResource("/view/client-management.fxml"));
            Parent clientRoot = cl.load();
            clientController = cl.getController();

            FXMLLoader ct = new FXMLLoader(getClass().getResource("/view/rental-contract-management.fxml"));
            Parent contractRoot = ct.load();
            contractController = ct.getController();

            FXMLLoader u = new FXMLLoader(getClass().getResource("/view/user-management.fxml"));
            Parent usersRoot = u.load();
            userManagementController = u.getController();
            if (authenticatedUser != null) {
                userManagementController.setCurrentUser(authenticatedUser);
            }

            FXMLLoader rep = new FXMLLoader(getClass().getResource("/view/report-management.fxml"));
            Parent reportRoot = rep.load();
            reportController = rep.getController();

            sections.put(homeNav, homeContent);
            sections.put(billboardNav, billboardRoot);
            sections.put(clientNav, clientRoot);
            sections.put(contractNav, contractRoot);
            sections.put(usersNav, usersRoot);
            sections.put(reportsNav, reportRoot);

            for (Node node : sections.values()) {
                if (node != homeContent) {
                    contentArea.getChildren().add(node);
                }
                node.setVisible(false);
                node.setOpacity(0.0);
            }
        } catch (IOException e) {
            e.printStackTrace();
            alert("Не вдалося завантажити інтерфейси: " + e.getMessage());
        }
    }

    private void bindNavToggles() {
        for (ToggleButton tb : sections.keySet()) {
            tb.setToggleGroup(navGroup);
        }
        navGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
            }
        });
    }

    private void setupIndicator() {
        menuIndicator.setManaged(false);
        sidebar.heightProperty().addListener((obs, o, n) -> repositionIndicator());
    }

    private void selectInitial() {
        homeNav.setSelected(true);
        showSectionFor(homeNav, false);
        Platform.runLater(this::repositionIndicator);
    }

    private void applyRolePermissions(UserRole role) {
        boolean isAdmin = role == UserRole.ADMIN;
        boolean isManager = role == UserRole.MANAGER || isAdmin;

        setNavEnabled(homeNav, true);
        setNavEnabled(billboardNav, isManager);
        setNavEnabled(clientNav, true);
        setNavEnabled(contractNav, true);
        setNavEnabled(reportsNav, true);
        setNavEnabled(usersNav, isAdmin);

        currentRoleLabel.setText("Роль: " + role.getDisplayName());

        ToggleButton selected = (ToggleButton) navGroup.getSelectedToggle();
        if (selected != null && selected.isDisabled()) {
            homeNav.setSelected(true);
            showSectionFor(homeNav, true);
        }
    }

    private void setNavEnabled(ToggleButton tb, boolean enabled) {
        tb.setDisable(!enabled);
    }

    private void setupWelcomeText() {
        String name = authenticatedUser == null
                ? "невідомий користувач"
                : authenticatedUser.getUsername() + " ("
                        + authenticatedUser.getRole().getDisplayName() + ")";
        welcomeLabel.setText("Ласкаво просимо до AdAgency — системи управління рекламним агентством.\n\n"
                + "Скористайтеся бічним меню ліворуч, щоб перейти до потрібного розділу.\n"
                + "Натисніть ≡ нагорі, щоб згорнути меню до іконок.\n\nУвійшли як: "
                + name);
    }

    @FXML
    private void showHome() {
        showSectionFor(homeNav, true);
    }

    @FXML
    private void showBillboards() {
        showSectionFor(billboardNav, true);
        if (billboardController != null) {
            billboardController.refreshData();
        }
    }

    @FXML
    private void showClients() {
        showSectionFor(clientNav, true);
        if (clientController != null) {
            clientController.refreshData();
        }
    }

    @FXML
    private void showContracts() {
        showSectionFor(contractNav, true);
        if (contractController != null) {
            contractController.refreshData();
        }
    }

    @FXML
    private void showUsers() {
        showSectionFor(usersNav, true);
        if (userManagementController != null) {
            userManagementController.refreshData();
        }
    }

    @FXML
    private void showReports() {
        showSectionFor(reportsNav, true);
        if (reportController != null) {
            reportController.refreshData();
        }
    }

    private void showSectionFor(ToggleButton button, boolean animate) {
        Node target = sections.get(button);
        if (target == null) {
            return;
        }
        button.setSelected(true);
        Platform.runLater(this::repositionIndicator);

        if (target == currentSection) {
            return;
        }

        Node previous = currentSection;
        currentSection = target;

        if (!animate || previous == null) {
            if (previous != null) {
                previous.setVisible(false);
                previous.setOpacity(0.0);
            }
            target.setVisible(true);
            target.setOpacity(1.0);
            target.setTranslateX(0);
            return;
        }

        FadeTransition out = new FadeTransition(FADE_DURATION, previous);
        out.setFromValue(previous.getOpacity());
        out.setToValue(0.0);
        out.setOnFinished(e -> previous.setVisible(false));

        target.setVisible(true);
        target.setOpacity(0.0);
        target.setTranslateX(12);
        FadeTransition in = new FadeTransition(FADE_DURATION, target);
        in.setFromValue(0.0);
        in.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(FADE_DURATION, target);
        slide.setFromX(12);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        out.play();
        in.play();
        slide.play();
    }

    private void repositionIndicator() {
        ToggleButton selected = (ToggleButton) navGroup.getSelectedToggle();
        if (selected == null) {
            menuIndicator.setVisible(false);
            return;
        }
        menuIndicator.setVisible(true);
        double targetY = computeIndicatorY(selected);
        menuIndicator.setPrefHeight(Math.max(selected.getHeight() - 8, 30));

        Timeline tl = new Timeline(new KeyFrame(
                INDICATOR_DURATION,
                new KeyValue(menuIndicator.layoutYProperty(), targetY, Interpolator.EASE_BOTH)));
        tl.play();
    }

    private double computeIndicatorY(ToggleButton button) {
        double y = button.getBoundsInParent().getMinY();
        Node parent = button.getParent();
        while (parent != null && parent != sidebar) {
            y += parent.getBoundsInParent().getMinY();
            parent = parent.getParent();
        }
        return y + 4;
    }

    @FXML
    private void toggleSidebar() {
        double to;
        if (collapsed) {
            to = SIDEBAR_EXPANDED;
            sidebar.getStyleClass().remove("collapsed");
        } else {
            to = SIDEBAR_COLLAPSED;
            if (!sidebar.getStyleClass().contains("collapsed")) {
                sidebar.getStyleClass().add("collapsed");
            }
        }
        collapsed = !collapsed;

        Timeline tl = new Timeline(
                new KeyFrame(SIDEBAR_DURATION,
                        new KeyValue(sidebar.prefWidthProperty(), to, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.minWidthProperty(), to, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.maxWidthProperty(), to, Interpolator.EASE_BOTH)));
        tl.setOnFinished(e -> Platform.runLater(this::repositionIndicator));
        tl.play();
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Помилка");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
