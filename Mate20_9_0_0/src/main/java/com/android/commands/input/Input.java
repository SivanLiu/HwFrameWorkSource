package com.android.commands.input;

import android.hardware.input.InputManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class Input {
    private static final String INVALID_ARGUMENTS = "Error: Invalid arguments for command: ";
    private static final Map<String, Integer> SOURCES = new HashMap<String, Integer>() {
        {
            put("keyboard", Integer.valueOf(257));
            put("dpad", Integer.valueOf(513));
            put("gamepad", Integer.valueOf(1025));
            put("touchscreen", Integer.valueOf(4098));
            put("mouse", Integer.valueOf(8194));
            put("stylus", Integer.valueOf(16386));
            put("trackball", Integer.valueOf(65540));
            put("touchpad", Integer.valueOf(1048584));
            put("touchnavigation", Integer.valueOf(2097152));
            put("joystick", Integer.valueOf(16777232));
        }
    };
    private static final String TAG = "Input";

    public static void main(String[] args) {
        new Input().run(args);
    }

    /* JADX WARNING: Missing block: B:45:0x00d3, code skipped:
            sendSwipe(r8, java.lang.Float.parseFloat(r15[r0 + 1]), java.lang.Float.parseFloat(r15[r0 + 2]), java.lang.Float.parseFloat(r15[r0 + 3]), java.lang.Float.parseFloat(r15[r0 + 4]), r1);
     */
    /* JADX WARNING: Missing block: B:46:0x00f8, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:57:0x0117, code skipped:
            sendDragAndDrop(r8, java.lang.Float.parseFloat(r15[r0 + 1]), java.lang.Float.parseFloat(r15[r0 + 2]), java.lang.Float.parseFloat(r15[r0 + 3]), java.lang.Float.parseFloat(r15[r0 + 4]), r1);
     */
    /* JADX WARNING: Missing block: B:58:0x013c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void run(String[] args) {
        int inputSource;
        if (args.length < 1) {
            showUsage();
            return;
        }
        PrintStream printStream;
        StringBuilder stringBuilder;
        int index = 0;
        String command = args[0];
        int inputSource2 = 0;
        if (SOURCES.containsKey(command)) {
            inputSource2 = ((Integer) SOURCES.get(command)).intValue();
            index = 0 + 1;
            command = args[index];
        }
        int length = args.length - index;
        try {
            if (command.equals("text")) {
                if (length == 2) {
                    sendText(getSource(inputSource2, 257), args[index + 1]);
                    return;
                }
            } else if (command.equals("keyevent")) {
                if (length >= 2) {
                    boolean longpress = "--longpress".equals(args[index + 1]);
                    int start = longpress ? index + 2 : index + 1;
                    inputSource2 = getSource(inputSource2, 257);
                    if (args.length > start) {
                        for (int i = start; i < args.length; i++) {
                            int keyCode = KeyEvent.keyCodeFromString(args[i]);
                            if (keyCode == 0) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("KEYCODE_");
                                stringBuilder2.append(args[i]);
                                keyCode = KeyEvent.keyCodeFromString(stringBuilder2.toString());
                            }
                            sendKeyEvent(inputSource2, keyCode, longpress);
                        }
                        return;
                    }
                }
            } else if (!command.equals("tap")) {
                int duration;
                if (command.equals("swipe")) {
                    duration = -1;
                    inputSource = getSource(inputSource2, 4098);
                    switch (length) {
                        case 5:
                            break;
                        case 6:
                            try {
                                duration = Integer.parseInt(args[index + 5]);
                                break;
                            } catch (NumberFormatException e) {
                                inputSource2 = inputSource;
                                break;
                            }
                    }
                } else if (command.equals("draganddrop")) {
                    duration = -1;
                    inputSource = getSource(inputSource2, 4098);
                    switch (length) {
                        case 5:
                            break;
                        case 6:
                            duration = Integer.parseInt(args[index + 5]);
                            break;
                    }
                } else if (command.equals("press")) {
                    inputSource2 = getSource(inputSource2, 65540);
                    if (length == 1) {
                        sendTap(inputSource2, 0.0f, 0.0f);
                        return;
                    }
                } else if (command.equals("roll")) {
                    inputSource2 = getSource(inputSource2, 65540);
                    if (length == 3) {
                        sendMove(inputSource2, Float.parseFloat(args[index + 1]), Float.parseFloat(args[index + 2]));
                        return;
                    }
                } else {
                    printStream = System.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Error: Unknown command: ");
                    stringBuilder.append(command);
                    printStream.println(stringBuilder.toString());
                    showUsage();
                    return;
                }
            } else if (length == 3) {
                sendTap(getSource(inputSource2, 4098), Float.parseFloat(args[index + 1]), Float.parseFloat(args[index + 2]));
                return;
            }
        } catch (NumberFormatException e2) {
        }
        printStream = System.err;
        stringBuilder = new StringBuilder();
        stringBuilder.append(INVALID_ARGUMENTS);
        stringBuilder.append(command);
        printStream.println(stringBuilder.toString());
        showUsage();
    }

    private void sendText(int source, String text) {
        StringBuffer buff = new StringBuffer(text);
        int i = 0;
        boolean escapeFlag = false;
        int i2 = 0;
        while (i2 < buff.length()) {
            if (escapeFlag) {
                escapeFlag = false;
                if (buff.charAt(i2) == 's') {
                    buff.setCharAt(i2, ' ');
                    i2--;
                    buff.deleteCharAt(i2);
                }
            }
            if (buff.charAt(i2) == '%') {
                escapeFlag = true;
            }
            i2++;
        }
        KeyEvent[] events = KeyCharacterMap.load(-1).getEvents(buff.toString().toCharArray());
        while (i < events.length) {
            KeyEvent e = events[i];
            if (source != e.getSource()) {
                e.setSource(source);
            }
            injectKeyEvent(e);
            i++;
        }
    }

    private void sendKeyEvent(int inputSource, int keyCode, boolean longpress) {
        long now = SystemClock.uptimeMillis();
        long now2 = now;
        KeyEvent keyEvent = r1;
        KeyEvent keyEvent2 = new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 0, inputSource);
        injectKeyEvent(keyEvent);
        if (longpress) {
            injectKeyEvent(new KeyEvent(now2, now2, 0, keyCode, 1, 0, -1, 0, 128, inputSource));
        }
        injectKeyEvent(new KeyEvent(now2, now2, 1, keyCode, 0, 0, -1, 0, 0, inputSource));
    }

    private void sendTap(int inputSource, float x, float y) {
        int i = inputSource;
        long uptimeMillis = SystemClock.uptimeMillis();
        float f = x;
        float f2 = y;
        injectMotionEvent(i, 0, uptimeMillis, f, f2, 1.0f);
        injectMotionEvent(i, 1, uptimeMillis, f, f2, 0.0f);
    }

    private void sendSwipe(int inputSource, float x1, float y1, float x2, float y2, int duration) {
        int duration2;
        if (duration < 0) {
            duration2 = 300;
        } else {
            duration2 = duration;
        }
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, 0, now, x1, y1, 1.0f);
        long startTime = now;
        long endTime = ((long) duration2) + startTime;
        long now2 = now;
        while (now2 < endTime) {
            long elapsedTime = now2 - startTime;
            float alpha = ((float) elapsedTime) / ((float) duration2);
            injectMotionEvent(inputSource, 2, now2, lerp(x1, x2, alpha), lerp(y1, y2, alpha), 1.0f);
            now2 = SystemClock.uptimeMillis();
        }
        injectMotionEvent(inputSource, 1, now2, x2, y2, 0.0f);
    }

    private void sendDragAndDrop(int inputSource, float x1, float y1, float x2, float y2, int dragDuration) {
        int dragDuration2 = dragDuration < 0 ? 300 : dragDuration;
        injectMotionEvent(inputSource, 0, SystemClock.uptimeMillis(), x1, y1, 1.0f);
        try {
            Thread.sleep((long) ViewConfiguration.getLongPressTimeout());
            long now = SystemClock.uptimeMillis();
            long startTime = now;
            long endTime = ((long) dragDuration2) + startTime;
            while (now < endTime) {
                long elapsedTime = now - startTime;
                float alpha = ((float) elapsedTime) / ((float) dragDuration2);
                injectMotionEvent(inputSource, 2, now, lerp(x1, x2, alpha), lerp(y1, y2, alpha), 1.0f);
                now = SystemClock.uptimeMillis();
            }
            injectMotionEvent(inputSource, 1, now, x2, y2, 0.0f);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMove(int inputSource, float dx, float dy) {
        injectMotionEvent(inputSource, 2, SystemClock.uptimeMillis(), dx, dy, 0.0f);
    }

    private void injectKeyEvent(KeyEvent event) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("injectKeyEvent: ");
        stringBuilder.append(event);
        Log.i(str, stringBuilder.toString());
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private int getInputDeviceId(int inputSource) {
        for (int devId : InputDevice.getDeviceIds()) {
            if (InputDevice.getDevice(devId).supportsSource(inputSource)) {
                return devId;
            }
        }
        return 0;
    }

    private void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, 1.0f, 0, 1.0f, 1.0f, getInputDeviceId(inputSource), 0);
        event.setSource(inputSource);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("injectMotionEvent: ");
        stringBuilder.append(event);
        Log.i(str, stringBuilder.toString());
        InputManager.getInstance().injectInputEvent(event, 2);
    }

    private static final float lerp(float a, float b, float alpha) {
        return ((b - a) * alpha) + a;
    }

    private static final int getSource(int inputSource, int defaultSource) {
        return inputSource == 0 ? defaultSource : inputSource;
    }

    private void showUsage() {
        System.err.println("Usage: input [<source>] <command> [<arg>...]");
        System.err.println();
        System.err.println("The sources are: ");
        for (String src : SOURCES.keySet()) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("      ");
            stringBuilder.append(src);
            printStream.println(stringBuilder.toString());
        }
        System.err.println();
        System.err.println("The commands and default sources are:");
        System.err.println("      text <string> (Default: touchscreen)");
        System.err.println("      keyevent [--longpress] <key code number or name> ... (Default: keyboard)");
        System.err.println("      tap <x> <y> (Default: touchscreen)");
        System.err.println("      swipe <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
        System.err.println("      draganddrop <x1> <y1> <x2> <y2> [duration(ms)] (Default: touchscreen)");
        System.err.println("      press (Default: trackball)");
        System.err.println("      roll <dx> <dy> (Default: trackball)");
    }
}
