package android.media;

import android.media.VolumeShaper.Configuration;
import android.media.VolumeShaper.Operation;

public class PlayerProxy {
    private static final boolean DEBUG = false;
    private static final String TAG = "PlayerProxy";
    private final AudioPlaybackConfiguration mConf;

    PlayerProxy(AudioPlaybackConfiguration apc) {
        if (apc == null) {
            throw new IllegalArgumentException("Illegal null AudioPlaybackConfiguration");
        }
        this.mConf = apc;
    }

    public void start() {
        try {
            this.mConf.getIPlayer().start();
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for start operation, player already released?", e);
        }
    }

    public void pause() {
        try {
            this.mConf.getIPlayer().pause();
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for pause operation, player already released?", e);
        }
    }

    public void stop() {
        try {
            this.mConf.getIPlayer().stop();
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for stop operation, player already released?", e);
        }
    }

    public void setVolume(float vol) {
        try {
            this.mConf.getIPlayer().setVolume(vol);
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for setVolume operation, player already released?", e);
        }
    }

    public void setPan(float pan) {
        try {
            this.mConf.getIPlayer().setPan(pan);
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for setPan operation, player already released?", e);
        }
    }

    public void setStartDelayMs(int delayMs) {
        try {
            this.mConf.getIPlayer().setStartDelayMs(delayMs);
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for setStartDelayMs operation, player already released?", e);
        }
    }

    public void applyVolumeShaper(Configuration configuration, Operation operation) {
        try {
            this.mConf.getIPlayer().applyVolumeShaper(configuration, operation);
        } catch (Exception e) {
            throw new IllegalStateException("No player to proxy for applyVolumeShaper operation, player already released?", e);
        }
    }
}
