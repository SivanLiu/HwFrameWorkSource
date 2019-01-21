package android.media;

import android.app.Instrumentation;
import android.util.Log;
import java.util.Vector;

/* compiled from: WebVttRenderer */
class WebVttParser {
    private static final String TAG = "WebVttParser";
    private String mBuffer = "";
    private TextTrackCue mCue;
    private Vector<String> mCueTexts;
    private WebVttCueListener mListener;
    private final Phase mParseCueId = new Phase() {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = WebVttParser.class;
        }

        public void parse(String line) {
            if (line.length() != 0) {
                if (line.equals("NOTE") || line.startsWith("NOTE ")) {
                    WebVttParser.this.mPhase = WebVttParser.this.mParseCueText;
                }
                WebVttParser.this.mCue = new TextTrackCue();
                WebVttParser.this.mCueTexts.clear();
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueTime;
                if (line.contains("-->")) {
                    WebVttParser.this.mPhase.parse(line);
                } else {
                    WebVttParser.this.mCue.mId = line;
                }
            }
        }
    };
    private final Phase mParseCueText = new Phase() {
        public void parse(String line) {
            if (line.length() == 0) {
                WebVttParser.this.yieldCue();
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
                return;
            }
            if (WebVttParser.this.mCue != null) {
                WebVttParser.this.mCueTexts.add(line);
            }
        }
    };
    private final Phase mParseCueTime = new Phase() {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = WebVttParser.class;
        }

        public void parse(String line) {
            String str = line;
            int arrowAt = str.indexOf("-->");
            if (arrowAt < 0) {
                WebVttParser.this.mCue = null;
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
                return;
            }
            int i = 0;
            String start = str.substring(0, arrowAt).trim();
            String rest = str.substring(arrowAt + 3).replaceFirst("^\\s+", "").replaceFirst("\\s+", " ");
            int spaceAt = rest.indexOf(32);
            String end = spaceAt > 0 ? rest.substring(0, spaceAt) : rest;
            String rest2 = spaceAt > 0 ? rest.substring(spaceAt + 1) : "";
            WebVttParser.this.mCue.mStartTimeMs = WebVttParser.parseTimestampMs(start);
            WebVttParser.this.mCue.mEndTimeMs = WebVttParser.parseTimestampMs(end);
            String[] split = rest2.split(" +");
            int length = split.length;
            int i2 = 0;
            while (i2 < length) {
                String setting = split[i2];
                int colonAt = setting.indexOf(58);
                if (colonAt > 0 && colonAt != setting.length() - 1) {
                    String name = setting.substring(i, colonAt);
                    String value = setting.substring(colonAt + 1);
                    if (name.equals(TtmlUtils.TAG_REGION)) {
                        WebVttParser.this.mCue.mRegionId = value;
                    } else if (name.equals("vertical")) {
                        if (value.equals("rl")) {
                            WebVttParser.this.mCue.mWritingDirection = 101;
                        } else if (value.equals("lr")) {
                            WebVttParser.this.mCue.mWritingDirection = 102;
                        } else {
                            WebVttParser.this.log_warning("cue setting", name, "has invalid value", value);
                        }
                    } else if (name.equals("line")) {
                        try {
                            if (value.endsWith("%")) {
                                WebVttParser.this.mCue.mSnapToLines = false;
                                WebVttParser.this.mCue.mLinePosition = Integer.valueOf(WebVttParser.parseIntPercentage(value));
                            } else if (value.matches(".*[^0-9].*")) {
                                WebVttParser.this.log_warning("cue setting", name, "contains an invalid character", value);
                            } else {
                                WebVttParser.this.mCue.mSnapToLines = true;
                                WebVttParser.this.mCue.mLinePosition = Integer.valueOf(Integer.parseInt(value));
                            }
                        } catch (NumberFormatException e) {
                            WebVttParser.this.log_warning("cue setting", name, "is not numeric or percentage", value);
                        }
                    } else if (name.equals("position")) {
                        try {
                            WebVttParser.this.mCue.mTextPosition = WebVttParser.parseIntPercentage(value);
                        } catch (NumberFormatException e2) {
                            WebVttParser.this.log_warning("cue setting", name, "is not numeric or percentage", value);
                        }
                    } else if (name.equals("size")) {
                        try {
                            WebVttParser.this.mCue.mSize = WebVttParser.parseIntPercentage(value);
                        } catch (NumberFormatException e3) {
                            WebVttParser.this.log_warning("cue setting", name, "is not numeric or percentage", value);
                        }
                    } else if (name.equals("align")) {
                        if (value.equals("start")) {
                            WebVttParser.this.mCue.mAlignment = 201;
                        } else if (value.equals("middle")) {
                            WebVttParser.this.mCue.mAlignment = 200;
                        } else if (value.equals(TtmlUtils.ATTR_END)) {
                            WebVttParser.this.mCue.mAlignment = 202;
                        } else if (value.equals("left")) {
                            WebVttParser.this.mCue.mAlignment = 203;
                        } else if (value.equals("right")) {
                            WebVttParser.this.mCue.mAlignment = 204;
                        } else {
                            WebVttParser.this.log_warning("cue setting", name, "has invalid value", value);
                        }
                    }
                }
                i2++;
                str = line;
                i = 0;
            }
            if (!(WebVttParser.this.mCue.mLinePosition == null && WebVttParser.this.mCue.mSize == 100 && WebVttParser.this.mCue.mWritingDirection == 100)) {
                WebVttParser.this.mCue.mRegionId = "";
            }
            WebVttParser.this.mPhase = WebVttParser.this.mParseCueText;
        }
    };
    private final Phase mParseHeader = new Phase() {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class cls = WebVttParser.class;
        }

        TextTrackRegion parseRegion(String s) {
            String anchorY;
            TextTrackRegion region = new TextTrackRegion();
            String[] split = s.split(" +");
            int length = split.length;
            String name = null;
            int i = 0;
            while (i < length) {
                String str;
                String setting = split[i];
                int equalAt = setting.indexOf(61);
                if (equalAt > 0 && equalAt != setting.length() - 1) {
                    String name2 = setting.substring(name, equalAt);
                    String value = setting.substring(equalAt + 1);
                    if (name2.equals(Instrumentation.REPORT_KEY_IDENTIFIER)) {
                        region.mId = value;
                    } else if (name2.equals(MediaFormat.KEY_WIDTH)) {
                        try {
                            region.mWidth = WebVttParser.parseFloatPercentage(value);
                        } catch (NumberFormatException e) {
                            WebVttParser.this.log_warning("region setting", name2, "has invalid value", e.getMessage(), value);
                        }
                    } else {
                        String value2 = value;
                        name = name2;
                        if (name.equals("lines")) {
                            name2 = value2;
                            if (name2.matches(".*[^0-9].*")) {
                                WebVttParser.this.log_warning("lines", name, "contains an invalid character", name2);
                            } else {
                                try {
                                    region.mLines = Integer.parseInt(name2);
                                } catch (NumberFormatException e2) {
                                    WebVttParser.this.log_warning("region setting", name, "is not numeric", name2);
                                }
                            }
                        } else {
                            name2 = value2;
                            if (name.equals("regionanchor") || name.equals("viewportanchor")) {
                                int commaAt = name2.indexOf(",");
                                if (commaAt < 0) {
                                    WebVttParser.this.log_warning("region setting", name, "contains no comma", name2);
                                } else {
                                    String anchorX = name2.substring(0, commaAt);
                                    String anchorY2 = name2.substring(commaAt + 1);
                                    try {
                                        float x = WebVttParser.parseFloatPercentage(anchorX);
                                        try {
                                            float y = WebVttParser.parseFloatPercentage(anchorY2);
                                            if (name.charAt(0) == 'r') {
                                                region.mAnchorPointX = x;
                                                region.mAnchorPointY = y;
                                            } else {
                                                region.mViewportAnchorPointX = x;
                                                region.mViewportAnchorPointY = y;
                                            }
                                        } catch (NumberFormatException e3) {
                                            NumberFormatException numberFormatException = e3;
                                            str = null;
                                            anchorY = anchorY2;
                                            value2 = name2;
                                            WebVttParser.this.log_warning("region setting", name, "has invalid y component", e3.getMessage(), anchorY);
                                        }
                                    } catch (NumberFormatException e32) {
                                        anchorY = anchorY2;
                                        int i2 = commaAt;
                                        value2 = name2;
                                        str = null;
                                        NumberFormatException numberFormatException2 = e32;
                                        anchorY2 = name;
                                        WebVttParser.this.log_warning("region setting", anchorY2, "has invalid x component", e32.getMessage(), anchorX);
                                    }
                                }
                            } else if (name.equals("scroll")) {
                                if (name2.equals("up")) {
                                    region.mScrollValue = 301;
                                } else {
                                    WebVttParser.this.log_warning("region setting", name, "has invalid value", name2);
                                }
                            }
                        }
                        str = null;
                        i++;
                        name = str;
                        anchorY = s;
                    }
                }
                str = name;
                i++;
                name = str;
                anchorY = s;
            }
            return region;
        }

        public void parse(String line) {
            if (line.length() == 0) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueId;
            } else if (line.contains("-->")) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseCueTime;
                WebVttParser.this.mPhase.parse(line);
            } else {
                int colonAt = line.indexOf(58);
                if (colonAt <= 0 || colonAt >= line.length() - 1) {
                    WebVttParser.this.log_warning("meta data header has invalid format", line);
                }
                String name = line.substring(null, colonAt);
                String value = line.substring(colonAt + 1);
                if (name.equals("Region")) {
                    WebVttParser.this.mListener.onRegionParsed(parseRegion(value));
                }
            }
        }
    };
    private final Phase mParseStart = new Phase() {
        public void parse(String line) {
            if (line.startsWith("﻿")) {
                line = line.substring(1);
            }
            if (line.equals("WEBVTT") || line.startsWith("WEBVTT ") || line.startsWith("WEBVTT\t")) {
                WebVttParser.this.mPhase = WebVttParser.this.mParseHeader;
                return;
            }
            WebVttParser.this.log_warning("Not a WEBVTT header", line);
            WebVttParser.this.mPhase = WebVttParser.this.mSkipRest;
        }
    };
    private Phase mPhase = this.mParseStart;
    private final Phase mSkipRest = new Phase() {
        public void parse(String line) {
        }
    };

    /* compiled from: WebVttRenderer */
    interface Phase {
        void parse(String str);
    }

    WebVttParser(WebVttCueListener listener) {
        this.mListener = listener;
        this.mCueTexts = new Vector();
    }

    public static float parseFloatPercentage(String s) throws NumberFormatException {
        if (s.endsWith("%")) {
            s = s.substring(0, s.length() - 1);
            if (s.matches(".*[^0-9.].*")) {
                throw new NumberFormatException("contains an invalid character");
            }
            try {
                float value = Float.parseFloat(s);
                if (value >= 0.0f && value <= 100.0f) {
                    return value;
                }
                throw new NumberFormatException("is out of range");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("is not a number");
            }
        }
        throw new NumberFormatException("does not end in %");
    }

    public static int parseIntPercentage(String s) throws NumberFormatException {
        if (s.endsWith("%")) {
            s = s.substring(0, s.length() - 1);
            if (s.matches(".*[^0-9].*")) {
                throw new NumberFormatException("contains an invalid character");
            }
            try {
                int value = Integer.parseInt(s);
                if (value >= 0 && value <= 100) {
                    return value;
                }
                throw new NumberFormatException("is out of range");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("is not a number");
            }
        }
        throw new NumberFormatException("does not end in %");
    }

    public static long parseTimestampMs(String s) throws NumberFormatException {
        if (s.matches("(\\d+:)?[0-5]\\d:[0-5]\\d\\.\\d{3}")) {
            String[] parts = s.split("\\.", 2);
            long value = 0;
            int i = 0;
            String[] split = parts[0].split(":");
            while (i < split.length) {
                value = (60 * value) + Long.parseLong(split[i]);
                i++;
            }
            return (1000 * value) + Long.parseLong(parts[1]);
        }
        throw new NumberFormatException("has invalid format");
    }

    public static String timeToString(long timeMs) {
        return String.format("%d:%02d:%02d.%03d", new Object[]{Long.valueOf(timeMs / 3600000), Long.valueOf((timeMs / 60000) % 60), Long.valueOf((timeMs / 1000) % 60), Long.valueOf(timeMs % 1000)});
    }

    public void parse(String s) {
        boolean trailingCR = false;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mBuffer);
        stringBuilder.append(s.replace("\u0000", "�"));
        this.mBuffer = stringBuilder.toString().replace("\r\n", "\n");
        int i = 0;
        if (this.mBuffer.endsWith("\r")) {
            trailingCR = true;
            this.mBuffer = this.mBuffer.substring(0, this.mBuffer.length() - 1);
        }
        String[] lines = this.mBuffer.split("[\r\n]");
        while (i < lines.length - 1) {
            this.mPhase.parse(lines[i]);
            i++;
        }
        this.mBuffer = lines[lines.length - 1];
        if (trailingCR) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mBuffer);
            stringBuilder2.append("\r");
            this.mBuffer = stringBuilder2.toString();
        }
    }

    public void eos() {
        if (this.mBuffer.endsWith("\r")) {
            this.mBuffer = this.mBuffer.substring(0, this.mBuffer.length() - 1);
        }
        this.mPhase.parse(this.mBuffer);
        this.mBuffer = "";
        yieldCue();
        this.mPhase = this.mParseStart;
    }

    public void yieldCue() {
        if (this.mCue != null && this.mCueTexts.size() > 0) {
            this.mCue.mStrings = new String[this.mCueTexts.size()];
            this.mCueTexts.toArray(this.mCue.mStrings);
            this.mCueTexts.clear();
            this.mListener.onCueParsed(this.mCue);
        }
        this.mCue = null;
    }

    private void log_warning(String nameType, String name, String message, String subMessage, String value) {
        String name2 = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(nameType);
        stringBuilder.append(" '");
        stringBuilder.append(name);
        stringBuilder.append("' ");
        stringBuilder.append(message);
        stringBuilder.append(" ('");
        stringBuilder.append(value);
        stringBuilder.append("' ");
        stringBuilder.append(subMessage);
        stringBuilder.append(")");
        Log.w(name2, stringBuilder.toString());
    }

    private void log_warning(String nameType, String name, String message, String value) {
        String name2 = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(nameType);
        stringBuilder.append(" '");
        stringBuilder.append(name);
        stringBuilder.append("' ");
        stringBuilder.append(message);
        stringBuilder.append(" ('");
        stringBuilder.append(value);
        stringBuilder.append("')");
        Log.w(name2, stringBuilder.toString());
    }

    private void log_warning(String message, String value) {
        String name = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(" ('");
        stringBuilder.append(value);
        stringBuilder.append("')");
        Log.w(name, stringBuilder.toString());
    }
}
