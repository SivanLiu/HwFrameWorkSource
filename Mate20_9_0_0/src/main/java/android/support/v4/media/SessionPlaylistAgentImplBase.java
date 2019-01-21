package android.support.v4.media;

import android.annotation.TargetApi;
import android.support.annotation.GuardedBy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.media.BaseMediaPlayer.PlayerEventCallback;
import android.support.v4.media.MediaSession2.OnDataSourceMissingHelper;
import android.support.v4.util.ArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TargetApi(19)
class SessionPlaylistAgentImplBase extends MediaPlaylistAgent {
    @VisibleForTesting
    static final int END_OF_PLAYLIST = -1;
    @VisibleForTesting
    static final int NO_VALID_ITEMS = -2;
    @GuardedBy("mLock")
    private PlayItem mCurrent;
    @GuardedBy("mLock")
    private OnDataSourceMissingHelper mDsmHelper;
    private final PlayItem mEopPlayItem = new PlayItem(-1, null);
    @GuardedBy("mLock")
    private Map<MediaItem2, DataSourceDesc> mItemDsdMap = new ArrayMap();
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaMetadata2 mMetadata;
    @GuardedBy("mLock")
    private BaseMediaPlayer mPlayer;
    private final MyPlayerEventCallback mPlayerCallback;
    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mPlaylist = new ArrayList();
    @GuardedBy("mLock")
    private int mRepeatMode;
    private final MediaSession2ImplBase mSession;
    @GuardedBy("mLock")
    private int mShuffleMode;
    @GuardedBy("mLock")
    private ArrayList<MediaItem2> mShuffledList = new ArrayList();

    private class PlayItem {
        public DataSourceDesc dsd;
        public MediaItem2 mediaItem;
        public int shuffledIdx;

        PlayItem(SessionPlaylistAgentImplBase sessionPlaylistAgentImplBase, int shuffledIdx) {
            this(shuffledIdx, null);
        }

        PlayItem(int shuffledIdx, DataSourceDesc dsd) {
            this.shuffledIdx = shuffledIdx;
            if (shuffledIdx >= 0) {
                this.mediaItem = (MediaItem2) SessionPlaylistAgentImplBase.this.mShuffledList.get(shuffledIdx);
                if (dsd == null) {
                    synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                        this.dsd = SessionPlaylistAgentImplBase.this.retrieveDataSourceDescLocked(this.mediaItem);
                    }
                    return;
                }
                this.dsd = dsd;
            }
        }

        boolean isValid() {
            if (this == SessionPlaylistAgentImplBase.this.mEopPlayItem) {
                return true;
            }
            if (this.mediaItem == null || this.dsd == null) {
                return false;
            }
            if (this.mediaItem.getDataSourceDesc() != null && !this.mediaItem.getDataSourceDesc().equals(this.dsd)) {
                return false;
            }
            synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                if (this.shuffledIdx >= SessionPlaylistAgentImplBase.this.mShuffledList.size()) {
                    return false;
                } else if (this.mediaItem != SessionPlaylistAgentImplBase.this.mShuffledList.get(this.shuffledIdx)) {
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    private class MyPlayerEventCallback extends PlayerEventCallback {
        private MyPlayerEventCallback() {
        }

        /* JADX WARNING: Missing block: B:12:0x0035, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onCurrentDataSourceChanged(@NonNull BaseMediaPlayer mpb, @Nullable DataSourceDesc dsd) {
            synchronized (SessionPlaylistAgentImplBase.this.mLock) {
                if (SessionPlaylistAgentImplBase.this.mPlayer != mpb) {
                } else if (dsd == null && SessionPlaylistAgentImplBase.this.mCurrent != null) {
                    SessionPlaylistAgentImplBase.this.mCurrent = SessionPlaylistAgentImplBase.this.getNextValidPlayItemLocked(SessionPlaylistAgentImplBase.this.mCurrent.shuffledIdx, 1);
                    SessionPlaylistAgentImplBase.this.updateCurrentIfNeededLocked();
                }
            }
        }
    }

    SessionPlaylistAgentImplBase(@NonNull MediaSession2ImplBase session, @NonNull BaseMediaPlayer player) {
        if (session == null) {
            throw new IllegalArgumentException("sessionImpl shouldn't be null");
        } else if (player != null) {
            this.mSession = session;
            this.mPlayer = player;
            this.mPlayerCallback = new MyPlayerEventCallback();
            this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
        } else {
            throw new IllegalArgumentException("player shouldn't be null");
        }
    }

    public void setPlayer(@NonNull BaseMediaPlayer player) {
        if (player != null) {
            synchronized (this.mLock) {
                if (player == this.mPlayer) {
                    return;
                }
                this.mPlayer.unregisterPlayerEventCallback(this.mPlayerCallback);
                this.mPlayer = player;
                this.mPlayer.registerPlayerEventCallback(this.mSession.getCallbackExecutor(), this.mPlayerCallback);
                updatePlayerDataSourceLocked();
                return;
            }
        }
        throw new IllegalArgumentException("player shouldn't be null");
    }

    public void setOnDataSourceMissingHelper(OnDataSourceMissingHelper helper) {
        synchronized (this.mLock) {
            this.mDsmHelper = helper;
        }
    }

    public void clearOnDataSourceMissingHelper() {
        synchronized (this.mLock) {
            this.mDsmHelper = null;
        }
    }

    @Nullable
    public List<MediaItem2> getPlaylist() {
        List unmodifiableList;
        synchronized (this.mLock) {
            unmodifiableList = Collections.unmodifiableList(this.mPlaylist);
        }
        return unmodifiableList;
    }

    public void setPlaylist(@NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
        if (list != null) {
            synchronized (this.mLock) {
                this.mItemDsdMap.clear();
                this.mPlaylist.clear();
                this.mPlaylist.addAll(list);
                applyShuffleModeLocked();
                this.mMetadata = metadata;
                this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                updatePlayerDataSourceLocked();
            }
            notifyPlaylistChanged();
            return;
        }
        throw new IllegalArgumentException("list shouldn't be null");
    }

    @Nullable
    public MediaMetadata2 getPlaylistMetadata() {
        MediaMetadata2 mediaMetadata2;
        synchronized (this.mLock) {
            mediaMetadata2 = this.mMetadata;
        }
        return mediaMetadata2;
    }

    public void updatePlaylistMetadata(@Nullable MediaMetadata2 metadata) {
        synchronized (this.mLock) {
            if (metadata == this.mMetadata) {
                return;
            }
            this.mMetadata = metadata;
            notifyPlaylistMetadataChanged();
        }
    }

    public MediaItem2 getCurrentMediaItem() {
        MediaItem2 mediaItem2;
        synchronized (this.mLock) {
            mediaItem2 = this.mCurrent == null ? null : this.mCurrent.mediaItem;
        }
        return mediaItem2;
    }

    public void addPlaylistItem(int index, @NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                index = clamp(index, this.mPlaylist.size());
                this.mPlaylist.add(index, item);
                if (this.mShuffleMode == 0) {
                    this.mShuffledList.add(index, item);
                } else {
                    this.mShuffledList.add((int) (Math.random() * ((double) (this.mShuffledList.size() + 1))), item);
                }
                if (hasValidItem()) {
                    updateCurrentIfNeededLocked();
                } else {
                    this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                    updatePlayerDataSourceLocked();
                }
            }
            notifyPlaylistChanged();
            return;
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    public void removePlaylistItem(@NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                if (this.mPlaylist.remove(item)) {
                    this.mShuffledList.remove(item);
                    this.mItemDsdMap.remove(item);
                    updateCurrentIfNeededLocked();
                    notifyPlaylistChanged();
                    return;
                }
                return;
            }
        }
        throw new IllegalArgumentException("item shouldn't be null");
    }

    /* JADX WARNING: Missing block: B:13:0x0052, code skipped:
            notifyPlaylistChanged();
     */
    /* JADX WARNING: Missing block: B:14:0x0055, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void replacePlaylistItem(int index, @NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                if (this.mPlaylist.size() <= 0) {
                    return;
                }
                index = clamp(index, this.mPlaylist.size() - 1);
                int shuffledIdx = this.mShuffledList.indexOf(this.mPlaylist.get(index));
                this.mItemDsdMap.remove(this.mShuffledList.get(shuffledIdx));
                this.mShuffledList.set(shuffledIdx, item);
                this.mPlaylist.set(index, item);
                if (hasValidItem()) {
                    updateCurrentIfNeededLocked();
                } else {
                    this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                    updatePlayerDataSourceLocked();
                }
            }
        } else {
            throw new IllegalArgumentException("item shouldn't be null");
        }
    }

    /* JADX WARNING: Missing block: B:17:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipToPlaylistItem(@NonNull MediaItem2 item) {
        if (item != null) {
            synchronized (this.mLock) {
                if (hasValidItem()) {
                    if (!item.equals(this.mCurrent.mediaItem)) {
                        int shuffledIdx = this.mShuffledList.indexOf(item);
                        if (shuffledIdx < 0) {
                            return;
                        }
                        this.mCurrent = new PlayItem(this, shuffledIdx);
                        updateCurrentIfNeededLocked();
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("item shouldn't be null");
        }
    }

    public void skipToPreviousItem() {
        synchronized (this.mLock) {
            if (hasValidItem()) {
                PlayItem prev = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, -1);
                if (prev != this.mEopPlayItem) {
                    this.mCurrent = prev;
                }
                updateCurrentIfNeededLocked();
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:15:0x0025, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void skipToNextItem() {
        synchronized (this.mLock) {
            if (hasValidItem()) {
                if (this.mCurrent != this.mEopPlayItem) {
                    PlayItem next = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, 1);
                    if (next != this.mEopPlayItem) {
                        this.mCurrent = next;
                    }
                    updateCurrentIfNeededLocked();
                }
            }
        }
    }

    public int getRepeatMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mRepeatMode;
        }
        return i;
    }

    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            notifyRepeatModeChanged();
     */
    /* JADX WARNING: Missing block: B:24:0x0041, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setRepeatMode(int repeatMode) {
        if (repeatMode >= 0 && repeatMode <= 3) {
            synchronized (this.mLock) {
                if (this.mRepeatMode == repeatMode) {
                    return;
                }
                this.mRepeatMode = repeatMode;
                switch (repeatMode) {
                    case 0:
                        break;
                    case 1:
                        if (!(this.mCurrent == null || this.mCurrent == this.mEopPlayItem)) {
                            this.mPlayer.loopCurrent(true);
                            break;
                        }
                    case 2:
                    case 3:
                        if (this.mCurrent == this.mEopPlayItem) {
                            this.mCurrent = getNextValidPlayItemLocked(-1, 1);
                            updatePlayerDataSourceLocked();
                            break;
                        }
                        break;
                    default:
                        break;
                }
                this.mPlayer.loopCurrent(false);
            }
        }
    }

    public int getShuffleMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mShuffleMode;
        }
        return i;
    }

    public void setShuffleMode(int shuffleMode) {
        if (shuffleMode >= 0 && shuffleMode <= 2) {
            synchronized (this.mLock) {
                if (this.mShuffleMode == shuffleMode) {
                    return;
                }
                this.mShuffleMode = shuffleMode;
                applyShuffleModeLocked();
                updateCurrentIfNeededLocked();
                notifyShuffleModeChanged();
            }
        }
    }

    public MediaItem2 getMediaItem(DataSourceDesc dsd) {
        return null;
    }

    @VisibleForTesting
    int getCurShuffledIndex() {
        int i;
        synchronized (this.mLock) {
            i = hasValidItem() ? this.mCurrent.shuffledIdx : -2;
        }
        return i;
    }

    private boolean hasValidItem() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mCurrent != null;
        }
        return z;
    }

    private DataSourceDesc retrieveDataSourceDescLocked(MediaItem2 item) {
        DataSourceDesc dsd = item.getDataSourceDesc();
        if (dsd != null) {
            this.mItemDsdMap.put(item, dsd);
            return dsd;
        }
        dsd = (DataSourceDesc) this.mItemDsdMap.get(item);
        if (dsd != null) {
            return dsd;
        }
        OnDataSourceMissingHelper helper = this.mDsmHelper;
        if (helper != null) {
            dsd = helper.onDataSourceMissing(this.mSession.getInstance(), item);
            if (dsd != null) {
                this.mItemDsdMap.put(item, dsd);
            }
        }
        return dsd;
    }

    private PlayItem getNextValidPlayItemLocked(int curShuffledIdx, int direction) {
        int size = this.mPlaylist.size();
        int i = -1;
        if (curShuffledIdx == -1) {
            if (direction <= 0) {
                i = size;
            }
            curShuffledIdx = i;
        }
        int curShuffledIdx2 = curShuffledIdx;
        curShuffledIdx = 0;
        while (true) {
            PlayItem playItem = null;
            if (curShuffledIdx >= size) {
                return null;
            }
            curShuffledIdx2 += direction;
            if (curShuffledIdx2 < 0 || curShuffledIdx2 >= this.mPlaylist.size()) {
                if (this.mRepeatMode == 0) {
                    if (curShuffledIdx != size - 1) {
                        playItem = this.mEopPlayItem;
                    }
                    return playItem;
                }
                curShuffledIdx2 = curShuffledIdx2 < 0 ? this.mPlaylist.size() - 1 : 0;
            }
            DataSourceDesc dsd = retrieveDataSourceDescLocked((MediaItem2) this.mShuffledList.get(curShuffledIdx2));
            if (dsd != null) {
                return new PlayItem(curShuffledIdx2, dsd);
            }
            curShuffledIdx++;
        }
    }

    private void updateCurrentIfNeededLocked() {
        if (hasValidItem() && !this.mCurrent.isValid()) {
            int shuffledIdx = this.mShuffledList.indexOf(this.mCurrent.mediaItem);
            if (shuffledIdx >= 0) {
                this.mCurrent.shuffledIdx = shuffledIdx;
                return;
            }
            if (this.mCurrent.shuffledIdx >= this.mShuffledList.size()) {
                this.mCurrent = getNextValidPlayItemLocked(this.mShuffledList.size() - 1, 1);
            } else {
                this.mCurrent.mediaItem = (MediaItem2) this.mShuffledList.get(this.mCurrent.shuffledIdx);
                if (retrieveDataSourceDescLocked(this.mCurrent.mediaItem) == null) {
                    this.mCurrent = getNextValidPlayItemLocked(this.mCurrent.shuffledIdx, 1);
                }
            }
            updatePlayerDataSourceLocked();
        }
    }

    /* JADX WARNING: Missing block: B:11:0x002d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updatePlayerDataSourceLocked() {
        if (!(this.mCurrent == null || this.mCurrent == this.mEopPlayItem || this.mPlayer.getCurrentDataSource() == this.mCurrent.dsd)) {
            this.mPlayer.setDataSource(this.mCurrent.dsd);
            BaseMediaPlayer baseMediaPlayer = this.mPlayer;
            boolean z = true;
            if (this.mRepeatMode != 1) {
                z = false;
            }
            baseMediaPlayer.loopCurrent(z);
        }
    }

    private void applyShuffleModeLocked() {
        this.mShuffledList.clear();
        this.mShuffledList.addAll(this.mPlaylist);
        if (this.mShuffleMode == 1 || this.mShuffleMode == 2) {
            Collections.shuffle(this.mShuffledList);
        }
    }

    private static int clamp(int value, int size) {
        if (value < 0) {
            return 0;
        }
        return value > size ? size : value;
    }
}
