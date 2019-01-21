package com.android.server.wm;

import android.graphics.Rect;
import android.os.Environment;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.pm.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DisplaySettings {
    private static final String TAG = "WindowManager";
    private final HashMap<String, Entry> mEntries = new HashMap();
    private final AtomicFile mFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "display_settings.xml"), "wm-displays");

    public static class Entry {
        public final String name;
        public int overscanBottom;
        public int overscanLeft;
        public int overscanRight;
        public int overscanTop;

        public Entry(String _name) {
            this.name = _name;
        }
    }

    /* JADX WARNING: Missing block: B:2:0x000b, code skipped:
            if (r0 == null) goto L_0x000d;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void getOverscanLocked(String name, String uniqueId, Rect outRect) {
        Entry entry;
        Entry entry2;
        if (uniqueId != null) {
            entry = (Entry) this.mEntries.get(uniqueId);
            entry2 = entry;
        }
        entry2 = (Entry) this.mEntries.get(name);
        entry = entry2;
        if (entry != null) {
            outRect.left = entry.overscanLeft;
            outRect.top = entry.overscanTop;
            outRect.right = entry.overscanRight;
            outRect.bottom = entry.overscanBottom;
            return;
        }
        outRect.set(0, 0, 0, 0);
    }

    public void setOverscanLocked(String uniqueId, String name, int left, int top, int right, int bottom) {
        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            this.mEntries.remove(uniqueId);
            this.mEntries.remove(name);
            return;
        }
        Entry entry = (Entry) this.mEntries.get(uniqueId);
        if (entry == null) {
            entry = new Entry(uniqueId);
            this.mEntries.put(uniqueId, entry);
        }
        entry.overscanLeft = left;
        entry.overscanTop = top;
        entry.overscanRight = right;
        entry.overscanBottom = bottom;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:76:0x0152=Splitter:B:76:0x0152, B:29:0x0074=Splitter:B:29:0x0074, B:41:0x00a6=Splitter:B:41:0x00a6, B:48:0x00c8=Splitter:B:48:0x00c8, B:55:0x00ea=Splitter:B:55:0x00ea, B:62:0x010c=Splitter:B:62:0x010c, B:69:0x012f=Splitter:B:69:0x012f} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x007c A:{SYNTHETIC, Splitter:B:32:0x007c} */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0024 A:{Catch:{ IllegalStateException -> 0x0134, NullPointerException -> 0x0111, NumberFormatException -> 0x00ee, XmlPullParserException -> 0x00cc, IOException -> 0x00aa, IndexOutOfBoundsException -> 0x0088, all -> 0x0085 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void readSettingsLocked() {
        String str;
        StringBuilder stringBuilder;
        try {
            FileInputStream stream = this.mFile.openRead();
            try {
                int type;
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type != 2) {
                            next = parser.getDepth();
                            while (true) {
                                int next2 = parser.next();
                                type = next2;
                                if (next2 != 1 && (type != 3 || parser.getDepth() > next)) {
                                    if (type != 3) {
                                        if (type != 4) {
                                            if (parser.getName().equals("display")) {
                                                readDisplay(parser);
                                            } else {
                                                String str2 = TAG;
                                                StringBuilder stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Unknown element under <display-settings>: ");
                                                stringBuilder2.append(parser.getName());
                                                Slog.w(str2, stringBuilder2.toString());
                                                XmlUtils.skipCurrentTag(parser);
                                            }
                                        }
                                    }
                                }
                            }
                            if (!true) {
                                this.mEntries.clear();
                            }
                            try {
                                stream.close();
                            } catch (IOException e) {
                            }
                            return;
                        }
                        throw new IllegalStateException("no start tag found");
                    }
                }
                if (type != 2) {
                }
            } catch (IllegalStateException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e2);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (NullPointerException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e3);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (NumberFormatException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e4);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (XmlPullParserException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e5);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (IOException e6) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e6);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (IndexOutOfBoundsException e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing ");
                stringBuilder.append(e7);
                Slog.w(str, stringBuilder.toString());
                if (null == null) {
                    this.mEntries.clear();
                }
                stream.close();
            } catch (Throwable th) {
                if (null == null) {
                    this.mEntries.clear();
                }
                try {
                    stream.close();
                } catch (IOException e8) {
                }
                throw th;
            }
        } catch (FileNotFoundException e9) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("No existing display settings ");
            stringBuilder3.append(this.mFile.getBaseFile());
            stringBuilder3.append("; starting empty");
            Slog.i(str3, stringBuilder3.toString());
        }
    }

    private int getIntAttribute(XmlPullParser parser, String name) {
        int i = 0;
        try {
            String str = parser.getAttributeValue(null, name);
            if (str != null) {
                i = Integer.parseInt(str);
            }
            return i;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void readDisplay(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        String name = parser.getAttributeValue(null, Settings.ATTR_NAME);
        if (name != null) {
            Entry entry = new Entry(name);
            entry.overscanLeft = getIntAttribute(parser, "overscanLeft");
            entry.overscanTop = getIntAttribute(parser, "overscanTop");
            entry.overscanRight = getIntAttribute(parser, "overscanRight");
            entry.overscanBottom = getIntAttribute(parser, "overscanBottom");
            this.mEntries.put(name, entry);
        }
        XmlUtils.skipCurrentTag(parser);
    }

    public void writeSettingsLocked() {
        try {
            FileOutputStream stream = this.mFile.startWrite();
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, "display-settings");
                for (Entry entry : this.mEntries.values()) {
                    out.startTag(null, "display");
                    out.attribute(null, Settings.ATTR_NAME, entry.name);
                    if (entry.overscanLeft != 0) {
                        out.attribute(null, "overscanLeft", Integer.toString(entry.overscanLeft));
                    }
                    if (entry.overscanTop != 0) {
                        out.attribute(null, "overscanTop", Integer.toString(entry.overscanTop));
                    }
                    if (entry.overscanRight != 0) {
                        out.attribute(null, "overscanRight", Integer.toString(entry.overscanRight));
                    }
                    if (entry.overscanBottom != 0) {
                        out.attribute(null, "overscanBottom", Integer.toString(entry.overscanBottom));
                    }
                    out.endTag(null, "display");
                }
                out.endTag(null, "display-settings");
                out.endDocument();
                this.mFile.finishWrite(stream);
            } catch (IOException e) {
                Slog.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mFile.failWrite(stream);
            }
        } catch (IOException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write display settings: ");
            stringBuilder.append(e2);
            Slog.w(str, stringBuilder.toString());
        }
    }
}
