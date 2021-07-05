package com.cwp.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import javax.imageio.ImageIO;

public class Main extends Application {

    private static final int FRAME_WIDTH = 290;

    private static final int FRAME_HEIGHT = 205;

    private static final String BACKGROUND_COLOR = "#3c3f41";

    private static final String CSS_FILE_NAME = "main.css";

    private static final double FRAME_OPACITY = 0.8;

    private static final String APP_NAME = "shutdown-tool";

    private static final String ICON_PATH = "/images/shutdown4.jpeg";

    private DatePicker datePicker;

    private ChoiceBox hourSelector;

    private ChoiceBox minuteSelector;

    private ChoiceBox secondSelector;

    private Label lastTimeLabel;

    private Timer countdownTimer;

    private RadioButton closeRb;

    private RadioButton rebootRb;

    public void setDatePicker(DatePicker datePicker) {
        this.datePicker = datePicker;
    }

    @Override
    public void start(Stage stage) throws IOException, AWTException {
        initConfig(stage);

        stage.setScene(getScene());

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Scene getScene() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15, 20, 15, 20));
        Scene scene = new Scene(vbox, FRAME_WIDTH, FRAME_HEIGHT);

        vbox.getChildren().add(getCloseTypeHbox());
        vbox.getChildren().add(getDateHbox());
        vbox.getChildren().add(getTimeHbox());
        vbox.getChildren().add(getLastTimeHbox());
        vbox.getChildren().add(getButtonHbox());

        scene.setFill(Paint.valueOf(BACKGROUND_COLOR));
        vbox.setBackground(Background.EMPTY);
        URL url_css = getClass().getResource(CSS_FILE_NAME);
        scene.getStylesheets().add(url_css.toExternalForm());
        return scene;
    }

    private Label getCommonLabel(String text, Node labelFor) {
        Label label = new Label();
        label.getStyleClass().add("common-label");
        if (null != labelFor) {
            label.setLabelFor(labelFor);
        }
        label.setText(text);
        return label;
    }

    private HBox getCommonHBox() {
        HBox hBox = new HBox(5);
        hBox.setPadding(new Insets(5, 0, 0, 0));
        hBox.getStyleClass().add("common-hbox");
        return hBox;
    }

    private HBox getCloseTypeHbox() {
        HBox hBox = getCommonHBox();
        hBox.setSpacing(10);
        ToggleGroup group = new ToggleGroup();
        Label label = getCommonLabel("类型：", null);
        closeRb = new RadioButton();
        closeRb.setText("关机");
        closeRb.getStyleClass().add("common-label");
        closeRb.setSelected(true);
        closeRb.setToggleGroup(group);

        rebootRb = new RadioButton();
        rebootRb.setText("重启");
        rebootRb.getStyleClass().add("common-label");
        rebootRb.setToggleGroup(group);

        hBox.getChildren().add(label);
        hBox.getChildren().add(closeRb);
        hBox.getChildren().add(rebootRb);
        return hBox;
    }

    private HBox getLastTimeHbox() {
        HBox hBox = getCommonHBox();
        Label label = getCommonLabel("剩余时间：", null);
        lastTimeLabel = getCommonLabel("未启动", null);

        hBox.getChildren().add(label);
        hBox.getChildren().add(lastTimeLabel);
        return hBox;
    }

    private String getLastTimeString(AtomicLong sec) {
        long second = sec.get();
        if (second < 60) {
            return second + "秒";
        } else if (second >= 60 && second < 3600) {
            long min = second / 60;
            long s = second - min * 60;
            return min + "分" + s + "秒";

        } else if (second >= 3600 && second < 3600 * 24) {
            long hour = second / 3600;
            long min = (second - hour * 3600) / 60;
            long s = second - min * 60 - hour * 3600;
            return hour + "时" + min + "分" + s + "秒";
        } else {
            long day = second / 86400;
            long hour = (second - day * 86400) / 3600;
            long min = (second - day * 86400 - hour * 3600) / 60;
            long s = second - min * 60 - hour * 3600 - day * 86400;
            return hour + "时" + min + "分" + s + "秒";
        }
    }

    private HBox getButtonHbox() {
        HBox hBox = getCommonHBox();
        Button button = new Button("启动");

        button.getStyleClass().add("button");
        button.setPrefWidth(242);
        button.setOnAction(event -> {
            String buttonText = button.getText();
            if (buttonText.equals("启动")) {
                LocalDate value = datePicker.getValue();
                int hour = (int) hourSelector.getValue();
                int minute = (int) minuteSelector.getValue();
                int second = (int) secondSelector.getValue();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime dateTime = LocalDateTime.now();
                dateTime = dateTime.withYear(value.getYear())
                        .withMonth(value.getMonthValue())
                        .withDayOfYear(value.getDayOfYear())
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(second);
                Duration duration = Duration.between(now, dateTime);
                if (duration.isNegative()) {
                    setDateAndTimeByNextCloseTime();
                    value = datePicker.getValue();
                    hour = (int) hourSelector.getValue();
                    minute = (int) minuteSelector.getValue();
                    second = (int) secondSelector.getValue();
                    dateTime = dateTime.withYear(value.getYear())
                            .withMonth(value.getMonthValue())
                            .withDayOfYear(value.getDayOfYear())
                            .withHour(hour)
                            .withMinute(minute)
                            .withSecond(second);
                    duration = Duration.between(now, dateTime);
                }
                long seconds = duration.toMillis() / 1000 + 10; //多10秒
                AtomicLong lastSeconds = new AtomicLong(seconds);
                try {
                    if (closeRb.isSelected()) {
                        Runtime.getRuntime().exec("shutdown -s -t " + seconds);//shutdown -s是关机
                    } else {
                        Runtime.getRuntime().exec("shutdown -r -t " + seconds);//shutdown -r是重启
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                button.getStyleClass().add("stop");
                button.setText("停止");
                countdownTimer = new Timer();
                countdownTimer.schedule(new TimerTask() {
                    public void run() {
                        Platform.runLater(() -> {
                            lastTimeLabel.setText(getLastTimeString(lastSeconds));
                            lastSeconds.decrementAndGet();

                        });
                    }
                }, 0, 1000);
            } else {
                button.getStyleClass().remove("stop");
                button.setText("启动");
                countdownTimer.cancel();
                lastTimeLabel.setText("未启动");
                try {
                    Runtime.getRuntime().exec("shutdown -a");//shutdown -r是重启
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

        hBox.getChildren().add(button);
        return hBox;
    }

    private HBox getDateHbox() {
        HBox hBox = getCommonHBox();

        getDatePicker();
        Label label = getCommonLabel("日期：", datePicker);

        hBox.getChildren().add(label);
        hBox.getChildren().add(datePicker);
        return hBox;
    }

    private HBox getTimeHbox() {
        HBox hBox = getCommonHBox();
        hBox.setSpacing(1);
        hourSelector = new ChoiceBox();
        LocalDateTime nextTime = getNextCloseTime();
        hourSelector.getStyleClass().add("common-width-short");
        for (int i = 0; i < 24; i++) {
            hourSelector.getItems().add(i);
        }
        hourSelector.setValue(nextTime.getHour());
        Label label = getCommonLabel("时：", hourSelector);

        hBox.getChildren().add(label);
        hBox.getChildren().add(hourSelector);

        minuteSelector = new ChoiceBox();
        minuteSelector.getStyleClass().add("common-width-short");
        for (int i = 0; i < 60; i++) {
            minuteSelector.getItems().add(i);
        }
        minuteSelector.setValue(nextTime.getMinute());
        label = getCommonLabel("  分：", minuteSelector);

        hBox.getChildren().add(label);
        hBox.getChildren().add(minuteSelector);

        secondSelector = new ChoiceBox();
        secondSelector.getStyleClass().add("common-width-short");
        for (int i = 0; i < 60; i++) {
            secondSelector.getItems().add(i);
        }
        secondSelector.setValue(nextTime.getSecond());
        label = getCommonLabel("  秒：", secondSelector);

        hBox.getChildren().add(label);
        hBox.getChildren().add(secondSelector);
        return hBox;
    }

    private void setDateAndTimeByNextCloseTime() {
        LocalDateTime nextTime = getNextCloseTime();
        hourSelector.setValue(nextTime.getHour());
        minuteSelector.setValue(nextTime.getMinute());
        secondSelector.setValue(nextTime.getSecond());
        datePicker.setValue(nextTime.toLocalDate());
    }

    private LocalDateTime getNextCloseTime() {
        LocalDateTime time = LocalDateTime.now();
        time = time.withSecond(0);
        int minute = time.getMinute();
        int hour = time.getHour();
        int day = time.getDayOfYear();
        if (minute % 5 == 0) {
            minute += 5;
        } else {
            while (minute % 5 != 0) {
                minute++;
            }
        }

        if (minute >= 60) {
            minute = minute - 60;
            hour++;
        }
        if (hour == 24) {
            hour = 0;
            day++;
        }
        time = time.withDayOfYear(day).withHour(hour).withMinute(minute);
        return time;
    }

    private void initConfig(Stage primaryStage) throws AWTException, IOException {
        // 无任务栏程序
        primaryStage.setAlwaysOnTop(true);
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.setTitle(APP_NAME);
        primaryStage.setOpacity(FRAME_OPACITY);
        primaryStage.setResizable(false);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_PATH)));
        // 不真正退出
        Platform.setImplicitExit(false);

        SystemTray tray = SystemTray.getSystemTray();
        BufferedImage image = ImageIO.read(getClass().getResourceAsStream(ICON_PATH));
        TrayIcon trayIcon = new TrayIcon(image, "shutdown-tool");
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(APP_NAME);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Platform.runLater(() -> {
                        if (primaryStage.isIconified()) {
                            primaryStage.setIconified(false);
                        }
                        if (!primaryStage.isShowing()) {
                            primaryStage.show();
                        }
                        primaryStage.toFront();
                    });
                }
            }
        });
        MenuItem exitItem = new MenuItem("exit");
        exitItem.addActionListener(e -> {
            System.exit(0);
        });
        final PopupMenu popup = new PopupMenu();
        popup.add(exitItem);
        trayIcon.setPopupMenu(popup);
        tray.add(trayIcon);
    }

    private DatePicker getDatePicker() {
        String pattern = "yyyy-MM-dd";
        DatePicker datePicker = new DatePicker();
        datePicker.getStyleClass().add("common-width");
        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(pattern);

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };
        datePicker.setConverter(converter);
        datePicker.setPromptText("年-月-日");
        datePicker.setValue(getNextCloseTime().toLocalDate());
        setDatePicker(datePicker);
        return datePicker;
    }

}
