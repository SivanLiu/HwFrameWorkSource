package com.android.commands.hid;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class Event {
    public static final String COMMAND_DELAY = "delay";
    public static final String COMMAND_REGISTER = "register";
    public static final String COMMAND_REPORT = "report";
    private static final String TAG = "HidEvent";
    private String mCommand;
    private byte[] mDescriptor;
    private int mDuration;
    private int mId;
    private String mName;
    private int mPid;
    private byte[] mReport;
    private int mVid;

    private static class Builder {
        private Event mEvent = new Event();

        public void setId(int id) {
            this.mEvent.mId = id;
        }

        private void setCommand(String command) {
            this.mEvent.mCommand = command;
        }

        public void setName(String name) {
            this.mEvent.mName = name;
        }

        public void setDescriptor(byte[] descriptor) {
            this.mEvent.mDescriptor = descriptor;
        }

        public void setReport(byte[] report) {
            this.mEvent.mReport = report;
        }

        public void setVid(int vid) {
            this.mEvent.mVid = vid;
        }

        public void setPid(int pid) {
            this.mEvent.mPid = pid;
        }

        public void setDuration(int duration) {
            this.mEvent.mDuration = duration;
        }

        public Event build() {
            if (this.mEvent.mId == -1) {
                throw new IllegalStateException("No event id");
            } else if (this.mEvent.mCommand != null) {
                if (Event.COMMAND_REGISTER.equals(this.mEvent.mCommand)) {
                    if (this.mEvent.mDescriptor == null) {
                        throw new IllegalStateException("Device registration is missing descriptor");
                    }
                } else if (Event.COMMAND_DELAY.equals(this.mEvent.mCommand)) {
                    if (this.mEvent.mDuration <= 0) {
                        throw new IllegalStateException("Delay has missing or invalid duration");
                    }
                } else if (Event.COMMAND_REPORT.equals(this.mEvent.mCommand) && this.mEvent.mReport == null) {
                    throw new IllegalStateException("Report command is missing report data");
                }
                return this.mEvent;
            } else {
                throw new IllegalStateException("Event does not contain a command");
            }
        }
    }

    public static class Reader {
        private JsonReader mReader;

        public Reader(InputStreamReader in) {
            this.mReader = new JsonReader(in);
            this.mReader.setLenient(true);
        }

        public Event getNextEvent() throws IOException {
            Event e = null;
            while (e == null && this.mReader.peek() != JsonToken.END_DOCUMENT) {
                Builder eb = new Builder();
                try {
                    this.mReader.beginObject();
                    while (this.mReader.hasNext()) {
                        String name = this.mReader.nextName();
                        Object obj = -1;
                        switch (name.hashCode()) {
                            case -1992012396:
                                if (name.equals("duration")) {
                                    obj = 7;
                                    break;
                                }
                                break;
                            case -934521548:
                                if (name.equals(Event.COMMAND_REPORT)) {
                                    obj = 6;
                                    break;
                                }
                                break;
                            case -748366993:
                                if (name.equals("descriptor")) {
                                    obj = 2;
                                    break;
                                }
                                break;
                            case 3355:
                                if (name.equals("id")) {
                                    obj = null;
                                    break;
                                }
                                break;
                            case 110987:
                                if (name.equals("pid")) {
                                    obj = 5;
                                    break;
                                }
                                break;
                            case 116753:
                                if (name.equals("vid")) {
                                    obj = 4;
                                    break;
                                }
                                break;
                            case 3373707:
                                if (name.equals("name")) {
                                    obj = 3;
                                    break;
                                }
                                break;
                            case 950394699:
                                if (name.equals("command")) {
                                    obj = 1;
                                    break;
                                }
                                break;
                        }
                        switch (obj) {
                            case null:
                                eb.setId(readInt());
                                break;
                            case 1:
                                eb.setCommand(this.mReader.nextString());
                                break;
                            case 2:
                                eb.setDescriptor(readData());
                                break;
                            case 3:
                                eb.setName(this.mReader.nextString());
                                break;
                            case 4:
                                eb.setVid(readInt());
                                break;
                            case 5:
                                eb.setPid(readInt());
                                break;
                            case 6:
                                eb.setReport(readData());
                                break;
                            case 7:
                                eb.setDuration(readInt());
                                break;
                            default:
                                this.mReader.skipValue();
                                break;
                        }
                    }
                    this.mReader.endObject();
                    e = eb.build();
                } catch (IllegalStateException ex) {
                    Event.error("Error reading in object, ignoring.", ex);
                    consumeRemainingElements();
                    this.mReader.endObject();
                }
            }
            return e;
        }

        /* JADX WARNING: Removed duplicated region for block: B:16:0x0050 A:{ExcHandler: java.lang.IllegalStateException (r1_9 'e' java.lang.RuntimeException), Splitter: B:1:0x0005} */
        /* JADX WARNING: Missing block: B:16:0x0050, code:
            r1 = move-exception;
     */
        /* JADX WARNING: Missing block: B:17:0x0051, code:
            consumeRemainingElements();
            r6.mReader.endArray();
     */
        /* JADX WARNING: Missing block: B:18:0x0060, code:
            throw new java.lang.IllegalStateException("Encountered malformed data.", r1);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private byte[] readData() throws IOException {
            ArrayList<Integer> data = new ArrayList();
            try {
                this.mReader.beginArray();
                while (this.mReader.hasNext()) {
                    data.add(Integer.decode(this.mReader.nextString()));
                }
                this.mReader.endArray();
                byte[] rawData = new byte[data.size()];
                int i = 0;
                while (i < data.size()) {
                    int d = ((Integer) data.get(i)).intValue();
                    if ((d & 255) == d) {
                        rawData[i] = (byte) d;
                        i++;
                    } else {
                        throw new IllegalStateException("Invalid data, all values must be byte-sized");
                    }
                }
                return rawData;
            } catch (RuntimeException e) {
            }
        }

        private int readInt() throws IOException {
            return Integer.decode(this.mReader.nextString()).intValue();
        }

        private void consumeRemainingElements() throws IOException {
            while (this.mReader.hasNext()) {
                this.mReader.skipValue();
            }
        }
    }

    public int getId() {
        return this.mId;
    }

    public String getCommand() {
        return this.mCommand;
    }

    public String getName() {
        return this.mName;
    }

    public byte[] getDescriptor() {
        return this.mDescriptor;
    }

    public int getVendorId() {
        return this.mVid;
    }

    public int getProductId() {
        return this.mPid;
    }

    public byte[] getReport() {
        return this.mReport;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Event{id=");
        stringBuilder.append(this.mId);
        stringBuilder.append(", command=");
        stringBuilder.append(String.valueOf(this.mCommand));
        stringBuilder.append(", name=");
        stringBuilder.append(String.valueOf(this.mName));
        stringBuilder.append(", descriptor=");
        stringBuilder.append(Arrays.toString(this.mDescriptor));
        stringBuilder.append(", vid=");
        stringBuilder.append(this.mVid);
        stringBuilder.append(", pid=");
        stringBuilder.append(this.mPid);
        stringBuilder.append(", report=");
        stringBuilder.append(Arrays.toString(this.mReport));
        stringBuilder.append(", duration=");
        stringBuilder.append(this.mDuration);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private static void error(String msg) {
        error(msg, null);
    }

    private static void error(String msg, Exception e) {
        System.out.println(msg);
        Log.e(TAG, msg);
        if (e != null) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }
}
