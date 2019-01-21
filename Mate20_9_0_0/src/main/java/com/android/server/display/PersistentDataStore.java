package com.android.server.display;

import android.graphics.Point;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessConfiguration.Builder;
import android.hardware.display.WifiDisplay;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseLongArray;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class PersistentDataStore {
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_DEVICE_ADDRESS = "deviceAddress";
    private static final String ATTR_DEVICE_ALIAS = "deviceAlias";
    private static final String ATTR_DEVICE_HDCP = "isSupportHDCP";
    private static final String ATTR_DEVICE_NAME = "deviceName";
    private static final String ATTR_LUX = "lux";
    private static final String ATTR_NITS = "nits";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_TIME_STAMP = "timestamp";
    private static final String ATTR_UNIQUE_ID = "unique-id";
    private static final String ATTR_USER_SERIAL = "user-serial";
    static final String TAG = "DisplayManager";
    private static final String TAG_BRIGHTNESS_CONFIGURATION = "brightness-configuration";
    private static final String TAG_BRIGHTNESS_CONFIGURATIONS = "brightness-configurations";
    private static final String TAG_BRIGHTNESS_CURVE = "brightness-curve";
    private static final String TAG_BRIGHTNESS_POINT = "brightness-point";
    private static final String TAG_COLOR_MODE = "color-mode";
    private static final String TAG_DISPLAY = "display";
    private static final String TAG_DISPLAY_MANAGER_STATE = "display-manager-state";
    private static final String TAG_DISPLAY_STATES = "display-states";
    private static final String TAG_REMEMBERED_WIFI_DISPLAYS = "remembered-wifi-displays";
    private static final String TAG_STABLE_DEVICE_VALUES = "stable-device-values";
    private static final String TAG_STABLE_DISPLAY_HEIGHT = "stable-display-height";
    private static final String TAG_STABLE_DISPLAY_WIDTH = "stable-display-width";
    private static final String TAG_WIFI_DISPLAY = "wifi-display";
    private BrightnessConfigurations mBrightnessConfigurations;
    private boolean mDirty;
    private final HashMap<String, DisplayState> mDisplayStates;
    private ArrayList<String> mHdcpSupportedList;
    private Injector mInjector;
    private boolean mLoaded;
    private ArrayList<WifiDisplay> mRememberedWifiDisplays;
    private final StableDeviceValues mStableDeviceValues;

    private static final class BrightnessConfigurations {
        private SparseArray<BrightnessConfiguration> mConfigurations = new SparseArray();
        private SparseArray<String> mPackageNames = new SparseArray();
        private SparseLongArray mTimeStamps = new SparseLongArray();

        private boolean setBrightnessConfigurationForUser(BrightnessConfiguration c, int userSerial, String packageName) {
            BrightnessConfiguration currentConfig = (BrightnessConfiguration) this.mConfigurations.get(userSerial);
            if (currentConfig == c || (currentConfig != null && currentConfig.equals(c))) {
                return false;
            }
            if (c != null) {
                if (packageName == null) {
                    this.mPackageNames.remove(userSerial);
                } else {
                    this.mPackageNames.put(userSerial, packageName);
                }
                this.mTimeStamps.put(userSerial, System.currentTimeMillis());
                this.mConfigurations.put(userSerial, c);
            } else {
                this.mPackageNames.remove(userSerial);
                this.mTimeStamps.delete(userSerial);
                this.mConfigurations.remove(userSerial);
            }
            return true;
        }

        public BrightnessConfiguration getBrightnessConfiguration(int userSerial) {
            return (BrightnessConfiguration) this.mConfigurations.get(userSerial);
        }

        public void loadFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION.equals(parser.getName())) {
                    int userSerial;
                    try {
                        userSerial = Integer.parseInt(parser.getAttributeValue(null, PersistentDataStore.ATTR_USER_SERIAL));
                    } catch (NumberFormatException nfe) {
                        Slog.e(PersistentDataStore.TAG, "Failed to read in brightness configuration", nfe);
                        userSerial = -1;
                    }
                    String packageName = parser.getAttributeValue(null, PersistentDataStore.ATTR_PACKAGE_NAME);
                    String timeStampString = parser.getAttributeValue(null, "timestamp");
                    long timeStamp = -1;
                    if (timeStampString != null) {
                        try {
                            timeStamp = Long.parseLong(timeStampString);
                        } catch (NumberFormatException e) {
                        }
                    }
                    try {
                        BrightnessConfiguration config = loadConfigurationFromXml(parser);
                        if (userSerial >= 0 && config != null) {
                            this.mConfigurations.put(userSerial, config);
                            if (timeStamp != -1) {
                                this.mTimeStamps.put(userSerial, timeStamp);
                            }
                            if (packageName != null) {
                                this.mPackageNames.put(userSerial, packageName);
                            }
                        }
                    } catch (IllegalArgumentException iae) {
                        Slog.e(PersistentDataStore.TAG, "Failed to load brightness configuration!", iae);
                    }
                }
            }
        }

        private static BrightnessConfiguration loadConfigurationFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            String description = null;
            Pair<float[], float[]> curve = null;
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_CURVE.equals(parser.getName())) {
                    description = parser.getAttributeValue(null, PersistentDataStore.ATTR_DESCRIPTION);
                    curve = loadCurveFromXml(parser);
                }
            }
            if (curve == null) {
                return null;
            }
            Builder builder = new Builder((float[]) curve.first, (float[]) curve.second);
            builder.setDescription(description);
            return builder.build();
        }

        private static Pair<float[], float[]> loadCurveFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            List<Float> luxLevels = new ArrayList();
            List<Float> nitLevels = new ArrayList();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (PersistentDataStore.TAG_BRIGHTNESS_POINT.equals(parser.getName())) {
                    luxLevels.add(Float.valueOf(loadFloat(parser.getAttributeValue(null, PersistentDataStore.ATTR_LUX))));
                    nitLevels.add(Float.valueOf(loadFloat(parser.getAttributeValue(null, PersistentDataStore.ATTR_NITS))));
                }
            }
            int N = luxLevels.size();
            float[] lux = new float[N];
            float[] nits = new float[N];
            for (int i = 0; i < N; i++) {
                lux[i] = ((Float) luxLevels.get(i)).floatValue();
                nits[i] = ((Float) nitLevels.get(i)).floatValue();
            }
            return Pair.create(lux, nits);
        }

        private static float loadFloat(String val) {
            try {
                return Float.parseFloat(val);
            } catch (NullPointerException | NumberFormatException e) {
                Slog.e(PersistentDataStore.TAG, "Failed to parse float loading brightness config", e);
                return Float.NEGATIVE_INFINITY;
            }
        }

        public void saveToXml(XmlSerializer serializer) throws IOException {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                int userSerial = this.mConfigurations.keyAt(i);
                BrightnessConfiguration config = (BrightnessConfiguration) this.mConfigurations.valueAt(i);
                serializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
                serializer.attribute(null, PersistentDataStore.ATTR_USER_SERIAL, Integer.toString(userSerial));
                String packageName = (String) this.mPackageNames.get(userSerial);
                if (packageName != null) {
                    serializer.attribute(null, PersistentDataStore.ATTR_PACKAGE_NAME, packageName);
                }
                long timestamp = this.mTimeStamps.get(userSerial, -1);
                if (timestamp != -1) {
                    serializer.attribute(null, "timestamp", Long.toString(timestamp));
                }
                saveConfigurationToXml(serializer, config);
                serializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_CONFIGURATION);
            }
        }

        private static void saveConfigurationToXml(XmlSerializer serializer, BrightnessConfiguration config) throws IOException {
            serializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_CURVE);
            if (config.getDescription() != null) {
                serializer.attribute(null, PersistentDataStore.ATTR_DESCRIPTION, config.getDescription());
            }
            Pair<float[], float[]> curve = config.getCurve();
            for (int i = 0; i < ((float[]) curve.first).length; i++) {
                serializer.startTag(null, PersistentDataStore.TAG_BRIGHTNESS_POINT);
                serializer.attribute(null, PersistentDataStore.ATTR_LUX, Float.toString(((float[]) curve.first)[i]));
                serializer.attribute(null, PersistentDataStore.ATTR_NITS, Float.toString(((float[]) curve.second)[i]));
                serializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_POINT);
            }
            serializer.endTag(null, PersistentDataStore.TAG_BRIGHTNESS_CURVE);
        }

        public void dump(PrintWriter pw, String prefix) {
            for (int i = 0; i < this.mConfigurations.size(); i++) {
                StringBuilder stringBuilder;
                int userSerial = this.mConfigurations.keyAt(i);
                long time = this.mTimeStamps.get(userSerial, -1);
                String packageName = (String) this.mPackageNames.get(userSerial);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("User ");
                stringBuilder2.append(userSerial);
                stringBuilder2.append(":");
                pw.println(stringBuilder2.toString());
                if (time != -1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append("  set at: ");
                    stringBuilder.append(TimeUtils.formatForLogging(time));
                    pw.println(stringBuilder.toString());
                }
                if (packageName != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(prefix);
                    stringBuilder.append("  set by: ");
                    stringBuilder.append(packageName);
                    pw.println(stringBuilder.toString());
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("  ");
                stringBuilder.append(this.mConfigurations.valueAt(i));
                pw.println(stringBuilder.toString());
            }
        }
    }

    private static final class DisplayState {
        private int mColorMode;

        private DisplayState() {
        }

        public boolean setColorMode(int colorMode) {
            if (colorMode == this.mColorMode) {
                return false;
            }
            this.mColorMode = colorMode;
            return true;
        }

        public int getColorMode() {
            return this.mColorMode;
        }

        public void loadFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (parser.getName().equals(PersistentDataStore.TAG_COLOR_MODE)) {
                    this.mColorMode = Integer.parseInt(parser.nextText());
                }
            }
        }

        public void saveToXml(XmlSerializer serializer) throws IOException {
            serializer.startTag(null, PersistentDataStore.TAG_COLOR_MODE);
            serializer.text(Integer.toString(this.mColorMode));
            serializer.endTag(null, PersistentDataStore.TAG_COLOR_MODE);
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("ColorMode=");
            stringBuilder.append(this.mColorMode);
            pw.println(stringBuilder.toString());
        }
    }

    @VisibleForTesting
    static class Injector {
        private final AtomicFile mAtomicFile = new AtomicFile(new File("/data/system/display-manager-state.xml"), "display-state");

        public InputStream openRead() throws FileNotFoundException {
            return this.mAtomicFile.openRead();
        }

        public OutputStream startWrite() throws IOException {
            return this.mAtomicFile.startWrite();
        }

        public void finishWrite(OutputStream os, boolean success) {
            if (os instanceof FileOutputStream) {
                FileOutputStream fos = (FileOutputStream) os;
                if (success) {
                    this.mAtomicFile.finishWrite(fos);
                    return;
                } else {
                    this.mAtomicFile.failWrite(fos);
                    return;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected OutputStream as argument: ");
            stringBuilder.append(os);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static final class StableDeviceValues {
        private int mHeight;
        private int mWidth;

        private StableDeviceValues() {
        }

        private Point getDisplaySize() {
            return new Point(this.mWidth, this.mHeight);
        }

        public boolean setDisplaySize(Point r) {
            if (this.mWidth == r.x && this.mHeight == r.y) {
                return false;
            }
            this.mWidth = r.x;
            this.mHeight = r.y;
            return true;
        }

        public void loadFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                String name = parser.getName();
                Object obj = -1;
                int hashCode = name.hashCode();
                if (hashCode != -1635792540) {
                    if (hashCode == 1069578729 && name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH)) {
                        obj = null;
                    }
                } else if (name.equals(PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT)) {
                    obj = 1;
                }
                switch (obj) {
                    case null:
                        this.mWidth = loadIntValue(parser);
                        break;
                    case 1:
                        this.mHeight = loadIntValue(parser);
                        break;
                    default:
                        break;
                }
            }
        }

        private static int loadIntValue(XmlPullParser parser) throws IOException, XmlPullParserException {
            try {
                return Integer.parseInt(parser.nextText());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void saveToXml(XmlSerializer serializer) throws IOException {
            if (this.mWidth > 0 && this.mHeight > 0) {
                serializer.startTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                serializer.text(Integer.toString(this.mWidth));
                serializer.endTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_WIDTH);
                serializer.startTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
                serializer.text(Integer.toString(this.mHeight));
                serializer.endTag(null, PersistentDataStore.TAG_STABLE_DISPLAY_HEIGHT);
            }
        }

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("StableDisplayWidth=");
            stringBuilder.append(this.mWidth);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("StableDisplayHeight=");
            stringBuilder.append(this.mHeight);
            pw.println(stringBuilder.toString());
        }
    }

    public PersistentDataStore() {
        this(new Injector());
    }

    @VisibleForTesting
    PersistentDataStore(Injector injector) {
        this.mRememberedWifiDisplays = new ArrayList();
        this.mDisplayStates = new HashMap();
        this.mHdcpSupportedList = new ArrayList();
        this.mStableDeviceValues = new StableDeviceValues();
        this.mBrightnessConfigurations = new BrightnessConfigurations();
        this.mInjector = injector;
    }

    public void saveIfNeeded() {
        if (this.mDirty) {
            save();
            this.mDirty = false;
        }
    }

    public WifiDisplay getRememberedWifiDisplay(String deviceAddress) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index >= 0) {
            return (WifiDisplay) this.mRememberedWifiDisplays.get(index);
        }
        return null;
    }

    public WifiDisplay[] getRememberedWifiDisplays() {
        loadIfNeeded();
        return (WifiDisplay[]) this.mRememberedWifiDisplays.toArray(new WifiDisplay[this.mRememberedWifiDisplays.size()]);
    }

    public WifiDisplay applyWifiDisplayAlias(WifiDisplay display) {
        if (display != null) {
            loadIfNeeded();
            String alias = null;
            int index = findRememberedWifiDisplay(display.getDeviceAddress());
            if (index >= 0) {
                alias = ((WifiDisplay) this.mRememberedWifiDisplays.get(index)).getDeviceAlias();
            }
            if (!Objects.equals(display.getDeviceAlias(), alias)) {
                return new WifiDisplay(display.getDeviceAddress(), display.getDeviceName(), alias, display.isAvailable(), display.canConnect(), display.isRemembered());
            }
        }
        return display;
    }

    public WifiDisplay[] applyWifiDisplayAliases(WifiDisplay[] displays) {
        WifiDisplay[] results = displays;
        if (results == null) {
            return results;
        }
        int count = displays.length;
        WifiDisplay[] results2 = results;
        for (int i = 0; i < count; i++) {
            WifiDisplay result = applyWifiDisplayAlias(displays[i]);
            if (result != displays[i]) {
                if (results2 == displays) {
                    results2 = new WifiDisplay[count];
                    System.arraycopy(displays, 0, results2, 0, count);
                }
                results2[i] = result;
            }
        }
        return results2;
    }

    public boolean rememberWifiDisplay(WifiDisplay display) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(display.getDeviceAddress());
        if (index < 0) {
            this.mRememberedWifiDisplays.add(display);
        } else if (((WifiDisplay) this.mRememberedWifiDisplays.get(index)).equals(display)) {
            return false;
        } else {
            this.mRememberedWifiDisplays.set(index, display);
        }
        setDirty();
        return true;
    }

    public boolean forgetWifiDisplay(String deviceAddress) {
        loadIfNeeded();
        int index = findRememberedWifiDisplay(deviceAddress);
        if (index < 0) {
            return false;
        }
        this.mRememberedWifiDisplays.remove(index);
        setDirty();
        return true;
    }

    private int findRememberedWifiDisplay(String deviceAddress) {
        int count = this.mRememberedWifiDisplays.size();
        for (int i = 0; i < count; i++) {
            if (((WifiDisplay) this.mRememberedWifiDisplays.get(i)).getDeviceAddress().equals(deviceAddress)) {
                return i;
            }
        }
        return -1;
    }

    public int getColorMode(DisplayDevice device) {
        if (!device.hasStableUniqueId()) {
            return -1;
        }
        DisplayState state = getDisplayState(device.getUniqueId(), false);
        if (state == null) {
            return -1;
        }
        return state.getColorMode();
    }

    public boolean setColorMode(DisplayDevice device, int colorMode) {
        if (!device.hasStableUniqueId() || !getDisplayState(device.getUniqueId(), true).setColorMode(colorMode)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Point getStableDisplaySize() {
        loadIfNeeded();
        return this.mStableDeviceValues.getDisplaySize();
    }

    public void setStableDisplaySize(Point size) {
        loadIfNeeded();
        if (this.mStableDeviceValues.setDisplaySize(size)) {
            setDirty();
        }
    }

    public void setBrightnessConfigurationForUser(BrightnessConfiguration c, int userSerial, String packageName) {
        loadIfNeeded();
        if (this.mBrightnessConfigurations.setBrightnessConfigurationForUser(c, userSerial, packageName)) {
            setDirty();
        }
    }

    public BrightnessConfiguration getBrightnessConfiguration(int userSerial) {
        loadIfNeeded();
        return this.mBrightnessConfigurations.getBrightnessConfiguration(userSerial);
    }

    private DisplayState getDisplayState(String uniqueId, boolean createIfAbsent) {
        loadIfNeeded();
        DisplayState state = (DisplayState) this.mDisplayStates.get(uniqueId);
        if (state != null || !createIfAbsent) {
            return state;
        }
        state = new DisplayState();
        this.mDisplayStates.put(uniqueId, state);
        setDirty();
        return state;
    }

    public void loadIfNeeded() {
        if (!this.mLoaded) {
            load();
            this.mLoaded = true;
        }
    }

    private void setDirty() {
        this.mDirty = true;
    }

    private void clearState() {
        this.mRememberedWifiDisplays.clear();
    }

    private void load() {
        clearState();
        try {
            InputStream is = this.mInjector.openRead();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new BufferedInputStream(is), StandardCharsets.UTF_8.name());
                loadFromXml(parser);
            } catch (IOException ex) {
                Slog.w(TAG, "Failed to load display manager persistent store data.", ex);
                clearState();
            } catch (XmlPullParserException ex2) {
                Slog.w(TAG, "Failed to load display manager persistent store data.", ex2);
                clearState();
            } catch (Throwable th) {
                IoUtils.closeQuietly(is);
            }
            IoUtils.closeQuietly(is);
        } catch (FileNotFoundException e) {
        }
    }

    private void save() {
        OutputStream os;
        try {
            os = this.mInjector.startWrite();
            XmlSerializer serializer = new FastXmlSerializer();
            serializer.setOutput(new BufferedOutputStream(os), StandardCharsets.UTF_8.name());
            saveToXml(serializer);
            serializer.flush();
            this.mInjector.finishWrite(os, true);
        } catch (IOException ex) {
            Slog.w(TAG, "Failed to save display manager persistent store data.", ex);
        } catch (Throwable th) {
            this.mInjector.finishWrite(os, false);
        }
    }

    private void loadFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        XmlUtils.beginDocument(parser, TAG_DISPLAY_MANAGER_STATE);
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_REMEMBERED_WIFI_DISPLAYS)) {
                loadRememberedWifiDisplaysFromXml(parser);
            }
            if (parser.getName().equals(TAG_DISPLAY_STATES)) {
                loadDisplaysFromXml(parser);
            }
            if (parser.getName().equals(TAG_STABLE_DEVICE_VALUES)) {
                this.mStableDeviceValues.loadFromXml(parser);
            }
            if (parser.getName().equals(TAG_BRIGHTNESS_CONFIGURATIONS)) {
                this.mBrightnessConfigurations.loadFromXml(parser);
            }
        }
    }

    private void loadRememberedWifiDisplaysFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_WIFI_DISPLAY)) {
                String deviceAddress = parser.getAttributeValue(null, ATTR_DEVICE_ADDRESS);
                String deviceName = parser.getAttributeValue(null, ATTR_DEVICE_NAME);
                String deviceAlias = parser.getAttributeValue(null, ATTR_DEVICE_ALIAS);
                if (deviceAddress == null || deviceName == null) {
                    throw new XmlPullParserException("Missing deviceAddress or deviceName attribute on wifi-display.");
                } else if (findRememberedWifiDisplay(deviceAddress) < 0) {
                    if (Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_DEVICE_HDCP))) {
                        addHdcpSupportedDevice(deviceAddress);
                    }
                    this.mRememberedWifiDisplays.add(new WifiDisplay(deviceAddress, deviceName, deviceAlias, false, false, false));
                } else {
                    throw new XmlPullParserException("Found duplicate wifi display device address.");
                }
            }
        }
    }

    private void loadDisplaysFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals(TAG_DISPLAY)) {
                String uniqueId = parser.getAttributeValue(null, ATTR_UNIQUE_ID);
                if (uniqueId == null) {
                    throw new XmlPullParserException("Missing unique-id attribute on display.");
                } else if (this.mDisplayStates.containsKey(uniqueId)) {
                    throw new XmlPullParserException("Found duplicate display.");
                } else {
                    DisplayState state = new DisplayState();
                    state.loadFromXml(parser);
                    this.mDisplayStates.put(uniqueId, state);
                }
            }
        }
    }

    private void saveToXml(XmlSerializer serializer) throws IOException {
        serializer.startDocument(null, Boolean.valueOf(true));
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        serializer.startTag(null, TAG_DISPLAY_MANAGER_STATE);
        serializer.startTag(null, TAG_REMEMBERED_WIFI_DISPLAYS);
        Iterator it = this.mRememberedWifiDisplays.iterator();
        while (it.hasNext()) {
            WifiDisplay display = (WifiDisplay) it.next();
            serializer.startTag(null, TAG_WIFI_DISPLAY);
            serializer.attribute(null, ATTR_DEVICE_ADDRESS, display.getDeviceAddress());
            serializer.attribute(null, ATTR_DEVICE_NAME, display.getDeviceName());
            if (display.getDeviceAlias() != null) {
                serializer.attribute(null, ATTR_DEVICE_ALIAS, display.getDeviceAlias());
            }
            serializer.attribute(null, ATTR_DEVICE_HDCP, Boolean.toString(isHdcpSupported(display.getDeviceAddress())));
            serializer.endTag(null, TAG_WIFI_DISPLAY);
        }
        serializer.endTag(null, TAG_REMEMBERED_WIFI_DISPLAYS);
        serializer.startTag(null, TAG_DISPLAY_STATES);
        for (Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            String uniqueId = (String) entry.getKey();
            DisplayState state = (DisplayState) entry.getValue();
            serializer.startTag(null, TAG_DISPLAY);
            serializer.attribute(null, ATTR_UNIQUE_ID, uniqueId);
            state.saveToXml(serializer);
            serializer.endTag(null, TAG_DISPLAY);
        }
        serializer.endTag(null, TAG_DISPLAY_STATES);
        serializer.startTag(null, TAG_STABLE_DEVICE_VALUES);
        this.mStableDeviceValues.saveToXml(serializer);
        serializer.endTag(null, TAG_STABLE_DEVICE_VALUES);
        serializer.startTag(null, TAG_BRIGHTNESS_CONFIGURATIONS);
        this.mBrightnessConfigurations.saveToXml(serializer);
        serializer.endTag(null, TAG_BRIGHTNESS_CONFIGURATIONS);
        serializer.endTag(null, TAG_DISPLAY_MANAGER_STATE);
        serializer.endDocument();
    }

    public void dump(PrintWriter pw) {
        StringBuilder stringBuilder;
        int i;
        pw.println("PersistentDataStore");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mLoaded=");
        stringBuilder2.append(this.mLoaded);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mDirty=");
        stringBuilder2.append(this.mDirty);
        pw.println(stringBuilder2.toString());
        pw.println("  RememberedWifiDisplays:");
        int i2 = 0;
        Iterator it = this.mRememberedWifiDisplays.iterator();
        while (it.hasNext()) {
            WifiDisplay display = (WifiDisplay) it.next();
            stringBuilder = new StringBuilder();
            stringBuilder.append("    ");
            i = i2 + 1;
            stringBuilder.append(i2);
            stringBuilder.append(": ");
            stringBuilder.append(display);
            pw.println(stringBuilder.toString());
            i2 = i;
        }
        pw.println("  DisplayStates:");
        i2 = 0;
        for (Entry<String, DisplayState> entry : this.mDisplayStates.entrySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("    ");
            i = i2 + 1;
            stringBuilder.append(i2);
            stringBuilder.append(": ");
            stringBuilder.append((String) entry.getKey());
            pw.println(stringBuilder.toString());
            ((DisplayState) entry.getValue()).dump(pw, "      ");
            i2 = i;
        }
        pw.println("  StableDeviceValues:");
        this.mStableDeviceValues.dump(pw, "      ");
        pw.println("  BrightnessConfigurations:");
        this.mBrightnessConfigurations.dump(pw, "      ");
    }

    public WifiDisplay applyWifiDisplayRemembered(WifiDisplay display) {
        if (display != null) {
            loadIfNeeded();
            if (findRememberedWifiDisplay(display.getDeviceAddress()) >= 0) {
                return new WifiDisplay(display.getDeviceAddress(), display.getDeviceName(), display.getDeviceAlias(), display.isAvailable(), display.canConnect(), true);
            }
        }
        return display;
    }

    /* JADX WARNING: Missing block: B:7:0x0017, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addHdcpSupportedDevice(String address) {
        if (!(address == null || address.length() == 0 || this.mHdcpSupportedList.contains(address))) {
            this.mHdcpSupportedList.add(address);
        }
    }

    public boolean isHdcpSupported(String address) {
        if (address == null || address.length() == 0) {
            return false;
        }
        return this.mHdcpSupportedList.contains(address);
    }
}
