package android.bluetooth;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public abstract class BluetoothHealthCallback {
    private static final String TAG = "BluetoothHealthCallback";

    public void onHealthAppConfigurationStatusChange(BluetoothHealthAppConfiguration config, int status) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHealthAppConfigurationStatusChange: ");
        stringBuilder.append(config);
        stringBuilder.append("Status: ");
        stringBuilder.append(status);
        Log.d(str, stringBuilder.toString());
    }

    public void onHealthChannelStateChange(BluetoothHealthAppConfiguration config, BluetoothDevice device, int prevState, int newState, ParcelFileDescriptor fd, int channelId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onHealthChannelStateChange: ");
        stringBuilder.append(config);
        stringBuilder.append("Device: ");
        stringBuilder.append(device);
        stringBuilder.append("prevState:");
        stringBuilder.append(prevState);
        stringBuilder.append("newState:");
        stringBuilder.append(newState);
        stringBuilder.append("ParcelFd:");
        stringBuilder.append(fd);
        stringBuilder.append("ChannelId:");
        stringBuilder.append(channelId);
        Log.d(str, stringBuilder.toString());
    }
}
