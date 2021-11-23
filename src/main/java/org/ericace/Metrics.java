package org.ericace;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger logger = LogManager.getLogger(Metrics.class);

    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private Instant start;
    private Instant finish;
    private Duration elapsed;
    private long binaryBytesWritten;
    private double binaryBytesDownloaded;
    private double binaryDownloadCount;

    private long downloadElapsed;

    public Metrics() {}

    /**
     * Maintains a running tally of {@link org.ericace.binary.BinaryObject} bytes written to the TAR
     *
     * @param binaryBytesWritten for one Binary
     */
    public void addBinaryBytesWritten(long binaryBytesWritten) {
        this.binaryBytesWritten += binaryBytesWritten;
    }

    public void setBinaryBytesDownloaded(double binaryBytesDownloaded) {
        this.binaryBytesDownloaded = binaryBytesDownloaded;
    }

    public void setDownloadElapsed(long downloadElapsed) {
        this.downloadElapsed = downloadElapsed;
    }

    public void setBinaryDownloadCount(double binaryDownloadCount) {
        this.binaryDownloadCount = binaryDownloadCount;
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
        StringBuilder metrics = new StringBuilder();
        metrics.append(String.format("%41s: %s\n", "Start", fmt.format(start)));
        metrics.append(String.format("%41s: %s\n", "Finish", fmt.format(finish)));
        metrics.append(String.format("%41s: %s\n", "Elapsed (HH:MM:SS:millis)", formatElapsed(elapsed)));
        metrics.append(String.format("%41s: %,d\n", "Binary bytes written", binaryBytesWritten));
        metrics.append(String.format("%41s: %,f\n", "Binary bytes downloaded", binaryBytesDownloaded));

        float bpsec = ((float) binaryBytesDownloaded / (float) downloadElapsed) * 1000F;
        metrics.append(String.format("%41s: %,f\n", "Binary bytes downloaded/sec", bpsec));
        metrics.append(String.format("%41s: %s\n", "Binary download elapsed (HH:MM:SS:millis)", formatElapsed(downloadElapsed)));

        logger.info(metrics);
    }

    private String formatElapsed(Duration elapsed) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(elapsed.toMillis()),
                TimeUnit.MILLISECONDS.toMinutes(elapsed.toMillis()) % 60,
                TimeUnit.MILLISECONDS.toSeconds(elapsed.toMillis()) % 60,
                TimeUnit.MILLISECONDS.toMillis(elapsed.toMillis()) % 1000);
    }

    private String formatElapsed(long millis) {
        return String.format("%02d:%02d:%02d.%03d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) % 60,
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60,
                TimeUnit.MILLISECONDS.toMillis(millis) % 1000);
    }
}
