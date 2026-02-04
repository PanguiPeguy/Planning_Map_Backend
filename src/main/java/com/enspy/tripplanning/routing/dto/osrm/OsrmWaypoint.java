package com.enspy.tripplanning.routing.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmWaypoint {
    private String hint;
    private Double distance;
    private String name;
    private List<Double> location;
}
