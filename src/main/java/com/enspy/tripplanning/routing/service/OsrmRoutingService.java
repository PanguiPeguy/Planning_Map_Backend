package com.enspy.tripplanning.routing.service;

import com.enspy.tripplanning.routing.dto.RouteResponse;
import com.enspy.tripplanning.routing.dto.RouteSegmentDTO;
import com.enspy.tripplanning.routing.dto.osrm.OsrmLeg;
import com.enspy.tripplanning.routing.dto.osrm.OsrmResponse;
import com.enspy.tripplanning.routing.dto.osrm.OsrmRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OsrmRoutingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${osrm.url:http://router.project-osrm.org}")
    private String osrmUrl;

    public Mono<RouteResponse> calculateRoute(Point start, Point end, List<Point> waypoints) {
        StringBuilder coordinatesBuilder = new StringBuilder();

        // Start
        coordinatesBuilder.append(start.getX()).append(",").append(start.getY());

        // Waypoints
        if (waypoints != null && !waypoints.isEmpty()) {
            for (Point wp : waypoints) {
                coordinatesBuilder.append(";").append(wp.getX()).append(",").append(wp.getY());
            }
        }

        // End
        coordinatesBuilder.append(";").append(end.getX()).append(",").append(end.getY());

        String coordinates = coordinatesBuilder.toString();

        String url = String.format("%s/route/v1/driving/%s?overview=full&geometries=polyline&steps=true",
                osrmUrl, coordinates);

        log.debug("Calling OSRM: {}", url);

        return webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(OsrmResponse.class)
                .map(osrmResponse -> mapToRouteResponse(osrmResponse, start, end));
    }

    private RouteResponse mapToRouteResponse(OsrmResponse osrmResponse, Point start, Point end) {
        if (osrmResponse == null || osrmResponse.getRoutes() == null || osrmResponse.getRoutes().isEmpty()) {
            return RouteResponse.builder()
                    .found(false)
                    .errorMessage("No route found by OSRM")
                    .build();
        }

        OsrmRoute osrmRoute = osrmResponse.getRoutes().get(0);

        // Basic stats
        Double distanceKm = osrmRoute.getDistance() != null ? osrmRoute.getDistance() / 1000.0 : 0.0;
        Integer timeSeconds = osrmRoute.getDuration() != null ? osrmRoute.getDuration().intValue() : 0;

        // Format time
        String formattedTime = formatTime(timeSeconds);

        // Map segments/instructions (simplified for now)
        List<RouteSegmentDTO> segments = new ArrayList<>();
        List<String> instructions = new ArrayList<>();

        // We could iterate over legs/steps to build detailed instructions
        if (osrmRoute.getLegs() != null) {
            for (OsrmLeg leg : osrmRoute.getLegs()) {
                if (leg.getSteps() != null) {
                    // Mapping simple instructions if available
                    leg.getSteps().forEach(step -> {
                        // Extract name or maneuver if possible (requires more complex DTO mapping for
                        // step)
                        // For now, we trust the geometry is enough for display
                    });
                }
            }
        }

        return RouteResponse.builder()
                .found(true)
                .totalDistanceKm(distanceKm)
                .totalTimeSeconds(timeSeconds)
                .formattedTime(formattedTime)
                .geometryEncoded(osrmRoute.getGeometry())
                .segments(segments) // Empty segments for now, frontend mainly needs geometry
                .instructions(instructions)
                .build();
    }

    private String formatTime(Integer totalSeconds) {
        if (totalSeconds == null)
            return "";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dmin", hours, minutes);
        } else {
            return String.format("%dmin", minutes);
        }
    }
}
