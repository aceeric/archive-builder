package org.ericace;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Metrics {

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private Instant start;
    private Instant finish;
    private Duration elapsed;

    public Metrics() {

    }

    public void start() {
        start = Instant.now();
    }

    public void finish() {
        finish = Instant.now();
        elapsed = Duration.between(start, finish);
    }

    public void finishAndPrint() {
        finish();
        print();
    }

    public void print() {
        StringBuilder metrics = new StringBuilder(String.format("Start: %s", fmt.format(start)));
        metrics.append(String.format("; Finish: %s", fmt.format(finish)));
        metrics.append(String.format("; Elapsed (HH:MM:SS:millis): %s", formatElapsed(elapsed)));
        System.out.println(metrics);
    }

    private String formatElapsed(Duration elapsed) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(elapsed.toMillis()),
                TimeUnit.MILLISECONDS.toMinutes(elapsed.toMillis()) % 60,
                TimeUnit.MILLISECONDS.toSeconds(elapsed.toMillis()) % 60,
                TimeUnit.MILLISECONDS.toMillis(elapsed.toMillis()) % 1000);
    }
}
