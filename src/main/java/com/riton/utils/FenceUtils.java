package com.riton.utils;

// 计算基础距离
public final class FenceUtils {

    private static final double EARTH_RADIUS_METERS = 6378137.0;

    private FenceUtils() {
    }

    public static Fence buildSquareFence(double lng, double lat, double distanceMeters) {
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("distanceMeters must be >= 0");
        }

        double latDelta = Math.toDegrees(distanceMeters / EARTH_RADIUS_METERS);
        double cosLat = Math.cos(Math.toRadians(lat));
        double lngDelta = cosLat == 0 ? 180.0 : Math.toDegrees(distanceMeters / (EARTH_RADIUS_METERS * cosLat));

        double minLng = Math.max(-180.0, lng - lngDelta);
        double maxLng = Math.min(180.0, lng + lngDelta);
        double minLat = Math.max(-90.0, lat - latDelta);
        double maxLat = Math.min(90.0, lat + latDelta);
        return new Fence(minLng, maxLng, minLat, maxLat);
    }

    public static final class Fence {
        private final double minLng;
        private final double maxLng;
        private final double minLat;
        private final double maxLat;

        public Fence(double minLng, double maxLng, double minLat, double maxLat) {
            this.minLng = minLng;
            this.maxLng = maxLng;
            this.minLat = minLat;
            this.maxLat = maxLat;
        }

        public double getMinLng() {
            return minLng;
        }

        public double getMaxLng() {
            return maxLng;
        }

        public double getMinLat() {
            return minLat;
        }

        public double getMaxLat() {
            return maxLat;
        }
    }
}
