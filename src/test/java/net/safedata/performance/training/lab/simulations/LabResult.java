package net.safedata.performance.training.lab.simulations;

/** One plain-language result line for labs judged by a count, printed when the run ends. */
final class LabResult {

    private static volatile String line = "LAB RESULT: no result - the final check did not run";

    private LabResult() {
    }

    static void record(String what, Long actual, long limit) {
        if (actual == null) {
            line = "LAB RESULT: no result - the application did not answer (did it run out of memory?)";
        } else {
            line = "LAB RESULT: " + (actual <= limit ? "PASSED" : "FAILED") + " - " + what + " = " + actual
                    + " (must be <= " + limit + ")";
        }
    }

    /** Prints the line when Gatling's JVM exits, i.e. after its own report, as the last thing before Maven's result. */
    static void printAtExit() {
        Runtime.getRuntime().addShutdownHook(new Thread(LabResult::print));
    }

    private static void print() {
        String rule = "=".repeat(line.length());
        System.out.println();
        System.out.println(rule);
        System.out.println(line);
        System.out.println(rule);
    }
}
