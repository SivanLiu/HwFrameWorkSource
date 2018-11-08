package android.telephony.mbms;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class DownloadRequest implements Parcelable {
    public static final Creator<DownloadRequest> CREATOR = new Creator<DownloadRequest>() {
        public DownloadRequest createFromParcel(Parcel in) {
            return new DownloadRequest(in);
        }

        public DownloadRequest[] newArray(int size) {
            return new DownloadRequest[size];
        }
    };
    private static final int CURRENT_VERSION = 1;
    private static final String LOG_TAG = "MbmsDownloadRequest";
    public static final int MAX_APP_INTENT_SIZE = 50000;
    public static final int MAX_DESTINATION_URI_SIZE = 50000;
    private final String fileServiceId;
    private final String serializedResultIntentForApp;
    private final Uri sourceUri;
    private final int subscriptionId;
    private final int version;

    public static class Builder {
        private String appIntent;
        private String fileServiceId;
        private Uri source;
        private int subscriptionId;
        private int version = 1;

        public Builder(Uri sourceUri) {
            if (sourceUri == null) {
                throw new IllegalArgumentException("Source URI must be non-null.");
            }
            this.source = sourceUri;
        }

        public Builder setServiceInfo(FileServiceInfo serviceInfo) {
            this.fileServiceId = serviceInfo.getServiceId();
            return this;
        }

        public Builder setServiceId(String serviceId) {
            this.fileServiceId = serviceId;
            return this;
        }

        public Builder setSubscriptionId(int subscriptionId) {
            this.subscriptionId = subscriptionId;
            return this;
        }

        public Builder setAppIntent(Intent intent) {
            this.appIntent = intent.toUri(0);
            if (this.appIntent.length() <= 50000) {
                return this;
            }
            throw new IllegalArgumentException("App intent must not exceed length 50000");
        }

        public Builder setOpaqueData(byte[] data) {
            try {
                OpaqueDataContainer dataContainer = (OpaqueDataContainer) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
                this.version = dataContainer.version;
                this.appIntent = dataContainer.appIntent;
                return this;
            } catch (IOException e) {
                Log.e(DownloadRequest.LOG_TAG, "Got IOException trying to parse opaque data");
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e2) {
                Log.e(DownloadRequest.LOG_TAG, "Got ClassNotFoundException trying to parse opaque data");
                throw new IllegalArgumentException(e2);
            }
        }

        public DownloadRequest build() {
            return new DownloadRequest(this.fileServiceId, this.source, this.subscriptionId, this.appIntent, this.version);
        }
    }

    private static class OpaqueDataContainer implements Serializable {
        private final String appIntent;
        private final int version;

        public OpaqueDataContainer(String appIntent, int version) {
            this.appIntent = appIntent;
            this.version = version;
        }
    }

    private DownloadRequest(String fileServiceId, Uri source, int sub, String appIntent, int version) {
        this.fileServiceId = fileServiceId;
        this.sourceUri = source;
        this.subscriptionId = sub;
        this.serializedResultIntentForApp = appIntent;
        this.version = version;
    }

    public static DownloadRequest copy(DownloadRequest other) {
        return new DownloadRequest(other);
    }

    private DownloadRequest(DownloadRequest dr) {
        this.fileServiceId = dr.fileServiceId;
        this.sourceUri = dr.sourceUri;
        this.subscriptionId = dr.subscriptionId;
        this.serializedResultIntentForApp = dr.serializedResultIntentForApp;
        this.version = dr.version;
    }

    private DownloadRequest(Parcel in) {
        this.fileServiceId = in.readString();
        this.sourceUri = (Uri) in.readParcelable(getClass().getClassLoader());
        this.subscriptionId = in.readInt();
        this.serializedResultIntentForApp = in.readString();
        this.version = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.fileServiceId);
        out.writeParcelable(this.sourceUri, flags);
        out.writeInt(this.subscriptionId);
        out.writeString(this.serializedResultIntentForApp);
        out.writeInt(this.version);
    }

    public String getFileServiceId() {
        return this.fileServiceId;
    }

    public Uri getSourceUri() {
        return this.sourceUri;
    }

    public int getSubscriptionId() {
        return this.subscriptionId;
    }

    public Intent getIntentForApp() {
        try {
            return Intent.parseUri(this.serializedResultIntentForApp, 0);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public byte[] getOpaqueData() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteArrayOutputStream);
            stream.writeObject(new OpaqueDataContainer(this.serializedResultIntentForApp, this.version));
            stream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Got IOException trying to serialize opaque data");
            return null;
        }
    }

    public int getVersion() {
        return this.version;
    }

    public static int getMaxAppIntentSize() {
        return 50000;
    }

    public static int getMaxDestinationUriSize() {
        return 50000;
    }

    public boolean isMultipartDownload() {
        if (getSourceUri().getLastPathSegment() != null) {
            return getSourceUri().getLastPathSegment().contains(PhoneConstants.APN_TYPE_ALL);
        }
        return false;
    }

    public String getHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256);
            if (this.version >= 1) {
                digest.update(this.sourceUri.toString().getBytes(StandardCharsets.UTF_8));
                if (this.serializedResultIntentForApp != null) {
                    digest.update(this.serializedResultIntentForApp.getBytes(StandardCharsets.UTF_8));
                }
            }
            return Base64.encodeToString(digest.digest(), 10);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not get sha256 hash object");
        }
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DownloadRequest)) {
            return false;
        }
        DownloadRequest request = (DownloadRequest) o;
        if (this.subscriptionId == request.subscriptionId && this.version == request.version && Objects.equals(this.fileServiceId, request.fileServiceId) && Objects.equals(this.sourceUri, request.sourceUri)) {
            z = Objects.equals(this.serializedResultIntentForApp, request.serializedResultIntentForApp);
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.fileServiceId, this.sourceUri, Integer.valueOf(this.subscriptionId), this.serializedResultIntentForApp, Integer.valueOf(this.version)});
    }
}
