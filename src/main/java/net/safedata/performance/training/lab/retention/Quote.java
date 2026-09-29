package net.safedata.performance.training.lab.retention;

import java.time.Instant;

public record Quote(String id, String sku, double price, Instant createdAt, byte[] pricingSnapshot) {
}
