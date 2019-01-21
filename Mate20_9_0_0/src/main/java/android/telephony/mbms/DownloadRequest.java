package android.telephony.mbms;

import android.annotation.SystemApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class DownloadRequest implements Parcelable {
    public static final Creator<DownloadRequest> CREATOR = new Creator<DownloadRequest>() {
        public DownloadRequest createFromParcel(Parcel in) {
            return new DownloadRequest(in, null);
        }

        public DownloadRequest[] newArray(int size) {
            return new DownloadRequest[size];
        }
    };
    private static final int CURRENT_VERSION = 1;
    private static final String LOG_TAG = "MbmsDownloadRequest";
    public static final int MAX_APP_INTENT_SIZE = 50000;
    public static final int MAX_DESTINATION_URI_SIZE = 50000;
    private final Uri destinationUri;
    private final String fileServiceId;
    private final String serializedResultIntentForApp;
    private final Uri sourceUri;
    private final int subscriptionId;
    private final int version;

    public static class Builder {
        private String appIntent;
        private Uri destination;
        private String fileServiceId;
        private Uri source;
        private int subscriptionId;
        private int version = 1;

        public static Builder fromDownloadRequest(DownloadRequest other) {
            Builder result = new Builder(other.sourceUri, other.destinationUri).setServiceId(other.fileServiceId).setSubscriptionId(other.subscriptionId);
            result.appIntent = other.serializedResultIntentForApp;
            return result;
        }

        public static Builder fromSerializedRequest(byte[] data) {
            try {
                SerializationDataContainer dataContainer = (SerializationDataContainer) new ObjectInputStream(new ByteArrayInputStream(data)).readObject();
                Builder builder = new Builder(dataContainer.source, dataContainer.destination);
                builder.version = dataContainer.version;
                builder.appIntent = dataContainer.appIntent;
                builder.fileServiceId = dataContainer.fileServiceId;
                builder.subscriptionId = dataContainer.subscriptionId;
                return builder;
            } catch (IOException e) {
                Log.e(DownloadRequest.LOG_TAG, "Got IOException trying to parse opaque data");
                throw new IllegalArgumentException(e);
            } catch (ClassNotFoundException e2) {
                Log.e(DownloadRequest.LOG_TAG, "Got ClassNotFoundException trying to parse opaque data");
                throw new IllegalArgumentException(e2);
            }
        }

        public Builder(Uri sourceUri, Uri destinationUri) {
            if (sourceUri == null || destinationUri == null) {
                throw new IllegalArgumentException("Source and destination URIs must be non-null.");
            }
            this.source = sourceUri;
            this.destination = destinationUri;
        }

        public Builder setServiceInfo(FileServiceInfo serviceInfo) {
            this.fileServiceId = serviceInfo.getServiceId();
            return this;
        }

        @SystemApi
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

        public DownloadRequest build() {
            return new DownloadRequest(this.fileServiceId, this.source, this.destination, this.subscriptionId, this.appIntent, this.version, null);
        }
    }

    private static class SerializationDataContainer implements Externalizable {
        private String appIntent;
        private Uri destination;
        private String fileServiceId;
        private Uri source;
        private int subscriptionId;
        private int version;

        SerializationDataContainer(DownloadRequest request) {
            this.fileServiceId = request.fileServiceId;
            this.source = request.sourceUri;
            this.destination = request.destinationUri;
            this.subscriptionId = request.subscriptionId;
            this.appIntent = request.serializedResultIntentForApp;
            this.version = request.version;
        }

        public void writeExternal(ObjectOutput objectOutput) throws IOException {
            objectOutput.write(this.version);
            objectOutput.writeUTF(this.fileServiceId);
            objectOutput.writeUTF(this.source.toString());
            objectOutput.writeUTF(this.destination.toString());
            objectOutput.write(this.subscriptionId);
            objectOutput.writeUTF(this.appIntent);
        }

        public void readExternal(ObjectInput objectInput) throws IOException {
            this.version = objectInput.read();
            this.fileServiceId = objectInput.readUTF();
            this.source = Uri.parse(objectInput.readUTF());
            this.destination = Uri.parse(objectInput.readUTF());
            this.subscriptionId = objectInput.read();
            this.appIntent = objectInput.readUTF();
        }
    }

    /* synthetic */ DownloadRequest(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    /* synthetic */ DownloadRequest(String x0, Uri x1, Uri x2, int x3, String x4, int x5, AnonymousClass1 x6) {
        this(x0, x1, x2, x3, x4, x5);
    }

    private DownloadRequest(String fileServiceId, Uri source, Uri destination, int sub, String appIntent, int version) {
        this.fileServiceId = fileServiceId;
        this.sourceUri = source;
        this.subscriptionId = sub;
        this.destinationUri = destination;
        this.serializedResultIntentForApp = appIntent;
        this.version = version;
    }

    private DownloadRequest(Parcel in) {
        this.fileServiceId = in.readString();
        this.sourceUri = (Uri) in.readParcelable(getClass().getClassLoader());
        this.destinationUri = (Uri) in.readParcelable(getClass().getClassLoader());
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
        out.writeParcelable(this.destinationUri, flags);
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

    public Uri getDestinationUri() {
        return this.destinationUri;
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

    public byte[] toByteArray() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteArrayOutputStream);
            stream.writeObject(new SerializationDataContainer(this));
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

    public String getHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance(KeyProperties.DIGEST_SHA256);
            if (this.version >= 1) {
                digest.update(this.sourceUri.toString().getBytes(StandardCharsets.UTF_8));
                digest.update(this.destinationUri.toString().getBytes(StandardCharsets.UTF_8));
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
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof DownloadRequest)) {
            return false;
        }
        DownloadRequest request = (DownloadRequest) o;
        if (!(this.subscriptionId == request.subscriptionId && this.version == request.version && Objects.equals(this.fileServiceId, request.fileServiceId) && Objects.equals(this.sourceUri, request.sourceUri) && Objects.equals(this.destinationUri, request.destinationUri) && Objects.equals(this.serializedResultIntentForApp, request.serializedResultIntentForApp))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.fileServiceId, this.sourceUri, this.destinationUri, Integer.valueOf(this.subscriptionId), this.serializedResultIntentForApp, Integer.valueOf(this.version)});
    }
}
