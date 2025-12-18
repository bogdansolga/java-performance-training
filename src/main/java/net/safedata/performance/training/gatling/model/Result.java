package net.safedata.performance.training.gatling.model;

public enum Result {
    PASS,
    WARN,
    FAIL;

    public Result worseOf(Result other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
