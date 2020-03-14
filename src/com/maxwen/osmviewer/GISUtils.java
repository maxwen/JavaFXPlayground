package com.maxwen.osmviewer;

public class GISUtils {
    private static final int TILESIZE = 256;
    private static final double M_LN2 = 0.69314718055994530942;
    private static final int RADIUS_EARTH = 6371;
    private static final double PI2 = 2 * Math.PI;

    // see http://williams.best.vwh.net/avform.htm

    public static double deg2rad(double deg) {
        return Math.toRadians(deg);
    }

    public static double rad2deg(double rad) {
        return Math.toDegrees(rad);
    }

    private static double atanh(double x) {
        return 0.5*Math.log( Math.abs((x + 1.0) / (x - 1.0)));
    }

    public static double lat2pixel(int zoom, double lat) {
        double lat_m = atanh(Math.sin(lat));
        int z=(1 << zoom);
        return -(lat_m * TILESIZE * z ) / PI2 +(z * TILESIZE)/2;
    }

    public static double lon2pixel(int zoom, double lon) {
        int z=(1 << zoom);
        return  ( lon * TILESIZE * z ) / PI2 +(z * TILESIZE)/2;
    }

    public static double pixel2lon(int zoom, double pixel_x) {
        double z=Math.exp(zoom * M_LN2);
        return (((pixel_x - ( z * (TILESIZE/2) ) ) * PI2) / (TILESIZE * z ));
    }

    public static double pixel2lat(int zoom, double pixel_y) {
        double z=Math.exp(zoom * M_LN2);
        double lat_m = ((-( pixel_y - ( z * (TILESIZE/2) ) ) * PI2) /(TILESIZE * z));
        return Math.asin(Math.tanh(lat_m));
    }

    public static double degToMeter(double meter) {
        double deg_to_meter = (40000 * 1000) / 360;
        return meter / deg_to_meter;
    }
}
