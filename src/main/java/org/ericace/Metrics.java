package org.ericace;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A simple class to tabulate some basic metrics
 */
public class Metrics {

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private Instant start;
    private Instant finish;
    private Duration elapsed;
    private long binaryBytesWritten;

    public Metrics() {
    }

    /**
     * Maintains a running tally of {@link org.ericace.binary.BinaryObject} bytes written
     *
     * @param binaryBytesWritten for one Binary
     */
    public void addBinaryBytesWritten(long binaryBytesWritten) {
        this.binaryBytesWritten += binaryBytesWritten;
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
        StringBuilder metrics = new StringBuilder(String.format(" Start: %s\n", fmt.format(start)));
        metrics.append(String.format(" Finish: %s\n", fmt.format(finish)));
        metrics.append(String.format(" Elapsed (HH:MM:SS:millis): %s\n", formatElapsed(elapsed)));
        metrics.append(String.format(" Binary bytes written: %,d\n", binaryBytesWritten));

        float bpsec = ((float) binaryBytesWritten / (float) elapsed.toMillis()) * 1000F;
        metrics.append(String.format(" Binary bytes/sec: %,f\n", bpsec));

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
