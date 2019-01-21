package android.media;

import android.annotation.SystemApi;
import android.media.AudioAttributes.Builder;
import android.media.IPlayer.Stub;
import android.media.PlayerBase.PlayerIdCard;
import android.os.Binder;
import android.os.IBinder.DeathRecipient;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.util.Log;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

public final class AudioPlaybackConfiguration implements Parcelable {
    public static final Creator<AudioPlaybackConfiguration> CREATOR = new Creator<AudioPlaybackConfiguration>() {
        public AudioPlaybackConfiguration createFromParcel(Parcel p) {
            return new AudioPlaybackConfiguration(p, null);
        }

        public AudioPlaybackConfiguration[] newArray(int size) {
            return new AudioPlaybackConfiguration[size];
        }
    };
    private static final boolean DEBUG = false;
    public static final int PLAYER_PIID_INVALID = -1;
    public static final int PLAYER_PIID_UNASSIGNED = 0;
    @SystemApi
    public static final int PLAYER_STATE_IDLE = 1;
    @SystemApi
    public static final int PLAYER_STATE_PAUSED = 3;
    @SystemApi
    public static final int PLAYER_STATE_RELEASED = 0;
    @SystemApi
    public static final int PLAYER_STATE_STARTED = 2;
    @SystemApi
    public static final int PLAYER_STATE_STOPPED = 4;
    @SystemApi
    public static final int PLAYER_STATE_UNKNOWN = -1;
    public static final int PLAYER_TYPE_AAUDIO = 13;
    public static final int PLAYER_TYPE_EXTERNAL_PROXY = 15;
    public static final int PLAYER_TYPE_HW_SOURCE = 14;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_AUDIOTRACK = 1;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_MEDIAPLAYER = 2;
    @SystemApi
    public static final int PLAYER_TYPE_JAM_SOUNDPOOL = 3;
    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_BUFFERQUEUE = 11;
    @SystemApi
    public static final int PLAYER_TYPE_SLES_AUDIOPLAYER_URI_FD = 12;
    @SystemApi
    public static final int PLAYER_TYPE_UNKNOWN = -1;
    public static final int PLAYER_UPID_INVALID = -1;
    private static final String TAG = new String("AudioPlaybackConfiguration");
    public static PlayerDeathMonitor sPlayerDeathMonitor;
    private int mClientPid;
    private int mClientUid;
    private IPlayerShell mIPlayerShell;
    private String mPkgName;
    private AudioAttributes mPlayerAttr;
    private final int mPlayerIId;
    private int mPlayerState;
    private int mPlayerType;

    public interface PlayerDeathMonitor {
        void playerDeath(int i);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerType {
    }

    static final class IPlayerShell implements DeathRecipient {
        private volatile IPlayer mIPlayer;
        final AudioPlaybackConfiguration mMonitor;

        IPlayerShell(AudioPlaybackConfiguration monitor, IPlayer iplayer) {
            this.mMonitor = monitor;
            this.mIPlayer = iplayer;
        }

        /* JADX WARNING: Missing block: B:15:0x0040, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        synchronized void monitorDeath() {
            if (this.mIPlayer != null) {
                try {
                    this.mIPlayer.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    if (this.mMonitor != null) {
                        String access$100 = AudioPlaybackConfiguration.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not link to client death for piid=");
                        stringBuilder.append(this.mMonitor.mPlayerIId);
                        Log.w(access$100, stringBuilder.toString(), e);
                    } else {
                        Log.w(AudioPlaybackConfiguration.TAG, "Could not link to client death", e);
                    }
                }
            }
        }

        IPlayer getIPlayer() {
            return this.mIPlayer;
        }

        public void binderDied() {
            if (this.mMonitor != null) {
                this.mMonitor.playerDied();
            }
        }

        synchronized void release() {
            if (this.mIPlayer != null) {
                this.mIPlayer.asBinder().unlinkToDeath(this, 0);
                this.mIPlayer = null;
                Binder.flushPendingCommands();
            }
        }
    }

    /* synthetic */ AudioPlaybackConfiguration(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    private AudioPlaybackConfiguration(int piid) {
        this.mPlayerIId = piid;
        this.mIPlayerShell = null;
    }

    public AudioPlaybackConfiguration(PlayerIdCard pic, int piid, int uid, int pid) {
        this.mPlayerIId = piid;
        this.mPlayerType = pic.mPlayerType;
        this.mClientUid = uid;
        this.mClientPid = pid;
        this.mPlayerState = 1;
        this.mPlayerAttr = pic.mAttributes;
        if (sPlayerDeathMonitor == null || pic.mIPlayer == null) {
            this.mIPlayerShell = null;
        } else {
            this.mIPlayerShell = new IPlayerShell(this, pic.mIPlayer);
        }
        this.mPkgName = pic.getPkgName();
    }

    public String getPkgName() {
        return this.mPkgName;
    }

    public void init() {
        synchronized (this) {
            if (this.mIPlayerShell != null) {
                this.mIPlayerShell.monitorDeath();
            }
        }
    }

    public static AudioPlaybackConfiguration anonymizedCopy(AudioPlaybackConfiguration in) {
        AudioPlaybackConfiguration anonymCopy = new AudioPlaybackConfiguration(in.mPlayerIId);
        anonymCopy.mPlayerState = in.mPlayerState;
        anonymCopy.mPlayerAttr = new Builder().setUsage(in.mPlayerAttr.getUsage()).setContentType(in.mPlayerAttr.getContentType()).setFlags(in.mPlayerAttr.getFlags()).build();
        anonymCopy.mPlayerType = -1;
        anonymCopy.mClientUid = -1;
        anonymCopy.mClientPid = -1;
        anonymCopy.mIPlayerShell = null;
        return anonymCopy;
    }

    public AudioAttributes getAudioAttributes() {
        return this.mPlayerAttr;
    }

    @SystemApi
    public int getClientUid() {
        return this.mClientUid;
    }

    @SystemApi
    public int getClientPid() {
        return this.mClientPid;
    }

    @SystemApi
    public int getPlayerType() {
        switch (this.mPlayerType) {
            case 13:
            case 14:
            case 15:
                return -1;
            default:
                return this.mPlayerType;
        }
    }

    @SystemApi
    public int getPlayerState() {
        return this.mPlayerState;
    }

    @SystemApi
    public int getPlayerInterfaceId() {
        return this.mPlayerIId;
    }

    @SystemApi
    public PlayerProxy getPlayerProxy() {
        IPlayerShell ips;
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        return ips == null ? null : new PlayerProxy(this);
    }

    IPlayer getIPlayer() {
        IPlayerShell ips;
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        return ips == null ? null : ips.getIPlayer();
    }

    public boolean handleAudioAttributesEvent(AudioAttributes attr) {
        boolean changed = attr.equals(this.mPlayerAttr) ^ 1;
        this.mPlayerAttr = attr;
        return changed;
    }

    public boolean handleStateEvent(int event) {
        boolean changed;
        synchronized (this) {
            changed = this.mPlayerState != event;
            this.mPlayerState = event;
            if (changed && event == 0 && this.mIPlayerShell != null) {
                this.mIPlayerShell.release();
                this.mIPlayerShell = null;
            }
        }
        return changed;
    }

    private void playerDied() {
        if (sPlayerDeathMonitor != null) {
            sPlayerDeathMonitor.playerDeath(this.mPlayerIId);
        }
    }

    public boolean isActive() {
        if (this.mPlayerState != 2) {
            return false;
        }
        return true;
    }

    public void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ");
        stringBuilder.append(toLogFriendlyString(this));
        pw.println(stringBuilder.toString());
    }

    public static String toLogFriendlyString(AudioPlaybackConfiguration apc) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ID:");
        stringBuilder.append(apc.mPlayerIId);
        stringBuilder.append(" -- type:");
        stringBuilder.append(toLogFriendlyPlayerType(apc.mPlayerType));
        stringBuilder.append(" -- u/pid:");
        stringBuilder.append(apc.mClientUid);
        stringBuilder.append("/");
        stringBuilder.append(apc.mClientPid);
        stringBuilder.append(" -- state:");
        stringBuilder.append(toLogFriendlyPlayerState(apc.mPlayerState));
        stringBuilder.append(" -- attr:");
        stringBuilder.append(apc.mPlayerAttr);
        return new String(stringBuilder.toString());
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(this.mPlayerIId), Integer.valueOf(this.mPlayerType), Integer.valueOf(this.mClientUid), Integer.valueOf(this.mClientPid)});
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        IPlayerShell ips;
        dest.writeInt(this.mPlayerIId);
        dest.writeInt(this.mPlayerType);
        dest.writeInt(this.mClientUid);
        dest.writeInt(this.mClientPid);
        dest.writeInt(this.mPlayerState);
        this.mPlayerAttr.writeToParcel(dest, 0);
        synchronized (this) {
            ips = this.mIPlayerShell;
        }
        dest.writeStrongInterface(ips == null ? null : ips.getIPlayer());
    }

    private AudioPlaybackConfiguration(Parcel in) {
        this.mPlayerIId = in.readInt();
        this.mPlayerType = in.readInt();
        this.mClientUid = in.readInt();
        this.mClientPid = in.readInt();
        this.mPlayerState = in.readInt();
        this.mPlayerAttr = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(in);
        IPlayer p = Stub.asInterface(in.readStrongBinder());
        IPlayerShell iPlayerShell = null;
        if (p != null) {
            iPlayerShell = new IPlayerShell(null, p);
        }
        this.mIPlayerShell = iPlayerShell;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof AudioPlaybackConfiguration)) {
            return false;
        }
        AudioPlaybackConfiguration that = (AudioPlaybackConfiguration) o;
        if (!(this.mPlayerIId == that.mPlayerIId && this.mPlayerType == that.mPlayerType && this.mClientUid == that.mClientUid && this.mClientPid == that.mClientPid)) {
            z = false;
        }
        return z;
    }

    public static String toLogFriendlyPlayerType(int type) {
        if (type == -1) {
            return "unknown";
        }
        switch (type) {
            case 1:
                return "android.media.AudioTrack";
            case 2:
                return "android.media.MediaPlayer";
            case 3:
                return "android.media.SoundPool";
            default:
                switch (type) {
                    case 11:
                        return "OpenSL ES AudioPlayer (Buffer Queue)";
                    case 12:
                        return "OpenSL ES AudioPlayer (URI/FD)";
                    case 13:
                        return "AAudio";
                    case 14:
                        return "hardware source";
                    case 15:
                        return "external proxy";
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unknown player type ");
                        stringBuilder.append(type);
                        stringBuilder.append(" - FIXME");
                        return stringBuilder.toString();
                }
        }
    }

    public static String toLogFriendlyPlayerState(int state) {
        switch (state) {
            case -1:
                return "unknown";
            case 0:
                return "released";
            case 1:
                return "idle";
            case 2:
                return "started";
            case 3:
                return "paused";
            case 4:
                return "stopped";
            default:
                return "unknown player state - FIXME";
        }
    }
}
