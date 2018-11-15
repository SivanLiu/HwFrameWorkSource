package com.android.commands.monkey;

import android.content.ComponentName;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Random;

public class MonkeySourceScript implements MonkeyEventSource {
    private static final String EVENT_KEYWORD_ACTIVITY = "LaunchActivity";
    private static final String EVENT_KEYWORD_DEVICE_WAKEUP = "DeviceWakeUp";
    private static final String EVENT_KEYWORD_DRAG = "Drag";
    private static final String EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE = "EndCaptureAppFramerate";
    private static final String EVENT_KEYWORD_END_FRAMERATE_CAPTURE = "EndCaptureFramerate";
    private static final String EVENT_KEYWORD_FLIP = "DispatchFlip";
    private static final String EVENT_KEYWORD_INPUT_STRING = "DispatchString";
    private static final String EVENT_KEYWORD_INSTRUMENTATION = "LaunchInstrumentation";
    private static final String EVENT_KEYWORD_KEY = "DispatchKey";
    private static final String EVENT_KEYWORD_KEYPRESS = "DispatchPress";
    private static final String EVENT_KEYWORD_LONGPRESS = "LongPress";
    private static final String EVENT_KEYWORD_PINCH_ZOOM = "PinchZoom";
    private static final String EVENT_KEYWORD_POINTER = "DispatchPointer";
    private static final String EVENT_KEYWORD_POWERLOG = "PowerLog";
    private static final String EVENT_KEYWORD_PRESSANDHOLD = "PressAndHold";
    private static final String EVENT_KEYWORD_PROFILE_WAIT = "ProfileWait";
    private static final String EVENT_KEYWORD_ROTATION = "RotateScreen";
    private static final String EVENT_KEYWORD_RUNCMD = "RunCmd";
    private static final String EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE = "StartCaptureAppFramerate";
    private static final String EVENT_KEYWORD_START_FRAMERATE_CAPTURE = "StartCaptureFramerate";
    private static final String EVENT_KEYWORD_TAP = "Tap";
    private static final String EVENT_KEYWORD_TRACKBALL = "DispatchTrackball";
    private static final String EVENT_KEYWORD_WAIT = "UserWait";
    private static final String EVENT_KEYWORD_WRITEPOWERLOG = "WriteLog";
    private static final String HEADER_COUNT = "count=";
    private static final String HEADER_LINE_BY_LINE = "linebyline";
    private static final String HEADER_SPEED = "speed=";
    private static int LONGPRESS_WAIT_TIME = 2000;
    private static final int MAX_ONE_TIME_READS = 100;
    private static final long SLEEP_COMPENSATE_DIFF = 16;
    private static final String STARTING_DATA_LINE = "start data >>";
    private static final boolean THIS_DEBUG = false;
    BufferedReader mBufferedReader;
    private long mDeviceSleepTime = 30000;
    private int mEventCountInScript = 0;
    FileInputStream mFStream;
    private boolean mFileOpened = THIS_DEBUG;
    DataInputStream mInputStream;
    private long mLastExportDownTimeKey = 0;
    private long mLastExportDownTimeMotion = 0;
    private long mLastExportEventTime = -1;
    private long mLastRecordedDownTimeKey = 0;
    private long mLastRecordedDownTimeMotion = 0;
    private long mLastRecordedEventTime = -1;
    private float[] mLastX = new float[2];
    private float[] mLastY = new float[2];
    private long mMonkeyStartTime = -1;
    private long mProfileWaitTime = 5000;
    private MonkeyEventQueue mQ;
    private boolean mReadScriptLineByLine = THIS_DEBUG;
    private String mScriptFileName;
    private long mScriptStartTime = -1;
    private double mSpeed = 1.0d;
    private int mVerbose = 0;

    public MonkeySourceScript(Random random, String filename, long throttle, boolean randomizeThrottle, long profileWaitTime, long deviceSleepTime) {
        this.mScriptFileName = filename;
        this.mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
        this.mProfileWaitTime = profileWaitTime;
        this.mDeviceSleepTime = deviceSleepTime;
    }

    private void resetValue() {
        this.mLastRecordedDownTimeKey = 0;
        this.mLastRecordedDownTimeMotion = 0;
        this.mLastRecordedEventTime = -1;
        this.mLastExportDownTimeKey = 0;
        this.mLastExportDownTimeMotion = 0;
        this.mLastExportEventTime = -1;
    }

    private boolean readHeader() throws IOException {
        Logger logger;
        StringBuilder stringBuilder;
        this.mFileOpened = true;
        this.mFStream = new FileInputStream(this.mScriptFileName);
        this.mInputStream = new DataInputStream(this.mFStream);
        this.mBufferedReader = new BufferedReader(new InputStreamReader(this.mInputStream));
        while (true) {
            String readLine = this.mBufferedReader.readLine();
            String line = readLine;
            if (readLine == null) {
                return THIS_DEBUG;
            }
            readLine = line.trim();
            if (readLine.indexOf(HEADER_COUNT) >= 0) {
                try {
                    this.mEventCountInScript = Integer.parseInt(readLine.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e) {
                    logger = Logger.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(e);
                    logger.println(stringBuilder.toString());
                    return THIS_DEBUG;
                }
            } else if (readLine.indexOf(HEADER_SPEED) >= 0) {
                try {
                    this.mSpeed = Double.parseDouble(readLine.substring(HEADER_COUNT.length() + 1).trim());
                } catch (NumberFormatException e2) {
                    logger = Logger.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(e2);
                    logger.println(stringBuilder.toString());
                    return THIS_DEBUG;
                }
            } else if (readLine.indexOf(HEADER_LINE_BY_LINE) >= 0) {
                this.mReadScriptLineByLine = true;
            } else if (readLine.indexOf(STARTING_DATA_LINE) >= null) {
                return true;
            }
        }
    }

    private int readLines() throws IOException {
        for (int i = 0; i < MAX_ONE_TIME_READS; i++) {
            String line = this.mBufferedReader.readLine();
            if (line == null) {
                return i;
            }
            line.trim();
            processLine(line);
        }
        return MAX_ONE_TIME_READS;
    }

    private int readOneLine() throws IOException {
        String line = this.mBufferedReader.readLine();
        if (line == null) {
            return 0;
        }
        line.trim();
        processLine(line);
        return 1;
    }

    private void handleEvent(String s, String[] args) {
        Logger logger;
        StringBuilder stringBuilder;
        String str = s;
        float[] fArr = args;
        long downTime;
        int code;
        long eventTime;
        float x;
        float size;
        int metaState;
        float xPrecision;
        float yPrecision;
        int device;
        float x2;
        float y;
        float yPrecision2;
        int pointerId;
        long downTime2;
        if (str.indexOf(EVENT_KEYWORD_KEY) >= 0 && fArr.length == 8) {
            try {
                Logger.out.println(" old key\n");
                downTime = Long.parseLong(fArr[0]);
                long eventTime2 = Long.parseLong(fArr[1]);
                int action = Integer.parseInt(fArr[2]);
                code = Integer.parseInt(fArr[3]);
                MonkeyEvent monkeyKeyEvent = new MonkeyKeyEvent(downTime, eventTime2, action, code, Integer.parseInt(fArr[4]), Integer.parseInt(fArr[5]), Integer.parseInt(fArr[6]), Integer.parseInt(fArr[7]));
                Logger logger2 = Logger.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" Key code ");
                stringBuilder2.append(code);
                stringBuilder2.append("\n");
                logger2.println(stringBuilder2.toString());
                this.mQ.addLast(monkeyKeyEvent);
                Logger.out.println("Added key up \n");
            } catch (NumberFormatException e) {
            }
        } else if ((str.indexOf(EVENT_KEYWORD_POINTER) >= 0 || str.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && fArr.length == 12) {
            try {
                MonkeyEvent e2;
                long downTime3 = Long.parseLong(fArr[0]);
                eventTime = Long.parseLong(fArr[1]);
                code = Integer.parseInt(fArr[2]);
                x = Float.parseFloat(fArr[3]);
                float y2 = Float.parseFloat(fArr[4]);
                float pressure = Float.parseFloat(fArr[5]);
                size = Float.parseFloat(fArr[6]);
                metaState = Integer.parseInt(fArr[7]);
                xPrecision = Float.parseFloat(fArr[8]);
                yPrecision = Float.parseFloat(fArr[9]);
                device = Integer.parseInt(fArr[10]);
                int edgeFlags = Integer.parseInt(fArr[11]);
                if (str.indexOf("Pointer") > 0) {
                    e2 = new MonkeyTouchEvent(code);
                } else {
                    e2 = new MonkeyTrackballEvent(code);
                }
                e2.setDownTime(downTime3).setEventTime(eventTime).setMetaState(metaState).setPrecision(xPrecision, yPrecision).setDeviceId(device).setEdgeFlags(edgeFlags).addPointer(0, x, y2, pressure, size);
                this.mQ.addLast(e2);
            } catch (NumberFormatException e3) {
            }
        } else if ((str.indexOf(EVENT_KEYWORD_POINTER) >= 0 || str.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0) && fArr.length == 13) {
            try {
                MonkeyEvent e4;
                long downTime4 = Long.parseLong(fArr[0]);
                long eventTime3 = Long.parseLong(fArr[1]);
                code = Integer.parseInt(fArr[2]);
                x2 = Float.parseFloat(fArr[3]);
                y = Float.parseFloat(fArr[4]);
                float pressure2 = Float.parseFloat(fArr[5]);
                float size2 = Float.parseFloat(fArr[6]);
                metaState = Integer.parseInt(fArr[7]);
                xPrecision = Float.parseFloat(fArr[8]);
                yPrecision2 = Float.parseFloat(fArr[9]);
                int device2 = Integer.parseInt(fArr[10]);
                int edgeFlags2 = Integer.parseInt(fArr[11]);
                pointerId = Integer.parseInt(fArr[12]);
                if (str.indexOf("Pointer") > 0) {
                    if (code == 5) {
                        e4 = new MonkeyTouchEvent(5 | (pointerId << 8)).setIntermediateNote(true);
                    } else {
                        e4 = new MonkeyTouchEvent(code);
                    }
                    if (this.mScriptStartTime < 0) {
                        this.mMonkeyStartTime = SystemClock.uptimeMillis();
                        eventTime = eventTime3;
                        this.mScriptStartTime = eventTime;
                    } else {
                        eventTime = eventTime3;
                    }
                } else {
                    eventTime = eventTime3;
                    e4 = new MonkeyTrackballEvent(code);
                }
                long j;
                if (pointerId == 1) {
                    downTime2 = downTime4;
                    e4.setDownTime(downTime2).setEventTime(eventTime).setMetaState(metaState).setPrecision(xPrecision, yPrecision2).setDeviceId(device2).setEdgeFlags(edgeFlags2).addPointer(0, this.mLastX[0], this.mLastY[0], pressure2, size2).addPointer(1, x2, y, pressure2, size2);
                    this.mLastX[1] = x2;
                    this.mLastY[1] = y;
                    int i = code;
                    j = downTime2;
                } else {
                    downTime2 = downTime4;
                    if (pointerId == 0) {
                        e4.setDownTime(downTime2).setEventTime(eventTime).setMetaState(metaState).setPrecision(xPrecision, yPrecision2).setDeviceId(device2).setEdgeFlags(edgeFlags2).addPointer(0, x2, y, pressure2, size2);
                        if (code == 6) {
                            e4.addPointer(1, this.mLastX[1], this.mLastY[1]);
                        } else {
                            j = downTime2;
                        }
                        this.mLastX[0] = x2;
                        this.mLastY[0] = y;
                    } else {
                        j = downTime2;
                    }
                }
                if (this.mReadScriptLineByLine) {
                    downTime2 = SystemClock.uptimeMillis();
                    xPrecision = downTime2 - this.mMonkeyStartTime;
                    eventTime3 = eventTime - this.mScriptStartTime;
                    if (xPrecision < eventTime3) {
                        this.mQ.addLast(new MonkeyWaitEvent(eventTime3 - xPrecision));
                    }
                } else {
                    int i2 = metaState;
                }
                this.mQ.addLast(e4);
            } catch (NumberFormatException e5) {
            }
        } else {
            String[] strArr;
            str = s;
            if (str.indexOf(EVENT_KEYWORD_ROTATION) >= 0) {
                strArr = args;
                if (strArr.length == 2) {
                    try {
                        code = Integer.parseInt(strArr[0]);
                        metaState = Integer.parseInt(strArr[1]);
                        if (code == 0 || code == 1 || code == 2 || code == 3) {
                            this.mQ.addLast(new MonkeyRotationEvent(code, metaState != 0 ? true : THIS_DEBUG));
                        }
                    } catch (NumberFormatException e6) {
                    }
                    return;
                }
            }
            strArr = args;
            long tapDuration;
            long downTime5;
            float yStep;
            MonkeyEvent stepCount;
            if (str.indexOf(EVENT_KEYWORD_TAP) >= 0 && strArr.length >= 2) {
                try {
                    size = Float.parseFloat(strArr[0]);
                    float y3 = Float.parseFloat(strArr[1]);
                    tapDuration = 0;
                    if (strArr.length == 3) {
                        tapDuration = Long.parseLong(strArr[2]);
                    }
                    downTime5 = SystemClock.uptimeMillis();
                    this.mQ.addLast(new MonkeyTouchEvent(0).setDownTime(downTime5).setEventTime(downTime5).addPointer(0, size, y3, 1.0f, 5.0f));
                    if (tapDuration > 0) {
                        this.mQ.addLast(new MonkeyWaitEvent(tapDuration));
                    }
                    this.mQ.addLast(new MonkeyTouchEvent(1).setDownTime(downTime5).setEventTime(downTime5).addPointer(0, size, y3, 1.0f, 5.0f));
                } catch (NumberFormatException e7) {
                    logger = Logger.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("// ");
                    stringBuilder.append(e7.toString());
                    logger.println(stringBuilder.toString());
                }
            } else if (str.indexOf(EVENT_KEYWORD_PRESSANDHOLD) < 0 || strArr.length != 3) {
                float xStart;
                float yEnd;
                long downTime6;
                if (str.indexOf(EVENT_KEYWORD_DRAG) >= 0 && strArr.length == 5) {
                    xStart = Float.parseFloat(strArr[0]);
                    yPrecision = Float.parseFloat(strArr[1]);
                    float xEnd = Float.parseFloat(strArr[2]);
                    yEnd = Float.parseFloat(strArr[3]);
                    pointerId = Integer.parseInt(strArr[4]);
                    float x3 = xStart;
                    x = yPrecision;
                    tapDuration = SystemClock.uptimeMillis();
                    long eventTime4 = SystemClock.uptimeMillis();
                    if (pointerId > 0) {
                        float yStart;
                        x2 = (xEnd - xStart) / ((float) pointerId);
                        yStep = (yEnd - yPrecision) / ((float) pointerId);
                        code = pointerId;
                        MonkeyEvent e8 = new MonkeyTouchEvent(0).setDownTime(tapDuration).setEventTime(eventTime4).addPointer(0, x3, x, 1.0f, 5.0f);
                        this.mQ.addLast(e8);
                        y = x3;
                        yPrecision2 = x;
                        device = 0;
                        while (device < code) {
                            y += x2;
                            yPrecision2 += yStep;
                            int stepCount2 = code;
                            yStart = yPrecision;
                            stepCount = new MonkeyTouchEvent(2.8E-45f).setDownTime(tapDuration).setEventTime(SystemClock.uptimeMillis()).addPointer(0, y, yPrecision2, 1.0f, 5.0f);
                            this.mQ.addLast(stepCount);
                            device++;
                            MonkeyEvent monkeyEvent = stepCount;
                            code = stepCount2;
                            yPrecision = yStart;
                        }
                        yStart = yPrecision;
                        this.mQ.addLast(new MonkeyTouchEvent(1).setDownTime(tapDuration).setEventTime(SystemClock.uptimeMillis()).addPointer(0, y, yPrecision2, 1.0f, 5.0f));
                    }
                }
                if (str.indexOf(EVENT_KEYWORD_PINCH_ZOOM) >= 0 && strArr.length == 9) {
                    xStart = Float.parseFloat(strArr[0]);
                    xPrecision = Float.parseFloat(strArr[1]);
                    float pt1xEnd = Float.parseFloat(strArr[2]);
                    yPrecision = Float.parseFloat(strArr[3]);
                    float pt2xStart = Float.parseFloat(strArr[4]);
                    y = Float.parseFloat(strArr[5]);
                    x2 = Float.parseFloat(strArr[6]);
                    yPrecision2 = Float.parseFloat(strArr[7]);
                    int stepCount3 = Integer.parseInt(strArr[8]);
                    float x1 = xStart;
                    yEnd = y;
                    float x22 = pt2xStart;
                    float y1 = xPrecision;
                    long downTime7 = SystemClock.uptimeMillis();
                    downTime2 = SystemClock.uptimeMillis();
                    if (stepCount3 > 0) {
                        float pt1yStep;
                        xStart = (pt1xEnd - xStart) / ((float) stepCount3);
                        xPrecision = (yPrecision - xPrecision) / ((float) stepCount3);
                        pt1xEnd = (x2 - pt2xStart) / ((float) stepCount3);
                        yPrecision = (yPrecision2 - y) / ((float) stepCount3);
                        MonkeyEventQueue monkeyEventQueue = this.mQ;
                        MonkeyMotionEvent eventTime5 = new MonkeyTouchEvent(0.0f).setDownTime(downTime7).setEventTime(downTime2);
                        float y22 = yEnd;
                        float pt2xStep = pt1xEnd;
                        float pt2yStep = yPrecision;
                        downTime6 = downTime7;
                        float x23 = x22;
                        y = y1;
                        x2 = x1;
                        monkeyEventQueue.addLast(eventTime5.addPointer(0, x1, y, 1.0f, 5.0f));
                        this.mQ.addLast(new MonkeyTouchEvent(261).setDownTime(downTime6).addPointer(0, x2, y).addPointer(1, x23, y22).setIntermediateNote(true));
                        pt2xStart = y22;
                        int i3 = 0;
                        while (i3 < stepCount3) {
                            x2 += xStart;
                            y += xPrecision;
                            x23 += pt2xStep;
                            pt2xStart += pt2yStep;
                            downTime = SystemClock.uptimeMillis();
                            float pt1xStep = xStart;
                            pt1yStep = xPrecision;
                            long eventTime6 = downTime;
                            this.mQ.addLast(new MonkeyTouchEvent(2.8E-45f).setDownTime(downTime6).setEventTime(downTime).addPointer(0, x2, y, 1.0f, 5.0f).addPointer(1, x23, pt2xStart, 1.0f, 5.0f));
                            i3++;
                            long j2 = eventTime6;
                            xStart = pt1xStep;
                            xPrecision = pt1yStep;
                        }
                        pt1yStep = xPrecision;
                        this.mQ.addLast(new MonkeyTouchEvent(6).setDownTime(downTime6).setEventTime(SystemClock.uptimeMillis()).addPointer(0, x2, y).addPointer(1, x23, pt2xStart));
                    }
                }
                str = s;
                if (str.indexOf(EVENT_KEYWORD_FLIP) >= 0) {
                    strArr = args;
                    if (strArr.length == 1) {
                        this.mQ.addLast(new MonkeyFlipEvent(Boolean.parseBoolean(strArr[0])));
                    }
                } else {
                    strArr = args;
                }
                if (str.indexOf(EVENT_KEYWORD_ACTIVITY) >= 0 && strArr.length >= 2) {
                    downTime5 = 0;
                    ComponentName mApp = new ComponentName(strArr[0], strArr[1]);
                    if (strArr.length > 2) {
                        try {
                            downTime5 = Long.parseLong(strArr[2]);
                        } catch (NumberFormatException e72) {
                            Logger logger3 = Logger.err;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("// ");
                            stringBuilder3.append(e72.toString());
                            logger3.println(stringBuilder3.toString());
                            return;
                        }
                    }
                    if (strArr.length == 2) {
                        this.mQ.addLast(new MonkeyActivityEvent(mApp));
                    } else {
                        this.mQ.addLast(new MonkeyActivityEvent(mApp, downTime5));
                    }
                } else if (str.indexOf(EVENT_KEYWORD_DEVICE_WAKEUP) >= 0) {
                    downTime6 = this.mDeviceSleepTime;
                    this.mQ.addLast(new MonkeyActivityEvent(new ComponentName("com.google.android.powerutil", "com.google.android.powerutil.WakeUpScreen"), downTime6));
                    this.mQ.addLast(new MonkeyKeyEvent(0, 7));
                    this.mQ.addLast(new MonkeyKeyEvent(1, 7));
                    this.mQ.addLast(new MonkeyWaitEvent(3000 + downTime6));
                    this.mQ.addLast(new MonkeyKeyEvent(0, 82));
                    this.mQ.addLast(new MonkeyKeyEvent(1, 82));
                    this.mQ.addLast(new MonkeyKeyEvent(0, 4));
                    this.mQ.addLast(new MonkeyKeyEvent(1, 4));
                } else if (str.indexOf(EVENT_KEYWORD_INSTRUMENTATION) >= 0 && strArr.length == 2) {
                    this.mQ.addLast(new MonkeyInstrumentationEvent(strArr[null], strArr[1]));
                } else if (str.indexOf(EVENT_KEYWORD_WAIT) >= 0 && strArr.length == 1) {
                    try {
                        this.mQ.addLast(new MonkeyWaitEvent((long) Integer.parseInt(strArr[0])));
                    } catch (NumberFormatException e9) {
                    }
                } else if (str.indexOf(EVENT_KEYWORD_PROFILE_WAIT) >= 0) {
                    this.mQ.addLast(new MonkeyWaitEvent(this.mProfileWaitTime));
                } else if (str.indexOf(EVENT_KEYWORD_KEYPRESS) < 0 || strArr.length != 1) {
                    if (str.indexOf(EVENT_KEYWORD_LONGPRESS) >= 0) {
                        this.mQ.addLast(new MonkeyKeyEvent(0, 23));
                        this.mQ.addLast(new MonkeyWaitEvent((long) LONGPRESS_WAIT_TIME));
                        this.mQ.addLast(new MonkeyKeyEvent(1, 23));
                    }
                    if (str.indexOf(EVENT_KEYWORD_POWERLOG) >= 0 && strArr.length > 0) {
                        String power_log_type = strArr[0];
                        if (strArr.length == 1) {
                            this.mQ.addLast(new MonkeyPowerEvent(power_log_type));
                        } else if (strArr.length == 2) {
                            this.mQ.addLast(new MonkeyPowerEvent(power_log_type, strArr[1]));
                        }
                    }
                    if (str.indexOf(EVENT_KEYWORD_WRITEPOWERLOG) >= 0) {
                        this.mQ.addLast(new MonkeyPowerEvent());
                    }
                    if (str.indexOf(EVENT_KEYWORD_RUNCMD) >= 0 && strArr.length == 1) {
                        this.mQ.addLast(new MonkeyCommandEvent(strArr[0]));
                    }
                    if (str.indexOf(EVENT_KEYWORD_INPUT_STRING) >= 0 && strArr.length == 1) {
                        String input = strArr[null];
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("input text ");
                        stringBuilder4.append(input);
                        this.mQ.addLast(new MonkeyCommandEvent(stringBuilder4.toString()));
                    } else if (str.indexOf(EVENT_KEYWORD_START_FRAMERATE_CAPTURE) >= 0) {
                        this.mQ.addLast(new MonkeyGetFrameRateEvent("start"));
                    } else if (str.indexOf(EVENT_KEYWORD_END_FRAMERATE_CAPTURE) >= 0 && strArr.length == 1) {
                        this.mQ.addLast(new MonkeyGetFrameRateEvent("end", strArr[null]));
                    } else if (str.indexOf(EVENT_KEYWORD_START_APP_FRAMERATE_CAPTURE) >= 0 && strArr.length == 1) {
                        this.mQ.addLast(new MonkeyGetAppFrameRateEvent("start", strArr[null]));
                    } else if (str.indexOf(EVENT_KEYWORD_END_APP_FRAMERATE_CAPTURE) >= 0 && strArr.length == 2) {
                        this.mQ.addLast(new MonkeyGetAppFrameRateEvent("end", strArr[0], strArr[1]));
                    }
                } else {
                    metaState = MonkeySourceRandom.getKeyCode(strArr[0]);
                    if (metaState != 0) {
                        this.mQ.addLast(new MonkeyKeyEvent(0, metaState));
                        this.mQ.addLast(new MonkeyKeyEvent(1, metaState));
                    }
                }
            } else {
                try {
                    yStep = Float.parseFloat(strArr[0]);
                    float y4 = Float.parseFloat(strArr[1]);
                    tapDuration = Long.parseLong(strArr[2]);
                    downTime5 = SystemClock.uptimeMillis();
                    stepCount = new MonkeyTouchEvent(0).setDownTime(downTime5).setEventTime(downTime5).addPointer(0, yStep, y4, 1.0f, 5.0f);
                    MonkeyEvent e22 = new MonkeyWaitEvent(tapDuration);
                    MonkeyMotionEvent e32 = new MonkeyTouchEvent(1).setDownTime(downTime5 + tapDuration).setEventTime(downTime5 + tapDuration).addPointer(0, yStep, y4, 1.0f, 5.0f);
                    this.mQ.addLast(stepCount);
                    this.mQ.addLast(e22);
                    this.mQ.addLast(e22);
                } catch (NumberFormatException e722) {
                    logger = Logger.err;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("// ");
                    stringBuilder.append(e722.toString());
                    logger.println(stringBuilder.toString());
                }
            }
        }
    }

    private void processLine(String line) {
        int index1 = line.indexOf(40);
        int index2 = line.indexOf(41);
        if (index1 >= 0 && index2 >= 0) {
            String[] args = line.substring(index1 + 1, index2).split(",");
            for (int i = 0; i < args.length; i++) {
                args[i] = args[i].trim();
            }
            handleEvent(line, args);
        }
    }

    private void closeFile() throws IOException {
        this.mFileOpened = THIS_DEBUG;
        try {
            this.mFStream.close();
            this.mInputStream.close();
        } catch (NullPointerException e) {
        }
    }

    private void readNextBatch() throws IOException {
        int linesRead;
        if (!this.mFileOpened) {
            resetValue();
            readHeader();
        }
        if (this.mReadScriptLineByLine) {
            linesRead = readOneLine();
        } else {
            linesRead = readLines();
        }
        if (linesRead == 0) {
            closeFile();
        }
    }

    private void needSleep(long time) {
        if (time >= 1) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
        }
    }

    public boolean validate() {
        try {
            boolean validHeader = readHeader();
            closeFile();
            if (this.mVerbose > 0) {
                Logger logger = Logger.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Replaying ");
                stringBuilder.append(this.mEventCountInScript);
                stringBuilder.append(" events with speed ");
                stringBuilder.append(this.mSpeed);
                logger.println(stringBuilder.toString());
            }
            return validHeader;
        } catch (IOException e) {
            return THIS_DEBUG;
        }
    }

    public void setVerbose(int verbose) {
        this.mVerbose = verbose;
    }

    private void adjustKeyEventTime(MonkeyKeyEvent e) {
        if (e.getEventTime() >= 0) {
            long thisDownTime;
            long thisEventTime;
            if (this.mLastRecordedEventTime <= 0) {
                thisDownTime = SystemClock.uptimeMillis();
                thisEventTime = thisDownTime;
            } else {
                if (e.getDownTime() != this.mLastRecordedDownTimeKey) {
                    thisDownTime = e.getDownTime();
                } else {
                    thisDownTime = this.mLastExportDownTimeKey;
                }
                long expectedDelay = (long) (((double) (e.getEventTime() - this.mLastRecordedEventTime)) * this.mSpeed);
                thisEventTime = this.mLastExportEventTime + expectedDelay;
                needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
            }
            this.mLastRecordedDownTimeKey = e.getDownTime();
            this.mLastRecordedEventTime = e.getEventTime();
            e.setDownTime(thisDownTime);
            e.setEventTime(thisEventTime);
            this.mLastExportDownTimeKey = thisDownTime;
            this.mLastExportEventTime = thisEventTime;
        }
    }

    private void adjustMotionEventTime(MonkeyMotionEvent e) {
        long thisEventTime = SystemClock.uptimeMillis();
        long thisDownTime = e.getDownTime();
        if (thisDownTime == this.mLastRecordedDownTimeMotion) {
            e.setDownTime(this.mLastExportDownTimeMotion);
        } else {
            this.mLastRecordedDownTimeMotion = thisDownTime;
            e.setDownTime(thisEventTime);
            this.mLastExportDownTimeMotion = thisEventTime;
        }
        e.setEventTime(thisEventTime);
    }

    public MonkeyEvent getNextEvent() {
        if (this.mQ.isEmpty()) {
            try {
                readNextBatch();
            } catch (IOException e) {
                return null;
            }
        }
        try {
            MonkeyEvent ev = (MonkeyEvent) this.mQ.getFirst();
            this.mQ.removeFirst();
            if (ev.getEventType() == 0) {
                adjustKeyEventTime((MonkeyKeyEvent) ev);
            } else if (ev.getEventType() == 1 || ev.getEventType() == 2) {
                adjustMotionEventTime((MonkeyMotionEvent) ev);
            }
            return ev;
        } catch (NoSuchElementException e2) {
            return null;
        }
    }
}
