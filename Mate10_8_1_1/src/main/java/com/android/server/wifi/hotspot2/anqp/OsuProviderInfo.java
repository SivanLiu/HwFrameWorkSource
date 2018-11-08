package com.android.server.wifi.hotspot2.anqp;

import android.net.Uri;
import android.text.TextUtils;
import com.android.server.wifi.ByteBufferReader;
import java.net.ProtocolException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OsuProviderInfo {
    private static final int MAXIMUM_I18N_STRING_LENGTH = 252;
    public static final int MINIMUM_LENGTH = 9;
    private final List<I18Name> mFriendlyNames;
    private final List<IconInfo> mIconInfoList;
    private final List<Integer> mMethodList;
    private final String mNetworkAccessIdentifier;
    private final Uri mServerUri;
    private final List<I18Name> mServiceDescriptions;

    public OsuProviderInfo(List<I18Name> friendlyNames, Uri serverUri, List<Integer> methodList, List<IconInfo> iconInfoList, String nai, List<I18Name> serviceDescriptions) {
        this.mFriendlyNames = friendlyNames;
        this.mServerUri = serverUri;
        this.mMethodList = methodList;
        this.mIconInfoList = iconInfoList;
        this.mNetworkAccessIdentifier = nai;
        this.mServiceDescriptions = serviceDescriptions;
    }

    public static OsuProviderInfo parse(ByteBuffer payload) throws ProtocolException {
        int length = ((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK;
        if (length < 9) {
            throw new ProtocolException("Invalid length value: " + length);
        }
        ByteBuffer byteBuffer = payload;
        List<I18Name> friendlyNameList = parseI18Names(getSubBuffer(byteBuffer, ((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK));
        Uri serverUri = Uri.parse(ByteBufferReader.readStringWithByteLength(payload, StandardCharsets.UTF_8));
        List<Integer> methodList = new ArrayList();
        for (int methodListLength = payload.get() & Constants.BYTE_MASK; methodListLength > 0; methodListLength--) {
            methodList.add(Integer.valueOf(payload.get() & Constants.BYTE_MASK));
        }
        byteBuffer = payload;
        ByteBuffer iconBuffer = getSubBuffer(byteBuffer, ((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK);
        List<IconInfo> iconInfoList = new ArrayList();
        while (iconBuffer.hasRemaining()) {
            iconInfoList.add(IconInfo.parse(iconBuffer));
        }
        return new OsuProviderInfo(friendlyNameList, serverUri, methodList, iconInfoList, ByteBufferReader.readStringWithByteLength(payload, StandardCharsets.UTF_8), parseI18Names(getSubBuffer(payload, ((int) ByteBufferReader.readInteger(payload, ByteOrder.LITTLE_ENDIAN, 2)) & Constants.SHORT_MASK)));
    }

    public List<I18Name> getFriendlyNames() {
        return Collections.unmodifiableList(this.mFriendlyNames);
    }

    public Uri getServerUri() {
        return this.mServerUri;
    }

    public List<Integer> getMethodList() {
        return Collections.unmodifiableList(this.mMethodList);
    }

    public List<IconInfo> getIconInfoList() {
        return Collections.unmodifiableList(this.mIconInfoList);
    }

    public String getNetworkAccessIdentifier() {
        return this.mNetworkAccessIdentifier;
    }

    public List<I18Name> getServiceDescriptions() {
        return Collections.unmodifiableList(this.mServiceDescriptions);
    }

    public String getFriendlyName() {
        return getI18String(this.mFriendlyNames);
    }

    public String getServiceDescription() {
        return getI18String(this.mServiceDescriptions);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof OsuProviderInfo)) {
            return false;
        }
        OsuProviderInfo that = (OsuProviderInfo) thatObject;
        if (this.mFriendlyNames != null ? this.mFriendlyNames.equals(that.mFriendlyNames) : that.mFriendlyNames == null) {
            if (this.mServerUri != null) {
                if (this.mServerUri.equals(that.mServerUri)) {
                }
            }
            if (this.mMethodList != null) {
                if (this.mMethodList.equals(that.mMethodList)) {
                }
            }
            if (this.mIconInfoList != null) {
                if (this.mIconInfoList.equals(that.mIconInfoList)) {
                }
            }
            if (TextUtils.equals(this.mNetworkAccessIdentifier, that.mNetworkAccessIdentifier)) {
                if (this.mServiceDescriptions != null) {
                    z = this.mServiceDescriptions.equals(that.mServiceDescriptions);
                } else if (that.mServiceDescriptions != null) {
                    z = false;
                }
                return z;
            }
        }
        z = false;
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.mFriendlyNames, this.mServerUri, this.mMethodList, this.mIconInfoList, this.mNetworkAccessIdentifier, this.mServiceDescriptions});
    }

    public String toString() {
        return "OsuProviderInfo{mFriendlyNames=" + this.mFriendlyNames + ", mServerUri=" + this.mServerUri + ", mMethodList=" + this.mMethodList + ", mIconInfoList=" + this.mIconInfoList + ", mNetworkAccessIdentifier=" + this.mNetworkAccessIdentifier + ", mServiceDescriptions=" + this.mServiceDescriptions + "}";
    }

    private static List<I18Name> parseI18Names(ByteBuffer payload) throws ProtocolException {
        List<I18Name> results = new ArrayList();
        while (payload.hasRemaining()) {
            I18Name name = I18Name.parse(payload);
            int textBytes = name.getText().getBytes(StandardCharsets.UTF_8).length;
            if (textBytes > 252) {
                throw new ProtocolException("I18Name string exceeds the maximum allowed " + textBytes);
            }
            results.add(name);
        }
        return results;
    }

    private static ByteBuffer getSubBuffer(ByteBuffer payload, int length) {
        if (payload.remaining() < length) {
            throw new BufferUnderflowException();
        }
        ByteBuffer subBuffer = payload.slice();
        subBuffer.limit(length);
        payload.position(payload.position() + length);
        return subBuffer;
    }

    private static String getI18String(List<I18Name> i18Strings) {
        for (I18Name name : i18Strings) {
            if (name.getLanguage().equals(Locale.getDefault().getLanguage())) {
                return name.getText();
            }
        }
        if (i18Strings.size() > 0) {
            return ((I18Name) i18Strings.get(0)).getText();
        }
        return null;
    }
}
