package com.android.server;

import com.android.server.display.DisplayTransformManager;
import com.google.android.collect.Lists;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class NativeDaemonEvent {
    public static final String SENSITIVE_MARKER = "{{sensitive}}";
    private final int mCmdNumber;
    private final int mCode;
    private FileDescriptor[] mFdList;
    private final String mLogMessage;
    private final String mMessage;
    private String[] mParsed = null;
    private final String mRawEvent;

    private NativeDaemonEvent(int cmdNumber, int code, String message, String rawEvent, String logMessage, FileDescriptor[] fdList) {
        this.mCmdNumber = cmdNumber;
        this.mCode = code;
        this.mMessage = message;
        this.mRawEvent = rawEvent;
        this.mLogMessage = logMessage;
        this.mFdList = fdList;
    }

    public int getCmdNumber() {
        return this.mCmdNumber;
    }

    public int getCode() {
        return this.mCode;
    }

    public String getMessage() {
        return this.mMessage;
    }

    public FileDescriptor[] getFileDescriptors() {
        return this.mFdList;
    }

    @Deprecated
    public String getRawEvent() {
        return this.mRawEvent;
    }

    public String toString() {
        return this.mLogMessage;
    }

    public boolean isClassContinue() {
        return this.mCode >= 100 && this.mCode < DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE;
    }

    public boolean isClassOk() {
        return this.mCode >= DisplayTransformManager.LEVEL_COLOR_MATRIX_GRAYSCALE && this.mCode < DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
    }

    public boolean isClassServerError() {
        return this.mCode >= 400 && this.mCode < 500;
    }

    public boolean isClassClientError() {
        return this.mCode >= 500 && this.mCode < 600;
    }

    public boolean isClassUnsolicited() {
        return isClassUnsolicited(this.mCode);
    }

    private static boolean isClassUnsolicited(int code) {
        return (code >= 600 && code < 700) || (code >= 800 && code < 910);
    }

    public void checkCode(int code) {
        if (this.mCode != code) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected ");
            stringBuilder.append(code);
            stringBuilder.append(" but was: ");
            stringBuilder.append(this);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public static NativeDaemonEvent parseRawEvent(String rawEvent, FileDescriptor[] fdList) {
        String[] parsed = rawEvent.split(" ");
        if (parsed.length >= 2) {
            int skiplength = 0;
            try {
                int skiplength2;
                String logMessage;
                int code = Integer.parseInt(parsed[0]);
                int skiplength3 = parsed[0].length() + 1;
                skiplength = -1;
                if (!isClassUnsolicited(code)) {
                    if (parsed.length >= 3) {
                        try {
                            skiplength = Integer.parseInt(parsed[1]);
                            skiplength3 += parsed[1].length() + 1;
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("problem parsing cmdNumber", e);
                        }
                    }
                    throw new IllegalArgumentException("Insufficient arguemnts");
                }
                String logMessage2 = rawEvent;
                if (parsed.length <= 2 || !parsed[2].equals(SENSITIVE_MARKER)) {
                    skiplength2 = skiplength3;
                    logMessage = logMessage2;
                } else {
                    skiplength3 += parsed[2].length() + 1;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(parsed[0]);
                    stringBuilder.append(" ");
                    stringBuilder.append(parsed[1]);
                    stringBuilder.append(" {}");
                    logMessage = stringBuilder.toString();
                    skiplength2 = skiplength3;
                }
                return new NativeDaemonEvent(skiplength, code, rawEvent.substring(skiplength2), rawEvent, logMessage, fdList);
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("problem parsing code", e2);
            }
        }
        throw new IllegalArgumentException("Insufficient arguments");
    }

    public static String[] filterMessageList(NativeDaemonEvent[] events, int matchCode) {
        ArrayList<String> result = Lists.newArrayList();
        for (NativeDaemonEvent event : events) {
            if (event.getCode() == matchCode) {
                result.add(event.getMessage());
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public String getField(int n) {
        if (this.mParsed == null) {
            this.mParsed = unescapeArgs(this.mRawEvent);
        }
        n += 2;
        if (n > this.mParsed.length) {
            return null;
        }
        return this.mParsed[n];
    }

    public static String[] unescapeArgs(String rawEvent) {
        String LOGTAG = "unescapeArgs";
        ArrayList<String> parsed = new ArrayList();
        int length = rawEvent.length();
        int current = 0;
        boolean quoted = false;
        if (rawEvent.charAt(0) == '\"') {
            quoted = true;
            current = 0 + 1;
        }
        while (current < length) {
            char terminator = quoted ? '\"' : ' ';
            int wordEnd = current;
            while (wordEnd < length && rawEvent.charAt(wordEnd) != terminator) {
                if (rawEvent.charAt(wordEnd) == '\\') {
                    wordEnd++;
                }
                wordEnd++;
            }
            if (wordEnd > length) {
                wordEnd = length;
            }
            String word = rawEvent.substring(current, wordEnd);
            current += word.length();
            if (quoted) {
                current++;
            } else {
                word = word.trim();
            }
            parsed.add(word.replace("\\\\", "\\").replace("\\\"", "\""));
            int current2 = rawEvent.indexOf(32, current);
            int nextQuote = rawEvent.indexOf(" \"", current);
            if (nextQuote <= -1 || nextQuote > current2) {
                quoted = false;
                if (current2 > -1) {
                    current = current2 + 1;
                }
            } else {
                quoted = true;
                current = nextQuote + 2;
            }
        }
        return (String[]) parsed.toArray(new String[parsed.size()]);
    }
}
