package com.huawei.nb.utils.logger;

public final class AndroidLogger {
    private static final Printer LOG_PRINTER = new LoggerPrinter();

    private AndroidLogger() {
    }

    public static Settings init(String tag) {
        return LOG_PRINTER.init(tag);
    }

    public static void v(String message, Object... args) {
        LOG_PRINTER.v(message, args);
    }

    public static void d(String message, Object... args) {
        LOG_PRINTER.d(message, args);
    }

    public static void i(String message, Object... args) {
        LOG_PRINTER.i(message, args);
    }

    public static void w(String message, Object... args) {
        LOG_PRINTER.w(message, args);
    }

    public static void e(String message, Object... args) {
        LOG_PRINTER.e(null, message, args);
    }

    public static void e(Throwable throwable, String message, Object... args) {
        LOG_PRINTER.e(throwable, message, args);
    }
}
