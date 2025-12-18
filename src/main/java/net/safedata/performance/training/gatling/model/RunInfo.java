package net.safedata.performance.training.gatling.model;

import java.time.LocalDateTime;

public record RunInfo(
    String simulationName,
    LocalDateTime date,
    String duration,
    String description
) {}
