package com.enspy.tripplanning.routing.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmLeg {
    private Double distance; // meters
    private Double duration; // seconds
    private String summary;
    private Double weight; // Added just in case
    private List<Map<String, Object>> steps;
}
