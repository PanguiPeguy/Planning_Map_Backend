package com.enspy.tripplanning.planning.service;

import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private static final Map<String, Point> CITY_COORDINATES = new HashMap<>();

    static {
        CITY_COORDINATES.put("Douala", new Point(9.7022, 4.0435));
        CITY_COORDINATES.put("Yaoundé", new Point(11.5021, 3.8480));
        CITY_COORDINATES.put("Bafoussam", new Point(10.4176, 5.4771));
        CITY_COORDINATES.put("Bamenda", new Point(10.1591, 5.9631));
        CITY_COORDINATES.put("Garoua", new Point(13.3924, 9.3034));
        CITY_COORDINATES.put("Maroua", new Point(14.3311, 10.5916));
        CITY_COORDINATES.put("Ngaoundéré", new Point(13.5786, 7.3276));
        CITY_COORDINATES.put("Kribi", new Point(9.9077, 2.9506));
        CITY_COORDINATES.put("Limbe", new Point(9.2140, 4.0121));
        CITY_COORDINATES.put("Buea", new Point(9.2314, 4.1521));
    }

    public Optional<Point> getCoordinates(String cityName) {
        if (cityName == null)
            return Optional.empty();

        // Match exact or case-insensitive
        return CITY_COORDINATES.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(cityName.trim()))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
