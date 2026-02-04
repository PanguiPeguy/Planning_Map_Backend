package com.enspy.tripplanning.planning.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportPlanningRequest {
    private UUID externalId;
    private String name;
}
