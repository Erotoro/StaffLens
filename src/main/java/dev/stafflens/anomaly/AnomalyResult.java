package dev.stafflens.anomaly;

import java.util.List;

/** Outcome of an anomaly check: whether anything matched and the human-readable reasons. */
public record AnomalyResult(boolean flagged, List<String> reasons) {

    private static final AnomalyResult CLEAN = new AnomalyResult(false, List.of());

    public static AnomalyResult clean() {
        return CLEAN;
    }

    public static AnomalyResult flagged(List<String> reasons) {
        return new AnomalyResult(true, List.copyOf(reasons));
    }

    public String describe() {
        return String.join("; ", reasons);
    }
}
