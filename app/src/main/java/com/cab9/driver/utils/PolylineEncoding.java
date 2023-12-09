package com.cab9.driver.utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by parag on 12/20/2018.
 */

public class PolylineEncoding {

    private static final double EARTHRADIUS = 6366198;

    /** Decodes an encoded path string into a sequence of LatLngs. */
    public static List<LatLng> decode(final String encodedPath) {
        if(encodedPath==null)
            return new ArrayList<>();
        int len = encodedPath.length();

        final List<LatLng> path = new ArrayList<LatLng>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new LatLng(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    public static List<LatLng> bspline(List<LatLng> poly) {

        if (poly.get(0).latitude != poly.get(poly.size()-1).latitude || poly.get(0).longitude != poly.get(poly.size()-1).longitude){
            poly.add(new LatLng(poly.get(0).latitude,poly.get(0).longitude));
        }
        else{
            poly.remove(poly.size()-1);
        }
        poly.add(0,new LatLng(poly.get(poly.size()-1).latitude,poly.get(poly.size()-1).longitude));
        poly.add(new LatLng(poly.get(1).latitude,poly.get(1).longitude));

        Double[] lats = new Double[poly.size()];
        Double[] lons = new Double[poly.size()];

        for (int i=0;i<poly.size();i++){
            lats[i] = poly.get(i).latitude;
            lons[i] = poly.get(i).longitude;
        }

        double ax, ay, bx, by, cx, cy, dx, dy, lat, lon;
        float t;
        int i;
        List<LatLng> points = new ArrayList<>();
        // For every point
        for (i = 2; i < lats.length - 2; i++) {
            for (t = 0; t < 1; t += 0.2) {
                ax = (-lats[i - 2] + 3 * lats[i - 1] - 3 * lats[i] + lats[i + 1]) / 6;
                ay = (-lons[i - 2] + 3 * lons[i - 1] - 3 * lons[i] + lons[i + 1]) / 6;
                bx = (lats[i - 2] - 2 * lats[i - 1] + lats[i]) / 2;
                by = (lons[i - 2] - 2 * lons[i - 1] + lons[i]) / 2;
                cx = (-lats[i - 2] + lats[i]) / 2;
                cy = (-lons[i - 2] + lons[i]) / 2;
                dx = (lats[i - 2] + 4 * lats[i - 1] + lats[i]) / 6;
                dy = (lons[i - 2] + 4 * lons[i - 1] + lons[i]) / 6;
                lat = ax * Math.pow(t + 0.1, 3) + bx * Math.pow(t + 0.1, 2) + cx * (t + 0.1) + dx;
                lon = ay * Math.pow(t + 0.1, 3) + by * Math.pow(t + 0.1, 2) + cy * (t + 0.1) + dy;
                points.add(new LatLng(lat, lon));
            }
        }
        return points;

    }

    /**
     * Create a new LatLng which lies toNorth meters north and toEast meters
     * east of startLL
     */
    public static LatLng move(LatLng startLL, double toNorth, double toEast) {
        double lonDiff = meterToLongitude(toEast, startLL.latitude);
        double latDiff = meterToLatitude(toNorth);
        return new LatLng(startLL.latitude + latDiff, startLL.longitude
                + lonDiff);
    }

    public static double meterToLongitude(double meterToEast, double latitude) {
        double latArc = Math.toRadians(latitude);
        double radius = Math.cos(latArc) * EARTHRADIUS;
        double rad = meterToEast / radius;
        return Math.toDegrees(rad);
    }


    public static double meterToLatitude(double meterToNorth) {
        double rad = meterToNorth / EARTHRADIUS;
        return Math.toDegrees(rad);
    }

}
