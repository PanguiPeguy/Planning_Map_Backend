package com.enspy.tripplanning.routing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * Représente une portion du graphe routier chargée en mémoire.
 * Utilisé pour optimiser l'algorithme A* en évitant les appels DB répétitifs.
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subgraph {
    private Map<Long, RoadNode> nodes;
    private Map<Long, List<RoadEdge>> neighbors;

    public void index(List<RoadNode> nodeList, List<RoadEdge> edgeList) {
        this.nodes = new java.util.HashMap<>(nodeList.size());
        for (RoadNode n : nodeList) {
            this.nodes.put(n.getNodeId(), n);
        }

        this.neighbors = new java.util.HashMap<>(nodeList.size());
        for (RoadEdge edge : edgeList) {
            // Source -> Target
            Long src = edge.getSourceNodeId();
            Long tgt = edge.getTargetNodeId();

            // Loguer pour débogage du mapping
            log.debug("Indexing Edge ID: {}, Source: {}, Target: {}", edge.getEdgeId(), src, tgt);

            if (src != null) {
                this.neighbors.computeIfAbsent(src, k -> new java.util.ArrayList<>()).add(edge);
            }

            // Target -> Source (if not one way)
            if (edge.getOneWay() == null || !edge.getOneWay()) {
                if (tgt != null) {
                    this.neighbors.computeIfAbsent(tgt, k -> new java.util.ArrayList<>()).add(edge);
                }
            }
        }
    }

    public RoadNode getNode(Long id) {
        return nodes.get(id);
    }

    public List<RoadEdge> getNeighbors(Long id) {
        return neighbors.getOrDefault(id, List.of());
    }
}
