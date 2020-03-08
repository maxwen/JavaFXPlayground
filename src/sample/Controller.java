package sample;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polyline;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    MenuItem closeMenu;
    @FXML
    MenuItem loadMenu;
    @FXML
    Pane mainPane;
    @FXML
    MenuBar menuBar;

    private int mMapZoom = 16;
    private double mMapZeroX;
    private double mMapZeroY;
    private double mCenterLat = 47.793938;
    private double mCenterLon = 12.992203;
    private double mCenterPosX;
    private double mCenterPosY;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        closeMenu.setOnAction(e -> {
            handleExit();
        });
        loadMenu.setOnAction(e -> {
            loadWays();
        });
        mCenterPosX = getPixelXPosForLocationDeg(mCenterLon);
        mCenterPosY = getPixelYPosForLocationDeg(mCenterLat);
        System.out.println("initialize " + mCenterPosX + " : " + mCenterPosY);
    }

    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }

    private void loadWays() {
        DatabaseController db = new DatabaseController();
        db.connextAll();
        List<Double> bbBox = getVisibleBBoxDeg();
        System.out.println(bbBox);
        JsonArray ways = db.getWaysInBboxWithGeom(bbBox.get(0), bbBox.get(1), bbBox.get(2), bbBox.get(3));
        db.disconnectAll();

        for (int i = 0; i < ways.size(); i++) {
            JsonObject way = (JsonObject) ways.get(i);
            JsonArray coords = (JsonArray) way.get("coords");
            displayCoords(coords);
        }
    }

    private Double getPixelXPosForLocationDeg(double lon) {
        return getPixelXPosForLocationRad(GISUtils.deg2rad(lon));
    }

    private Double getPixelXPosForLocationRad(double lon) {
        return GISUtils.lon2pixel(mMapZoom, lon);
    }

    private Double getPixelYPosForLocationDeg(double lat) {
        return getPixelYPosForLocationRad(GISUtils.deg2rad(lat));
    }

    private  Double getPixelYPosForLocationRad(double lat) {
        return GISUtils.lat2pixel(mMapZoom, lat);
    }

    private void displayCoords(JsonArray coords) {
        calcMapZeroPos();
        Polyline polyline = new Polyline();
        Double[] points = new Double[coords.size() * 2];
        int j = 0;
        for (int i = 0; i < coords.size(); i++) {
            JsonArray coord = (JsonArray) coords.get(i);
            double lon = coord.getDouble(0);
            double lat = coord.getDouble(1);

            Double posX = getPixelXPosForLocationDeg(lon);
            Double posY = getPixelYPosForLocationDeg(lat);

            points[j] = posX;
            points[j + 1] = posY;
            j += 2;
        }
        polyline.getPoints().addAll(points);
        polyline.setTranslateX(-mMapZeroX);
        polyline.setTranslateY(-mMapZeroY);
        mainPane.getChildren().add(polyline);
    }

    private void calcMapZeroPos() {
        mMapZeroX = mCenterPosX - mainPane.getWidth() / 2;
        mMapZeroY = mCenterPosY - mainPane.getHeight() / 2;
    }

    private List<Double> getVisibleBBoxDeg() {
        calcMapZeroPos();

        double lat1 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY));
        double lon1 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX));

        double lat2 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY));
        double lon2 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX + mainPane.getWidth()));

        double lat3 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY + mainPane.getHeight()));
        double lon3 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX));

        double lat4 = GISUtils.rad2deg(GISUtils.pixel2lat(mMapZoom, mMapZeroY+ mainPane.getHeight()));
        double lon4 = GISUtils.rad2deg(GISUtils.pixel2lon(mMapZoom, mMapZeroX + mainPane.getWidth()));

        System.out.println(mainPane.getWidth() + " " + mainPane.getHeight());

        List<Double> lonList = new ArrayList<>();
        Collections.addAll(lonList, lon1, lon2, lon3, lon4);
        System.out.println(lonList);

        List<Double> latList = new ArrayList<>();
        Collections.addAll(latList, lat1, lat2, lat3, lat4);
        System.out.println(latList);

        double bboxLon1 = Collections.min(lonList);
        double bboxLat1 = Collections.min(latList);
        double bboxLon2 = Collections.max(lonList);
        double bboxLat2 = Collections.max(latList);

        List<Double> l = new ArrayList<>();
        Collections.addAll(l, bboxLon1, bboxLat1, bboxLon2, bboxLat2);
        return l;
    }
}
