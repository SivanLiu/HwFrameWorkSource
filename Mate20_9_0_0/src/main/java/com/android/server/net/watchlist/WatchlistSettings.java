package com.android.server.net.watchlist;

import android.os.Environment;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.HexDump;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class WatchlistSettings {
    private static final String FILE_NAME = "watchlist_settings.xml";
    private static final int SECRET_KEY_LENGTH = 48;
    private static final String TAG = "WatchlistSettings";
    private static final WatchlistSettings sInstance = new WatchlistSettings();
    private byte[] mPrivacySecretKey;
    private final AtomicFile mXmlFile;

    public static WatchlistSettings getInstance() {
        return sInstance;
    }

    private WatchlistSettings() {
        this(getSystemWatchlistFile());
    }

    static File getSystemWatchlistFile() {
        return new File(Environment.getDataSystemDirectory(), FILE_NAME);
    }

    @VisibleForTesting
    protected WatchlistSettings(File xmlFile) {
        this.mPrivacySecretKey = null;
        this.mXmlFile = new AtomicFile(xmlFile, "net-watchlist");
        reloadSettings();
        if (this.mPrivacySecretKey == null) {
            this.mPrivacySecretKey = generatePrivacySecretKey();
            saveSettings();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0063 A:{Splitter: B:3:0x0009, ExcHandler: java.lang.IllegalStateException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0063 A:{Splitter: B:3:0x0009, ExcHandler: java.lang.IllegalStateException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0063 A:{Splitter: B:3:0x0009, ExcHandler: java.lang.IllegalStateException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0063 A:{Splitter: B:3:0x0009, ExcHandler: java.lang.IllegalStateException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x0063 A:{Splitter: B:3:0x0009, ExcHandler: java.lang.IllegalStateException (r0_4 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:30:0x0063, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:31:0x0064, code:
            android.util.Slog.e(TAG, "Failed parsing xml", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void reloadSettings() {
        if (this.mXmlFile.exists()) {
            FileInputStream stream;
            try {
                stream = this.mXmlFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(parser, "network-watchlist-settings");
                int outerDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    if (parser.getName().equals("secret-key")) {
                        this.mPrivacySecretKey = parseSecretKey(parser);
                    }
                }
                Slog.i(TAG, "Reload watchlist settings done");
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
            } catch (Throwable th) {
                r1.addSuppressed(th);
            }
        }
    }

    private byte[] parseSecretKey(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(2, null, "secret-key");
        byte[] key = HexDump.hexStringToByteArray(parser.nextText());
        parser.require(3, null, "secret-key");
        if (key != null && key.length == 48) {
            return key;
        }
        Log.e(TAG, "Unable to parse secret key");
        return null;
    }

    synchronized byte[] getPrivacySecretKey() {
        byte[] key;
        key = new byte[48];
        System.arraycopy(this.mPrivacySecretKey, 0, key, 0, 48);
        return key;
    }

    private byte[] generatePrivacySecretKey() {
        byte[] key = new byte[48];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private void saveSettings() {
        try {
            FileOutputStream stream = this.mXmlFile.startWrite();
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.startTag(null, "network-watchlist-settings");
                out.startTag(null, "secret-key");
                out.text(HexDump.toHexString(this.mPrivacySecretKey));
                out.endTag(null, "secret-key");
                out.endTag(null, "network-watchlist-settings");
                out.endDocument();
                this.mXmlFile.finishWrite(stream);
            } catch (IOException e) {
                Log.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mXmlFile.failWrite(stream);
            }
        } catch (IOException e2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write display settings: ");
            stringBuilder.append(e2);
            Log.w(str, stringBuilder.toString());
        }
    }
}
