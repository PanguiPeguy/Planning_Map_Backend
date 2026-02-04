package com.enspy.tripplanning.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // Statistiques globales
    private Long totalUsers;
    private Long totalPois;
    private Long totalTrips;
    private Long totalCategories;
    private Long totalLikedPois;
    private Long totalFavoritedPois;

    // Statistiques par catégorie
    private Map<String, Long> poisByCategory;

    // POI les plus populaires (rating)
    private List<TopPoiDTO> topPois;

    // Trips récents
    private List<RecentTripDTO> recentTrips;

    // Statistiques de croissance (optionnel)
    private Map<String, Long> usersByMonth;
    private Map<String, Long> tripsByMonth;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopPoiDTO {
        private Long poiId;
        private String name;
        private String categoryName;
        private Double rating;
        private Integer reviewCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTripDTO {
        private String tripId;
        private String title;
        private String username;
        private String status;
        private String createdAt;
    }
}
