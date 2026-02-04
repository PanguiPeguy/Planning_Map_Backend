package com.enspy.tripplanning.routing.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRoute {
    private String geometry;
    private List<OsrmLeg> legs;
    private Double distance; // meters
    private Double duration; // seconds
    private String weight_name;
    private Double weight;
}
