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
 * so - is fragile with respect to parsing errors.
 */
class Args {
    static Scenario scenario = null;
    static BinaryProvider binaryProvider = null;
    static int documentCount = 0;
    static List<Integer> binarySizes = new ArrayList<>();
    static int cacheSize = 0;
    static int threadCount = 0;
    static int metricsPort = 0;
    static String archiveFqpn = null;
    static String bucketName = null;
    static String region = null;
    static List<String> keys = new ArrayList<>();

    private static String parseMessage;

    /**
     * Displays the configuration derived from the command line to the console.
     */
    static void showConfig() {
        StringBuilder cfg = new StringBuilder();
        cfg.append("Scenario: " + scenario  + "\n");
        cfg.append("Binary Provider: " + binaryProvider  + "\n");
        cfg.append("Document Count: " + documentCount  + "\n");
        cfg.append("Metrics Port: " + metricsPort  + "\n");
        cfg.append("TAR File: " + archiveFqpn  + "\n");
        if (scenario == Scenario.multi) {
            cfg.append("Cache Size: " + cacheSize + "\n");
            cfg.append("Thread Count: " + threadCount + "\n");
        }
        if (binaryProvider == BinaryProvider.fake) {
            cfg.append("Binary Sizes: " + binarySizes + "\n");
        }
        if (binaryProvider == BinaryProvider.s3client) {
            cfg.append("Bucket Name: " + bucketName + "\n");
            cfg.append("Region: " + region + "\n");
            cfg.append("Keys: " + keys + "\n");
        }
        System.out.println(cfg);
    }
    /**
     * Parses the command line.
     *
     * @param args command-line args
     * @return False if there was an arg parse error, else return True
     */
    static boolean parseArgs(String[] args) {
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
                    case "-h":
                    case "--help":
                        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("help");
                             InputStreamReader rdr = new InputStreamReader(is);
                             BufferedReader reader = new BufferedReader(rdr)) {
                            reader.lines().forEach(System.out::println);
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

    private static void setDefaults() {
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

    private static boolean optsAreValid() {
        if (archiveFqpn == null) {
            parseMessage = "Missing required FQPN of TAR file to create";
            return false;
        }
        if (binaryProvider == BinaryProvider.s3client) {
            if (region == null || bucketName == null || keys.size() == 0) {
                parseMessage = "The s3client binary provider requires all three of: bucket, region, and keys";
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

    private static boolean parseScenario(String param) {
        if (notParseable(param)) return false;
        List<String> scenarios = Arrays.asList("single", "multi");
        if (!scenarios.contains(param)) {
            parseMessage = "Unknown scenario: " + param;
            return false;
        }
        Args.scenario = Scenario.valueOf(param);
        return true;
    }

    private static boolean parseBinaryProvider(String param) {
        if (notParseable(param)) return false;
        List<String> binaryProviders = Arrays.asList("fake", "s3client");
        if (!binaryProviders.contains(param)) {
            parseMessage = "Unknown binary provider: " + param;
            return false;
        }
        Args.binaryProvider = BinaryProvider.valueOf(param);
        return true;
    }

    private static boolean parseDocumentCount(String param) {
        if (notParseable(param)) return false;
        documentCount = safeParseInt(param);
        if (documentCount < 0) {
            parseMessage = "Invalid value for document count: " + param;
            return false;
        }
        return true;
    }

    private static boolean parseMetricsPort(String param) {
        if (notParseable(param)) return false;
        metricsPort = safeParseInt(param);
        if (metricsPort < 0) {
            parseMessage = "Invalid value for metrics port: " + param;
            return false;
        }
        return true;
    }

    private static boolean parseBinarySizes(String param) {
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

    private static boolean parseCacheSize(String param) {
        if (notParseable(param)) return false;
        cacheSize = safeParseInt(param);
        if (cacheSize < 0) {
            parseMessage = "Invalid value for cache size: " + param;
            return false;
        }
        return true;
    }

    private static boolean parseThreadCount(String param) {
        if (notParseable(param)) return false;
        threadCount = safeParseInt(param);
        if (threadCount < 0) {
            parseMessage = "Invalid value for thread count: " + param;
            return false;
        }
        return true;
    }

    private static boolean parseArchiveFqpn(String param) {
        if (notParseable(param)) return false;
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

    private static boolean parseBucketName(String param) {
        if (notParseable(param)) return false;
        bucketName = param;
        return true;
    }

    private static boolean parseRegion(String param) {
        if (notParseable(param)) return false;
        region = param;
        return true;
    }

    private static boolean parseKeys(String param) {
        if (notParseable(param)) return false;
        keys = Arrays.asList(param.split(","));
        return true;
    }

    private static boolean notParseable(String param) {
        if (param == null || param.trim().length() == 0) {
            parseMessage = "Expected a parameter";
            return true;
        } else if (param.startsWith("-")) {
            parseMessage = "Expected a parameter, but looks like an option: " + param;
            return true;
        }
        return false;
    }

    private static int safeParseInt(String param) {
        try {
            return Integer.parseInt(param);
        } catch (Exception e) {
            return -1;
        }
    }

    enum Scenario {single, multi}

    enum BinaryProvider {fake, s3client}

}
