package android.media;

import android.content.Context;
import android.net.Uri;
import com.android.internal.util.Preconditions;
import java.io.FileDescriptor;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataSourceDesc {
    public static final long LONG_MAX = 576460752303423487L;
    public static final int TYPE_CALLBACK = 1;
    public static final int TYPE_FD = 2;
    public static final int TYPE_NONE = 0;
    public static final int TYPE_URI = 3;
    private long mEndPositionMs;
    private FileDescriptor mFD;
    private long mFDLength;
    private long mFDOffset;
    private Media2DataSource mMedia2DataSource;
    private String mMediaId;
    private long mStartPositionMs;
    private int mType;
    private Uri mUri;
    private Context mUriContext;
    private List<HttpCookie> mUriCookies;
    private Map<String, String> mUriHeader;

    public static class Builder {
        private long mEndPositionMs = DataSourceDesc.LONG_MAX;
        private FileDescriptor mFD;
        private long mFDLength = DataSourceDesc.LONG_MAX;
        private long mFDOffset = 0;
        private Media2DataSource mMedia2DataSource;
        private String mMediaId;
        private long mStartPositionMs = 0;
        private int mType = 0;
        private Uri mUri;
        private Context mUriContext;
        private List<HttpCookie> mUriCookies;
        private Map<String, String> mUriHeader;

        public Builder(DataSourceDesc dsd) {
            this.mType = dsd.mType;
            this.mMedia2DataSource = dsd.mMedia2DataSource;
            this.mFD = dsd.mFD;
            this.mFDOffset = dsd.mFDOffset;
            this.mFDLength = dsd.mFDLength;
            this.mUri = dsd.mUri;
            this.mUriHeader = dsd.mUriHeader;
            this.mUriCookies = dsd.mUriCookies;
            this.mUriContext = dsd.mUriContext;
            this.mMediaId = dsd.mMediaId;
            this.mStartPositionMs = dsd.mStartPositionMs;
            this.mEndPositionMs = dsd.mEndPositionMs;
        }

        public DataSourceDesc build() {
            StringBuilder stringBuilder;
            if (this.mType != 1 && this.mType != 2 && this.mType != 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal type: ");
                stringBuilder.append(this.mType);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.mStartPositionMs <= this.mEndPositionMs) {
                DataSourceDesc dsd = new DataSourceDesc();
                dsd.mType = this.mType;
                dsd.mMedia2DataSource = this.mMedia2DataSource;
                dsd.mFD = this.mFD;
                dsd.mFDOffset = this.mFDOffset;
                dsd.mFDLength = this.mFDLength;
                dsd.mUri = this.mUri;
                dsd.mUriHeader = this.mUriHeader;
                dsd.mUriCookies = this.mUriCookies;
                dsd.mUriContext = this.mUriContext;
                dsd.mMediaId = this.mMediaId;
                dsd.mStartPositionMs = this.mStartPositionMs;
                dsd.mEndPositionMs = this.mEndPositionMs;
                return dsd;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal start/end position: ");
                stringBuilder.append(this.mStartPositionMs);
                stringBuilder.append(" : ");
                stringBuilder.append(this.mEndPositionMs);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        public Builder setMediaId(String mediaId) {
            this.mMediaId = mediaId;
            return this;
        }

        public Builder setStartPosition(long position) {
            if (position < 0) {
                position = 0;
            }
            this.mStartPositionMs = position;
            return this;
        }

        public Builder setEndPosition(long position) {
            if (position < 0) {
                position = DataSourceDesc.LONG_MAX;
            }
            this.mEndPositionMs = position;
            return this;
        }

        public Builder setDataSource(Media2DataSource m2ds) {
            Preconditions.checkNotNull(m2ds);
            resetDataSource();
            this.mType = 1;
            this.mMedia2DataSource = m2ds;
            return this;
        }

        public Builder setDataSource(FileDescriptor fd) {
            Preconditions.checkNotNull(fd);
            resetDataSource();
            this.mType = 2;
            this.mFD = fd;
            return this;
        }

        public Builder setDataSource(FileDescriptor fd, long offset, long length) {
            Preconditions.checkNotNull(fd);
            if (offset < 0) {
                offset = 0;
            }
            if (length < 0) {
                length = DataSourceDesc.LONG_MAX;
            }
            resetDataSource();
            this.mType = 2;
            this.mFD = fd;
            this.mFDOffset = offset;
            this.mFDLength = length;
            return this;
        }

        public Builder setDataSource(Context context, Uri uri) {
            Preconditions.checkNotNull(context, "context cannot be null");
            Preconditions.checkNotNull(uri, "uri cannot be null");
            resetDataSource();
            this.mType = 3;
            this.mUri = uri;
            this.mUriContext = context;
            return this;
        }

        public Builder setDataSource(Context context, Uri uri, Map<String, String> headers, List<HttpCookie> cookies) {
            Preconditions.checkNotNull(context, "context cannot be null");
            Preconditions.checkNotNull(uri);
            if (cookies != null) {
                CookieHandler cookieHandler = CookieHandler.getDefault();
                if (!(cookieHandler == null || (cookieHandler instanceof CookieManager))) {
                    throw new IllegalArgumentException("The cookie handler has to be of CookieManager type when cookies are provided.");
                }
            }
            resetDataSource();
            this.mType = 3;
            this.mUri = uri;
            if (headers != null) {
                this.mUriHeader = new HashMap(headers);
            }
            if (cookies != null) {
                this.mUriCookies = new ArrayList(cookies);
            }
            this.mUriContext = context;
            return this;
        }

        private void resetDataSource() {
            this.mType = 0;
            this.mMedia2DataSource = null;
            this.mFD = null;
            this.mFDOffset = 0;
            this.mFDLength = DataSourceDesc.LONG_MAX;
            this.mUri = null;
            this.mUriHeader = null;
            this.mUriCookies = null;
            this.mUriContext = null;
        }
    }

    private DataSourceDesc() {
        this.mType = 0;
        this.mFDOffset = 0;
        this.mFDLength = LONG_MAX;
        this.mStartPositionMs = 0;
        this.mEndPositionMs = LONG_MAX;
    }

    public String getMediaId() {
        return this.mMediaId;
    }

    public long getStartPosition() {
        return this.mStartPositionMs;
    }

    public long getEndPosition() {
        return this.mEndPositionMs;
    }

    public int getType() {
        return this.mType;
    }

    public Media2DataSource getMedia2DataSource() {
        return this.mMedia2DataSource;
    }

    public FileDescriptor getFileDescriptor() {
        return this.mFD;
    }

    public long getFileDescriptorOffset() {
        return this.mFDOffset;
    }

    public long getFileDescriptorLength() {
        return this.mFDLength;
    }

    public Uri getUri() {
        return this.mUri;
    }

    public Map<String, String> getUriHeaders() {
        if (this.mUriHeader == null) {
            return null;
        }
        return new HashMap(this.mUriHeader);
    }

    public List<HttpCookie> getUriCookies() {
        if (this.mUriCookies == null) {
            return null;
        }
        return new ArrayList(this.mUriCookies);
    }

    public Context getUriContext() {
        return this.mUriContext;
    }
}
