package android.app.usage;

import android.annotation.SystemApi;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;

public final class UsageEvents implements Parcelable {
    public static final Creator<UsageEvents> CREATOR = new Creator<UsageEvents>() {
        public UsageEvents createFromParcel(Parcel source) {
            return new UsageEvents(source);
        }

        public UsageEvents[] newArray(int size) {
            return new UsageEvents[size];
        }
    };
    public static final String INSTANT_APP_CLASS_NAME = "android.instant_class";
    public static final String INSTANT_APP_PACKAGE_NAME = "android.instant_app";
    private final int mEventCount;
    private List<Event> mEventsToWrite;
    private int mIndex;
    private Parcel mParcel;
    private String[] mStringPool;

    public static final class Event {
        public static final int CHOOSER_ACTION = 9;
        public static final int CONFIGURATION_CHANGE = 5;
        public static final int CONTINUE_PREVIOUS_DAY = 4;
        public static final int END_OF_DAY = 3;
        public static final int FLAG_IS_PACKAGE_INSTANT_APP = 1;
        public static final int KEYGUARD_HIDDEN = 18;
        public static final int KEYGUARD_SHOWN = 17;
        public static final int MOVE_TO_BACKGROUND = 2;
        public static final int MOVE_TO_FOREGROUND = 1;
        public static final int NONE = 0;
        @SystemApi
        public static final int NOTIFICATION_INTERRUPTION = 12;
        @SystemApi
        public static final int NOTIFICATION_SEEN = 10;
        public static final int SCREEN_INTERACTIVE = 15;
        public static final int SCREEN_NON_INTERACTIVE = 16;
        public static final int SHORTCUT_INVOCATION = 8;
        @SystemApi
        public static final int SLICE_PINNED = 14;
        @SystemApi
        public static final int SLICE_PINNED_PRIV = 13;
        public static final int STANDBY_BUCKET_CHANGED = 11;
        @SystemApi
        public static final int SYSTEM_INTERACTION = 6;
        public static final int USER_INTERACTION = 7;
        public String mAction;
        public int mBucketAndReason;
        public String mClass;
        public Configuration mConfiguration;
        public String[] mContentAnnotations;
        public String mContentType;
        public int mDisplayId;
        public int mEventType;
        public int mFlags;
        public String mNotificationChannelId;
        public String mPackage;
        public String mShortcutId;
        public long mTimeStamp;

        @Retention(RetentionPolicy.SOURCE)
        public @interface EventFlags {
        }

        public Event(Event orig) {
            this.mPackage = orig.mPackage;
            this.mClass = orig.mClass;
            this.mTimeStamp = orig.mTimeStamp;
            this.mEventType = orig.mEventType;
            this.mConfiguration = orig.mConfiguration;
            this.mShortcutId = orig.mShortcutId;
            this.mAction = orig.mAction;
            this.mContentType = orig.mContentType;
            this.mContentAnnotations = orig.mContentAnnotations;
            this.mFlags = orig.mFlags;
            this.mBucketAndReason = orig.mBucketAndReason;
            this.mDisplayId = orig.mDisplayId;
            this.mNotificationChannelId = orig.mNotificationChannelId;
        }

        public String getPackageName() {
            return this.mPackage;
        }

        public String getClassName() {
            return this.mClass;
        }

        public long getTimeStamp() {
            return this.mTimeStamp;
        }

        public int getEventType() {
            return this.mEventType;
        }

        public Configuration getConfiguration() {
            return this.mConfiguration;
        }

        public String getShortcutId() {
            return this.mShortcutId;
        }

        public int getStandbyBucket() {
            return (this.mBucketAndReason & Color.RED) >>> 16;
        }

        public int getAppStandbyBucket() {
            return (this.mBucketAndReason & Color.RED) >>> 16;
        }

        public int getStandbyReason() {
            return this.mBucketAndReason & 65535;
        }

        @SystemApi
        public String getNotificationChannelId() {
            return this.mNotificationChannelId;
        }

        public Event getObfuscatedIfInstantApp() {
            if ((this.mFlags & 1) == 0) {
                return this;
            }
            Event ret = new Event(this);
            ret.mPackage = UsageEvents.INSTANT_APP_PACKAGE_NAME;
            ret.mClass = UsageEvents.INSTANT_APP_CLASS_NAME;
            return ret;
        }
    }

    public UsageEvents(Parcel in) {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        byte[] bytes = in.readBlob();
        Parcel data = Parcel.obtain();
        try {
            data.unmarshall(bytes, 0, bytes.length);
            data.setDataPosition(0);
            this.mEventCount = data.readInt();
            this.mIndex = data.readInt();
            if (this.mEventCount > 0) {
                this.mStringPool = data.createStringArray();
                int listByteLength = data.readInt();
                int positionInParcel = data.readInt();
                this.mParcel = Parcel.obtain();
                this.mParcel.setDataPosition(0);
                this.mParcel.appendFrom(data, data.dataPosition(), listByteLength);
                this.mParcel.setDataSize(this.mParcel.dataPosition());
                this.mParcel.setDataPosition(positionInParcel);
            }
            if (data != null) {
                data.recycle();
            }
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
        }
    }

    UsageEvents() {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        this.mEventCount = 0;
    }

    public UsageEvents(List<Event> events, String[] stringPool) {
        this.mEventsToWrite = null;
        this.mParcel = null;
        this.mIndex = 0;
        this.mStringPool = stringPool;
        this.mEventCount = events.size();
        this.mEventsToWrite = events;
    }

    public boolean hasNextEvent() {
        return this.mIndex < this.mEventCount;
    }

    public boolean getNextEvent(Event eventOut) {
        if (this.mIndex >= this.mEventCount) {
            return false;
        }
        readEventFromParcel(this.mParcel, eventOut);
        this.mIndex++;
        if (this.mIndex >= this.mEventCount) {
            this.mParcel.recycle();
            this.mParcel = null;
        }
        return true;
    }

    public void resetToStart() {
        this.mIndex = 0;
        if (this.mParcel != null) {
            this.mParcel.setDataPosition(0);
        }
    }

    private int findStringIndex(String str) {
        int index = Arrays.binarySearch(this.mStringPool, str);
        if (index >= 0) {
            return index;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("String '");
        stringBuilder.append(str);
        stringBuilder.append("' is not in the string pool");
        throw new IllegalStateException(stringBuilder.toString());
    }

    private void writeEventToParcel(Event event, Parcel p, int flags) {
        int packageIndex;
        int classIndex = -1;
        if (event.mPackage != null) {
            packageIndex = findStringIndex(event.mPackage);
        } else {
            packageIndex = -1;
        }
        if (event.mClass != null) {
            classIndex = findStringIndex(event.mClass);
        }
        p.writeInt(packageIndex);
        p.writeInt(classIndex);
        p.writeInt(event.mEventType);
        p.writeLong(event.mTimeStamp);
        switch (event.mEventType) {
            case 5:
                event.mConfiguration.writeToParcel(p, flags);
                return;
            case 8:
                p.writeString(event.mShortcutId);
                return;
            case 9:
                p.writeString(event.mAction);
                p.writeString(event.mContentType);
                p.writeStringArray(event.mContentAnnotations);
                return;
            case 11:
                p.writeInt(event.mBucketAndReason);
                return;
            case 12:
                p.writeString(event.mNotificationChannelId);
                return;
            default:
                return;
        }
    }

    private void readEventFromParcel(Parcel p, Event eventOut) {
        int packageIndex = p.readInt();
        if (packageIndex >= 0) {
            eventOut.mPackage = this.mStringPool[packageIndex];
        } else {
            eventOut.mPackage = null;
        }
        int classIndex = p.readInt();
        if (classIndex >= 0) {
            eventOut.mClass = this.mStringPool[classIndex];
        } else {
            eventOut.mClass = null;
        }
        eventOut.mEventType = p.readInt();
        eventOut.mTimeStamp = p.readLong();
        eventOut.mConfiguration = null;
        eventOut.mShortcutId = null;
        eventOut.mAction = null;
        eventOut.mContentType = null;
        eventOut.mContentAnnotations = null;
        eventOut.mNotificationChannelId = null;
        switch (eventOut.mEventType) {
            case 5:
                eventOut.mConfiguration = (Configuration) Configuration.CREATOR.createFromParcel(p);
                return;
            case 8:
                eventOut.mShortcutId = p.readString();
                return;
            case 9:
                eventOut.mAction = p.readString();
                eventOut.mContentType = p.readString();
                eventOut.mContentAnnotations = p.createStringArray();
                return;
            case 11:
                eventOut.mBucketAndReason = p.readInt();
                return;
            case 12:
                eventOut.mNotificationChannelId = p.readString();
                return;
            default:
                return;
        }
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        Parcel data = Parcel.obtain();
        Parcel th;
        try {
            data.writeInt(this.mEventCount);
            data.writeInt(this.mIndex);
            if (this.mEventCount > 0) {
                data.writeStringArray(this.mStringPool);
                if (this.mEventsToWrite != null) {
                    int i;
                    th = Parcel.obtain();
                    th.setDataPosition(0);
                    for (i = 0; i < this.mEventCount; i++) {
                        writeEventToParcel((Event) this.mEventsToWrite.get(i), th, flags);
                    }
                    i = th.dataPosition();
                    data.writeInt(i);
                    data.writeInt(0);
                    data.appendFrom(th, 0, i);
                } else if (this.mParcel != null) {
                    data.writeInt(this.mParcel.dataSize());
                    data.writeInt(this.mParcel.dataPosition());
                    data.appendFrom(this.mParcel, 0, this.mParcel.dataSize());
                } else {
                    throw new IllegalStateException("Either mParcel or mEventsToWrite must not be null");
                }
            }
        } catch (Throwable th2) {
            th = th2;
            if (data != null) {
            }
        } finally {
            
/*
Method generation error in method: android.app.usage.UsageEvents.writeToParcel(android.os.Parcel, int):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x0049: INVOKE  (wrap: android.os.Parcel
  ?: MERGE  (r1_14 'p' android.os.Parcel) = (r1_13 'th' android.os.Parcel), (r0_0 'data' android.os.Parcel)) android.os.Parcel.recycle():void type: VIRTUAL in method: android.app.usage.UsageEvents.writeToParcel(android.os.Parcel, int):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:102)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:52)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:300)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:65)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r1_14 'p' android.os.Parcel) = (r1_13 'th' android.os.Parcel), (r0_0 'data' android.os.Parcel) in method: android.app.usage.UsageEvents.writeToParcel(android.os.Parcel, int):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:101)
	at jadx.core.codegen.InsnGen.addArgDot(InsnGen.java:84)
	at jadx.core.codegen.InsnGen.makeInvoke(InsnGen.java:634)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:340)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 21 more
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
	... 26 more

*/
}
