package com.maxwen.osmviewer.nmea;

import java.util.List;

public class NMEAAdapter implements NMEAHandler {
    @Override
    public void onStart() {

    }

    @Override
    public void onLocation1(double lon, double lat, double altitude) {

    }

    @Override
    public void onLocation2(double speed, double bearing) {

    }

    @Override
    public void onSatellites(List<GpsSatellite> satellites) {

    }

    @Override
    public void onUnrecognized(String sentence) {

    }

    @Override
    public void onBadChecksum(int expected, int actual) {

    }

    @Override
    public void onException(Exception e) {

    }

    @Override
    public void onFinish() {

    }
}
