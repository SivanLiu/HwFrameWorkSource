package android_maps_conflict_avoidance.com.google.googlenav;

import android_maps_conflict_avoidance.com.google.common.Log;

public class GmmLogger {
    public static void logTimingTileLatency(String tileType, int timeToWrite, int timeToFirstByteMsec, int timeToLastByteMsec, int numTiles, int numBytes) {
        String eventData = new String[6];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("t=");
        stringBuilder.append(tileType);
        eventData[0] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("tw=");
        stringBuilder.append(timeToWrite);
        eventData[1] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("tf=");
        stringBuilder.append(timeToFirstByteMsec);
        eventData[2] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("tl=");
        stringBuilder.append(timeToLastByteMsec);
        eventData[3] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("n=");
        stringBuilder.append(numTiles);
        eventData[4] = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append("b=");
        stringBuilder.append(numBytes);
        eventData[5] = stringBuilder.toString();
        Log.addEvent((short) 22, "TL", Log.createEventTuple(eventData));
    }
}
