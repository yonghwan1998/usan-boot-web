package com.usanmap.usan.util;

/**
 * @date    2026-01-30
 * @author  yongss
 * @desc    두 좌표 간 거리 계산 클래스
 */
public final class GeoDistance {

    private static final double EARTH_RADIUS_M = 6378137.0;

    private GeoDistance() {}

    public static double meters(double centerLat, double centerLng, double targetLat, double targetLng) {
        double dLat = Math.toRadians(targetLat - centerLat);
        double dLng = Math.toRadians(targetLng - centerLng);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLat)) * Math.cos(Math.toRadians(targetLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }
}
