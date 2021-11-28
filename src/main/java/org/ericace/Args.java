package org.ericace;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.*;

/**
 * A very rudimentary command-line option parser. Accepts short-form opts like -t and long-form like --threads.
 * Accepts this form: -t 1 and --threads 1, as well as this form -t=1 and --threads=1. Does not accept
 * concatenated short form opts in cases where such opts don't accept params. E.g. doesn't handle: -ot=1 where
 * -o is a parameterless option, and -t takes a value (one in this example.) Doesn't have great error handling
 * so - is somewhat fragile with respect to parsing errors.
 */
class Args {
    Scenario scenario = null;
    BinaryProvider binaryProvider = null;
    int documentCount = 0;
    List<Integer> binarySizes = new ArrayList<>();
    int cacheSize = 0;
    int threadCount = 0;
    int metricsPort = 0;
    String archiveFqpn = null;
    String bucketName = null;
    String region = null;
    boolean showConfig = false;
    List<String> keys = new ArrayList<>();
    List<String> loggers = new ArrayList<>();

    private String parseMessage;

    /**
     * Parse the command line args and return them in an {@link Args} instance
     *
     * @param args from the JVM command line
     * @return parsed args, or null if there was an error. If an error, the message will have been
     * displayed to the console by the class before this method returns to the caller.
     */
    static Args parse(String[] args) {
        Args parsedArgs = new Args();
        return parsedArgs.parseArgs(args) ? parsedArgs : null;
    }

    /**
     * Displays the configuration derived from the command line to the console.
     */
    void showConfig() {
        String cfg = "Scenario: " + scenario + "\n" +
                "Binary Provider: " + binaryProvider + "\n" +
                "Document Count: " + documentCount + "\n" +
                "Metrics Port: " + metricsPort + "\n" +
                "TAR File: " + archiveFqpn + "\n" +
                "Show Config: " + showConfig + "\n" +
                "Loggers: " + loggers + "\n";
        if (scenario == Scenario.multi) {
            cfg += "Cache Size: " + cacheSize + "\n" +
                    "Thread Count: " + threadCount + "\n";
        }
        if (binaryProvider == BinaryProvider.fake) {
            cfg += "Binary Sizes: " + binarySizes + "\n";
        }
        if (binaryProvider == BinaryProvider.s3client || binaryProvider == BinaryProvider.transfermanager) {
            cfg += "Bucket Name: " + bucketName + "\n" +
                    "Region: " + region + "\n" +
                    "Keys: " + keys + "\n";
        }
        System.out.println(cfg);
    }

    /**
     * Parses the command line.
     *
     * @param args command-line args
     * @return False if there was an arg parse error, else true
     */
    private boolean parseArgs(String[] args) {
        Queue<String> argQueue = new LinkedList<>();
        for (String arg : args) {
            String[] s = arg.split("=");
            argQueue.add(s[0]);
            if (s.length == 2) {
                argQueue.add(s[1]);
            }
        }
        String arg;
        boolean parsedOk = true;
        while (parsedOk & (arg = argQueue.poll()) != null) {
            try {
                switch (arg.toLowerCase()) {
                    case "-l":
                    case "--loggers":
                        if (!parseLoggers(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-c":
                    case "--scenario":
                        if (!parseScenario(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-b":
                    case "--binary-provider":
                        if (!parseBinaryProvider(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-d":
                    case "--document-count":
                        if (!parseDocumentCount(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-s":
                    case "--binary-size":
                        if (!parseBinarySizes(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-t":
                    case "--threads":
                        if (!parseThreadCount(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-z":
                    case "--cache-size":
                        if (!parseCacheSize(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-a":
                    case "--archive":
                        if (!parseArchiveFqpn(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-u":
                    case "--bucket":
                        if (!parseBucketName(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-r":
                    case "--region":
                        if (!parseRegion(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-k":
                    case "--keys":
                        if (!parseKeys(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-m":
                    case "--metrics-port":
                        if (!parseMetricsPort(argQueue.poll())) {
                            parsedOk = false;
                        }
                        break;
                    case "-f":
                    case "--show-config":
                        showConfig = true;
                        break;
                    case "-h":
                    case "--help":
                        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("help");
                             InputStreamReader isrdr = new InputStreamReader(is);
                             BufferedReader brdr = new BufferedReader(isrdr)) {
                            brdr.lines().forEach(System.out::println);
                        }
                        return false;
                    default:
                        parseMessage = "ERROR: unknown option: " + arg;
                        parsedOk = false;
                        break;
                }
            } catch (Exception e) {
                parseMessage = "ERROR parsing the command line: " + e.getMessage();
                parsedOk = false;
                break;
            }
        }
        if (!parsedOk) {
            System.out.println("Error parsing args: " + parseMessage);
            return false;
        }
        setDefaults();
        if (!optsAreValid()) {
            System.out.println("Error parsing args: " + parseMessage);
            return false;
        }
        return true;
    }

    /**
     * Sets default values for various options
     */
    private void setDefaults() {
        if (scenario == null) scenario = Scenario.single;
        if (binaryProvider == null) binaryProvider = BinaryProvider.fake;
        if (documentCount == 0) documentCount = 50_000;
        if (metricsPort == 0) metricsPort = 1234;
        if (binaryProvider == BinaryProvider.fake && binarySizes.size() == 0) binarySizes.add(1000);
        if (scenario == Scenario.multi) {
            if (cacheSize == 0) cacheSize = 10_000;
            if (threadCount == 0) threadCount = 10;
        }
    }

    /**
     * Validates the current args in instance state
     *
     * @return true if valid, else false
     */
    private boolean optsAreValid() {
        if (archiveFqpn == null) {
            parseMessage = "Missing required FQPN of TAR file to create";
            return false;
        }
        if (binaryProvider == BinaryProvider.s3client || binaryProvider == BinaryProvider.transfermanager) {
            if (region == null || bucketName == null || keys.size() == 0) {
                parseMessage = "The s3client and transfer manager binary providers requires all three of: bucket, region, and keys";
                return false;
            }
        } else if (region != null || bucketName != null || keys.size() != 0) {
            parseMessage = "The fake binary provider doesn't use: bucket, region, or keys";
            return false;
        }
        if (scenario == Scenario.single && (threadCount != 0 || cacheSize != 0)) {
            parseMessage = "Thread count and cache size only valid for the multi-threaded scenario";
            return false;
        }
        return true;
    }

    /**
     * Parses the --scenario opt
     * @return true if ok
     */
    private boolean parseScenario(String param) {
        if (notParseable(param)) return false;
        List<String> scenarios = Arrays.asList(Scenario.single.name(), Scenario.multi.name());
        if (!scenarios.contains(param)) {
            parseMessage = "Unknown scenario: " + param;
            return false;
        }
        scenario = Scenario.valueOf(param);
        return true;
    }

    /**
     * Parses the --binary-provider opt
     * @return true if ok
     */
    private boolean parseBinaryProvider(String param) {
        if (notParseable(param)) return false;
        List<String> binaryProviders = Arrays.asList(BinaryProvider.fake.name(), BinaryProvider.s3client.name(),
                BinaryProvider.transfermanager.name());
        if (!binaryProviders.contains(param)) {
            parseMessage = "Unknown binary provider: " + param;
            return false;
        }
        binaryProvider = BinaryProvider.valueOf(param);
        return true;
    }

    /**
     * Parses the --document-count opt
     * @return true if ok
     */
    private boolean parseDocumentCount(String param) {
        if (notParseable(param)) return false;
        documentCount = safeParseInt(param);
        if (documentCount < 0) {
            parseMessage = "Invalid value for document count: " + param;
            return false;
        }
        return true;
    }

    /**
     * Parses the --metrics-port opt
     * @return true if ok
     */
    private boolean parseMetricsPort(String param) {
        if (notParseable(param)) return false;
        metricsPort = safeParseInt(param);
        if (metricsPort < 0) {
            parseMessage = "Invalid value for metrics port: " + param;
            return false;
        }
        return true;
    }

    /**
     * Parses the --binary-size opt
     * @return true if ok
     */
    private boolean parseBinarySizes(String param) {
        if (notParseable(param)) return false;
        String[] params = param.split(",");
        for (String item : params) {
            if (safeParseInt(item) < 0) {
                parseMessage = "Invalid value for binary size: " + item;
                return false;
            }
            binarySizes.add(safeParseInt(item));
            if (binarySizes.size() > 2) {
                parseMessage = "Invalid too many values for binary size option, at: " + item;
                return false;
            }
        }
        return true;
    }

    /**
     * Parses the --cache-size opt
     * @return true if ok
     */
    private boolean parseCacheSize(String param) {
        if (notParseable(param)) return false;
        cacheSize = safeParseInt(param);
        if (cacheSize < 0) {
            parseMessage = "Invalid value for cache size: " + param;
            return false;
        }
        return true;
    }

    /**
     * Parses the --threads opt
     * @return true if ok
     */
    private boolean parseThreadCount(String param) {
        if (notParseable(param)) return false;
        threadCount = safeParseInt(param);
        if (threadCount < 0) {
            parseMessage = "Invalid value for thread count: " + param;
            return false;
        }
        return true;
    }

    /**
     * Parses the --archive opt
     * @return true if ok
     */
    private boolean parseArchiveFqpn(String param) {
        if (notParseable(param)) return false;
        param = param.replaceFirst("^~", System.getProperty("user.home"));
        try {
            if (Paths.get(param).getParent().toAbsolutePath().toFile().isDirectory()) {
                archiveFqpn = Paths.get(param).toAbsolutePath().toString();
            }
        } catch (Exception e) {
            // NOP
        }
        if (archiveFqpn == null) {
            parseMessage = "TAR FQPN does not specify an existing directory on the filesystem: " + param;
            return false;
        }
        return true;
    }

    /**
     * Parses the --bucket opt
     * @return true if ok
     */
    private boolean parseBucketName(String param) {
        if (notParseable(param)) return false;
        bucketName = param;
        return true;
    }

    /**
     * Parses the --region opt
     * @return true if ok
     */
    private boolean parseRegion(String param) {
        if (notParseable(param)) return false;
        region = param;
        return true;
    }

    /**
     * Parses the --loggers opt
     * @return true if ok
     */
    private boolean parseLoggers(String param) {
        if (notParseable(param)) return false;
        loggers = Arrays.asList(param.split(","));
        return true;
    }

    /**
     * Parses the --keys opt
     * @return true if ok
     */
    private boolean parseKeys(String param) {
        if (notParseable(param)) return false;
        keys = Arrays.asList(param.split(","));
        return true;
    }

    /**
     * Determines if a param is parseable.
     *
     * @return true if parseable
     */
    private boolean notParseable(String param) {
        if (param == null || param.trim().length() == 0) {
            parseMessage = "Expected a parameter";
            return true;
        } else if (param.startsWith("-")) {
            parseMessage = "Expected a parameter, but looks like an option: " + param;
            return true;
        }
        return false;
    }

    /**
     * Safely parses the passed param as an integer value
     *
     * @return the value, or -1 if it was not parseable
     */
    private int safeParseInt(String param) {
        try {
            return Integer.parseInt(param);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Defines the archive builder scenarios - single threaded, or multi-threaded
     */
    enum Scenario {single, multi}

    /**
     * Defines the binary providers - fake, s3client, or transfer manager
     */
    enum BinaryProvider {fake, s3client, transfermanager}

}
