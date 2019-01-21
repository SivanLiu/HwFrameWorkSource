package android.app.assist;

import android.app.Activity;
import android.app.slice.Slice;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.PooledStringReader;
import android.os.PooledStringWriter;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.ViewStructure;
import android.view.ViewStructure.HtmlInfo;
import android.view.ViewStructure.HtmlInfo.Builder;
import android.view.WindowManagerGlobal;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssistStructure implements Parcelable {
    public static final Creator<AssistStructure> CREATOR = new Creator<AssistStructure>() {
        public AssistStructure createFromParcel(Parcel in) {
            return new AssistStructure(in);
        }

        public AssistStructure[] newArray(int size) {
            return new AssistStructure[size];
        }
    };
    static final boolean DEBUG_PARCEL = false;
    static final boolean DEBUG_PARCEL_CHILDREN = false;
    static final boolean DEBUG_PARCEL_TREE = false;
    static final String DESCRIPTOR = "android.app.AssistStructure";
    static final String TAG = "AssistStructure";
    static final int TRANSACTION_XFER = 2;
    static final int VALIDATE_VIEW_TOKEN = 572662306;
    static final int VALIDATE_WINDOW_TOKEN = 286331153;
    private long mAcquisitionEndTime;
    private long mAcquisitionStartTime;
    ComponentName mActivityComponent;
    private int mFlags;
    boolean mHaveData;
    private boolean mIsHomeActivity;
    final ArrayList<ViewNodeBuilder> mPendingAsyncChildren;
    IBinder mReceiveChannel;
    boolean mSanitizeOnWrite;
    SendChannel mSendChannel;
    Rect mTmpRect;
    final ArrayList<WindowNode> mWindowNodes;

    public static class AutofillOverlay {
        public boolean focused;
        public AutofillValue value;
    }

    final class ParcelTransferReader {
        private final IBinder mChannel;
        private Parcel mCurParcel;
        int mNumReadViews;
        int mNumReadWindows;
        PooledStringReader mStringReader;
        final float[] mTmpMatrix = new float[9];
        private IBinder mTransferToken;

        ParcelTransferReader(IBinder channel) {
            this.mChannel = channel;
        }

        void go() {
            fetchData();
            AssistStructure.this.mActivityComponent = ComponentName.readFromParcel(this.mCurParcel);
            AssistStructure.this.mFlags = this.mCurParcel.readInt();
            AssistStructure.this.mAcquisitionStartTime = this.mCurParcel.readLong();
            AssistStructure.this.mAcquisitionEndTime = this.mCurParcel.readLong();
            int N = this.mCurParcel.readInt();
            if (N > 0) {
                this.mStringReader = new PooledStringReader(this.mCurParcel);
                for (int i = 0; i < N; i++) {
                    AssistStructure.this.mWindowNodes.add(new WindowNode(this));
                }
            }
            this.mCurParcel.recycle();
            this.mCurParcel = null;
        }

        Parcel readParcel(int validateToken, int level) {
            int token = this.mCurParcel.readInt();
            if (token == 0) {
                this.mTransferToken = this.mCurParcel.readStrongBinder();
                if (this.mTransferToken != null) {
                    fetchData();
                    this.mStringReader = new PooledStringReader(this.mCurParcel);
                    this.mCurParcel.readInt();
                    return this.mCurParcel;
                }
                throw new IllegalStateException("Reached end of partial data without transfer token");
            } else if (token == validateToken) {
                return this.mCurParcel;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got token ");
                stringBuilder.append(Integer.toHexString(token));
                stringBuilder.append(", expected token ");
                stringBuilder.append(Integer.toHexString(validateToken));
                throw new BadParcelableException(stringBuilder.toString());
            }
        }

        private void fetchData() {
            Parcel data = Parcel.obtain();
            try {
                data.writeInterfaceToken(AssistStructure.DESCRIPTOR);
                data.writeStrongBinder(this.mTransferToken);
                if (this.mCurParcel != null) {
                    this.mCurParcel.recycle();
                }
                this.mCurParcel = Parcel.obtain();
                this.mChannel.transact(2, data, this.mCurParcel, 0);
                data.recycle();
                this.mNumReadViews = 0;
                this.mNumReadWindows = 0;
            } catch (RemoteException e) {
                Log.w(AssistStructure.TAG, "Failure reading AssistStructure data", e);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failure reading AssistStructure data: ");
                stringBuilder.append(e);
                throw new IllegalStateException(stringBuilder.toString());
            } catch (Throwable th) {
                data.recycle();
            }
        }
    }

    public static class ViewNode {
        static final int FLAGS_ACCESSIBILITY_FOCUSED = 4096;
        static final int FLAGS_ACTIVATED = 8192;
        static final int FLAGS_ALL_CONTROL = -1048576;
        static final int FLAGS_ASSIST_BLOCKED = 128;
        static final int FLAGS_CHECKABLE = 256;
        static final int FLAGS_CHECKED = 512;
        static final int FLAGS_CLICKABLE = 1024;
        static final int FLAGS_CONTEXT_CLICKABLE = 16384;
        static final int FLAGS_DISABLED = 1;
        static final int FLAGS_FOCUSABLE = 16;
        static final int FLAGS_FOCUSED = 32;
        static final int FLAGS_HAS_ALPHA = 536870912;
        static final int FLAGS_HAS_AUTOFILL_DATA = Integer.MIN_VALUE;
        static final int FLAGS_HAS_CHILDREN = 1048576;
        static final int FLAGS_HAS_COMPLEX_TEXT = 8388608;
        static final int FLAGS_HAS_CONTENT_DESCRIPTION = 33554432;
        static final int FLAGS_HAS_ELEVATION = 268435456;
        static final int FLAGS_HAS_EXTRAS = 4194304;
        static final int FLAGS_HAS_ID = 2097152;
        static final int FLAGS_HAS_INPUT_TYPE = 262144;
        static final int FLAGS_HAS_LARGE_COORDS = 67108864;
        static final int FLAGS_HAS_LOCALE_LIST = 65536;
        static final int FLAGS_HAS_MATRIX = 1073741824;
        static final int FLAGS_HAS_SCROLL = 134217728;
        static final int FLAGS_HAS_TEXT = 16777216;
        static final int FLAGS_HAS_URL = 524288;
        static final int FLAGS_LONG_CLICKABLE = 2048;
        static final int FLAGS_OPAQUE = 32768;
        static final int FLAGS_SELECTED = 64;
        static final int FLAGS_VISIBILITY_MASK = 12;
        public static final int TEXT_COLOR_UNDEFINED = 1;
        public static final int TEXT_STYLE_BOLD = 1;
        public static final int TEXT_STYLE_ITALIC = 2;
        public static final int TEXT_STYLE_STRIKE_THRU = 8;
        public static final int TEXT_STYLE_UNDERLINE = 4;
        float mAlpha;
        String[] mAutofillHints;
        AutofillId mAutofillId;
        CharSequence[] mAutofillOptions;
        AutofillOverlay mAutofillOverlay;
        int mAutofillType;
        AutofillValue mAutofillValue;
        ViewNode[] mChildren;
        String mClassName;
        CharSequence mContentDescription;
        float mElevation;
        Bundle mExtras;
        int mFlags;
        int mHeight;
        HtmlInfo mHtmlInfo;
        int mId;
        String mIdEntry;
        String mIdPackage;
        String mIdType;
        int mImportantForAutofill;
        int mInputType;
        LocaleList mLocaleList;
        Matrix mMatrix;
        int mMaxEms;
        int mMaxLength;
        int mMinEms;
        boolean mSanitized;
        int mScrollX;
        int mScrollY;
        ViewNodeText mText;
        String mTextIdEntry;
        String mWebDomain;
        String mWebScheme;
        int mWidth;
        int mX;
        int mY;

        ViewNode() {
            this.mId = -1;
            this.mAutofillType = 0;
            this.mMinEms = -1;
            this.mMaxEms = -1;
            this.mMaxLength = -1;
            this.mAlpha = 1.0f;
        }

        ViewNode(ParcelTransferReader reader, int nestingLevel) {
            int val;
            this.mId = -1;
            int i = 0;
            this.mAutofillType = 0;
            this.mMinEms = -1;
            this.mMaxEms = -1;
            this.mMaxLength = -1;
            this.mAlpha = 1.0f;
            Parcel in = reader.readParcel(AssistStructure.VALIDATE_VIEW_TOKEN, nestingLevel);
            boolean z = true;
            reader.mNumReadViews++;
            PooledStringReader preader = reader.mStringReader;
            this.mClassName = preader.readString();
            this.mFlags = in.readInt();
            int flags = this.mFlags;
            if ((2097152 & flags) != 0) {
                this.mId = in.readInt();
                if (this.mId != -1) {
                    this.mIdEntry = preader.readString();
                    if (this.mIdEntry != null) {
                        this.mIdType = preader.readString();
                        this.mIdPackage = preader.readString();
                    }
                }
            }
            if ((Integer.MIN_VALUE & flags) != 0) {
                this.mSanitized = in.readInt() == 1;
                this.mAutofillId = (AutofillId) in.readParcelable(null);
                this.mAutofillType = in.readInt();
                this.mAutofillHints = in.readStringArray();
                this.mAutofillValue = (AutofillValue) in.readParcelable(null);
                this.mAutofillOptions = in.readCharSequenceArray();
                Parcelable p = in.readParcelable(null);
                if (p instanceof HtmlInfo) {
                    this.mHtmlInfo = (HtmlInfo) p;
                }
                this.mMinEms = in.readInt();
                this.mMaxEms = in.readInt();
                this.mMaxLength = in.readInt();
                this.mTextIdEntry = preader.readString();
                this.mImportantForAutofill = in.readInt();
            }
            if ((67108864 & flags) != 0) {
                this.mX = in.readInt();
                this.mY = in.readInt();
                this.mWidth = in.readInt();
                this.mHeight = in.readInt();
            } else {
                val = in.readInt();
                this.mX = val & 32767;
                this.mY = (val >> 16) & 32767;
                val = in.readInt();
                this.mWidth = val & 32767;
                this.mHeight = (val >> 16) & 32767;
            }
            if ((134217728 & flags) != 0) {
                this.mScrollX = in.readInt();
                this.mScrollY = in.readInt();
            }
            if ((1073741824 & flags) != 0) {
                this.mMatrix = new Matrix();
                in.readFloatArray(reader.mTmpMatrix);
                this.mMatrix.setValues(reader.mTmpMatrix);
            }
            if ((268435456 & flags) != 0) {
                this.mElevation = in.readFloat();
            }
            if ((536870912 & flags) != 0) {
                this.mAlpha = in.readFloat();
            }
            if ((33554432 & flags) != 0) {
                this.mContentDescription = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            }
            if ((16777216 & flags) != 0) {
                if ((8388608 & flags) != 0) {
                    z = false;
                }
                this.mText = new ViewNodeText(in, z);
            }
            if ((262144 & flags) != 0) {
                this.mInputType = in.readInt();
            }
            if ((524288 & flags) != 0) {
                this.mWebScheme = in.readString();
                this.mWebDomain = in.readString();
            }
            if ((65536 & flags) != 0) {
                this.mLocaleList = (LocaleList) in.readParcelable(null);
            }
            if ((4194304 & flags) != 0) {
                this.mExtras = in.readBundle();
            }
            if ((1048576 & flags) != 0) {
                val = in.readInt();
                this.mChildren = new ViewNode[val];
                while (i < val) {
                    this.mChildren[i] = new ViewNode(reader, nestingLevel + 1);
                    i++;
                }
            }
        }

        /* JADX WARNING: Missing block: B:19:0x0040, code skipped:
            if ((((r0.mWidth & -32768) != 0 ? 1 : 0) | ((r0.mHeight & -32768) != 0 ? 1 : 0)) != 0) goto L_0x0042;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        int writeSelfToParcel(Parcel out, PooledStringWriter pwriter, boolean sanitizeOnWrite, float[] tmpMatrix) {
            Parcel parcel = out;
            PooledStringWriter pooledStringWriter = pwriter;
            float[] fArr = tmpMatrix;
            boolean writeSensitive = true;
            int flags = this.mFlags & 1048575;
            if (this.mId != -1) {
                flags |= 2097152;
            }
            if (this.mAutofillId != null) {
                flags |= Integer.MIN_VALUE;
            }
            if ((this.mX & -32768) == 0 && (this.mY & -32768) == 0) {
            }
            flags |= 67108864;
            if (!(this.mScrollX == 0 && this.mScrollY == 0)) {
                flags |= 134217728;
            }
            if (this.mMatrix != null) {
                flags |= 1073741824;
            }
            if (this.mElevation != 0.0f) {
                flags |= 268435456;
            }
            if (this.mAlpha != 1.0f) {
                flags |= 536870912;
            }
            if (this.mContentDescription != null) {
                flags |= 33554432;
            }
            if (this.mText != null) {
                flags |= 16777216;
                if (!this.mText.isSimple()) {
                    flags |= 8388608;
                }
            }
            if (this.mInputType != 0) {
                flags |= 262144;
            }
            if (!(this.mWebScheme == null && this.mWebDomain == null)) {
                flags |= 524288;
            }
            if (this.mLocaleList != null) {
                flags |= 65536;
            }
            if (this.mExtras != null) {
                flags |= 4194304;
            }
            if (this.mChildren != null) {
                flags |= 1048576;
            }
            pooledStringWriter.writeString(this.mClassName);
            int writtenFlags = flags;
            if ((flags & Integer.MIN_VALUE) != 0 && (this.mSanitized || !sanitizeOnWrite)) {
                writtenFlags = flags & -513;
            }
            if (this.mAutofillOverlay != null) {
                if (this.mAutofillOverlay.focused) {
                    writtenFlags |= 32;
                } else {
                    writtenFlags &= -33;
                }
            }
            parcel.writeInt(writtenFlags);
            if ((2097152 & flags) != 0) {
                parcel.writeInt(this.mId);
                if (this.mId != -1) {
                    pooledStringWriter.writeString(this.mIdEntry);
                    if (this.mIdEntry != null) {
                        pooledStringWriter.writeString(this.mIdType);
                        pooledStringWriter.writeString(this.mIdPackage);
                    }
                }
            }
            if ((flags & Integer.MIN_VALUE) != 0) {
                AutofillValue sanitizedValue;
                boolean z = this.mSanitized || !sanitizeOnWrite;
                writeSensitive = z;
                parcel.writeInt(this.mSanitized);
                parcel.writeParcelable(this.mAutofillId, 0);
                parcel.writeInt(this.mAutofillType);
                parcel.writeStringArray(this.mAutofillHints);
                if (writeSensitive) {
                    sanitizedValue = this.mAutofillValue;
                } else if (this.mAutofillOverlay == null || this.mAutofillOverlay.value == null) {
                    sanitizedValue = null;
                } else {
                    sanitizedValue = this.mAutofillOverlay.value;
                }
                parcel.writeParcelable(sanitizedValue, 0);
                parcel.writeCharSequenceArray(this.mAutofillOptions);
                if (this.mHtmlInfo instanceof Parcelable) {
                    parcel.writeParcelable((Parcelable) this.mHtmlInfo, 0);
                } else {
                    parcel.writeParcelable(null, 0);
                }
                parcel.writeInt(this.mMinEms);
                parcel.writeInt(this.mMaxEms);
                parcel.writeInt(this.mMaxLength);
                pooledStringWriter.writeString(this.mTextIdEntry);
                parcel.writeInt(this.mImportantForAutofill);
            }
            if ((flags & 67108864) != 0) {
                parcel.writeInt(this.mX);
                parcel.writeInt(this.mY);
                parcel.writeInt(this.mWidth);
                parcel.writeInt(this.mHeight);
            } else {
                parcel.writeInt((this.mY << 16) | this.mX);
                parcel.writeInt((this.mHeight << 16) | this.mWidth);
            }
            if ((flags & 134217728) != 0) {
                parcel.writeInt(this.mScrollX);
                parcel.writeInt(this.mScrollY);
            }
            if ((flags & 1073741824) != 0) {
                this.mMatrix.getValues(fArr);
                parcel.writeFloatArray(fArr);
            }
            if ((flags & 268435456) != 0) {
                parcel.writeFloat(this.mElevation);
            }
            if ((flags & 536870912) != 0) {
                parcel.writeFloat(this.mAlpha);
            }
            if ((flags & 33554432) != 0) {
                TextUtils.writeToParcel(this.mContentDescription, parcel, 0);
            }
            if ((flags & 16777216) != 0) {
                this.mText.writeToParcel(parcel, (flags & 8388608) == 0, writeSensitive);
            }
            if ((flags & 262144) != 0) {
                parcel.writeInt(this.mInputType);
            }
            if ((524288 & flags) != 0) {
                parcel.writeString(this.mWebScheme);
                parcel.writeString(this.mWebDomain);
            }
            if ((65536 & flags) != 0) {
                parcel.writeParcelable(this.mLocaleList, 0);
            }
            if ((4194304 & flags) != 0) {
                parcel.writeBundle(this.mExtras);
            }
            return flags;
        }

        public int getId() {
            return this.mId;
        }

        public String getIdPackage() {
            return this.mIdPackage;
        }

        public String getIdType() {
            return this.mIdType;
        }

        public String getIdEntry() {
            return this.mIdEntry;
        }

        public AutofillId getAutofillId() {
            return this.mAutofillId;
        }

        public int getAutofillType() {
            return this.mAutofillType;
        }

        public String[] getAutofillHints() {
            return this.mAutofillHints;
        }

        public AutofillValue getAutofillValue() {
            return this.mAutofillValue;
        }

        public void setAutofillOverlay(AutofillOverlay overlay) {
            this.mAutofillOverlay = overlay;
        }

        public CharSequence[] getAutofillOptions() {
            return this.mAutofillOptions;
        }

        public int getInputType() {
            return this.mInputType;
        }

        public boolean isSanitized() {
            return this.mSanitized;
        }

        public void updateAutofillValue(AutofillValue value) {
            this.mAutofillValue = value;
            if (value.isText()) {
                if (this.mText == null) {
                    this.mText = new ViewNodeText();
                }
                this.mText.mText = value.getTextValue();
            }
        }

        public int getLeft() {
            return this.mX;
        }

        public int getTop() {
            return this.mY;
        }

        public int getScrollX() {
            return this.mScrollX;
        }

        public int getScrollY() {
            return this.mScrollY;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public Matrix getTransformation() {
            return this.mMatrix;
        }

        public float getElevation() {
            return this.mElevation;
        }

        public float getAlpha() {
            return this.mAlpha;
        }

        public int getVisibility() {
            return this.mFlags & 12;
        }

        public boolean isAssistBlocked() {
            return (this.mFlags & 128) != 0;
        }

        public boolean isEnabled() {
            return (this.mFlags & 1) == 0;
        }

        public boolean isClickable() {
            return (this.mFlags & 1024) != 0;
        }

        public boolean isFocusable() {
            return (this.mFlags & 16) != 0;
        }

        public boolean isFocused() {
            return (this.mFlags & 32) != 0;
        }

        public boolean isAccessibilityFocused() {
            return (this.mFlags & 4096) != 0;
        }

        public boolean isCheckable() {
            return (this.mFlags & 256) != 0;
        }

        public boolean isChecked() {
            return (this.mFlags & 512) != 0;
        }

        public boolean isSelected() {
            return (this.mFlags & 64) != 0;
        }

        public boolean isActivated() {
            return (this.mFlags & 8192) != 0;
        }

        public boolean isOpaque() {
            return (this.mFlags & 32768) != 0;
        }

        public boolean isLongClickable() {
            return (this.mFlags & 2048) != 0;
        }

        public boolean isContextClickable() {
            return (this.mFlags & 16384) != 0;
        }

        public String getClassName() {
            return this.mClassName;
        }

        public CharSequence getContentDescription() {
            return this.mContentDescription;
        }

        public String getWebDomain() {
            return this.mWebDomain;
        }

        public void setWebDomain(String domain) {
            if (domain != null) {
                Uri uri = Uri.parse(domain);
                if (uri == null) {
                    Log.w(AssistStructure.TAG, "Failed to parse web domain");
                    return;
                }
                this.mWebScheme = uri.getScheme();
                this.mWebDomain = uri.getHost();
            }
        }

        public String getWebScheme() {
            return this.mWebScheme;
        }

        public HtmlInfo getHtmlInfo() {
            return this.mHtmlInfo;
        }

        public LocaleList getLocaleList() {
            return this.mLocaleList;
        }

        public CharSequence getText() {
            return this.mText != null ? this.mText.mText : null;
        }

        public int getTextSelectionStart() {
            return this.mText != null ? this.mText.mTextSelectionStart : -1;
        }

        public int getTextSelectionEnd() {
            return this.mText != null ? this.mText.mTextSelectionEnd : -1;
        }

        public int getTextColor() {
            return this.mText != null ? this.mText.mTextColor : 1;
        }

        public int getTextBackgroundColor() {
            return this.mText != null ? this.mText.mTextBackgroundColor : 1;
        }

        public float getTextSize() {
            return this.mText != null ? this.mText.mTextSize : 0.0f;
        }

        public int getTextStyle() {
            return this.mText != null ? this.mText.mTextStyle : 0;
        }

        public int[] getTextLineCharOffsets() {
            return this.mText != null ? this.mText.mLineCharOffsets : null;
        }

        public int[] getTextLineBaselines() {
            return this.mText != null ? this.mText.mLineBaselines : null;
        }

        public String getTextIdEntry() {
            return this.mTextIdEntry;
        }

        public String getHint() {
            return this.mText != null ? this.mText.mHint : null;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }

        public int getChildCount() {
            return this.mChildren != null ? this.mChildren.length : 0;
        }

        public ViewNode getChildAt(int index) {
            return this.mChildren[index];
        }

        public int getMinTextEms() {
            return this.mMinEms;
        }

        public int getMaxTextEms() {
            return this.mMaxEms;
        }

        public int getMaxTextLength() {
            return this.mMaxLength;
        }

        public int getImportantForAutofill() {
            return this.mImportantForAutofill;
        }
    }

    static final class ViewNodeText {
        String mHint;
        int[] mLineBaselines;
        int[] mLineCharOffsets;
        CharSequence mText;
        int mTextBackgroundColor = 1;
        int mTextColor = 1;
        int mTextSelectionEnd;
        int mTextSelectionStart;
        float mTextSize;
        int mTextStyle;

        ViewNodeText() {
        }

        boolean isSimple() {
            return this.mTextBackgroundColor == 1 && this.mTextSelectionStart == 0 && this.mTextSelectionEnd == 0 && this.mLineCharOffsets == null && this.mLineBaselines == null && this.mHint == null;
        }

        ViewNodeText(Parcel in, boolean simple) {
            this.mText = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.mTextSize = in.readFloat();
            this.mTextStyle = in.readInt();
            this.mTextColor = in.readInt();
            if (!simple) {
                this.mTextBackgroundColor = in.readInt();
                this.mTextSelectionStart = in.readInt();
                this.mTextSelectionEnd = in.readInt();
                this.mLineCharOffsets = in.createIntArray();
                this.mLineBaselines = in.createIntArray();
                this.mHint = in.readString();
            }
        }

        void writeToParcel(Parcel out, boolean simple, boolean writeSensitive) {
            TextUtils.writeToParcel(writeSensitive ? this.mText : "", out, 0);
            out.writeFloat(this.mTextSize);
            out.writeInt(this.mTextStyle);
            out.writeInt(this.mTextColor);
            if (!simple) {
                out.writeInt(this.mTextBackgroundColor);
                out.writeInt(this.mTextSelectionStart);
                out.writeInt(this.mTextSelectionEnd);
                out.writeIntArray(this.mLineCharOffsets);
                out.writeIntArray(this.mLineBaselines);
                out.writeString(this.mHint);
            }
        }
    }

    static final class ViewStackEntry {
        int curChild;
        ViewNode node;
        int numChildren;

        ViewStackEntry() {
        }
    }

    public static class WindowNode {
        final int mDisplayId;
        final int mHeight;
        final ViewNode mRoot;
        final CharSequence mTitle;
        final int mWidth;
        final int mX;
        final int mY;

        WindowNode(AssistStructure assist, ViewRootImpl root, boolean forAutoFill, int flags) {
            View view = root.getView();
            Rect rect = new Rect();
            view.getBoundsOnScreen(rect);
            this.mX = rect.left - view.getLeft();
            this.mY = rect.top - view.getTop();
            this.mWidth = rect.width();
            this.mHeight = rect.height();
            this.mTitle = root.getTitle();
            this.mDisplayId = root.getDisplayId();
            this.mRoot = new ViewNode();
            ViewNodeBuilder builder = new ViewNodeBuilder(assist, this.mRoot, false);
            if ((root.getWindowFlags() & 8192) != 0) {
                if (forAutoFill) {
                    view.onProvideAutofillStructure(builder, resolveViewAutofillFlags(view.getContext(), flags));
                } else {
                    view.onProvideStructure(builder);
                    builder.setAssistBlocked(true);
                    return;
                }
            }
            if (forAutoFill) {
                view.dispatchProvideAutofillStructure(builder, resolveViewAutofillFlags(view.getContext(), flags));
            } else {
                view.dispatchProvideStructure(builder);
            }
        }

        WindowNode(ParcelTransferReader reader) {
            Parcel in = reader.readParcel(AssistStructure.VALIDATE_WINDOW_TOKEN, 0);
            reader.mNumReadWindows++;
            this.mX = in.readInt();
            this.mY = in.readInt();
            this.mWidth = in.readInt();
            this.mHeight = in.readInt();
            this.mTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            this.mDisplayId = in.readInt();
            this.mRoot = new ViewNode(reader, 0);
        }

        int resolveViewAutofillFlags(Context context, int fillRequestFlags) {
            if ((fillRequestFlags & 1) != 0 || context.isAutofillCompatibilityEnabled()) {
                return 1;
            }
            return 0;
        }

        void writeSelfToParcel(Parcel out, PooledStringWriter pwriter, float[] tmpMatrix) {
            out.writeInt(this.mX);
            out.writeInt(this.mY);
            out.writeInt(this.mWidth);
            out.writeInt(this.mHeight);
            TextUtils.writeToParcel(this.mTitle, out, 0);
            out.writeInt(this.mDisplayId);
        }

        public int getLeft() {
            return this.mX;
        }

        public int getTop() {
            return this.mY;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public CharSequence getTitle() {
            return this.mTitle;
        }

        public int getDisplayId() {
            return this.mDisplayId;
        }

        public ViewNode getRootViewNode() {
            return this.mRoot;
        }
    }

    private static final class HtmlInfoNode extends HtmlInfo implements Parcelable {
        public static final Creator<HtmlInfoNode> CREATOR = new Creator<HtmlInfoNode>() {
            public HtmlInfoNode createFromParcel(Parcel parcel) {
                HtmlInfoNodeBuilder builder = new HtmlInfoNodeBuilder(parcel.readString());
                String[] names = parcel.readStringArray();
                String[] values = parcel.readStringArray();
                if (!(names == null || values == null)) {
                    if (names.length != values.length) {
                        String str = AssistStructure.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HtmlInfo attributes mismatch: names=");
                        stringBuilder.append(names.length);
                        stringBuilder.append(", values=");
                        stringBuilder.append(values.length);
                        Log.w(str, stringBuilder.toString());
                    } else {
                        for (int i = 0; i < names.length; i++) {
                            builder.addAttribute(names[i], values[i]);
                        }
                    }
                }
                return builder.build();
            }

            public HtmlInfoNode[] newArray(int size) {
                return new HtmlInfoNode[size];
            }
        };
        private ArrayList<Pair<String, String>> mAttributes;
        private final String[] mNames;
        private final String mTag;
        private final String[] mValues;

        /* synthetic */ HtmlInfoNode(HtmlInfoNodeBuilder x0, AnonymousClass1 x1) {
            this(x0);
        }

        private HtmlInfoNode(HtmlInfoNodeBuilder builder) {
            this.mTag = builder.mTag;
            if (builder.mNames == null) {
                this.mNames = null;
                this.mValues = null;
                return;
            }
            this.mNames = new String[builder.mNames.size()];
            this.mValues = new String[builder.mValues.size()];
            builder.mNames.toArray(this.mNames);
            builder.mValues.toArray(this.mValues);
        }

        public String getTag() {
            return this.mTag;
        }

        public List<Pair<String, String>> getAttributes() {
            if (this.mAttributes == null && this.mNames != null) {
                this.mAttributes = new ArrayList(this.mNames.length);
                for (int i = 0; i < this.mNames.length; i++) {
                    this.mAttributes.add(i, new Pair(this.mNames[i], this.mValues[i]));
                }
            }
            return this.mAttributes;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(this.mTag);
            parcel.writeStringArray(this.mNames);
            parcel.writeStringArray(this.mValues);
        }
    }

    private static final class HtmlInfoNodeBuilder extends Builder {
        private ArrayList<String> mNames;
        private final String mTag;
        private ArrayList<String> mValues;

        HtmlInfoNodeBuilder(String tag) {
            this.mTag = tag;
        }

        public Builder addAttribute(String name, String value) {
            if (this.mNames == null) {
                this.mNames = new ArrayList();
                this.mValues = new ArrayList();
            }
            this.mNames.add(name);
            this.mValues.add(value);
            return this;
        }

        public HtmlInfoNode build() {
            return new HtmlInfoNode(this, null);
        }
    }

    static class ViewNodeBuilder extends ViewStructure {
        final AssistStructure mAssist;
        final boolean mAsync;
        final ViewNode mNode;

        ViewNodeBuilder(AssistStructure assist, ViewNode node, boolean async) {
            this.mAssist = assist;
            this.mNode = node;
            this.mAsync = async;
        }

        public void setId(int id, String packageName, String typeName, String entryName) {
            this.mNode.mId = id;
            this.mNode.mIdPackage = packageName;
            this.mNode.mIdType = typeName;
            this.mNode.mIdEntry = entryName;
        }

        public void setDimens(int left, int top, int scrollX, int scrollY, int width, int height) {
            this.mNode.mX = left;
            this.mNode.mY = top;
            this.mNode.mScrollX = scrollX;
            this.mNode.mScrollY = scrollY;
            this.mNode.mWidth = width;
            this.mNode.mHeight = height;
        }

        public void setTransformation(Matrix matrix) {
            if (matrix == null) {
                this.mNode.mMatrix = null;
                return;
            }
            this.mNode.mMatrix = new Matrix(matrix);
        }

        public void setElevation(float elevation) {
            this.mNode.mElevation = elevation;
        }

        public void setAlpha(float alpha) {
            this.mNode.mAlpha = alpha;
        }

        public void setVisibility(int visibility) {
            this.mNode.mFlags = (this.mNode.mFlags & -13) | visibility;
        }

        public void setAssistBlocked(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -129) | (state ? 128 : 0);
        }

        public void setEnabled(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -2) | (state ^ 1);
        }

        public void setClickable(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -1025) | (state ? 1024 : 0);
        }

        public void setLongClickable(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -2049) | (state ? 2048 : 0);
        }

        public void setContextClickable(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -16385) | (state ? 16384 : 0);
        }

        public void setFocusable(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -17) | (state ? 16 : 0);
        }

        public void setFocused(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -33) | (state ? 32 : 0);
        }

        public void setAccessibilityFocused(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -4097) | (state ? 4096 : 0);
        }

        public void setCheckable(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -257) | (state ? 256 : 0);
        }

        public void setChecked(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -513) | (state ? 512 : 0);
        }

        public void setSelected(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -65) | (state ? 64 : 0);
        }

        public void setActivated(boolean state) {
            this.mNode.mFlags = (this.mNode.mFlags & -8193) | (state ? 8192 : 0);
        }

        public void setOpaque(boolean opaque) {
            this.mNode.mFlags = (this.mNode.mFlags & -32769) | (opaque ? 32768 : 0);
        }

        public void setClassName(String className) {
            this.mNode.mClassName = className;
        }

        public void setContentDescription(CharSequence contentDescription) {
            this.mNode.mContentDescription = contentDescription;
        }

        private final ViewNodeText getNodeText() {
            if (this.mNode.mText != null) {
                return this.mNode.mText;
            }
            this.mNode.mText = new ViewNodeText();
            return this.mNode.mText;
        }

        public void setText(CharSequence text) {
            ViewNodeText t = getNodeText();
            t.mText = TextUtils.trimNoCopySpans(text);
            t.mTextSelectionEnd = -1;
            t.mTextSelectionStart = -1;
        }

        public void setText(CharSequence text, int selectionStart, int selectionEnd) {
            ViewNodeText t = getNodeText();
            t.mText = TextUtils.trimNoCopySpans(text);
            t.mTextSelectionStart = selectionStart;
            t.mTextSelectionEnd = selectionEnd;
        }

        public void setTextStyle(float size, int fgColor, int bgColor, int style) {
            ViewNodeText t = getNodeText();
            t.mTextColor = fgColor;
            t.mTextBackgroundColor = bgColor;
            t.mTextSize = size;
            t.mTextStyle = style;
        }

        public void setTextLines(int[] charOffsets, int[] baselines) {
            ViewNodeText t = getNodeText();
            t.mLineCharOffsets = charOffsets;
            t.mLineBaselines = baselines;
        }

        public void setTextIdEntry(String entryName) {
            this.mNode.mTextIdEntry = (String) Preconditions.checkNotNull(entryName);
        }

        public void setHint(CharSequence hint) {
            getNodeText().mHint = hint != null ? hint.toString() : null;
        }

        public CharSequence getText() {
            return this.mNode.mText != null ? this.mNode.mText.mText : null;
        }

        public int getTextSelectionStart() {
            return this.mNode.mText != null ? this.mNode.mText.mTextSelectionStart : -1;
        }

        public int getTextSelectionEnd() {
            return this.mNode.mText != null ? this.mNode.mText.mTextSelectionEnd : -1;
        }

        public CharSequence getHint() {
            return this.mNode.mText != null ? this.mNode.mText.mHint : null;
        }

        public Bundle getExtras() {
            if (this.mNode.mExtras != null) {
                return this.mNode.mExtras;
            }
            this.mNode.mExtras = new Bundle();
            return this.mNode.mExtras;
        }

        public boolean hasExtras() {
            return this.mNode.mExtras != null;
        }

        public void setChildCount(int num) {
            this.mNode.mChildren = new ViewNode[num];
        }

        public int addChildCount(int num) {
            if (this.mNode.mChildren == null) {
                setChildCount(num);
                return 0;
            }
            int start = this.mNode.mChildren.length;
            ViewNode[] newArray = new ViewNode[(start + num)];
            System.arraycopy(this.mNode.mChildren, 0, newArray, 0, start);
            this.mNode.mChildren = newArray;
            return start;
        }

        public int getChildCount() {
            return this.mNode.mChildren != null ? this.mNode.mChildren.length : 0;
        }

        public ViewStructure newChild(int index) {
            ViewNode node = new ViewNode();
            this.mNode.mChildren[index] = node;
            return new ViewNodeBuilder(this.mAssist, node, false);
        }

        public ViewStructure asyncNewChild(int index) {
            ViewNodeBuilder builder;
            synchronized (this.mAssist) {
                ViewNode node = new ViewNode();
                this.mNode.mChildren[index] = node;
                builder = new ViewNodeBuilder(this.mAssist, node, true);
                this.mAssist.mPendingAsyncChildren.add(builder);
            }
            return builder;
        }

        public void asyncCommit() {
            synchronized (this.mAssist) {
                StringBuilder stringBuilder;
                if (!this.mAsync) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Child ");
                    stringBuilder.append(this);
                    stringBuilder.append(" was not created with ViewStructure.asyncNewChild");
                    throw new IllegalStateException(stringBuilder.toString());
                } else if (this.mAssist.mPendingAsyncChildren.remove(this)) {
                    this.mAssist.notifyAll();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Child ");
                    stringBuilder.append(this);
                    stringBuilder.append(" already committed");
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
        }

        public Rect getTempRect() {
            return this.mAssist.mTmpRect;
        }

        public void setAutofillId(AutofillId id) {
            this.mNode.mAutofillId = id;
        }

        public void setAutofillId(AutofillId parentId, int virtualId) {
            this.mNode.mAutofillId = new AutofillId(parentId, virtualId);
        }

        public AutofillId getAutofillId() {
            return this.mNode.mAutofillId;
        }

        public void setAutofillType(int type) {
            this.mNode.mAutofillType = type;
        }

        public void setAutofillHints(String[] hints) {
            this.mNode.mAutofillHints = hints;
        }

        public void setAutofillValue(AutofillValue value) {
            this.mNode.mAutofillValue = value;
        }

        public void setAutofillOptions(CharSequence[] options) {
            this.mNode.mAutofillOptions = options;
        }

        public void setImportantForAutofill(int mode) {
            this.mNode.mImportantForAutofill = mode;
        }

        public void setInputType(int inputType) {
            this.mNode.mInputType = inputType;
        }

        public void setMinTextEms(int minEms) {
            this.mNode.mMinEms = minEms;
        }

        public void setMaxTextEms(int maxEms) {
            this.mNode.mMaxEms = maxEms;
        }

        public void setMaxTextLength(int maxLength) {
            this.mNode.mMaxLength = maxLength;
        }

        public void setDataIsSensitive(boolean sensitive) {
            this.mNode.mSanitized = sensitive ^ 1;
        }

        public void setWebDomain(String domain) {
            this.mNode.setWebDomain(domain);
        }

        public void setLocaleList(LocaleList localeList) {
            this.mNode.mLocaleList = localeList;
        }

        public Builder newHtmlInfoBuilder(String tagName) {
            return new HtmlInfoNodeBuilder(tagName);
        }

        public void setHtmlInfo(HtmlInfo htmlInfo) {
            this.mNode.mHtmlInfo = htmlInfo;
        }
    }

    static final class ParcelTransferWriter extends Binder {
        ViewStackEntry mCurViewStackEntry;
        int mCurViewStackPos;
        int mCurWindow;
        int mNumWindows;
        int mNumWrittenViews;
        int mNumWrittenWindows;
        final boolean mSanitizeOnWrite;
        final float[] mTmpMatrix = new float[9];
        final ArrayList<ViewStackEntry> mViewStack = new ArrayList();
        final boolean mWriteStructure;

        ParcelTransferWriter(AssistStructure as, Parcel out) {
            this.mSanitizeOnWrite = as.mSanitizeOnWrite;
            this.mWriteStructure = as.waitForReady();
            ComponentName.writeToParcel(as.mActivityComponent, out);
            out.writeInt(as.mFlags);
            out.writeLong(as.mAcquisitionStartTime);
            out.writeLong(as.mAcquisitionEndTime);
            this.mNumWindows = as.mWindowNodes.size();
            if (!this.mWriteStructure || this.mNumWindows <= 0) {
                out.writeInt(0);
            } else {
                out.writeInt(this.mNumWindows);
            }
        }

        void writeToParcel(AssistStructure as, Parcel out) {
            int start = out.dataPosition();
            this.mNumWrittenWindows = 0;
            this.mNumWrittenViews = 0;
            boolean more = writeToParcelInner(as, out);
            String str = AssistStructure.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Flattened ");
            stringBuilder.append(more ? Slice.HINT_PARTIAL : "final");
            stringBuilder.append(" assist data: ");
            stringBuilder.append(out.dataPosition() - start);
            stringBuilder.append(" bytes, containing ");
            stringBuilder.append(this.mNumWrittenWindows);
            stringBuilder.append(" windows, ");
            stringBuilder.append(this.mNumWrittenViews);
            stringBuilder.append(" views");
            Log.i(str, stringBuilder.toString());
        }

        boolean writeToParcelInner(AssistStructure as, Parcel out) {
            if (this.mNumWindows == 0) {
                return false;
            }
            PooledStringWriter pwriter = new PooledStringWriter(out);
            while (writeNextEntryToParcel(as, out, pwriter)) {
                if (out.dataSize() > 65536) {
                    out.writeInt(0);
                    out.writeStrongBinder(this);
                    pwriter.finish();
                    return true;
                }
            }
            pwriter.finish();
            this.mViewStack.clear();
            return false;
        }

        void pushViewStackEntry(ViewNode node, int pos) {
            ViewStackEntry entry;
            if (pos >= this.mViewStack.size()) {
                entry = new ViewStackEntry();
                this.mViewStack.add(entry);
            } else {
                entry = (ViewStackEntry) this.mViewStack.get(pos);
            }
            entry.node = node;
            entry.numChildren = node.getChildCount();
            entry.curChild = 0;
            this.mCurViewStackEntry = entry;
        }

        void writeView(ViewNode child, Parcel out, PooledStringWriter pwriter, int levelAdj) {
            out.writeInt(AssistStructure.VALIDATE_VIEW_TOKEN);
            int flags = child.writeSelfToParcel(out, pwriter, this.mSanitizeOnWrite, this.mTmpMatrix);
            this.mNumWrittenViews++;
            if ((1048576 & flags) != 0) {
                out.writeInt(child.mChildren.length);
                int pos = this.mCurViewStackPos + 1;
                this.mCurViewStackPos = pos;
                pushViewStackEntry(child, pos);
            }
        }

        boolean writeNextEntryToParcel(AssistStructure as, Parcel out, PooledStringWriter pwriter) {
            int pos;
            if (this.mCurViewStackEntry == null) {
                pos = this.mCurWindow;
                if (pos >= this.mNumWindows) {
                    return false;
                }
                WindowNode win = (WindowNode) as.mWindowNodes.get(pos);
                this.mCurWindow++;
                out.writeInt(AssistStructure.VALIDATE_WINDOW_TOKEN);
                win.writeSelfToParcel(out, pwriter, this.mTmpMatrix);
                this.mNumWrittenWindows++;
                ViewNode root = win.mRoot;
                this.mCurViewStackPos = 0;
                writeView(root, out, pwriter, 0);
                return true;
            } else if (this.mCurViewStackEntry.curChild < this.mCurViewStackEntry.numChildren) {
                ViewNode child = this.mCurViewStackEntry.node.mChildren[this.mCurViewStackEntry.curChild];
                ViewStackEntry viewStackEntry = this.mCurViewStackEntry;
                viewStackEntry.curChild++;
                writeView(child, out, pwriter, 1);
                return true;
            } else {
                do {
                    pos = this.mCurViewStackPos - 1;
                    this.mCurViewStackPos = pos;
                    if (pos < 0) {
                        this.mCurViewStackEntry = null;
                        break;
                    }
                    this.mCurViewStackEntry = (ViewStackEntry) this.mViewStack.get(pos);
                } while (this.mCurViewStackEntry.curChild >= this.mCurViewStackEntry.numChildren);
                return true;
            }
        }
    }

    static final class SendChannel extends Binder {
        volatile AssistStructure mAssistStructure;

        SendChannel(AssistStructure as) {
            this.mAssistStructure = as;
        }

        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 2) {
                return super.onTransact(code, data, reply, flags);
            }
            AssistStructure as = this.mAssistStructure;
            if (as == null) {
                return true;
            }
            data.enforceInterface(AssistStructure.DESCRIPTOR);
            IBinder token = data.readStrongBinder();
            if (token == null) {
                new ParcelTransferWriter(as, reply).writeToParcel(as, reply);
                return true;
            } else if (token instanceof ParcelTransferWriter) {
                ((ParcelTransferWriter) token).writeToParcel(as, reply);
                return true;
            } else {
                String str = AssistStructure.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caller supplied bad token type: ");
                stringBuilder.append(token);
                Log.w(str, stringBuilder.toString());
                return true;
            }
        }
    }

    public void setAcquisitionStartTime(long acquisitionStartTime) {
        this.mAcquisitionStartTime = acquisitionStartTime;
    }

    public void setAcquisitionEndTime(long acquisitionEndTime) {
        this.mAcquisitionEndTime = acquisitionEndTime;
    }

    public void setHomeActivity(boolean isHomeActivity) {
        this.mIsHomeActivity = isHomeActivity;
    }

    public long getAcquisitionStartTime() {
        ensureData();
        return this.mAcquisitionStartTime;
    }

    public long getAcquisitionEndTime() {
        ensureData();
        return this.mAcquisitionEndTime;
    }

    public AssistStructure(Activity activity, boolean forAutoFill, int flags) {
        this.mWindowNodes = new ArrayList();
        this.mPendingAsyncChildren = new ArrayList();
        this.mTmpRect = new Rect();
        int i = 0;
        this.mSanitizeOnWrite = false;
        this.mHaveData = true;
        this.mActivityComponent = activity.getComponentName();
        this.mFlags = flags;
        ArrayList<ViewRootImpl> views = WindowManagerGlobal.getInstance().getRootViews(activity.getActivityToken());
        while (i < views.size()) {
            ViewRootImpl root = (ViewRootImpl) views.get(i);
            if (root.getView() == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping window with dettached view: ");
                stringBuilder.append(root.getTitle());
                Log.w(str, stringBuilder.toString());
            } else {
                this.mWindowNodes.add(new WindowNode(this, root, forAutoFill, flags));
            }
            i++;
        }
    }

    public AssistStructure() {
        this.mWindowNodes = new ArrayList();
        this.mPendingAsyncChildren = new ArrayList();
        this.mTmpRect = new Rect();
        this.mSanitizeOnWrite = false;
        this.mHaveData = true;
        this.mActivityComponent = null;
        this.mFlags = 0;
    }

    public AssistStructure(Parcel in) {
        this.mWindowNodes = new ArrayList();
        this.mPendingAsyncChildren = new ArrayList();
        this.mTmpRect = new Rect();
        boolean z = false;
        this.mSanitizeOnWrite = false;
        if (in.readInt() == 1) {
            z = true;
        }
        this.mIsHomeActivity = z;
        this.mReceiveChannel = in.readStrongBinder();
    }

    public void sanitizeForParceling(boolean sanitize) {
        this.mSanitizeOnWrite = sanitize;
    }

    public void dump(boolean showSensitive) {
        if (this.mActivityComponent == null) {
            Log.i(TAG, "dump(): calling ensureData() first");
            ensureData();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Activity: ");
        stringBuilder.append(this.mActivityComponent.flattenToShortString());
        Log.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Sanitize on write: ");
        stringBuilder.append(this.mSanitizeOnWrite);
        Log.i(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Flags: ");
        stringBuilder.append(this.mFlags);
        Log.i(str, stringBuilder.toString());
        int N = getWindowNodeCount();
        for (int i = 0; i < N; i++) {
            WindowNode node = getWindowNodeAt(i);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Window #");
            stringBuilder2.append(i);
            stringBuilder2.append(" [");
            stringBuilder2.append(node.getLeft());
            stringBuilder2.append(",");
            stringBuilder2.append(node.getTop());
            stringBuilder2.append(" ");
            stringBuilder2.append(node.getWidth());
            stringBuilder2.append("x");
            stringBuilder2.append(node.getHeight());
            stringBuilder2.append("] ");
            stringBuilder2.append(node.getTitle());
            Log.i(str2, stringBuilder2.toString());
            dump("  ", node.getRootViewNode(), showSensitive);
        }
    }

    void dump(String prefix, ViewNode node, boolean showSensitive) {
        String type;
        String pkg;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        String safeText;
        String str;
        StringBuilder stringBuilder3;
        String str2;
        StringBuilder stringBuilder4;
        String str3;
        StringBuilder stringBuilder5;
        String str4 = prefix;
        boolean z = showSensitive;
        String str5 = TAG;
        StringBuilder stringBuilder6 = new StringBuilder();
        stringBuilder6.append(str4);
        stringBuilder6.append("View [");
        stringBuilder6.append(node.getLeft());
        stringBuilder6.append(",");
        stringBuilder6.append(node.getTop());
        stringBuilder6.append(" ");
        stringBuilder6.append(node.getWidth());
        stringBuilder6.append("x");
        stringBuilder6.append(node.getHeight());
        stringBuilder6.append("] ");
        stringBuilder6.append(node.getClassName());
        Log.i(str5, stringBuilder6.toString());
        int id = node.getId();
        if (id != 0) {
            stringBuilder6 = new StringBuilder();
            stringBuilder6.append(str4);
            stringBuilder6.append("  ID: #");
            stringBuilder6.append(Integer.toHexString(id));
            String entry = node.getIdEntry();
            if (entry != null) {
                type = node.getIdType();
                pkg = node.getIdPackage();
                stringBuilder6.append(" ");
                stringBuilder6.append(pkg);
                stringBuilder6.append(":");
                stringBuilder6.append(type);
                stringBuilder6.append("/");
                stringBuilder6.append(entry);
            }
            Log.i(TAG, stringBuilder6.toString());
        }
        int scrollX = node.getScrollX();
        int scrollY = node.getScrollY();
        if (!(scrollX == 0 && scrollY == 0)) {
            type = TAG;
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append(str4);
            stringBuilder7.append("  Scroll: ");
            stringBuilder7.append(scrollX);
            stringBuilder7.append(",");
            stringBuilder7.append(scrollY);
            Log.i(type, stringBuilder7.toString());
        }
        Matrix matrix = node.getTransformation();
        if (matrix != null) {
            pkg = TAG;
            StringBuilder stringBuilder8 = new StringBuilder();
            stringBuilder8.append(str4);
            stringBuilder8.append("  Transformation: ");
            stringBuilder8.append(matrix);
            Log.i(pkg, stringBuilder8.toString());
        }
        float elevation = node.getElevation();
        if (elevation != 0.0f) {
            String str6 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(str4);
            stringBuilder.append("  Elevation: ");
            stringBuilder.append(elevation);
            Log.i(str6, stringBuilder.toString());
        }
        if (node.getAlpha() != 0.0f) {
            String str7 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(str4);
            stringBuilder.append("  Alpha: ");
            stringBuilder.append(elevation);
            Log.i(str7, stringBuilder.toString());
        }
        CharSequence contentDescription = node.getContentDescription();
        if (contentDescription != null) {
            String str8 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str4);
            stringBuilder2.append("  Content description: ");
            stringBuilder2.append(contentDescription);
            Log.i(str8, stringBuilder2.toString());
        }
        CharSequence text = node.getText();
        if (text != null) {
            if (node.isSanitized() || z) {
                safeText = text.toString();
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("REDACTED[");
                stringBuilder2.append(text.length());
                stringBuilder2.append(" chars]");
                safeText = stringBuilder2.toString();
            }
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Text (sel ");
            stringBuilder3.append(node.getTextSelectionStart());
            stringBuilder3.append("-");
            stringBuilder3.append(node.getTextSelectionEnd());
            stringBuilder3.append("): ");
            stringBuilder3.append(safeText);
            Log.i(str, stringBuilder3.toString());
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Text size: ");
            stringBuilder3.append(node.getTextSize());
            stringBuilder3.append(" , style: #");
            stringBuilder3.append(node.getTextStyle());
            Log.i(str, stringBuilder3.toString());
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Text color fg: #");
            stringBuilder3.append(Integer.toHexString(node.getTextColor()));
            stringBuilder3.append(", bg: #");
            stringBuilder3.append(Integer.toHexString(node.getTextBackgroundColor()));
            Log.i(str, stringBuilder3.toString());
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Input type: ");
            stringBuilder3.append(node.getInputType());
            Log.i(str, stringBuilder3.toString());
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Resource id: ");
            stringBuilder3.append(node.getTextIdEntry());
            Log.i(str, stringBuilder3.toString());
        }
        safeText = node.getWebDomain();
        if (safeText != null) {
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str4);
            stringBuilder3.append("  Web domain: ");
            stringBuilder3.append(safeText);
            Log.i(str, stringBuilder3.toString());
        }
        HtmlInfo htmlInfo = node.getHtmlInfo();
        if (htmlInfo != null) {
            String str9 = TAG;
            StringBuilder stringBuilder9 = new StringBuilder();
            stringBuilder9.append(str4);
            stringBuilder9.append("  HtmlInfo: tag=");
            stringBuilder9.append(htmlInfo.getTag());
            stringBuilder9.append(", attr=");
            stringBuilder9.append(htmlInfo.getAttributes());
            Log.i(str9, stringBuilder9.toString());
        }
        LocaleList localeList = node.getLocaleList();
        if (localeList != null) {
            str2 = TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(str4);
            stringBuilder4.append("  LocaleList: ");
            stringBuilder4.append(localeList);
            Log.i(str2, stringBuilder4.toString());
        }
        str2 = node.getHint();
        if (str2 != null) {
            str3 = TAG;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append(str4);
            stringBuilder5.append("  Hint: ");
            stringBuilder5.append(str2);
            Log.i(str3, stringBuilder5.toString());
        }
        Bundle extras = node.getExtras();
        if (extras != null) {
            str3 = TAG;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append(str4);
            stringBuilder5.append("  Extras: ");
            stringBuilder5.append(extras);
            Log.i(str3, stringBuilder5.toString());
        }
        if (node.isAssistBlocked()) {
            String str10 = TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(str4);
            stringBuilder4.append("  BLOCKED");
            Log.i(str10, stringBuilder4.toString());
        }
        AutofillId autofillId = node.getAutofillId();
        if (autofillId == null) {
            str3 = TAG;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append(str4);
            stringBuilder5.append(" NO autofill ID");
            Log.i(str3, stringBuilder5.toString());
        } else {
            str5 = TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(str4);
            stringBuilder4.append("Autofill info: id= ");
            stringBuilder4.append(autofillId);
            stringBuilder4.append(", type=");
            stringBuilder4.append(node.getAutofillType());
            stringBuilder4.append(", options=");
            stringBuilder4.append(Arrays.toString(node.getAutofillOptions()));
            stringBuilder4.append(", hints=");
            stringBuilder4.append(Arrays.toString(node.getAutofillHints()));
            stringBuilder4.append(", value=");
            stringBuilder4.append(node.getAutofillValue());
            stringBuilder4.append(", sanitized=");
            stringBuilder4.append(node.isSanitized());
            stringBuilder4.append(", importantFor=");
            stringBuilder4.append(node.getImportantForAutofill());
            Log.i(str5, stringBuilder4.toString());
        }
        id = node.getChildCount();
        int NCHILDREN;
        ViewNode viewNode;
        if (id > 0) {
            str3 = TAG;
            stringBuilder5 = new StringBuilder();
            stringBuilder5.append(str4);
            stringBuilder5.append("  Children:");
            Log.i(str3, stringBuilder5.toString());
            autofillId = new StringBuilder();
            autofillId.append(str4);
            autofillId.append("    ");
            autofillId = autofillId.toString();
            int i = 0;
            while (i < id) {
                NCHILDREN = id;
                dump(autofillId, node.getChildAt(i), z);
                i++;
                id = NCHILDREN;
                str4 = prefix;
            }
            viewNode = node;
            NCHILDREN = id;
            return;
        }
        viewNode = node;
        NCHILDREN = id;
        AutofillId autofillId2 = autofillId;
    }

    public ComponentName getActivityComponent() {
        ensureData();
        return this.mActivityComponent;
    }

    public void setActivityComponent(ComponentName componentName) {
        ensureData();
        this.mActivityComponent = componentName;
    }

    public int getFlags() {
        return this.mFlags;
    }

    public boolean isHomeActivity() {
        return this.mIsHomeActivity;
    }

    public int getWindowNodeCount() {
        ensureData();
        return this.mWindowNodes.size();
    }

    public WindowNode getWindowNodeAt(int index) {
        ensureData();
        return (WindowNode) this.mWindowNodes.get(index);
    }

    public void ensureDataForAutofill() {
        if (!this.mHaveData) {
            this.mHaveData = true;
            Binder.allowBlocking(this.mReceiveChannel);
            try {
                new ParcelTransferReader(this.mReceiveChannel).go();
            } finally {
                Binder.defaultBlocking(this.mReceiveChannel);
            }
        }
    }

    public void ensureData() {
        if (!this.mHaveData) {
            this.mHaveData = true;
            new ParcelTransferReader(this.mReceiveChannel).go();
        }
    }

    boolean waitForReady() {
        boolean skipStructure = false;
        synchronized (this) {
            long endTime = SystemClock.uptimeMillis() + 5000;
            while (this.mPendingAsyncChildren.size() > 0) {
                long uptimeMillis = SystemClock.uptimeMillis();
                long now = uptimeMillis;
                if (uptimeMillis < endTime) {
                    try {
                        wait(endTime - now);
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (this.mPendingAsyncChildren.size() > 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping assist structure, waiting too long for async children (have ");
                stringBuilder.append(this.mPendingAsyncChildren.size());
                stringBuilder.append(" remaining");
                Log.w(str, stringBuilder.toString());
                skipStructure = true;
            }
        }
        return !skipStructure;
    }

    public void clearSendChannel() {
        if (this.mSendChannel != null) {
            this.mSendChannel.mAssistStructure = null;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.mIsHomeActivity);
        if (this.mHaveData) {
            if (this.mSendChannel == null) {
                this.mSendChannel = new SendChannel(this);
            }
            out.writeStrongBinder(this.mSendChannel);
            return;
        }
        out.writeStrongBinder(this.mReceiveChannel);
    }
}
