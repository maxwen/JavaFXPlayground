package com.maxwen.osmviewer.nmea;

import java.util.List;

public interface NMEAHandler {
    void onStart();

    void onLocation1(double lon, double lat, double altitude);

    void onLocation2(double speed, double bearing);

    void onSatellites(List<GpsSatellite> satellites);

    void onUnrecognized(String sentence);

    void onBadChecksum(int expected, int actual);

    void onException(Exception e);

    void onFinish();
}
