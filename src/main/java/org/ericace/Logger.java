package org.ericace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple console logger that supports the ability to filter messages by the emanating class
 * name and/or message content.
 */
public class Logger {

    private final static List<Class<?>> classFilters = new ArrayList<>();
    private final static List<String> msgFilters = new ArrayList<>();
    private static boolean silent = false;

    public static void classFilter(Class<?>... classes) {
        classFilters.addAll(Arrays.asList(classes));
    }

    public static void messageFilter(String... fragments) {
        msgFilters.addAll(Arrays.asList(fragments));
    }

    public static void log(Class<?> c, String msg) {
        log(c, msg, false);
    }

    public static void setSilent() {
        silent = true;
    }

    public static void log(Class<?> c, String msg, boolean withThreadID) {
        if (silent) {
            return;
        }

        boolean shouldPrint = classFilters.size() == 0;

        if (!shouldPrint) {
            for (Class<?> filterClass : classFilters) {
                if (c == filterClass) {
                    shouldPrint = true;
                    break;
                }
            }
        }

        if (!shouldPrint) {
            return;
        }

        if (msgFilters.size() != 0) {
            shouldPrint = false;
            for (String msgFilter : msgFilters) {
                if (msg.toLowerCase().contains(msgFilter.toLowerCase())) {
                    shouldPrint = true;
                    break;
                }
            }
            if (!shouldPrint) {
                return;
            }
        }

        String className = c.getSimpleName();
        if (withThreadID) {
            className += "(" + Thread.currentThread().getId() + ")";
        }
        System.out.println(className + ": " + msg);
    }
}
