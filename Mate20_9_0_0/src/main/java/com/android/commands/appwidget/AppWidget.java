package com.android.commands.appwidget;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import com.android.internal.appwidget.IAppWidgetService.Stub;
import java.io.PrintStream;

public class AppWidget {
    private static final String USAGE = "usage: adb shell appwidget [subcommand] [options]\n\nusage: adb shell appwidget grantbind --package <PACKAGE>  [--user <USER_ID> | current]\n  <PACKAGE> an Android package name.\n  <USER_ID> The user id under which the package is installed.\n  Example:\n  # Grant the \"foo.bar.baz\" package to bind app widgets for the current user.\n  adb shell grantbind --package foo.bar.baz --user current\n\nusage: adb shell appwidget revokebind --package <PACKAGE> [--user <USER_ID> | current]\n  <PACKAGE> an Android package name.\n  <USER_ID> The user id under which the package is installed.\n  Example:\n  # Revoke the permisison to bind app widgets from the \"foo.bar.baz\" package.\n  adb shell revokebind --package foo.bar.baz --user current\n\n";

    private static class Parser {
        private static final String ARGUMENT_GRANT_BIND = "grantbind";
        private static final String ARGUMENT_PACKAGE = "--package";
        private static final String ARGUMENT_PREFIX = "--";
        private static final String ARGUMENT_REVOKE_BIND = "revokebind";
        private static final String ARGUMENT_USER = "--user";
        private static final String VALUE_USER_CURRENT = "current";
        private final Tokenizer mTokenizer;

        public Parser(String[] args) {
            this.mTokenizer = new Tokenizer(args);
        }

        public Runnable parseCommand() {
            StringBuilder stringBuilder;
            try {
                String operation = this.mTokenizer.nextArg();
                if (ARGUMENT_GRANT_BIND.equals(operation)) {
                    return parseSetGrantBindAppWidgetPermissionCommand(true);
                }
                if (ARGUMENT_REVOKE_BIND.equals(operation)) {
                    return parseSetGrantBindAppWidgetPermissionCommand(false);
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported operation: ");
                stringBuilder.append(operation);
                throw new IllegalArgumentException(stringBuilder.toString());
            } catch (IllegalArgumentException iae) {
                System.out.println(AppWidget.USAGE);
                PrintStream printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("[ERROR] ");
                stringBuilder.append(iae.getMessage());
                printStream.println(stringBuilder.toString());
                return null;
            }
        }

        private SetBindAppWidgetPermissionCommand parseSetGrantBindAppWidgetPermissionCommand(boolean granted) {
            String packageName = null;
            int userId = 0;
            while (true) {
                String access$000 = this.mTokenizer.nextArg();
                String argument = access$000;
                if (access$000 != null) {
                    if (ARGUMENT_PACKAGE.equals(argument)) {
                        packageName = argumentValueRequired(argument);
                    } else if (ARGUMENT_USER.equals(argument)) {
                        access$000 = argumentValueRequired(argument);
                        if (VALUE_USER_CURRENT.equals(access$000)) {
                            userId = -2;
                        } else {
                            userId = Integer.parseInt(access$000);
                        }
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unsupported argument: ");
                        stringBuilder.append(argument);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                } else if (packageName != null) {
                    return new SetBindAppWidgetPermissionCommand(packageName, granted, userId);
                } else {
                    throw new IllegalArgumentException("Package name not specified. Did you specify --package argument?");
                }
            }
        }

        private String argumentValueRequired(String argument) {
            String value = this.mTokenizer.nextArg();
            if (!TextUtils.isEmpty(value) && !value.startsWith(ARGUMENT_PREFIX)) {
                return value;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No value for argument: ");
            stringBuilder.append(argument);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static class SetBindAppWidgetPermissionCommand implements Runnable {
        final boolean mGranted;
        final String mPackageName;
        final int mUserId;

        public SetBindAppWidgetPermissionCommand(String packageName, boolean granted, int userId) {
            this.mPackageName = packageName;
            this.mGranted = granted;
            this.mUserId = userId;
        }

        public void run() {
            try {
                Stub.asInterface(ServiceManager.getService("appwidget")).setBindAppWidgetPermission(this.mPackageName, this.mUserId, this.mGranted);
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] args) {
            this.mArgs = args;
        }

        private String nextArg() {
            if (this.mNextArg >= this.mArgs.length) {
                return null;
            }
            String[] strArr = this.mArgs;
            int i = this.mNextArg;
            this.mNextArg = i + 1;
            return strArr[i];
        }
    }

    public static void main(String[] args) {
        Runnable command = new Parser(args).parseCommand();
        if (command != null) {
            command.run();
        }
    }
}
