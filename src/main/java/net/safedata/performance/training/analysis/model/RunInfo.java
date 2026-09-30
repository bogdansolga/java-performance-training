package net.safedata.performance.training.analysis.model;

import java.time.LocalDateTime;

public record RunInfo(
    String simulationName,
    LocalDateTime date,
    String duration,
    String description
) {}
