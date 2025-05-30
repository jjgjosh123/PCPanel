package com.getpcpanel.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.getpcpanel.device.PCPanelRGBUI;
import com.getpcpanel.profile.LightingConfig;
import com.getpcpanel.profile.LightingConfig.LightingMode;
import com.getpcpanel.profile.SaveService;
import com.getpcpanel.spring.Prototype;
import com.getpcpanel.ui.UIInitializer.SingleParamInitializer;
import com.getpcpanel.ui.colorpicker.ColorDialog;
import com.getpcpanel.ui.colorpicker.HueSlider;
import com.getpcpanel.util.Util;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@Prototype
@RequiredArgsConstructor
public class RGBLightingDialog extends Application implements UIInitializer<SingleParamInitializer<PCPanelRGBUI>> {
    private final SaveService saveService;
    private final ApplicationEventPublisher eventPublisher;

    private Stage stage;
    @FXML private TabPane knobsTabbedPane;
    @FXML private TabPane allKnobsTabbedPane;
    @FXML private Slider rainbowPhaseShift;
    @FXML private Slider rainbowBrightness;
    @FXML private Slider rainbowSpeed;
    @FXML private CheckBox rainbowReverse;
    private HueSlider waveHue;
    @FXML private Slider waveBrightness;
    @FXML private Slider waveSpeed;
    @FXML private CheckBox waveReverse;
    @FXML private CheckBox waveBounce;
    private HueSlider breathHue;
    @FXML private Slider breathBrightness;
    @FXML private Slider breathSpeed;
    @FXML private VBox wavebox;
    @FXML private VBox breathbox;
    @FXML private HBox volumeFollowBox;
    @FXML private HBox volumeFollowCheckboxContainer;
    private ColorDialog allKnobColor;
    private final List<ColorDialog> cds = new ArrayList<>();
    private final List<CheckBox> volumeFollowingCheckBoxes = new ArrayList<>();
    private PCPanelRGBUI device;
    private boolean pressedOk;
    @FXML private Pane root;
    private LightingConfig lightingConfig;

    @Override
    public void initUI(@Nonnull SingleParamInitializer<PCPanelRGBUI> args) {
        device = args.param();
        lightingConfig = device.getSavedLightingConfig().deepCopy();
        setDeviceLighting();
        postInit();
    }

    private void setDeviceLighting() {
        device.setLighting(lightingConfig.deepCopy(), true);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        UIHelper.closeOnEscape(stage);
        var scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/assets/dark_theme.css")).toExternalForm());
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/assets/256x256.png")).toExternalForm()));
        stage.setOnHiding(e -> {
            if (!pressedOk) {
                device.setLighting(device.getSavedLightingConfig(), true);
            }
            eventPublisher.publishEvent(LightingChangedToDefaultEvent.INSTANCE);
        });
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(HomePage.stage);
        stage.setScene(scene);
        stage.sizeToScene();
        stage.setTitle("Lighting Dialog");
        stage.show();
    }

    public RGBLightingDialog select(int idx) {
        knobsTabbedPane.getSelectionModel().select(idx + 1);
        return this;
    }

    @FXML
    private void onCancel(ActionEvent event) {
        stage.close();
    }

    @FXML
    private void ok(ActionEvent event) {
        log.debug("{} {}", stage.getWidth(), stage.getHeight());
        pressedOk = true;
        device.setSavedLighting(lightingConfig);
        saveService.save();
        stage.close();
    }

    @FXML
    private void turnOffLights(ActionEvent event) {
        allKnobColor.setCustomColor(Color.WHITE);
        allKnobColor.setCustomColor(Color.BLACK);
        knobsTabbedPane.getSelectionModel().select(0);
        allKnobsTabbedPane.getSelectionModel().select(0);
    }

    private void postInit() {
        for (var i = 0; i < device.getKnobCount(); i++) {
            var knob = i + 1;
            var tab = new Tab("Knob " + knob);
            var cd = new ColorDialog(Color.BLACK);
            cds.add(cd);
            var cb = new CheckBox("K" + knob);
            volumeFollowingCheckBoxes.add(cb);
            volumeFollowCheckboxContainer.getChildren().add(cb);
            tab.setContent(cd);
            knobsTabbedPane.getTabs().add(tab);
        }
        Util.adjustTabs(allKnobsTabbedPane, 120, 30);
        allKnobColor = new ColorDialog();
        allKnobsTabbedPane.getTabs().get(0).setContent(allKnobColor);
        var allSliders = new Slider[] { rainbowPhaseShift, rainbowBrightness, rainbowSpeed,
                waveBrightness, waveSpeed,
                breathBrightness, breathSpeed };
        var allCheckBoxes = new CheckBox[] { rainbowReverse,
                waveReverse, waveBounce };
        waveHue = new HueSlider();
        wavebox.getChildren().add(1, waveHue);
        breathHue = new HueSlider();
        breathbox.getChildren().add(1, breathHue);
        initFields();
        initListeners(allSliders, allCheckBoxes);
    }

    private void initFields() {
        var mode = lightingConfig.getLightingMode();
        setFollowingControlsVisible(false);
        if (mode == LightingMode.ALL_COLOR) {
            setFollowingControlsVisible(true);
            setVolumeTrackingData(lightingConfig.getVolumeBrightnessTrackingEnabled());
            knobsTabbedPane.getSelectionModel().select(0);
            allKnobsTabbedPane.getSelectionModel().select(0);
            var color = Color.valueOf(lightingConfig.getAllColor());
            allKnobColor.setCustomColor(color);
            for (var cd : cds)
                cd.setCustomColor(color);
        } else if (mode == LightingMode.SINGLE_COLOR) {
            setFollowingControlsVisible(true);
            setVolumeTrackingData(lightingConfig.getVolumeBrightnessTrackingEnabled());
            knobsTabbedPane.getSelectionModel().select(1);
            for (var i = 0; i < device.getKnobCount(); i++)
                cds.get(i).setCustomColor(Color.valueOf(lightingConfig.getIndividualColors()[i]));
        } else if (mode == LightingMode.ALL_RAINBOW) {
            knobsTabbedPane.getSelectionModel().select(0);
            allKnobsTabbedPane.getSelectionModel().select(1);
            rainbowPhaseShift.setValue(lightingConfig.getRainbowPhaseShift() & 0xFF);
            rainbowBrightness.setValue(lightingConfig.getRainbowBrightness() & 0xFF);
            rainbowSpeed.setValue(lightingConfig.getRainbowSpeed() & 0xFF);
            rainbowReverse.setSelected(lightingConfig.getRainbowReverse() == 1);
        } else if (mode == LightingMode.ALL_WAVE) {
            knobsTabbedPane.getSelectionModel().select(0);
            allKnobsTabbedPane.getSelectionModel().select(2);
            waveHue.setHue(lightingConfig.getWaveHue() & 0xFF);
            waveBrightness.setValue(lightingConfig.getWaveBrightness() & 0xFF);
            waveSpeed.setValue(lightingConfig.getWaveSpeed() & 0xFF);
            waveReverse.setSelected(lightingConfig.getWaveReverse() == 1);
            waveBounce.setSelected(lightingConfig.getWaveBounce() == 1);
        } else if (mode == LightingMode.ALL_BREATH) {
            knobsTabbedPane.getSelectionModel().select(0);
            allKnobsTabbedPane.getSelectionModel().select(3);
            breathHue.setHue(lightingConfig.getBreathHue() & 0xFF);
            breathBrightness.setValue(lightingConfig.getBreathBrightness() & 0xFF);
            breathSpeed.setValue(lightingConfig.getBreathSpeed() & 0xFF);
        } else {
            log.error("unexpected mode in lightingdialog");
        }
    }

    private void initListeners(Slider[] allSliders, CheckBox[] allCheckBoxes) {
        for (var cd : cds)
            cd.customColorProperty().addListener((observable, oldValue, newValue) -> updateColors());
        allKnobColor.customColorProperty().addListener((observable, oldValue, newValue) -> {
            for (var cd : cds)
                cd.setCustomColor(newValue);
            updateColors();
        });
        for (var slider : allSliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        }
        for (var cb : allCheckBoxes) {
            cb.selectedProperty().addListener((observable, oldValue, newValue) -> updateColors());
        }
        for (var cb : volumeFollowingCheckBoxes)
            cb.selectedProperty().addListener((observable, oldValue, newValue) -> updateColors());
        waveHue.getHueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        breathHue.getHueProperty().addListener((observable, oldValue, newValue) -> updateColors());
        knobsTabbedPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateColors());
        allKnobsTabbedPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateColors());
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    private void updateColors() {
        setFollowingControlsVisible(false);
        if (knobsTabbedPane.getSelectionModel().getSelectedIndex() == 0) {
            if (allKnobsTabbedPane.getSelectionModel().getSelectedIndex() == 0) {
                setFollowingControlsVisible(true);
                lightingConfig = LightingConfig.createAllColor(allKnobColor.getCustomColor(), getVolumeTrackingData());
                setDeviceLighting();
            } else if (allKnobsTabbedPane.getSelectionModel().getSelectedIndex() == 1) {
                lightingConfig = LightingConfig.createRainbowAnimation((byte) (int) rainbowPhaseShift.getValue(), (byte) (int) rainbowBrightness.getValue(),
                        (byte) (int) rainbowSpeed.getValue(), rainbowReverse.isSelected());
                setDeviceLighting();
            } else if (allKnobsTabbedPane.getSelectionModel().getSelectedIndex() == 2) {
                lightingConfig = LightingConfig.createWaveAnimation((byte) waveHue.getHue(), (byte) (int) waveBrightness.getValue(), (byte) (int) waveSpeed.getValue(),
                        waveReverse.isSelected(), waveBounce.isSelected());
                setDeviceLighting();
            } else if (allKnobsTabbedPane.getSelectionModel().getSelectedIndex() == 3) {
                lightingConfig = LightingConfig.createBreathAnimation((byte) breathHue.getHue(), (byte) (int) breathBrightness.getValue(), (byte) (int) breathSpeed.getValue());
                setDeviceLighting();
            }
        } else {
            setFollowingControlsVisible(true);
            var colors = IntStream.range(0, device.getKnobCount()).mapToObj(i -> cds.get(i).getCustomColor()).toArray(Color[]::new);
            lightingConfig = LightingConfig.createSingleColor(colors, getVolumeTrackingData());
            setDeviceLighting();
        }
    }

    private void setFollowingControlsVisible(boolean b) {
        volumeFollowBox.setVisible(b);
    }

    private boolean[] getVolumeTrackingData() {
        var ret = new boolean[device.getKnobCount()];
        for (var i = 0; i < ret.length; i++)
            ret[i] = volumeFollowingCheckBoxes.get(i).isSelected();
        return ret;
    }

    private void setVolumeTrackingData(boolean[] data) {
        for (var i = 0; i < data.length; i++)
            volumeFollowingCheckBoxes.get(i).setSelected(data[i]);
    }
}
