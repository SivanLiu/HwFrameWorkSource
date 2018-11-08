package com.android.internal.telephony.nano;

import com.android.internal.telephony.AbstractPhoneBase;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano;
import com.android.internal.telephony.protobuf.nano.ExtendableMessageNano;
import com.android.internal.telephony.protobuf.nano.InternalNano;
import com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException;
import com.android.internal.telephony.protobuf.nano.MessageNano;
import com.android.internal.telephony.protobuf.nano.WireFormatNano;
import java.io.IOException;

public interface TelephonyProto {

    public static final class ImsCapabilities extends ExtendableMessageNano<ImsCapabilities> {
        private static volatile ImsCapabilities[] _emptyArray;
        public boolean utOverLte;
        public boolean utOverWifi;
        public boolean videoOverLte;
        public boolean videoOverWifi;
        public boolean voiceOverLte;
        public boolean voiceOverWifi;

        public static ImsCapabilities[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsCapabilities[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsCapabilities() {
            clear();
        }

        public ImsCapabilities clear() {
            this.voiceOverLte = false;
            this.voiceOverWifi = false;
            this.videoOverLte = false;
            this.videoOverWifi = false;
            this.utOverLte = false;
            this.utOverWifi = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.voiceOverLte) {
                output.writeBool(1, this.voiceOverLte);
            }
            if (this.voiceOverWifi) {
                output.writeBool(2, this.voiceOverWifi);
            }
            if (this.videoOverLte) {
                output.writeBool(3, this.videoOverLte);
            }
            if (this.videoOverWifi) {
                output.writeBool(4, this.videoOverWifi);
            }
            if (this.utOverLte) {
                output.writeBool(5, this.utOverLte);
            }
            if (this.utOverWifi) {
                output.writeBool(6, this.utOverWifi);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.voiceOverLte) {
                size += CodedOutputByteBufferNano.computeBoolSize(1, this.voiceOverLte);
            }
            if (this.voiceOverWifi) {
                size += CodedOutputByteBufferNano.computeBoolSize(2, this.voiceOverWifi);
            }
            if (this.videoOverLte) {
                size += CodedOutputByteBufferNano.computeBoolSize(3, this.videoOverLte);
            }
            if (this.videoOverWifi) {
                size += CodedOutputByteBufferNano.computeBoolSize(4, this.videoOverWifi);
            }
            if (this.utOverLte) {
                size += CodedOutputByteBufferNano.computeBoolSize(5, this.utOverLte);
            }
            if (this.utOverWifi) {
                return size + CodedOutputByteBufferNano.computeBoolSize(6, this.utOverWifi);
            }
            return size;
        }

        public ImsCapabilities mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.voiceOverLte = input.readBool();
                        break;
                    case 16:
                        this.voiceOverWifi = input.readBool();
                        break;
                    case 24:
                        this.videoOverLte = input.readBool();
                        break;
                    case 32:
                        this.videoOverWifi = input.readBool();
                        break;
                    case 40:
                        this.utOverLte = input.readBool();
                        break;
                    case 48:
                        this.utOverWifi = input.readBool();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static ImsCapabilities parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (ImsCapabilities) MessageNano.mergeFrom(new ImsCapabilities(), data);
        }

        public static ImsCapabilities parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new ImsCapabilities().mergeFrom(input);
        }
    }

    public static final class ImsConnectionState extends ExtendableMessageNano<ImsConnectionState> {
        private static volatile ImsConnectionState[] _emptyArray;
        public ImsReasonInfo reasonInfo;
        public int state;

        public interface State {
            public static final int CONNECTED = 1;
            public static final int DISCONNECTED = 3;
            public static final int PROGRESSING = 2;
            public static final int RESUMED = 4;
            public static final int STATE_UNKNOWN = 0;
            public static final int SUSPENDED = 5;
        }

        public static ImsConnectionState[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsConnectionState[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsConnectionState() {
            clear();
        }

        public ImsConnectionState clear() {
            this.state = 0;
            this.reasonInfo = null;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.state != 0) {
                output.writeInt32(1, this.state);
            }
            if (this.reasonInfo != null) {
                output.writeMessage(2, this.reasonInfo);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.state != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.state);
            }
            if (this.reasonInfo != null) {
                return size + CodedOutputByteBufferNano.computeMessageSize(2, this.reasonInfo);
            }
            return size;
        }

        public ImsConnectionState mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        int initialPos = input.getPosition();
                        int value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                                this.state = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 18:
                        if (this.reasonInfo == null) {
                            this.reasonInfo = new ImsReasonInfo();
                        }
                        input.readMessage(this.reasonInfo);
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static ImsConnectionState parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (ImsConnectionState) MessageNano.mergeFrom(new ImsConnectionState(), data);
        }

        public static ImsConnectionState parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new ImsConnectionState().mergeFrom(input);
        }
    }

    public static final class ImsReasonInfo extends ExtendableMessageNano<ImsReasonInfo> {
        private static volatile ImsReasonInfo[] _emptyArray;
        public int extraCode;
        public String extraMessage;
        public int reasonCode;

        public static ImsReasonInfo[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new ImsReasonInfo[0];
                    }
                }
            }
            return _emptyArray;
        }

        public ImsReasonInfo() {
            clear();
        }

        public ImsReasonInfo clear() {
            this.reasonCode = 0;
            this.extraCode = 0;
            this.extraMessage = "";
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.reasonCode != 0) {
                output.writeInt32(1, this.reasonCode);
            }
            if (this.extraCode != 0) {
                output.writeInt32(2, this.extraCode);
            }
            if (!this.extraMessage.equals("")) {
                output.writeString(3, this.extraMessage);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.reasonCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.reasonCode);
            }
            if (this.extraCode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.extraCode);
            }
            if (this.extraMessage.equals("")) {
                return size;
            }
            return size + CodedOutputByteBufferNano.computeStringSize(3, this.extraMessage);
        }

        public ImsReasonInfo mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.reasonCode = input.readInt32();
                        break;
                    case 16:
                        this.extraCode = input.readInt32();
                        break;
                    case 26:
                        this.extraMessage = input.readString();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static ImsReasonInfo parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (ImsReasonInfo) MessageNano.mergeFrom(new ImsReasonInfo(), data);
        }

        public static ImsReasonInfo parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new ImsReasonInfo().mergeFrom(input);
        }
    }

    public interface PdpType {
        public static final int PDP_TYPE_IP = 1;
        public static final int PDP_TYPE_IPV4V6 = 3;
        public static final int PDP_TYPE_IPV6 = 2;
        public static final int PDP_TYPE_PPP = 4;
        public static final int PDP_UNKNOWN = 0;
    }

    public interface RadioAccessTechnology {
        public static final int RAT_1XRTT = 6;
        public static final int RAT_EDGE = 2;
        public static final int RAT_EHRPD = 13;
        public static final int RAT_EVDO_0 = 7;
        public static final int RAT_EVDO_A = 8;
        public static final int RAT_EVDO_B = 12;
        public static final int RAT_GPRS = 1;
        public static final int RAT_GSM = 16;
        public static final int RAT_HSDPA = 9;
        public static final int RAT_HSPA = 11;
        public static final int RAT_HSPAP = 15;
        public static final int RAT_HSUPA = 10;
        public static final int RAT_IS95A = 4;
        public static final int RAT_IS95B = 5;
        public static final int RAT_IWLAN = 18;
        public static final int RAT_LTE = 14;
        public static final int RAT_LTE_CA = 19;
        public static final int RAT_TD_SCDMA = 17;
        public static final int RAT_UMTS = 3;
        public static final int RAT_UNKNOWN = 0;
        public static final int UNKNOWN = -1;
    }

    public static final class RilDataCall extends ExtendableMessageNano<RilDataCall> {
        private static volatile RilDataCall[] _emptyArray;
        public int cid;
        public String iframe;
        public int type;

        public static RilDataCall[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new RilDataCall[0];
                    }
                }
            }
            return _emptyArray;
        }

        public RilDataCall() {
            clear();
        }

        public RilDataCall clear() {
            this.cid = 0;
            this.type = 0;
            this.iframe = "";
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.cid != 0) {
                output.writeInt32(1, this.cid);
            }
            if (this.type != 0) {
                output.writeInt32(2, this.type);
            }
            if (!this.iframe.equals("")) {
                output.writeString(3, this.iframe);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.cid != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.cid);
            }
            if (this.type != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.type);
            }
            if (this.iframe.equals("")) {
                return size;
            }
            return size + CodedOutputByteBufferNano.computeStringSize(3, this.iframe);
        }

        public RilDataCall mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.cid = input.readInt32();
                        break;
                    case 16:
                        int initialPos = input.getPosition();
                        int value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                this.type = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 26:
                        this.iframe = input.readString();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static RilDataCall parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (RilDataCall) MessageNano.mergeFrom(new RilDataCall(), data);
        }

        public static RilDataCall parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new RilDataCall().mergeFrom(input);
        }
    }

    public interface RilErrno {
        public static final int RIL_E_CANCELLED = 8;
        public static final int RIL_E_DIAL_MODIFIED_TO_DIAL = 21;
        public static final int RIL_E_DIAL_MODIFIED_TO_SS = 20;
        public static final int RIL_E_DIAL_MODIFIED_TO_USSD = 19;
        public static final int RIL_E_FDN_CHECK_FAILURE = 15;
        public static final int RIL_E_GENERIC_FAILURE = 3;
        public static final int RIL_E_ILLEGAL_SIM_OR_ME = 16;
        public static final int RIL_E_LCE_NOT_SUPPORTED = 36;
        public static final int RIL_E_LCE_NOT_SUPPORTED_NEW = 37;
        public static final int RIL_E_MISSING_RESOURCE = 17;
        public static final int RIL_E_MODE_NOT_SUPPORTED = 14;
        public static final int RIL_E_NO_SUCH_ELEMENT = 18;
        public static final int RIL_E_OP_NOT_ALLOWED_BEFORE_REG_TO_NW = 10;
        public static final int RIL_E_OP_NOT_ALLOWED_DURING_VOICE_CALL = 9;
        public static final int RIL_E_PASSWORD_INCORRECT = 4;
        public static final int RIL_E_RADIO_NOT_AVAILABLE = 2;
        public static final int RIL_E_REQUEST_NOT_SUPPORTED = 7;
        public static final int RIL_E_SIM_ABSENT = 12;
        public static final int RIL_E_SIM_PIN2 = 5;
        public static final int RIL_E_SIM_PUK2 = 6;
        public static final int RIL_E_SMS_SEND_FAIL_RETRY = 11;
        public static final int RIL_E_SS_MODIFIED_TO_DIAL = 25;
        public static final int RIL_E_SS_MODIFIED_TO_SS = 28;
        public static final int RIL_E_SS_MODIFIED_TO_USSD = 26;
        public static final int RIL_E_SUBSCRIPTION_NOT_AVAILABLE = 13;
        public static final int RIL_E_SUBSCRIPTION_NOT_SUPPORTED = 27;
        public static final int RIL_E_SUCCESS = 1;
        public static final int RIL_E_UNKNOWN = 0;
        public static final int RIL_E_USSD_MODIFIED_TO_DIAL = 22;
        public static final int RIL_E_USSD_MODIFIED_TO_SS = 23;
        public static final int RIL_E_USSD_MODIFIED_TO_USSD = 24;
    }

    public static final class SmsSession extends ExtendableMessageNano<SmsSession> {
        private static volatile SmsSession[] _emptyArray;
        public Event[] events;
        public boolean eventsDropped;
        public int phoneId;
        public int startTimeMinutes;

        public static final class Event extends ExtendableMessageNano<Event> {
            private static volatile Event[] _emptyArray;
            public RilDataCall[] dataCalls;
            public int delay;
            public int error;
            public int errorCode;
            public int format;
            public ImsCapabilities imsCapabilities;
            public ImsConnectionState imsConnectionState;
            public int rilRequestId;
            public TelephonyServiceState serviceState;
            public TelephonySettings settings;
            public int tech;
            public int type;

            public interface Format {
                public static final int SMS_FORMAT_3GPP = 1;
                public static final int SMS_FORMAT_3GPP2 = 2;
                public static final int SMS_FORMAT_UNKNOWN = 0;
            }

            public interface Tech {
                public static final int SMS_CDMA = 2;
                public static final int SMS_GSM = 1;
                public static final int SMS_IMS = 3;
                public static final int SMS_UNKNOWN = 0;
            }

            public interface Type {
                public static final int DATA_CALL_LIST_CHANGED = 5;
                public static final int EVENT_UNKNOWN = 0;
                public static final int IMS_CAPABILITIES_CHANGED = 4;
                public static final int IMS_CONNECTION_STATE_CHANGED = 3;
                public static final int RIL_SERVICE_STATE_CHANGED = 2;
                public static final int SETTINGS_CHANGED = 1;
                public static final int SMS_RECEIVED = 8;
                public static final int SMS_SEND = 6;
                public static final int SMS_SEND_RESULT = 7;
            }

            public static Event[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new Event[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public Event() {
                clear();
            }

            public Event clear() {
                this.type = 0;
                this.delay = 0;
                this.settings = null;
                this.serviceState = null;
                this.imsConnectionState = null;
                this.imsCapabilities = null;
                this.dataCalls = RilDataCall.emptyArray();
                this.format = 0;
                this.tech = 0;
                this.errorCode = 0;
                this.error = 0;
                this.rilRequestId = 0;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.type != 0) {
                    output.writeInt32(1, this.type);
                }
                if (this.delay != 0) {
                    output.writeInt32(2, this.delay);
                }
                if (this.settings != null) {
                    output.writeMessage(3, this.settings);
                }
                if (this.serviceState != null) {
                    output.writeMessage(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    output.writeMessage(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    output.writeMessage(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    for (RilDataCall element : this.dataCalls) {
                        if (element != null) {
                            output.writeMessage(7, element);
                        }
                    }
                }
                if (this.format != 0) {
                    output.writeInt32(8, this.format);
                }
                if (this.tech != 0) {
                    output.writeInt32(9, this.tech);
                }
                if (this.errorCode != 0) {
                    output.writeInt32(10, this.errorCode);
                }
                if (this.error != 0) {
                    output.writeInt32(11, this.error);
                }
                if (this.rilRequestId != 0) {
                    output.writeInt32(12, this.rilRequestId);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.type != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.type);
                }
                if (this.delay != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.delay);
                }
                if (this.settings != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(3, this.settings);
                }
                if (this.serviceState != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(4, this.serviceState);
                }
                if (this.imsConnectionState != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(5, this.imsConnectionState);
                }
                if (this.imsCapabilities != null) {
                    size += CodedOutputByteBufferNano.computeMessageSize(6, this.imsCapabilities);
                }
                if (this.dataCalls != null && this.dataCalls.length > 0) {
                    for (RilDataCall element : this.dataCalls) {
                        if (element != null) {
                            size += CodedOutputByteBufferNano.computeMessageSize(7, element);
                        }
                    }
                }
                if (this.format != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(8, this.format);
                }
                if (this.tech != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(9, this.tech);
                }
                if (this.errorCode != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(10, this.errorCode);
                }
                if (this.error != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(11, this.error);
                }
                if (this.rilRequestId != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(12, this.rilRequestId);
                }
                return size;
            }

            public Event mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    int initialPos;
                    int value;
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                    this.type = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 16:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                case 9:
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.delay = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 26:
                            if (this.settings == null) {
                                this.settings = new TelephonySettings();
                            }
                            input.readMessage(this.settings);
                            break;
                        case 34:
                            if (this.serviceState == null) {
                                this.serviceState = new TelephonyServiceState();
                            }
                            input.readMessage(this.serviceState);
                            break;
                        case 42:
                            if (this.imsConnectionState == null) {
                                this.imsConnectionState = new ImsConnectionState();
                            }
                            input.readMessage(this.imsConnectionState);
                            break;
                        case 50:
                            if (this.imsCapabilities == null) {
                                this.imsCapabilities = new ImsCapabilities();
                            }
                            input.readMessage(this.imsCapabilities);
                            break;
                        case 58:
                            int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 58);
                            int i = this.dataCalls == null ? 0 : this.dataCalls.length;
                            RilDataCall[] newArray = new RilDataCall[(i + arrayLength)];
                            if (i != 0) {
                                System.arraycopy(this.dataCalls, 0, newArray, 0, i);
                            }
                            while (i < newArray.length - 1) {
                                newArray[i] = new RilDataCall();
                                input.readMessage(newArray[i]);
                                input.readTag();
                                i++;
                            }
                            newArray[i] = new RilDataCall();
                            input.readMessage(newArray[i]);
                            this.dataCalls = newArray;
                            break;
                        case 64:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                    this.format = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 72:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.tech = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case RadioNVItems.RIL_NV_LTE_NEXT_SCAN /*80*/:
                            this.errorCode = input.readInt32();
                            break;
                        case 88:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                case 9:
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20:
                                case 21:
                                case 22:
                                case 23:
                                case 24:
                                case 25:
                                case 26:
                                case 27:
                                case 28:
                                case 36:
                                case 37:
                                    this.error = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 96:
                            this.rilRequestId = input.readInt32();
                            break;
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static Event parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (Event) MessageNano.mergeFrom(new Event(), data);
            }

            public static Event parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new Event().mergeFrom(input);
            }
        }

        public static SmsSession[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new SmsSession[0];
                    }
                }
            }
            return _emptyArray;
        }

        public SmsSession() {
            clear();
        }

        public SmsSession clear() {
            this.startTimeMinutes = 0;
            this.phoneId = 0;
            this.events = Event.emptyArray();
            this.eventsDropped = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.startTimeMinutes != 0) {
                output.writeInt32(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                output.writeInt32(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (Event element : this.events) {
                    if (element != null) {
                        output.writeMessage(3, element);
                    }
                }
            }
            if (this.eventsDropped) {
                output.writeBool(4, this.eventsDropped);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.startTimeMinutes != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.startTimeMinutes);
            }
            if (this.phoneId != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.phoneId);
            }
            if (this.events != null && this.events.length > 0) {
                for (Event element : this.events) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(3, element);
                    }
                }
            }
            if (this.eventsDropped) {
                return size + CodedOutputByteBufferNano.computeBoolSize(4, this.eventsDropped);
            }
            return size;
        }

        public SmsSession mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.startTimeMinutes = input.readInt32();
                        break;
                    case 16:
                        this.phoneId = input.readInt32();
                        break;
                    case 26:
                        int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 26);
                        int i = this.events == null ? 0 : this.events.length;
                        Event[] newArray = new Event[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.events, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new Event();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new Event();
                        input.readMessage(newArray[i]);
                        this.events = newArray;
                        break;
                    case 32:
                        this.eventsDropped = input.readBool();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static SmsSession parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (SmsSession) MessageNano.mergeFrom(new SmsSession(), data);
        }

        public static SmsSession parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new SmsSession().mergeFrom(input);
        }
    }

    public static final class TelephonyCallSession extends ExtendableMessageNano<TelephonyCallSession> {
        private static volatile TelephonyCallSession[] _emptyArray;
        public Event[] events;
        public boolean eventsDropped;
        public int phoneId;
        public int startTimeMinutes;

        public static final class Event extends ExtendableMessageNano<Event> {
            private static volatile Event[] _emptyArray;
            public int callIndex;
            public int callState;
            public RilCall[] calls;
            public RilDataCall[] dataCalls;
            public int delay;
            public int error;
            public ImsCapabilities imsCapabilities;
            public int imsCommand;
            public ImsConnectionState imsConnectionState;
            public int mergedCallIndex;
            public long nitzTimestampMillis;
            public int phoneState;
            public ImsReasonInfo reasonInfo;
            public int rilRequest;
            public int rilRequestId;
            public TelephonyServiceState serviceState;
            public TelephonySettings settings;
            public int srcAccessTech;
            public int srvccState;
            public int targetAccessTech;
            public int type;

            public interface CallState {
                public static final int CALL_ACTIVE = 2;
                public static final int CALL_ALERTING = 5;
                public static final int CALL_DIALING = 4;
                public static final int CALL_DISCONNECTED = 8;
                public static final int CALL_DISCONNECTING = 9;
                public static final int CALL_HOLDING = 3;
                public static final int CALL_IDLE = 1;
                public static final int CALL_INCOMING = 6;
                public static final int CALL_UNKNOWN = 0;
                public static final int CALL_WAITING = 7;
            }

            public interface ImsCommand {
                public static final int IMS_CMD_ACCEPT = 2;
                public static final int IMS_CMD_CONFERENCE_EXTEND = 9;
                public static final int IMS_CMD_HOLD = 5;
                public static final int IMS_CMD_INVITE_PARTICIPANT = 10;
                public static final int IMS_CMD_MERGE = 7;
                public static final int IMS_CMD_REJECT = 3;
                public static final int IMS_CMD_REMOVE_PARTICIPANT = 11;
                public static final int IMS_CMD_RESUME = 6;
                public static final int IMS_CMD_START = 1;
                public static final int IMS_CMD_TERMINATE = 4;
                public static final int IMS_CMD_UNKNOWN = 0;
                public static final int IMS_CMD_UPDATE = 8;
            }

            public interface PhoneState {
                public static final int STATE_IDLE = 1;
                public static final int STATE_OFFHOOK = 3;
                public static final int STATE_RINGING = 2;
                public static final int STATE_UNKNOWN = 0;
            }

            public static final class RilCall extends ExtendableMessageNano<RilCall> {
                private static volatile RilCall[] _emptyArray;
                public int callEndReason;
                public int index;
                public boolean isMultiparty;
                public int state;
                public int type;

                public interface Type {
                    public static final int MO = 1;
                    public static final int MT = 2;
                    public static final int UNKNOWN = 0;
                }

                public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall[] emptyArray() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r0 = _emptyArray;
                    if (r0 != 0) goto L_0x0011;
                L_0x0004:
                    r1 = com.android.internal.telephony.protobuf.nano.InternalNano.LAZY_INIT_LOCK;
                    monitor-enter(r1);
                    r0 = _emptyArray;
                    if (r0 != 0) goto L_0x0010;
                L_0x000b:
                    r0 = 0;
                    r0 = new com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall[r0];
                    _emptyArray = r0;
                L_0x0010:
                    monitor-exit(r1);
                L_0x0011:
                    r0 = _emptyArray;
                    return r0;
                L_0x0014:
                    r0 = move-exception;
                    monitor-exit(r1);
                    throw r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.emptyArray():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall[]");
                }

                public RilCall() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r0 = this;
                    r0.<init>();
                    r0.clear();
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.<init>():void");
                }

                public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall clear() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r1 = this;
                    r0 = 0;
                    r1.index = r0;
                    r1.state = r0;
                    r1.type = r0;
                    r1.callEndReason = r0;
                    r1.isMultiparty = r0;
                    r0 = 0;
                    r1.unknownFieldData = r0;
                    r0 = -1;
                    r1.cachedSize = r0;
                    return r1;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.clear():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall");
                }

                public void writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano r3) throws java.io.IOException {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r2 = this;
                    r0 = r2.index;
                    if (r0 == 0) goto L_0x000a;
                L_0x0004:
                    r0 = r2.index;
                    r1 = 1;
                    r3.writeInt32(r1, r0);
                L_0x000a:
                    r0 = r2.state;
                    if (r0 == 0) goto L_0x0014;
                L_0x000e:
                    r0 = r2.state;
                    r1 = 2;
                    r3.writeInt32(r1, r0);
                L_0x0014:
                    r0 = r2.type;
                    if (r0 == 0) goto L_0x001e;
                L_0x0018:
                    r0 = r2.type;
                    r1 = 3;
                    r3.writeInt32(r1, r0);
                L_0x001e:
                    r0 = r2.callEndReason;
                    if (r0 == 0) goto L_0x0028;
                L_0x0022:
                    r0 = r2.callEndReason;
                    r1 = 4;
                    r3.writeInt32(r1, r0);
                L_0x0028:
                    r0 = r2.isMultiparty;
                    if (r0 == 0) goto L_0x0032;
                L_0x002c:
                    r0 = r2.isMultiparty;
                    r1 = 5;
                    r3.writeBool(r1, r0);
                L_0x0032:
                    super.writeTo(r3);
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano):void");
                }

                protected int computeSerializedSize() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r3 = this;
                    r0 = super.computeSerializedSize();
                    r1 = r3.index;
                    if (r1 == 0) goto L_0x0010;
                L_0x0008:
                    r1 = r3.index;
                    r2 = 1;
                    r1 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r2, r1);
                    r0 = r0 + r1;
                L_0x0010:
                    r1 = r3.state;
                    if (r1 == 0) goto L_0x001c;
                L_0x0014:
                    r1 = r3.state;
                    r2 = 2;
                    r1 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r2, r1);
                    r0 = r0 + r1;
                L_0x001c:
                    r1 = r3.type;
                    if (r1 == 0) goto L_0x0028;
                L_0x0020:
                    r1 = r3.type;
                    r2 = 3;
                    r1 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r2, r1);
                    r0 = r0 + r1;
                L_0x0028:
                    r1 = r3.callEndReason;
                    if (r1 == 0) goto L_0x0034;
                L_0x002c:
                    r1 = r3.callEndReason;
                    r2 = 4;
                    r1 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r2, r1);
                    r0 = r0 + r1;
                L_0x0034:
                    r1 = r3.isMultiparty;
                    if (r1 == 0) goto L_0x0040;
                L_0x0038:
                    r1 = r3.isMultiparty;
                    r2 = 5;
                    r1 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeBoolSize(r2, r1);
                    r0 = r0 + r1;
                L_0x0040:
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.computeSerializedSize():int");
                }

                public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r5) throws java.io.IOException {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r4 = this;
                L_0x0000:
                    r1 = r5.readTag();
                    switch(r1) {
                        case 0: goto L_0x000e;
                        case 8: goto L_0x000f;
                        case 16: goto L_0x0016;
                        case 24: goto L_0x002b;
                        case 32: goto L_0x0040;
                        case 40: goto L_0x0047;
                        default: goto L_0x0007;
                    };
                L_0x0007:
                    r3 = r4.storeUnknownField(r5, r1);
                    if (r3 != 0) goto L_0x0000;
                L_0x000d:
                    return r4;
                L_0x000e:
                    return r4;
                L_0x000f:
                    r3 = r5.readInt32();
                    r4.index = r3;
                    goto L_0x0000;
                L_0x0016:
                    r0 = r5.getPosition();
                    r2 = r5.readInt32();
                    switch(r2) {
                        case 0: goto L_0x0028;
                        case 1: goto L_0x0028;
                        case 2: goto L_0x0028;
                        case 3: goto L_0x0028;
                        case 4: goto L_0x0028;
                        case 5: goto L_0x0028;
                        case 6: goto L_0x0028;
                        case 7: goto L_0x0028;
                        case 8: goto L_0x0028;
                        case 9: goto L_0x0028;
                        default: goto L_0x0021;
                    };
                L_0x0021:
                    r5.rewindToPosition(r0);
                    r4.storeUnknownField(r5, r1);
                    goto L_0x0000;
                L_0x0028:
                    r4.state = r2;
                    goto L_0x0000;
                L_0x002b:
                    r0 = r5.getPosition();
                    r2 = r5.readInt32();
                    switch(r2) {
                        case 0: goto L_0x003d;
                        case 1: goto L_0x003d;
                        case 2: goto L_0x003d;
                        default: goto L_0x0036;
                    };
                L_0x0036:
                    r5.rewindToPosition(r0);
                    r4.storeUnknownField(r5, r1);
                    goto L_0x0000;
                L_0x003d:
                    r4.type = r2;
                    goto L_0x0000;
                L_0x0040:
                    r3 = r5.readInt32();
                    r4.callEndReason = r3;
                    goto L_0x0000;
                L_0x0047:
                    r3 = r5.readBool();
                    r4.isMultiparty = r3;
                    goto L_0x0000;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall");
                }

                public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall parseFrom(byte[] r1) throws com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall;
                    r0.<init>();
                    r0 = com.android.internal.telephony.protobuf.nano.MessageNano.mergeFrom(r0, r1);
                    r0 = (com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall) r0;
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.parseFrom(byte[]):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall");
                }

                public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r1) throws java.io.IOException {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall;
                    r0.<init>();
                    r0 = r0.mergeFrom(r1);
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall");
                }
            }

            public interface RilRequest {
                public static final int RIL_REQUEST_ANSWER = 2;
                public static final int RIL_REQUEST_CDMA_FLASH = 6;
                public static final int RIL_REQUEST_CONFERENCE = 7;
                public static final int RIL_REQUEST_DIAL = 1;
                public static final int RIL_REQUEST_HANGUP = 3;
                public static final int RIL_REQUEST_SET_CALL_WAITING = 4;
                public static final int RIL_REQUEST_SWITCH_HOLDING_AND_ACTIVE = 5;
                public static final int RIL_REQUEST_UNKNOWN = 0;
            }

            public interface RilSrvccState {
                public static final int HANDOVER_CANCELED = 4;
                public static final int HANDOVER_COMPLETED = 2;
                public static final int HANDOVER_FAILED = 3;
                public static final int HANDOVER_STARTED = 1;
                public static final int HANDOVER_UNKNOWN = 0;
            }

            public interface Type {
                public static final int DATA_CALL_LIST_CHANGED = 5;
                public static final int EVENT_UNKNOWN = 0;
                public static final int IMS_CALL_HANDOVER = 18;
                public static final int IMS_CALL_HANDOVER_FAILED = 19;
                public static final int IMS_CALL_RECEIVE = 15;
                public static final int IMS_CALL_STATE_CHANGED = 16;
                public static final int IMS_CALL_TERMINATED = 17;
                public static final int IMS_CAPABILITIES_CHANGED = 4;
                public static final int IMS_COMMAND = 11;
                public static final int IMS_COMMAND_COMPLETE = 14;
                public static final int IMS_COMMAND_FAILED = 13;
                public static final int IMS_COMMAND_RECEIVED = 12;
                public static final int IMS_CONNECTION_STATE_CHANGED = 3;
                public static final int NITZ_TIME = 21;
                public static final int PHONE_STATE_CHANGED = 20;
                public static final int RIL_CALL_LIST_CHANGED = 10;
                public static final int RIL_CALL_RING = 8;
                public static final int RIL_CALL_SRVCC = 9;
                public static final int RIL_REQUEST = 6;
                public static final int RIL_RESPONSE = 7;
                public static final int RIL_SERVICE_STATE_CHANGED = 2;
                public static final int SETTINGS_CHANGED = 1;
            }

            public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event[] emptyArray() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = _emptyArray;
                if (r0 != 0) goto L_0x0011;
            L_0x0004:
                r1 = com.android.internal.telephony.protobuf.nano.InternalNano.LAZY_INIT_LOCK;
                monitor-enter(r1);
                r0 = _emptyArray;
                if (r0 != 0) goto L_0x0010;
            L_0x000b:
                r0 = 0;
                r0 = new com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event[r0];
                _emptyArray = r0;
            L_0x0010:
                monitor-exit(r1);
            L_0x0011:
                r0 = _emptyArray;
                return r0;
            L_0x0014:
                r0 = move-exception;
                monitor-exit(r1);
                throw r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.emptyArray():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event[]");
            }

            public Event() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                r0.<init>();
                r0.clear();
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.<init>():void");
            }

            public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event clear() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r3 = -1;
                r2 = 0;
                r1 = 0;
                r4.type = r1;
                r4.delay = r1;
                r4.settings = r2;
                r4.serviceState = r2;
                r4.imsConnectionState = r2;
                r4.imsCapabilities = r2;
                r0 = com.android.internal.telephony.nano.TelephonyProto.RilDataCall.emptyArray();
                r4.dataCalls = r0;
                r4.phoneState = r1;
                r4.callState = r1;
                r4.callIndex = r1;
                r4.mergedCallIndex = r1;
                r0 = com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall.emptyArray();
                r4.calls = r0;
                r4.error = r1;
                r4.rilRequest = r1;
                r4.rilRequestId = r1;
                r4.srvccState = r1;
                r4.imsCommand = r1;
                r4.reasonInfo = r2;
                r4.srcAccessTech = r3;
                r4.targetAccessTech = r3;
                r0 = 0;
                r4.nitzTimestampMillis = r0;
                r4.unknownFieldData = r2;
                r4.cachedSize = r3;
                return r4;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.clear():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event");
            }

            public void writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano r9) throws java.io.IOException {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r8 = this;
                r5 = -1;
                r3 = r8.type;
                if (r3 == 0) goto L_0x000b;
            L_0x0005:
                r3 = r8.type;
                r4 = 1;
                r9.writeInt32(r4, r3);
            L_0x000b:
                r3 = r8.delay;
                if (r3 == 0) goto L_0x0015;
            L_0x000f:
                r3 = r8.delay;
                r4 = 2;
                r9.writeInt32(r4, r3);
            L_0x0015:
                r3 = r8.settings;
                if (r3 == 0) goto L_0x001f;
            L_0x0019:
                r3 = r8.settings;
                r4 = 3;
                r9.writeMessage(r4, r3);
            L_0x001f:
                r3 = r8.serviceState;
                if (r3 == 0) goto L_0x0029;
            L_0x0023:
                r3 = r8.serviceState;
                r4 = 4;
                r9.writeMessage(r4, r3);
            L_0x0029:
                r3 = r8.imsConnectionState;
                if (r3 == 0) goto L_0x0033;
            L_0x002d:
                r3 = r8.imsConnectionState;
                r4 = 5;
                r9.writeMessage(r4, r3);
            L_0x0033:
                r3 = r8.imsCapabilities;
                if (r3 == 0) goto L_0x003d;
            L_0x0037:
                r3 = r8.imsCapabilities;
                r4 = 6;
                r9.writeMessage(r4, r3);
            L_0x003d:
                r3 = r8.dataCalls;
                if (r3 == 0) goto L_0x0059;
            L_0x0041:
                r3 = r8.dataCalls;
                r3 = r3.length;
                if (r3 <= 0) goto L_0x0059;
            L_0x0046:
                r2 = 0;
            L_0x0047:
                r3 = r8.dataCalls;
                r3 = r3.length;
                if (r2 >= r3) goto L_0x0059;
            L_0x004c:
                r3 = r8.dataCalls;
                r0 = r3[r2];
                if (r0 == 0) goto L_0x0056;
            L_0x0052:
                r3 = 7;
                r9.writeMessage(r3, r0);
            L_0x0056:
                r2 = r2 + 1;
                goto L_0x0047;
            L_0x0059:
                r3 = r8.phoneState;
                if (r3 == 0) goto L_0x0064;
            L_0x005d:
                r3 = r8.phoneState;
                r4 = 8;
                r9.writeInt32(r4, r3);
            L_0x0064:
                r3 = r8.callState;
                if (r3 == 0) goto L_0x006f;
            L_0x0068:
                r3 = r8.callState;
                r4 = 9;
                r9.writeInt32(r4, r3);
            L_0x006f:
                r3 = r8.callIndex;
                if (r3 == 0) goto L_0x007a;
            L_0x0073:
                r3 = r8.callIndex;
                r4 = 10;
                r9.writeInt32(r4, r3);
            L_0x007a:
                r3 = r8.mergedCallIndex;
                if (r3 == 0) goto L_0x0085;
            L_0x007e:
                r3 = r8.mergedCallIndex;
                r4 = 11;
                r9.writeInt32(r4, r3);
            L_0x0085:
                r3 = r8.calls;
                if (r3 == 0) goto L_0x00a2;
            L_0x0089:
                r3 = r8.calls;
                r3 = r3.length;
                if (r3 <= 0) goto L_0x00a2;
            L_0x008e:
                r2 = 0;
            L_0x008f:
                r3 = r8.calls;
                r3 = r3.length;
                if (r2 >= r3) goto L_0x00a2;
            L_0x0094:
                r3 = r8.calls;
                r1 = r3[r2];
                if (r1 == 0) goto L_0x009f;
            L_0x009a:
                r3 = 12;
                r9.writeMessage(r3, r1);
            L_0x009f:
                r2 = r2 + 1;
                goto L_0x008f;
            L_0x00a2:
                r3 = r8.error;
                if (r3 == 0) goto L_0x00ad;
            L_0x00a6:
                r3 = r8.error;
                r4 = 13;
                r9.writeInt32(r4, r3);
            L_0x00ad:
                r3 = r8.rilRequest;
                if (r3 == 0) goto L_0x00b8;
            L_0x00b1:
                r3 = r8.rilRequest;
                r4 = 14;
                r9.writeInt32(r4, r3);
            L_0x00b8:
                r3 = r8.rilRequestId;
                if (r3 == 0) goto L_0x00c3;
            L_0x00bc:
                r3 = r8.rilRequestId;
                r4 = 15;
                r9.writeInt32(r4, r3);
            L_0x00c3:
                r3 = r8.srvccState;
                if (r3 == 0) goto L_0x00ce;
            L_0x00c7:
                r3 = r8.srvccState;
                r4 = 16;
                r9.writeInt32(r4, r3);
            L_0x00ce:
                r3 = r8.imsCommand;
                if (r3 == 0) goto L_0x00d9;
            L_0x00d2:
                r3 = r8.imsCommand;
                r4 = 17;
                r9.writeInt32(r4, r3);
            L_0x00d9:
                r3 = r8.reasonInfo;
                if (r3 == 0) goto L_0x00e4;
            L_0x00dd:
                r3 = r8.reasonInfo;
                r4 = 18;
                r9.writeMessage(r4, r3);
            L_0x00e4:
                r3 = r8.srcAccessTech;
                if (r3 == r5) goto L_0x00ef;
            L_0x00e8:
                r3 = r8.srcAccessTech;
                r4 = 19;
                r9.writeInt32(r4, r3);
            L_0x00ef:
                r3 = r8.targetAccessTech;
                if (r3 == r5) goto L_0x00fa;
            L_0x00f3:
                r3 = r8.targetAccessTech;
                r4 = 20;
                r9.writeInt32(r4, r3);
            L_0x00fa:
                r4 = r8.nitzTimestampMillis;
                r6 = 0;
                r3 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
                if (r3 == 0) goto L_0x0109;
            L_0x0102:
                r4 = r8.nitzTimestampMillis;
                r3 = 21;
                r9.writeInt64(r3, r4);
            L_0x0109:
                super.writeTo(r9);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano):void");
            }

            protected int computeSerializedSize() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r8 = this;
                r6 = -1;
                r3 = super.computeSerializedSize();
                r4 = r8.type;
                if (r4 == 0) goto L_0x0011;
            L_0x0009:
                r4 = r8.type;
                r5 = 1;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0011:
                r4 = r8.delay;
                if (r4 == 0) goto L_0x001d;
            L_0x0015:
                r4 = r8.delay;
                r5 = 2;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x001d:
                r4 = r8.settings;
                if (r4 == 0) goto L_0x0029;
            L_0x0021:
                r4 = r8.settings;
                r5 = 3;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r5, r4);
                r3 = r3 + r4;
            L_0x0029:
                r4 = r8.serviceState;
                if (r4 == 0) goto L_0x0035;
            L_0x002d:
                r4 = r8.serviceState;
                r5 = 4;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r5, r4);
                r3 = r3 + r4;
            L_0x0035:
                r4 = r8.imsConnectionState;
                if (r4 == 0) goto L_0x0041;
            L_0x0039:
                r4 = r8.imsConnectionState;
                r5 = 5;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r5, r4);
                r3 = r3 + r4;
            L_0x0041:
                r4 = r8.imsCapabilities;
                if (r4 == 0) goto L_0x004d;
            L_0x0045:
                r4 = r8.imsCapabilities;
                r5 = 6;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r5, r4);
                r3 = r3 + r4;
            L_0x004d:
                r4 = r8.dataCalls;
                if (r4 == 0) goto L_0x006b;
            L_0x0051:
                r4 = r8.dataCalls;
                r4 = r4.length;
                if (r4 <= 0) goto L_0x006b;
            L_0x0056:
                r2 = 0;
            L_0x0057:
                r4 = r8.dataCalls;
                r4 = r4.length;
                if (r2 >= r4) goto L_0x006b;
            L_0x005c:
                r4 = r8.dataCalls;
                r0 = r4[r2];
                if (r0 == 0) goto L_0x0068;
            L_0x0062:
                r4 = 7;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r4, r0);
                r3 = r3 + r4;
            L_0x0068:
                r2 = r2 + 1;
                goto L_0x0057;
            L_0x006b:
                r4 = r8.phoneState;
                if (r4 == 0) goto L_0x0078;
            L_0x006f:
                r4 = r8.phoneState;
                r5 = 8;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0078:
                r4 = r8.callState;
                if (r4 == 0) goto L_0x0085;
            L_0x007c:
                r4 = r8.callState;
                r5 = 9;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0085:
                r4 = r8.callIndex;
                if (r4 == 0) goto L_0x0092;
            L_0x0089:
                r4 = r8.callIndex;
                r5 = 10;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0092:
                r4 = r8.mergedCallIndex;
                if (r4 == 0) goto L_0x009f;
            L_0x0096:
                r4 = r8.mergedCallIndex;
                r5 = 11;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x009f:
                r4 = r8.calls;
                if (r4 == 0) goto L_0x00be;
            L_0x00a3:
                r4 = r8.calls;
                r4 = r4.length;
                if (r4 <= 0) goto L_0x00be;
            L_0x00a8:
                r2 = 0;
            L_0x00a9:
                r4 = r8.calls;
                r4 = r4.length;
                if (r2 >= r4) goto L_0x00be;
            L_0x00ae:
                r4 = r8.calls;
                r1 = r4[r2];
                if (r1 == 0) goto L_0x00bb;
            L_0x00b4:
                r4 = 12;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r4, r1);
                r3 = r3 + r4;
            L_0x00bb:
                r2 = r2 + 1;
                goto L_0x00a9;
            L_0x00be:
                r4 = r8.error;
                if (r4 == 0) goto L_0x00cb;
            L_0x00c2:
                r4 = r8.error;
                r5 = 13;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x00cb:
                r4 = r8.rilRequest;
                if (r4 == 0) goto L_0x00d8;
            L_0x00cf:
                r4 = r8.rilRequest;
                r5 = 14;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x00d8:
                r4 = r8.rilRequestId;
                if (r4 == 0) goto L_0x00e5;
            L_0x00dc:
                r4 = r8.rilRequestId;
                r5 = 15;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x00e5:
                r4 = r8.srvccState;
                if (r4 == 0) goto L_0x00f2;
            L_0x00e9:
                r4 = r8.srvccState;
                r5 = 16;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x00f2:
                r4 = r8.imsCommand;
                if (r4 == 0) goto L_0x00ff;
            L_0x00f6:
                r4 = r8.imsCommand;
                r5 = 17;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x00ff:
                r4 = r8.reasonInfo;
                if (r4 == 0) goto L_0x010c;
            L_0x0103:
                r4 = r8.reasonInfo;
                r5 = 18;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r5, r4);
                r3 = r3 + r4;
            L_0x010c:
                r4 = r8.srcAccessTech;
                if (r4 == r6) goto L_0x0119;
            L_0x0110:
                r4 = r8.srcAccessTech;
                r5 = 19;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0119:
                r4 = r8.targetAccessTech;
                if (r4 == r6) goto L_0x0126;
            L_0x011d:
                r4 = r8.targetAccessTech;
                r5 = 20;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r5, r4);
                r3 = r3 + r4;
            L_0x0126:
                r4 = r8.nitzTimestampMillis;
                r6 = 0;
                r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1));
                if (r4 == 0) goto L_0x0137;
            L_0x012e:
                r4 = r8.nitzTimestampMillis;
                r6 = 21;
                r4 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt64Size(r6, r4);
                r3 = r3 + r4;
            L_0x0137:
                return r3;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.computeSerializedSize():int");
            }

            public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r12) throws java.io.IOException {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r11 = this;
                r10 = 0;
            L_0x0001:
                r5 = r12.readTag();
                switch(r5) {
                    case 0: goto L_0x000f;
                    case 8: goto L_0x0010;
                    case 16: goto L_0x0025;
                    case 26: goto L_0x003a;
                    case 34: goto L_0x004b;
                    case 42: goto L_0x005c;
                    case 50: goto L_0x006d;
                    case 58: goto L_0x007e;
                    case 64: goto L_0x00bf;
                    case 72: goto L_0x00d6;
                    case 80: goto L_0x00ed;
                    case 88: goto L_0x00f5;
                    case 98: goto L_0x00fd;
                    case 104: goto L_0x013e;
                    case 112: goto L_0x0155;
                    case 120: goto L_0x016c;
                    case 128: goto L_0x0174;
                    case 136: goto L_0x018b;
                    case 146: goto L_0x01a2;
                    case 152: goto L_0x01b4;
                    case 160: goto L_0x01cb;
                    case 168: goto L_0x01e2;
                    default: goto L_0x0008;
                };
            L_0x0008:
                r7 = r11.storeUnknownField(r12, r5);
                if (r7 != 0) goto L_0x0001;
            L_0x000e:
                return r11;
            L_0x000f:
                return r11;
            L_0x0010:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x0022;
                    case 1: goto L_0x0022;
                    case 2: goto L_0x0022;
                    case 3: goto L_0x0022;
                    case 4: goto L_0x0022;
                    case 5: goto L_0x0022;
                    case 6: goto L_0x0022;
                    case 7: goto L_0x0022;
                    case 8: goto L_0x0022;
                    case 9: goto L_0x0022;
                    case 10: goto L_0x0022;
                    case 11: goto L_0x0022;
                    case 12: goto L_0x0022;
                    case 13: goto L_0x0022;
                    case 14: goto L_0x0022;
                    case 15: goto L_0x0022;
                    case 16: goto L_0x0022;
                    case 17: goto L_0x0022;
                    case 18: goto L_0x0022;
                    case 19: goto L_0x0022;
                    case 20: goto L_0x0022;
                    case 21: goto L_0x0022;
                    default: goto L_0x001b;
                };
            L_0x001b:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x0022:
                r11.type = r6;
                goto L_0x0001;
            L_0x0025:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x0037;
                    case 1: goto L_0x0037;
                    case 2: goto L_0x0037;
                    case 3: goto L_0x0037;
                    case 4: goto L_0x0037;
                    case 5: goto L_0x0037;
                    case 6: goto L_0x0037;
                    case 7: goto L_0x0037;
                    case 8: goto L_0x0037;
                    case 9: goto L_0x0037;
                    case 10: goto L_0x0037;
                    case 11: goto L_0x0037;
                    case 12: goto L_0x0037;
                    case 13: goto L_0x0037;
                    case 14: goto L_0x0037;
                    case 15: goto L_0x0037;
                    case 16: goto L_0x0037;
                    case 17: goto L_0x0037;
                    case 18: goto L_0x0037;
                    case 19: goto L_0x0037;
                    default: goto L_0x0030;
                };
            L_0x0030:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x0037:
                r11.delay = r6;
                goto L_0x0001;
            L_0x003a:
                r7 = r11.settings;
                if (r7 != 0) goto L_0x0045;
            L_0x003e:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$TelephonySettings;
                r7.<init>();
                r11.settings = r7;
            L_0x0045:
                r7 = r11.settings;
                r12.readMessage(r7);
                goto L_0x0001;
            L_0x004b:
                r7 = r11.serviceState;
                if (r7 != 0) goto L_0x0056;
            L_0x004f:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyServiceState;
                r7.<init>();
                r11.serviceState = r7;
            L_0x0056:
                r7 = r11.serviceState;
                r12.readMessage(r7);
                goto L_0x0001;
            L_0x005c:
                r7 = r11.imsConnectionState;
                if (r7 != 0) goto L_0x0067;
            L_0x0060:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$ImsConnectionState;
                r7.<init>();
                r11.imsConnectionState = r7;
            L_0x0067:
                r7 = r11.imsConnectionState;
                r12.readMessage(r7);
                goto L_0x0001;
            L_0x006d:
                r7 = r11.imsCapabilities;
                if (r7 != 0) goto L_0x0078;
            L_0x0071:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$ImsCapabilities;
                r7.<init>();
                r11.imsCapabilities = r7;
            L_0x0078:
                r7 = r11.imsCapabilities;
                r12.readMessage(r7);
                goto L_0x0001;
            L_0x007e:
                r7 = 58;
                r0 = com.android.internal.telephony.protobuf.nano.WireFormatNano.getRepeatedFieldArrayLength(r12, r7);
                r7 = r11.dataCalls;
                if (r7 != 0) goto L_0x00ab;
            L_0x0088:
                r1 = 0;
            L_0x0089:
                r7 = r1 + r0;
                r3 = new com.android.internal.telephony.nano.TelephonyProto.RilDataCall[r7];
                if (r1 == 0) goto L_0x0094;
            L_0x008f:
                r7 = r11.dataCalls;
                java.lang.System.arraycopy(r7, r10, r3, r10, r1);
            L_0x0094:
                r7 = r3.length;
                r7 = r7 + -1;
                if (r1 >= r7) goto L_0x00af;
            L_0x0099:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$RilDataCall;
                r7.<init>();
                r3[r1] = r7;
                r7 = r3[r1];
                r12.readMessage(r7);
                r12.readTag();
                r1 = r1 + 1;
                goto L_0x0094;
            L_0x00ab:
                r7 = r11.dataCalls;
                r1 = r7.length;
                goto L_0x0089;
            L_0x00af:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$RilDataCall;
                r7.<init>();
                r3[r1] = r7;
                r7 = r3[r1];
                r12.readMessage(r7);
                r11.dataCalls = r3;
                goto L_0x0001;
            L_0x00bf:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x00d2;
                    case 1: goto L_0x00d2;
                    case 2: goto L_0x00d2;
                    case 3: goto L_0x00d2;
                    default: goto L_0x00ca;
                };
            L_0x00ca:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x00d2:
                r11.phoneState = r6;
                goto L_0x0001;
            L_0x00d6:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x00e9;
                    case 1: goto L_0x00e9;
                    case 2: goto L_0x00e9;
                    case 3: goto L_0x00e9;
                    case 4: goto L_0x00e9;
                    case 5: goto L_0x00e9;
                    case 6: goto L_0x00e9;
                    case 7: goto L_0x00e9;
                    case 8: goto L_0x00e9;
                    case 9: goto L_0x00e9;
                    default: goto L_0x00e1;
                };
            L_0x00e1:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x00e9:
                r11.callState = r6;
                goto L_0x0001;
            L_0x00ed:
                r7 = r12.readInt32();
                r11.callIndex = r7;
                goto L_0x0001;
            L_0x00f5:
                r7 = r12.readInt32();
                r11.mergedCallIndex = r7;
                goto L_0x0001;
            L_0x00fd:
                r7 = 98;
                r0 = com.android.internal.telephony.protobuf.nano.WireFormatNano.getRepeatedFieldArrayLength(r12, r7);
                r7 = r11.calls;
                if (r7 != 0) goto L_0x012a;
            L_0x0107:
                r1 = 0;
            L_0x0108:
                r7 = r1 + r0;
                r4 = new com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.RilCall[r7];
                if (r1 == 0) goto L_0x0113;
            L_0x010e:
                r7 = r11.calls;
                java.lang.System.arraycopy(r7, r10, r4, r10, r1);
            L_0x0113:
                r7 = r4.length;
                r7 = r7 + -1;
                if (r1 >= r7) goto L_0x012e;
            L_0x0118:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall;
                r7.<init>();
                r4[r1] = r7;
                r7 = r4[r1];
                r12.readMessage(r7);
                r12.readTag();
                r1 = r1 + 1;
                goto L_0x0113;
            L_0x012a:
                r7 = r11.calls;
                r1 = r7.length;
                goto L_0x0108;
            L_0x012e:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event$RilCall;
                r7.<init>();
                r4[r1] = r7;
                r7 = r4[r1];
                r12.readMessage(r7);
                r11.calls = r4;
                goto L_0x0001;
            L_0x013e:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x0151;
                    case 1: goto L_0x0151;
                    case 2: goto L_0x0151;
                    case 3: goto L_0x0151;
                    case 4: goto L_0x0151;
                    case 5: goto L_0x0151;
                    case 6: goto L_0x0151;
                    case 7: goto L_0x0151;
                    case 8: goto L_0x0151;
                    case 9: goto L_0x0151;
                    case 10: goto L_0x0151;
                    case 11: goto L_0x0151;
                    case 12: goto L_0x0151;
                    case 13: goto L_0x0151;
                    case 14: goto L_0x0151;
                    case 15: goto L_0x0151;
                    case 16: goto L_0x0151;
                    case 17: goto L_0x0151;
                    case 18: goto L_0x0151;
                    case 19: goto L_0x0151;
                    case 20: goto L_0x0151;
                    case 21: goto L_0x0151;
                    case 22: goto L_0x0151;
                    case 23: goto L_0x0151;
                    case 24: goto L_0x0151;
                    case 25: goto L_0x0151;
                    case 26: goto L_0x0151;
                    case 27: goto L_0x0151;
                    case 28: goto L_0x0151;
                    case 29: goto L_0x0149;
                    case 30: goto L_0x0149;
                    case 31: goto L_0x0149;
                    case 32: goto L_0x0149;
                    case 33: goto L_0x0149;
                    case 34: goto L_0x0149;
                    case 35: goto L_0x0149;
                    case 36: goto L_0x0151;
                    case 37: goto L_0x0151;
                    default: goto L_0x0149;
                };
            L_0x0149:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x0151:
                r11.error = r6;
                goto L_0x0001;
            L_0x0155:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x0168;
                    case 1: goto L_0x0168;
                    case 2: goto L_0x0168;
                    case 3: goto L_0x0168;
                    case 4: goto L_0x0168;
                    case 5: goto L_0x0168;
                    case 6: goto L_0x0168;
                    case 7: goto L_0x0168;
                    default: goto L_0x0160;
                };
            L_0x0160:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x0168:
                r11.rilRequest = r6;
                goto L_0x0001;
            L_0x016c:
                r7 = r12.readInt32();
                r11.rilRequestId = r7;
                goto L_0x0001;
            L_0x0174:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x0187;
                    case 1: goto L_0x0187;
                    case 2: goto L_0x0187;
                    case 3: goto L_0x0187;
                    case 4: goto L_0x0187;
                    default: goto L_0x017f;
                };
            L_0x017f:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x0187:
                r11.srvccState = r6;
                goto L_0x0001;
            L_0x018b:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case 0: goto L_0x019e;
                    case 1: goto L_0x019e;
                    case 2: goto L_0x019e;
                    case 3: goto L_0x019e;
                    case 4: goto L_0x019e;
                    case 5: goto L_0x019e;
                    case 6: goto L_0x019e;
                    case 7: goto L_0x019e;
                    case 8: goto L_0x019e;
                    case 9: goto L_0x019e;
                    case 10: goto L_0x019e;
                    case 11: goto L_0x019e;
                    default: goto L_0x0196;
                };
            L_0x0196:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x019e:
                r11.imsCommand = r6;
                goto L_0x0001;
            L_0x01a2:
                r7 = r11.reasonInfo;
                if (r7 != 0) goto L_0x01ad;
            L_0x01a6:
                r7 = new com.android.internal.telephony.nano.TelephonyProto$ImsReasonInfo;
                r7.<init>();
                r11.reasonInfo = r7;
            L_0x01ad:
                r7 = r11.reasonInfo;
                r12.readMessage(r7);
                goto L_0x0001;
            L_0x01b4:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case -1: goto L_0x01c7;
                    case 0: goto L_0x01c7;
                    case 1: goto L_0x01c7;
                    case 2: goto L_0x01c7;
                    case 3: goto L_0x01c7;
                    case 4: goto L_0x01c7;
                    case 5: goto L_0x01c7;
                    case 6: goto L_0x01c7;
                    case 7: goto L_0x01c7;
                    case 8: goto L_0x01c7;
                    case 9: goto L_0x01c7;
                    case 10: goto L_0x01c7;
                    case 11: goto L_0x01c7;
                    case 12: goto L_0x01c7;
                    case 13: goto L_0x01c7;
                    case 14: goto L_0x01c7;
                    case 15: goto L_0x01c7;
                    case 16: goto L_0x01c7;
                    case 17: goto L_0x01c7;
                    case 18: goto L_0x01c7;
                    case 19: goto L_0x01c7;
                    default: goto L_0x01bf;
                };
            L_0x01bf:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x01c7:
                r11.srcAccessTech = r6;
                goto L_0x0001;
            L_0x01cb:
                r2 = r12.getPosition();
                r6 = r12.readInt32();
                switch(r6) {
                    case -1: goto L_0x01de;
                    case 0: goto L_0x01de;
                    case 1: goto L_0x01de;
                    case 2: goto L_0x01de;
                    case 3: goto L_0x01de;
                    case 4: goto L_0x01de;
                    case 5: goto L_0x01de;
                    case 6: goto L_0x01de;
                    case 7: goto L_0x01de;
                    case 8: goto L_0x01de;
                    case 9: goto L_0x01de;
                    case 10: goto L_0x01de;
                    case 11: goto L_0x01de;
                    case 12: goto L_0x01de;
                    case 13: goto L_0x01de;
                    case 14: goto L_0x01de;
                    case 15: goto L_0x01de;
                    case 16: goto L_0x01de;
                    case 17: goto L_0x01de;
                    case 18: goto L_0x01de;
                    case 19: goto L_0x01de;
                    default: goto L_0x01d6;
                };
            L_0x01d6:
                r12.rewindToPosition(r2);
                r11.storeUnknownField(r12, r5);
                goto L_0x0001;
            L_0x01de:
                r11.targetAccessTech = r6;
                goto L_0x0001;
            L_0x01e2:
                r8 = r12.readInt64();
                r11.nitzTimestampMillis = r8;
                goto L_0x0001;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event");
            }

            public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event parseFrom(byte[] r1) throws com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event;
                r0.<init>();
                r0 = com.android.internal.telephony.protobuf.nano.MessageNano.mergeFrom(r0, r1);
                r0 = (com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event) r0;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.parseFrom(byte[]):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event");
            }

            public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r1) throws java.io.IOException {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event;
                r0.<init>();
                r0 = r0.mergeFrom(r1);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event");
            }
        }

        public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession[] emptyArray() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r0 = _emptyArray;
            if (r0 != 0) goto L_0x0011;
        L_0x0004:
            r1 = com.android.internal.telephony.protobuf.nano.InternalNano.LAZY_INIT_LOCK;
            monitor-enter(r1);
            r0 = _emptyArray;
            if (r0 != 0) goto L_0x0010;
        L_0x000b:
            r0 = 0;
            r0 = new com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession[r0];
            _emptyArray = r0;
        L_0x0010:
            monitor-exit(r1);
        L_0x0011:
            r0 = _emptyArray;
            return r0;
        L_0x0014:
            r0 = move-exception;
            monitor-exit(r1);
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.emptyArray():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession[]");
        }

        public TelephonyCallSession() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r0 = this;
            r0.<init>();
            r0.clear();
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.<init>():void");
        }

        public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession clear() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 0;
            r2.startTimeMinutes = r1;
            r2.phoneId = r1;
            r0 = com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event.emptyArray();
            r2.events = r0;
            r2.eventsDropped = r1;
            r0 = 0;
            r2.unknownFieldData = r0;
            r0 = -1;
            r2.cachedSize = r0;
            return r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.clear():com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession");
        }

        public void writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano r5) throws java.io.IOException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r2 = r4.startTimeMinutes;
            if (r2 == 0) goto L_0x000a;
        L_0x0004:
            r2 = r4.startTimeMinutes;
            r3 = 1;
            r5.writeInt32(r3, r2);
        L_0x000a:
            r2 = r4.phoneId;
            if (r2 == 0) goto L_0x0014;
        L_0x000e:
            r2 = r4.phoneId;
            r3 = 2;
            r5.writeInt32(r3, r2);
        L_0x0014:
            r2 = r4.events;
            if (r2 == 0) goto L_0x0030;
        L_0x0018:
            r2 = r4.events;
            r2 = r2.length;
            if (r2 <= 0) goto L_0x0030;
        L_0x001d:
            r1 = 0;
        L_0x001e:
            r2 = r4.events;
            r2 = r2.length;
            if (r1 >= r2) goto L_0x0030;
        L_0x0023:
            r2 = r4.events;
            r0 = r2[r1];
            if (r0 == 0) goto L_0x002d;
        L_0x0029:
            r2 = 3;
            r5.writeMessage(r2, r0);
        L_0x002d:
            r1 = r1 + 1;
            goto L_0x001e;
        L_0x0030:
            r2 = r4.eventsDropped;
            if (r2 == 0) goto L_0x003a;
        L_0x0034:
            r2 = r4.eventsDropped;
            r3 = 4;
            r5.writeBool(r3, r2);
        L_0x003a:
            super.writeTo(r5);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.writeTo(com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano):void");
        }

        protected int computeSerializedSize() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r5 = this;
            r2 = super.computeSerializedSize();
            r3 = r5.startTimeMinutes;
            if (r3 == 0) goto L_0x0010;
        L_0x0008:
            r3 = r5.startTimeMinutes;
            r4 = 1;
            r3 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r4, r3);
            r2 = r2 + r3;
        L_0x0010:
            r3 = r5.phoneId;
            if (r3 == 0) goto L_0x001c;
        L_0x0014:
            r3 = r5.phoneId;
            r4 = 2;
            r3 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeInt32Size(r4, r3);
            r2 = r2 + r3;
        L_0x001c:
            r3 = r5.events;
            if (r3 == 0) goto L_0x003a;
        L_0x0020:
            r3 = r5.events;
            r3 = r3.length;
            if (r3 <= 0) goto L_0x003a;
        L_0x0025:
            r1 = 0;
        L_0x0026:
            r3 = r5.events;
            r3 = r3.length;
            if (r1 >= r3) goto L_0x003a;
        L_0x002b:
            r3 = r5.events;
            r0 = r3[r1];
            if (r0 == 0) goto L_0x0037;
        L_0x0031:
            r3 = 3;
            r3 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeMessageSize(r3, r0);
            r2 = r2 + r3;
        L_0x0037:
            r1 = r1 + 1;
            goto L_0x0026;
        L_0x003a:
            r3 = r5.eventsDropped;
            if (r3 == 0) goto L_0x0046;
        L_0x003e:
            r3 = r5.eventsDropped;
            r4 = 4;
            r3 = com.android.internal.telephony.protobuf.nano.CodedOutputByteBufferNano.computeBoolSize(r4, r3);
            r2 = r2 + r3;
        L_0x0046:
            return r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.computeSerializedSize():int");
        }

        public com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r7) throws java.io.IOException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r5 = 0;
        L_0x0001:
            r3 = r7.readTag();
            switch(r3) {
                case 0: goto L_0x000f;
                case 8: goto L_0x0010;
                case 16: goto L_0x0017;
                case 26: goto L_0x001e;
                case 32: goto L_0x005e;
                default: goto L_0x0008;
            };
        L_0x0008:
            r4 = r6.storeUnknownField(r7, r3);
            if (r4 != 0) goto L_0x0001;
        L_0x000e:
            return r6;
        L_0x000f:
            return r6;
        L_0x0010:
            r4 = r7.readInt32();
            r6.startTimeMinutes = r4;
            goto L_0x0001;
        L_0x0017:
            r4 = r7.readInt32();
            r6.phoneId = r4;
            goto L_0x0001;
        L_0x001e:
            r4 = 26;
            r0 = com.android.internal.telephony.protobuf.nano.WireFormatNano.getRepeatedFieldArrayLength(r7, r4);
            r4 = r6.events;
            if (r4 != 0) goto L_0x004b;
        L_0x0028:
            r1 = 0;
        L_0x0029:
            r4 = r1 + r0;
            r2 = new com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.Event[r4];
            if (r1 == 0) goto L_0x0034;
        L_0x002f:
            r4 = r6.events;
            java.lang.System.arraycopy(r4, r5, r2, r5, r1);
        L_0x0034:
            r4 = r2.length;
            r4 = r4 + -1;
            if (r1 >= r4) goto L_0x004f;
        L_0x0039:
            r4 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event;
            r4.<init>();
            r2[r1] = r4;
            r4 = r2[r1];
            r7.readMessage(r4);
            r7.readTag();
            r1 = r1 + 1;
            goto L_0x0034;
        L_0x004b:
            r4 = r6.events;
            r1 = r4.length;
            goto L_0x0029;
        L_0x004f:
            r4 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession$Event;
            r4.<init>();
            r2[r1] = r4;
            r4 = r2[r1];
            r7.readMessage(r4);
            r6.events = r2;
            goto L_0x0001;
        L_0x005e:
            r4 = r7.readBool();
            r6.eventsDropped = r4;
            goto L_0x0001;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.mergeFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession");
        }

        public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession parseFrom(byte[] r1) throws com.android.internal.telephony.protobuf.nano.InvalidProtocolBufferNanoException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession;
            r0.<init>();
            r0 = com.android.internal.telephony.protobuf.nano.MessageNano.mergeFrom(r0, r1);
            r0 = (com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession) r0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.parseFrom(byte[]):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession");
        }

        public static com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano r1) throws java.io.IOException {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r0 = new com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession;
            r0.<init>();
            r0 = r0.mergeFrom(r1);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.telephony.nano.TelephonyProto.TelephonyCallSession.parseFrom(com.android.internal.telephony.protobuf.nano.CodedInputByteBufferNano):com.android.internal.telephony.nano.TelephonyProto$TelephonyCallSession");
        }
    }

    public static final class TelephonyEvent extends ExtendableMessageNano<TelephonyEvent> {
        private static volatile TelephonyEvent[] _emptyArray;
        public RilDataCall[] dataCalls;
        public int dataStallAction;
        public RilDeactivateDataCall deactivateDataCall;
        public int error;
        public ImsCapabilities imsCapabilities;
        public ImsConnectionState imsConnectionState;
        public ModemRestart modemRestart;
        public long nitzTimestampMillis;
        public int phoneId;
        public TelephonyServiceState serviceState;
        public TelephonySettings settings;
        public RilSetupDataCall setupDataCall;
        public RilSetupDataCallResponse setupDataCallResponse;
        public long timestampMillis;
        public int type;

        public static final class ModemRestart extends ExtendableMessageNano<ModemRestart> {
            private static volatile ModemRestart[] _emptyArray;
            public String basebandVersion;
            public String reason;

            public static ModemRestart[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new ModemRestart[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public ModemRestart() {
                clear();
            }

            public ModemRestart clear() {
                this.basebandVersion = "";
                this.reason = "";
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (!this.basebandVersion.equals("")) {
                    output.writeString(1, this.basebandVersion);
                }
                if (!this.reason.equals("")) {
                    output.writeString(2, this.reason);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (!this.basebandVersion.equals("")) {
                    size += CodedOutputByteBufferNano.computeStringSize(1, this.basebandVersion);
                }
                if (this.reason.equals("")) {
                    return size;
                }
                return size + CodedOutputByteBufferNano.computeStringSize(2, this.reason);
            }

            public ModemRestart mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 10:
                            this.basebandVersion = input.readString();
                            break;
                        case 18:
                            this.reason = input.readString();
                            break;
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static ModemRestart parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (ModemRestart) MessageNano.mergeFrom(new ModemRestart(), data);
            }

            public static ModemRestart parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new ModemRestart().mergeFrom(input);
            }
        }

        public static final class RilDeactivateDataCall extends ExtendableMessageNano<RilDeactivateDataCall> {
            private static volatile RilDeactivateDataCall[] _emptyArray;
            public int cid;
            public int reason;

            public interface DeactivateReason {
                public static final int DEACTIVATE_REASON_NONE = 1;
                public static final int DEACTIVATE_REASON_PDP_RESET = 3;
                public static final int DEACTIVATE_REASON_RADIO_OFF = 2;
                public static final int DEACTIVATE_REASON_UNKNOWN = 0;
            }

            public static RilDeactivateDataCall[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilDeactivateDataCall[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilDeactivateDataCall() {
                clear();
            }

            public RilDeactivateDataCall clear() {
                this.cid = 0;
                this.reason = 0;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.cid != 0) {
                    output.writeInt32(1, this.cid);
                }
                if (this.reason != 0) {
                    output.writeInt32(2, this.reason);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.cid != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.cid);
                }
                if (this.reason != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(2, this.reason);
                }
                return size;
            }

            public RilDeactivateDataCall mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            this.cid = input.readInt32();
                            break;
                        case 16:
                            int initialPos = input.getPosition();
                            int value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                    this.reason = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static RilDeactivateDataCall parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (RilDeactivateDataCall) MessageNano.mergeFrom(new RilDeactivateDataCall(), data);
            }

            public static RilDeactivateDataCall parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new RilDeactivateDataCall().mergeFrom(input);
            }
        }

        public static final class RilSetupDataCall extends ExtendableMessageNano<RilSetupDataCall> {
            private static volatile RilSetupDataCall[] _emptyArray;
            public String apn;
            public int dataProfile;
            public int rat;
            public int type;

            public interface RilDataProfile {
                public static final int RIL_DATA_PROFILE_CBS = 5;
                public static final int RIL_DATA_PROFILE_DEFAULT = 1;
                public static final int RIL_DATA_PROFILE_FOTA = 4;
                public static final int RIL_DATA_PROFILE_IMS = 3;
                public static final int RIL_DATA_PROFILE_INVALID = 7;
                public static final int RIL_DATA_PROFILE_OEM_BASE = 6;
                public static final int RIL_DATA_PROFILE_TETHERED = 2;
                public static final int RIL_DATA_UNKNOWN = 0;
            }

            public static RilSetupDataCall[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilSetupDataCall[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilSetupDataCall() {
                clear();
            }

            public RilSetupDataCall clear() {
                this.rat = -1;
                this.dataProfile = 0;
                this.apn = "";
                this.type = 0;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.rat != -1) {
                    output.writeInt32(1, this.rat);
                }
                if (this.dataProfile != 0) {
                    output.writeInt32(2, this.dataProfile);
                }
                if (!this.apn.equals("")) {
                    output.writeString(3, this.apn);
                }
                if (this.type != 0) {
                    output.writeInt32(4, this.type);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.rat != -1) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.rat);
                }
                if (this.dataProfile != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.dataProfile);
                }
                if (!this.apn.equals("")) {
                    size += CodedOutputByteBufferNano.computeStringSize(3, this.apn);
                }
                if (this.type != 0) {
                    return size + CodedOutputByteBufferNano.computeInt32Size(4, this.type);
                }
                return size;
            }

            public RilSetupDataCall mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    int initialPos;
                    int value;
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case -1:
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                case 9:
                                case 10:
                                case 11:
                                case 12:
                                case 13:
                                case 14:
                                case 15:
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                    this.rat = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 16:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    this.dataProfile = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 26:
                            this.apn = input.readString();
                            break;
                        case 32:
                            initialPos = input.getPosition();
                            value = input.readInt32();
                            switch (value) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                    this.type = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static RilSetupDataCall parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (RilSetupDataCall) MessageNano.mergeFrom(new RilSetupDataCall(), data);
            }

            public static RilSetupDataCall parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new RilSetupDataCall().mergeFrom(input);
            }
        }

        public static final class RilSetupDataCallResponse extends ExtendableMessageNano<RilSetupDataCallResponse> {
            private static volatile RilSetupDataCallResponse[] _emptyArray;
            public RilDataCall call;
            public int status;
            public int suggestedRetryTimeMillis;

            public interface RilDataCallFailCause {
                public static final int PDP_FAIL_ACTIVATION_REJECT_GGSN = 30;
                public static final int PDP_FAIL_ACTIVATION_REJECT_UNSPECIFIED = 31;
                public static final int PDP_FAIL_APN_TYPE_CONFLICT = 112;
                public static final int PDP_FAIL_AUTH_FAILURE_ON_EMERGENCY_CALL = 122;
                public static final int PDP_FAIL_COMPANION_IFACE_IN_USE = 118;
                public static final int PDP_FAIL_CONDITIONAL_IE_ERROR = 100;
                public static final int PDP_FAIL_DATA_REGISTRATION_FAIL = -2;
                public static final int PDP_FAIL_EMERGENCY_IFACE_ONLY = 116;
                public static final int PDP_FAIL_EMM_ACCESS_BARRED = 115;
                public static final int PDP_FAIL_EMM_ACCESS_BARRED_INFINITE_RETRY = 121;
                public static final int PDP_FAIL_ERROR_UNSPECIFIED = 65535;
                public static final int PDP_FAIL_ESM_INFO_NOT_RECEIVED = 53;
                public static final int PDP_FAIL_FEATURE_NOT_SUPP = 40;
                public static final int PDP_FAIL_FILTER_SEMANTIC_ERROR = 44;
                public static final int PDP_FAIL_FILTER_SYTAX_ERROR = 45;
                public static final int PDP_FAIL_IFACE_AND_POL_FAMILY_MISMATCH = 120;
                public static final int PDP_FAIL_IFACE_MISMATCH = 117;
                public static final int PDP_FAIL_INSUFFICIENT_RESOURCES = 26;
                public static final int PDP_FAIL_INTERNAL_CALL_PREEMPT_BY_HIGH_PRIO_APN = 114;
                public static final int PDP_FAIL_INVALID_MANDATORY_INFO = 96;
                public static final int PDP_FAIL_INVALID_PCSCF_ADDR = 113;
                public static final int PDP_FAIL_INVALID_TRANSACTION_ID = 81;
                public static final int PDP_FAIL_IP_ADDRESS_MISMATCH = 119;
                public static final int PDP_FAIL_LLC_SNDCP = 25;
                public static final int PDP_FAIL_MAX_ACTIVE_PDP_CONTEXT_REACHED = 65;
                public static final int PDP_FAIL_MESSAGE_INCORRECT_SEMANTIC = 95;
                public static final int PDP_FAIL_MESSAGE_TYPE_UNSUPPORTED = 97;
                public static final int PDP_FAIL_MISSING_UKNOWN_APN = 27;
                public static final int PDP_FAIL_MSG_AND_PROTOCOL_STATE_UNCOMPATIBLE = 101;
                public static final int PDP_FAIL_MSG_TYPE_NONCOMPATIBLE_STATE = 98;
                public static final int PDP_FAIL_MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED = 55;
                public static final int PDP_FAIL_NAS_SIGNALLING = 14;
                public static final int PDP_FAIL_NETWORK_FAILURE = 38;
                public static final int PDP_FAIL_NONE = 1;
                public static final int PDP_FAIL_NSAPI_IN_USE = 35;
                public static final int PDP_FAIL_ONLY_IPV4_ALLOWED = 50;
                public static final int PDP_FAIL_ONLY_IPV6_ALLOWED = 51;
                public static final int PDP_FAIL_ONLY_SINGLE_BEARER_ALLOWED = 52;
                public static final int PDP_FAIL_OPERATOR_BARRED = 8;
                public static final int PDP_FAIL_PDN_CONN_DOES_NOT_EXIST = 54;
                public static final int PDP_FAIL_PDP_WITHOUT_ACTIVE_TFT = 46;
                public static final int PDP_FAIL_PREF_RADIO_TECH_CHANGED = -4;
                public static final int PDP_FAIL_PROTOCOL_ERRORS = 111;
                public static final int PDP_FAIL_QOS_NOT_ACCEPTED = 37;
                public static final int PDP_FAIL_RADIO_POWER_OFF = -5;
                public static final int PDP_FAIL_REGULAR_DEACTIVATION = 36;
                public static final int PDP_FAIL_SERVICE_OPTION_NOT_SUBSCRIBED = 33;
                public static final int PDP_FAIL_SERVICE_OPTION_NOT_SUPPORTED = 32;
                public static final int PDP_FAIL_SERVICE_OPTION_OUT_OF_ORDER = 34;
                public static final int PDP_FAIL_SIGNAL_LOST = -3;
                public static final int PDP_FAIL_TETHERED_CALL_ACTIVE = -6;
                public static final int PDP_FAIL_TFT_SEMANTIC_ERROR = 41;
                public static final int PDP_FAIL_TFT_SYTAX_ERROR = 42;
                public static final int PDP_FAIL_UMTS_REACTIVATION_REQ = 39;
                public static final int PDP_FAIL_UNKNOWN = 0;
                public static final int PDP_FAIL_UNKNOWN_INFO_ELEMENT = 99;
                public static final int PDP_FAIL_UNKNOWN_PDP_ADDRESS_TYPE = 28;
                public static final int PDP_FAIL_UNKNOWN_PDP_CONTEXT = 43;
                public static final int PDP_FAIL_UNSUPPORTED_APN_IN_CURRENT_PLMN = 66;
                public static final int PDP_FAIL_USER_AUTHENTICATION = 29;
                public static final int PDP_FAIL_VOICE_REGISTRATION_FAIL = -1;
            }

            public static RilSetupDataCallResponse[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new RilSetupDataCallResponse[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public RilSetupDataCallResponse() {
                clear();
            }

            public RilSetupDataCallResponse clear() {
                this.status = 0;
                this.suggestedRetryTimeMillis = 0;
                this.call = null;
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (this.status != 0) {
                    output.writeInt32(1, this.status);
                }
                if (this.suggestedRetryTimeMillis != 0) {
                    output.writeInt32(2, this.suggestedRetryTimeMillis);
                }
                if (this.call != null) {
                    output.writeMessage(3, this.call);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (this.status != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(1, this.status);
                }
                if (this.suggestedRetryTimeMillis != 0) {
                    size += CodedOutputByteBufferNano.computeInt32Size(2, this.suggestedRetryTimeMillis);
                }
                if (this.call != null) {
                    return size + CodedOutputByteBufferNano.computeMessageSize(3, this.call);
                }
                return size;
            }

            public RilSetupDataCallResponse mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 8:
                            int initialPos = input.getPosition();
                            int value = input.readInt32();
                            switch (value) {
                                case -6:
                                case -5:
                                case -4:
                                case -3:
                                case -2:
                                case -1:
                                case 0:
                                case 1:
                                case 8:
                                case 14:
                                case 25:
                                case 26:
                                case 27:
                                case 28:
                                case 29:
                                case 30:
                                case 31:
                                case 32:
                                case 33:
                                case 34:
                                case 35:
                                case 36:
                                case 37:
                                case 38:
                                case 39:
                                case 40:
                                case 41:
                                case 42:
                                case 43:
                                case 44:
                                case 45:
                                case 46:
                                case 50:
                                case 51:
                                case 52:
                                case 53:
                                case 54:
                                case 55:
                                case 65:
                                case 66:
                                case 81:
                                case 95:
                                case 96:
                                case 97:
                                case 98:
                                case 99:
                                case 100:
                                case 101:
                                case 111:
                                case 112:
                                case 113:
                                case 114:
                                case 115:
                                case 116:
                                case 117:
                                case 118:
                                case 119:
                                case 120:
                                case 121:
                                case 122:
                                case 65535:
                                    this.status = value;
                                    break;
                                default:
                                    input.rewindToPosition(initialPos);
                                    storeUnknownField(input, tag);
                                    break;
                            }
                        case 16:
                            this.suggestedRetryTimeMillis = input.readInt32();
                            break;
                        case 26:
                            if (this.call == null) {
                                this.call = new RilDataCall();
                            }
                            input.readMessage(this.call);
                            break;
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static RilSetupDataCallResponse parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (RilSetupDataCallResponse) MessageNano.mergeFrom(new RilSetupDataCallResponse(), data);
            }

            public static RilSetupDataCallResponse parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new RilSetupDataCallResponse().mergeFrom(input);
            }
        }

        public interface Type {
            public static final int DATA_CALL_DEACTIVATE = 8;
            public static final int DATA_CALL_DEACTIVATE_RESPONSE = 9;
            public static final int DATA_CALL_LIST_CHANGED = 7;
            public static final int DATA_CALL_SETUP = 5;
            public static final int DATA_CALL_SETUP_RESPONSE = 6;
            public static final int DATA_STALL_ACTION = 10;
            public static final int IMS_CAPABILITIES_CHANGED = 4;
            public static final int IMS_CONNECTION_STATE_CHANGED = 3;
            public static final int MODEM_RESTART = 11;
            public static final int NITZ_TIME = 12;
            public static final int RIL_SERVICE_STATE_CHANGED = 2;
            public static final int SETTINGS_CHANGED = 1;
            public static final int UNKNOWN = 0;
        }

        public static TelephonyEvent[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyEvent[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyEvent() {
            clear();
        }

        public TelephonyEvent clear() {
            this.timestampMillis = 0;
            this.phoneId = 0;
            this.type = 0;
            this.settings = null;
            this.serviceState = null;
            this.imsConnectionState = null;
            this.imsCapabilities = null;
            this.dataCalls = RilDataCall.emptyArray();
            this.error = 0;
            this.setupDataCall = null;
            this.setupDataCallResponse = null;
            this.deactivateDataCall = null;
            this.dataStallAction = 0;
            this.modemRestart = null;
            this.nitzTimestampMillis = 0;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.timestampMillis != 0) {
                output.writeInt64(1, this.timestampMillis);
            }
            if (this.phoneId != 0) {
                output.writeInt32(2, this.phoneId);
            }
            if (this.type != 0) {
                output.writeInt32(3, this.type);
            }
            if (this.settings != null) {
                output.writeMessage(4, this.settings);
            }
            if (this.serviceState != null) {
                output.writeMessage(5, this.serviceState);
            }
            if (this.imsConnectionState != null) {
                output.writeMessage(6, this.imsConnectionState);
            }
            if (this.imsCapabilities != null) {
                output.writeMessage(7, this.imsCapabilities);
            }
            if (this.dataCalls != null && this.dataCalls.length > 0) {
                for (RilDataCall element : this.dataCalls) {
                    if (element != null) {
                        output.writeMessage(8, element);
                    }
                }
            }
            if (this.error != 0) {
                output.writeInt32(9, this.error);
            }
            if (this.setupDataCall != null) {
                output.writeMessage(10, this.setupDataCall);
            }
            if (this.setupDataCallResponse != null) {
                output.writeMessage(11, this.setupDataCallResponse);
            }
            if (this.deactivateDataCall != null) {
                output.writeMessage(12, this.deactivateDataCall);
            }
            if (this.dataStallAction != 0) {
                output.writeInt32(13, this.dataStallAction);
            }
            if (this.modemRestart != null) {
                output.writeMessage(14, this.modemRestart);
            }
            if (this.nitzTimestampMillis != 0) {
                output.writeInt64(15, this.nitzTimestampMillis);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.timestampMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(1, this.timestampMillis);
            }
            if (this.phoneId != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.phoneId);
            }
            if (this.type != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.type);
            }
            if (this.settings != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(4, this.settings);
            }
            if (this.serviceState != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(5, this.serviceState);
            }
            if (this.imsConnectionState != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(6, this.imsConnectionState);
            }
            if (this.imsCapabilities != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(7, this.imsCapabilities);
            }
            if (this.dataCalls != null && this.dataCalls.length > 0) {
                for (RilDataCall element : this.dataCalls) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(8, element);
                    }
                }
            }
            if (this.error != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(9, this.error);
            }
            if (this.setupDataCall != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(10, this.setupDataCall);
            }
            if (this.setupDataCallResponse != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(11, this.setupDataCallResponse);
            }
            if (this.deactivateDataCall != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(12, this.deactivateDataCall);
            }
            if (this.dataStallAction != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(13, this.dataStallAction);
            }
            if (this.modemRestart != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(14, this.modemRestart);
            }
            if (this.nitzTimestampMillis != 0) {
                return size + CodedOutputByteBufferNano.computeInt64Size(15, this.nitzTimestampMillis);
            }
            return size;
        }

        public TelephonyEvent mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int initialPos;
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.timestampMillis = input.readInt64();
                        break;
                    case 16:
                        this.phoneId = input.readInt32();
                        break;
                    case 24:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                                this.type = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 34:
                        if (this.settings == null) {
                            this.settings = new TelephonySettings();
                        }
                        input.readMessage(this.settings);
                        break;
                    case 42:
                        if (this.serviceState == null) {
                            this.serviceState = new TelephonyServiceState();
                        }
                        input.readMessage(this.serviceState);
                        break;
                    case 50:
                        if (this.imsConnectionState == null) {
                            this.imsConnectionState = new ImsConnectionState();
                        }
                        input.readMessage(this.imsConnectionState);
                        break;
                    case 58:
                        if (this.imsCapabilities == null) {
                            this.imsCapabilities = new ImsCapabilities();
                        }
                        input.readMessage(this.imsCapabilities);
                        break;
                    case 66:
                        int arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 66);
                        int i = this.dataCalls == null ? 0 : this.dataCalls.length;
                        RilDataCall[] newArray = new RilDataCall[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.dataCalls, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new RilDataCall();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new RilDataCall();
                        input.readMessage(newArray[i]);
                        this.dataCalls = newArray;
                        break;
                    case 72:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                            case 24:
                            case 25:
                            case 26:
                            case 27:
                            case 28:
                            case 36:
                            case 37:
                                this.error = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME /*82*/:
                        if (this.setupDataCall == null) {
                            this.setupDataCall = new RilSetupDataCall();
                        }
                        input.readMessage(this.setupDataCall);
                        break;
                    case 90:
                        if (this.setupDataCallResponse == null) {
                            this.setupDataCallResponse = new RilSetupDataCallResponse();
                        }
                        input.readMessage(this.setupDataCallResponse);
                        break;
                    case 98:
                        if (this.deactivateDataCall == null) {
                            this.deactivateDataCall = new RilDeactivateDataCall();
                        }
                        input.readMessage(this.deactivateDataCall);
                        break;
                    case AbstractPhoneBase.EVENT_ECC_NUM /*104*/:
                        this.dataStallAction = input.readInt32();
                        break;
                    case 114:
                        if (this.modemRestart == null) {
                            this.modemRestart = new ModemRestart();
                        }
                        input.readMessage(this.modemRestart);
                        break;
                    case 120:
                        this.nitzTimestampMillis = input.readInt64();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static TelephonyEvent parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (TelephonyEvent) MessageNano.mergeFrom(new TelephonyEvent(), data);
        }

        public static TelephonyEvent parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new TelephonyEvent().mergeFrom(input);
        }
    }

    public static final class TelephonyHistogram extends ExtendableMessageNano<TelephonyHistogram> {
        private static volatile TelephonyHistogram[] _emptyArray;
        public int avgTimeMillis;
        public int bucketCount;
        public int[] bucketCounters;
        public int[] bucketEndPoints;
        public int category;
        public int count;
        public int id;
        public int maxTimeMillis;
        public int minTimeMillis;

        public static TelephonyHistogram[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyHistogram[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyHistogram() {
            clear();
        }

        public TelephonyHistogram clear() {
            this.category = 0;
            this.id = 0;
            this.minTimeMillis = 0;
            this.maxTimeMillis = 0;
            this.avgTimeMillis = 0;
            this.count = 0;
            this.bucketCount = 0;
            this.bucketEndPoints = WireFormatNano.EMPTY_INT_ARRAY;
            this.bucketCounters = WireFormatNano.EMPTY_INT_ARRAY;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.category != 0) {
                output.writeInt32(1, this.category);
            }
            if (this.id != 0) {
                output.writeInt32(2, this.id);
            }
            if (this.minTimeMillis != 0) {
                output.writeInt32(3, this.minTimeMillis);
            }
            if (this.maxTimeMillis != 0) {
                output.writeInt32(4, this.maxTimeMillis);
            }
            if (this.avgTimeMillis != 0) {
                output.writeInt32(5, this.avgTimeMillis);
            }
            if (this.count != 0) {
                output.writeInt32(6, this.count);
            }
            if (this.bucketCount != 0) {
                output.writeInt32(7, this.bucketCount);
            }
            if (this.bucketEndPoints != null && this.bucketEndPoints.length > 0) {
                for (int writeInt32 : this.bucketEndPoints) {
                    output.writeInt32(8, writeInt32);
                }
            }
            if (this.bucketCounters != null && this.bucketCounters.length > 0) {
                for (int writeInt322 : this.bucketCounters) {
                    output.writeInt32(9, writeInt322);
                }
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int dataSize;
            int size = super.computeSerializedSize();
            if (this.category != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(1, this.category);
            }
            if (this.id != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(2, this.id);
            }
            if (this.minTimeMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.minTimeMillis);
            }
            if (this.maxTimeMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.maxTimeMillis);
            }
            if (this.avgTimeMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.avgTimeMillis);
            }
            if (this.count != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(6, this.count);
            }
            if (this.bucketCount != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(7, this.bucketCount);
            }
            if (this.bucketEndPoints != null && this.bucketEndPoints.length > 0) {
                dataSize = 0;
                for (int element : this.bucketEndPoints) {
                    dataSize += CodedOutputByteBufferNano.computeInt32SizeNoTag(element);
                }
                size = (size + dataSize) + (this.bucketEndPoints.length * 1);
            }
            if (this.bucketCounters == null || this.bucketCounters.length <= 0) {
                return size;
            }
            dataSize = 0;
            for (int element2 : this.bucketCounters) {
                dataSize += CodedOutputByteBufferNano.computeInt32SizeNoTag(element2);
            }
            return (size + dataSize) + (this.bucketCounters.length * 1);
        }

        public TelephonyHistogram mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int arrayLength;
                int i;
                int[] newArray;
                int limit;
                int startPos;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.category = input.readInt32();
                        break;
                    case 16:
                        this.id = input.readInt32();
                        break;
                    case 24:
                        this.minTimeMillis = input.readInt32();
                        break;
                    case 32:
                        this.maxTimeMillis = input.readInt32();
                        break;
                    case 40:
                        this.avgTimeMillis = input.readInt32();
                        break;
                    case 48:
                        this.count = input.readInt32();
                        break;
                    case 56:
                        this.bucketCount = input.readInt32();
                        break;
                    case 64:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 64);
                        i = this.bucketEndPoints == null ? 0 : this.bucketEndPoints.length;
                        newArray = new int[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.bucketEndPoints, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = input.readInt32();
                            input.readTag();
                            i++;
                        }
                        newArray[i] = input.readInt32();
                        this.bucketEndPoints = newArray;
                        break;
                    case 66:
                        limit = input.pushLimit(input.readRawVarint32());
                        arrayLength = 0;
                        startPos = input.getPosition();
                        while (input.getBytesUntilLimit() > 0) {
                            input.readInt32();
                            arrayLength++;
                        }
                        input.rewindToPosition(startPos);
                        i = this.bucketEndPoints == null ? 0 : this.bucketEndPoints.length;
                        newArray = new int[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.bucketEndPoints, 0, newArray, 0, i);
                        }
                        while (i < newArray.length) {
                            newArray[i] = input.readInt32();
                            i++;
                        }
                        this.bucketEndPoints = newArray;
                        input.popLimit(limit);
                        break;
                    case 72:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 72);
                        i = this.bucketCounters == null ? 0 : this.bucketCounters.length;
                        newArray = new int[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.bucketCounters, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = input.readInt32();
                            input.readTag();
                            i++;
                        }
                        newArray[i] = input.readInt32();
                        this.bucketCounters = newArray;
                        break;
                    case 74:
                        limit = input.pushLimit(input.readRawVarint32());
                        arrayLength = 0;
                        startPos = input.getPosition();
                        while (input.getBytesUntilLimit() > 0) {
                            input.readInt32();
                            arrayLength++;
                        }
                        input.rewindToPosition(startPos);
                        i = this.bucketCounters == null ? 0 : this.bucketCounters.length;
                        newArray = new int[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.bucketCounters, 0, newArray, 0, i);
                        }
                        while (i < newArray.length) {
                            newArray[i] = input.readInt32();
                            i++;
                        }
                        this.bucketCounters = newArray;
                        input.popLimit(limit);
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static TelephonyHistogram parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (TelephonyHistogram) MessageNano.mergeFrom(new TelephonyHistogram(), data);
        }

        public static TelephonyHistogram parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new TelephonyHistogram().mergeFrom(input);
        }
    }

    public static final class TelephonyLog extends ExtendableMessageNano<TelephonyLog> {
        private static volatile TelephonyLog[] _emptyArray;
        public TelephonyCallSession[] callSessions;
        public Time endTime;
        public TelephonyEvent[] events;
        public boolean eventsDropped;
        public TelephonyHistogram[] histograms;
        public SmsSession[] smsSessions;
        public Time startTime;

        public static TelephonyLog[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyLog[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyLog() {
            clear();
        }

        public TelephonyLog clear() {
            this.events = TelephonyEvent.emptyArray();
            this.callSessions = TelephonyCallSession.emptyArray();
            this.smsSessions = SmsSession.emptyArray();
            this.histograms = TelephonyHistogram.emptyArray();
            this.eventsDropped = false;
            this.startTime = null;
            this.endTime = null;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.events != null && this.events.length > 0) {
                for (TelephonyEvent element : this.events) {
                    if (element != null) {
                        output.writeMessage(1, element);
                    }
                }
            }
            if (this.callSessions != null && this.callSessions.length > 0) {
                for (TelephonyCallSession element2 : this.callSessions) {
                    if (element2 != null) {
                        output.writeMessage(2, element2);
                    }
                }
            }
            if (this.smsSessions != null && this.smsSessions.length > 0) {
                for (SmsSession element3 : this.smsSessions) {
                    if (element3 != null) {
                        output.writeMessage(3, element3);
                    }
                }
            }
            if (this.histograms != null && this.histograms.length > 0) {
                for (TelephonyHistogram element4 : this.histograms) {
                    if (element4 != null) {
                        output.writeMessage(4, element4);
                    }
                }
            }
            if (this.eventsDropped) {
                output.writeBool(5, this.eventsDropped);
            }
            if (this.startTime != null) {
                output.writeMessage(6, this.startTime);
            }
            if (this.endTime != null) {
                output.writeMessage(7, this.endTime);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.events != null && this.events.length > 0) {
                for (TelephonyEvent element : this.events) {
                    if (element != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(1, element);
                    }
                }
            }
            if (this.callSessions != null && this.callSessions.length > 0) {
                for (TelephonyCallSession element2 : this.callSessions) {
                    if (element2 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(2, element2);
                    }
                }
            }
            if (this.smsSessions != null && this.smsSessions.length > 0) {
                for (SmsSession element3 : this.smsSessions) {
                    if (element3 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(3, element3);
                    }
                }
            }
            if (this.histograms != null && this.histograms.length > 0) {
                for (TelephonyHistogram element4 : this.histograms) {
                    if (element4 != null) {
                        size += CodedOutputByteBufferNano.computeMessageSize(4, element4);
                    }
                }
            }
            if (this.eventsDropped) {
                size += CodedOutputByteBufferNano.computeBoolSize(5, this.eventsDropped);
            }
            if (this.startTime != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(6, this.startTime);
            }
            if (this.endTime != null) {
                return size + CodedOutputByteBufferNano.computeMessageSize(7, this.endTime);
            }
            return size;
        }

        public TelephonyLog mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int arrayLength;
                int i;
                switch (tag) {
                    case 0:
                        return this;
                    case 10:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 10);
                        i = this.events == null ? 0 : this.events.length;
                        TelephonyEvent[] newArray = new TelephonyEvent[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.events, 0, newArray, 0, i);
                        }
                        while (i < newArray.length - 1) {
                            newArray[i] = new TelephonyEvent();
                            input.readMessage(newArray[i]);
                            input.readTag();
                            i++;
                        }
                        newArray[i] = new TelephonyEvent();
                        input.readMessage(newArray[i]);
                        this.events = newArray;
                        break;
                    case 18:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 18);
                        i = this.callSessions == null ? 0 : this.callSessions.length;
                        TelephonyCallSession[] newArray2 = new TelephonyCallSession[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.callSessions, 0, newArray2, 0, i);
                        }
                        while (i < newArray2.length - 1) {
                            newArray2[i] = new TelephonyCallSession();
                            input.readMessage(newArray2[i]);
                            input.readTag();
                            i++;
                        }
                        newArray2[i] = new TelephonyCallSession();
                        input.readMessage(newArray2[i]);
                        this.callSessions = newArray2;
                        break;
                    case 26:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 26);
                        i = this.smsSessions == null ? 0 : this.smsSessions.length;
                        SmsSession[] newArray3 = new SmsSession[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.smsSessions, 0, newArray3, 0, i);
                        }
                        while (i < newArray3.length - 1) {
                            newArray3[i] = new SmsSession();
                            input.readMessage(newArray3[i]);
                            input.readTag();
                            i++;
                        }
                        newArray3[i] = new SmsSession();
                        input.readMessage(newArray3[i]);
                        this.smsSessions = newArray3;
                        break;
                    case 34:
                        arrayLength = WireFormatNano.getRepeatedFieldArrayLength(input, 34);
                        i = this.histograms == null ? 0 : this.histograms.length;
                        TelephonyHistogram[] newArray4 = new TelephonyHistogram[(i + arrayLength)];
                        if (i != 0) {
                            System.arraycopy(this.histograms, 0, newArray4, 0, i);
                        }
                        while (i < newArray4.length - 1) {
                            newArray4[i] = new TelephonyHistogram();
                            input.readMessage(newArray4[i]);
                            input.readTag();
                            i++;
                        }
                        newArray4[i] = new TelephonyHistogram();
                        input.readMessage(newArray4[i]);
                        this.histograms = newArray4;
                        break;
                    case 40:
                        this.eventsDropped = input.readBool();
                        break;
                    case 50:
                        if (this.startTime == null) {
                            this.startTime = new Time();
                        }
                        input.readMessage(this.startTime);
                        break;
                    case 58:
                        if (this.endTime == null) {
                            this.endTime = new Time();
                        }
                        input.readMessage(this.endTime);
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static TelephonyLog parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (TelephonyLog) MessageNano.mergeFrom(new TelephonyLog(), data);
        }

        public static TelephonyLog parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new TelephonyLog().mergeFrom(input);
        }
    }

    public static final class TelephonyServiceState extends ExtendableMessageNano<TelephonyServiceState> {
        private static volatile TelephonyServiceState[] _emptyArray;
        public TelephonyOperator dataOperator;
        public int dataRat;
        public int dataRoamingType;
        public TelephonyOperator voiceOperator;
        public int voiceRat;
        public int voiceRoamingType;

        public interface RoamingType {
            public static final int ROAMING_TYPE_DOMESTIC = 2;
            public static final int ROAMING_TYPE_INTERNATIONAL = 3;
            public static final int ROAMING_TYPE_NOT_ROAMING = 0;
            public static final int ROAMING_TYPE_UNKNOWN = 1;
            public static final int UNKNOWN = -1;
        }

        public static final class TelephonyOperator extends ExtendableMessageNano<TelephonyOperator> {
            private static volatile TelephonyOperator[] _emptyArray;
            public String alphaLong;
            public String alphaShort;
            public String numeric;

            public static TelephonyOperator[] emptyArray() {
                if (_emptyArray == null) {
                    synchronized (InternalNano.LAZY_INIT_LOCK) {
                        if (_emptyArray == null) {
                            _emptyArray = new TelephonyOperator[0];
                        }
                    }
                }
                return _emptyArray;
            }

            public TelephonyOperator() {
                clear();
            }

            public TelephonyOperator clear() {
                this.alphaLong = "";
                this.alphaShort = "";
                this.numeric = "";
                this.unknownFieldData = null;
                this.cachedSize = -1;
                return this;
            }

            public void writeTo(CodedOutputByteBufferNano output) throws IOException {
                if (!this.alphaLong.equals("")) {
                    output.writeString(1, this.alphaLong);
                }
                if (!this.alphaShort.equals("")) {
                    output.writeString(2, this.alphaShort);
                }
                if (!this.numeric.equals("")) {
                    output.writeString(3, this.numeric);
                }
                super.writeTo(output);
            }

            protected int computeSerializedSize() {
                int size = super.computeSerializedSize();
                if (!this.alphaLong.equals("")) {
                    size += CodedOutputByteBufferNano.computeStringSize(1, this.alphaLong);
                }
                if (!this.alphaShort.equals("")) {
                    size += CodedOutputByteBufferNano.computeStringSize(2, this.alphaShort);
                }
                if (this.numeric.equals("")) {
                    return size;
                }
                return size + CodedOutputByteBufferNano.computeStringSize(3, this.numeric);
            }

            public TelephonyOperator mergeFrom(CodedInputByteBufferNano input) throws IOException {
                while (true) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            return this;
                        case 10:
                            this.alphaLong = input.readString();
                            break;
                        case 18:
                            this.alphaShort = input.readString();
                            break;
                        case 26:
                            this.numeric = input.readString();
                            break;
                        default:
                            if (storeUnknownField(input, tag)) {
                                break;
                            }
                            return this;
                    }
                }
            }

            public static TelephonyOperator parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
                return (TelephonyOperator) MessageNano.mergeFrom(new TelephonyOperator(), data);
            }

            public static TelephonyOperator parseFrom(CodedInputByteBufferNano input) throws IOException {
                return new TelephonyOperator().mergeFrom(input);
            }
        }

        public static TelephonyServiceState[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonyServiceState[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonyServiceState() {
            clear();
        }

        public TelephonyServiceState clear() {
            this.voiceOperator = null;
            this.dataOperator = null;
            this.voiceRoamingType = -1;
            this.dataRoamingType = -1;
            this.voiceRat = -1;
            this.dataRat = -1;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.voiceOperator != null) {
                output.writeMessage(1, this.voiceOperator);
            }
            if (this.dataOperator != null) {
                output.writeMessage(2, this.dataOperator);
            }
            if (this.voiceRoamingType != -1) {
                output.writeInt32(3, this.voiceRoamingType);
            }
            if (this.dataRoamingType != -1) {
                output.writeInt32(4, this.dataRoamingType);
            }
            if (this.voiceRat != -1) {
                output.writeInt32(5, this.voiceRat);
            }
            if (this.dataRat != -1) {
                output.writeInt32(6, this.dataRat);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.voiceOperator != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(1, this.voiceOperator);
            }
            if (this.dataOperator != null) {
                size += CodedOutputByteBufferNano.computeMessageSize(2, this.dataOperator);
            }
            if (this.voiceRoamingType != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(3, this.voiceRoamingType);
            }
            if (this.dataRoamingType != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.dataRoamingType);
            }
            if (this.voiceRat != -1) {
                size += CodedOutputByteBufferNano.computeInt32Size(5, this.voiceRat);
            }
            if (this.dataRat != -1) {
                return size + CodedOutputByteBufferNano.computeInt32Size(6, this.dataRat);
            }
            return size;
        }

        public TelephonyServiceState mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int initialPos;
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 10:
                        if (this.voiceOperator == null) {
                            this.voiceOperator = new TelephonyOperator();
                        }
                        input.readMessage(this.voiceOperator);
                        break;
                    case 18:
                        if (this.dataOperator == null) {
                            this.dataOperator = new TelephonyOperator();
                        }
                        input.readMessage(this.dataOperator);
                        break;
                    case 24:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case -1:
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.voiceRoamingType = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 32:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case -1:
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.dataRoamingType = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 40:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case -1:
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                                this.voiceRat = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 48:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case -1:
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                                this.dataRat = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static TelephonyServiceState parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (TelephonyServiceState) MessageNano.mergeFrom(new TelephonyServiceState(), data);
        }

        public static TelephonyServiceState parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new TelephonyServiceState().mergeFrom(input);
        }
    }

    public static final class TelephonySettings extends ExtendableMessageNano<TelephonySettings> {
        private static volatile TelephonySettings[] _emptyArray;
        public boolean isAirplaneMode;
        public boolean isCellularDataEnabled;
        public boolean isDataRoamingEnabled;
        public boolean isEnhanced4GLteModeEnabled;
        public boolean isVtOverLteEnabled;
        public boolean isVtOverWifiEnabled;
        public boolean isWifiCallingEnabled;
        public boolean isWifiEnabled;
        public int preferredNetworkMode;
        public int wifiCallingMode;

        public interface RilNetworkMode {
            public static final int NETWORK_MODE_CDMA = 5;
            public static final int NETWORK_MODE_CDMA_NO_EVDO = 6;
            public static final int NETWORK_MODE_EVDO_NO_CDMA = 7;
            public static final int NETWORK_MODE_GLOBAL = 8;
            public static final int NETWORK_MODE_GSM_ONLY = 2;
            public static final int NETWORK_MODE_GSM_UMTS = 4;
            public static final int NETWORK_MODE_LTE_CDMA_EVDO = 9;
            public static final int NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = 11;
            public static final int NETWORK_MODE_LTE_GSM_WCDMA = 10;
            public static final int NETWORK_MODE_LTE_ONLY = 12;
            public static final int NETWORK_MODE_LTE_TDSCDMA = 16;
            public static final int NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 23;
            public static final int NETWORK_MODE_LTE_TDSCDMA_GSM = 18;
            public static final int NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA = 21;
            public static final int NETWORK_MODE_LTE_TDSCDMA_WCDMA = 20;
            public static final int NETWORK_MODE_LTE_WCDMA = 13;
            public static final int NETWORK_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA = 22;
            public static final int NETWORK_MODE_TDSCDMA_GSM = 17;
            public static final int NETWORK_MODE_TDSCDMA_GSM_WCDMA = 19;
            public static final int NETWORK_MODE_TDSCDMA_ONLY = 14;
            public static final int NETWORK_MODE_TDSCDMA_WCDMA = 15;
            public static final int NETWORK_MODE_UNKNOWN = 0;
            public static final int NETWORK_MODE_WCDMA_ONLY = 3;
            public static final int NETWORK_MODE_WCDMA_PREF = 1;
        }

        public interface WiFiCallingMode {
            public static final int WFC_MODE_CELLULAR_PREFERRED = 2;
            public static final int WFC_MODE_UNKNOWN = 0;
            public static final int WFC_MODE_WIFI_ONLY = 1;
            public static final int WFC_MODE_WIFI_PREFERRED = 3;
        }

        public static TelephonySettings[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new TelephonySettings[0];
                    }
                }
            }
            return _emptyArray;
        }

        public TelephonySettings() {
            clear();
        }

        public TelephonySettings clear() {
            this.isAirplaneMode = false;
            this.isCellularDataEnabled = false;
            this.isDataRoamingEnabled = false;
            this.preferredNetworkMode = 0;
            this.isEnhanced4GLteModeEnabled = false;
            this.isWifiEnabled = false;
            this.isWifiCallingEnabled = false;
            this.wifiCallingMode = 0;
            this.isVtOverLteEnabled = false;
            this.isVtOverWifiEnabled = false;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.isAirplaneMode) {
                output.writeBool(1, this.isAirplaneMode);
            }
            if (this.isCellularDataEnabled) {
                output.writeBool(2, this.isCellularDataEnabled);
            }
            if (this.isDataRoamingEnabled) {
                output.writeBool(3, this.isDataRoamingEnabled);
            }
            if (this.preferredNetworkMode != 0) {
                output.writeInt32(4, this.preferredNetworkMode);
            }
            if (this.isEnhanced4GLteModeEnabled) {
                output.writeBool(5, this.isEnhanced4GLteModeEnabled);
            }
            if (this.isWifiEnabled) {
                output.writeBool(6, this.isWifiEnabled);
            }
            if (this.isWifiCallingEnabled) {
                output.writeBool(7, this.isWifiCallingEnabled);
            }
            if (this.wifiCallingMode != 0) {
                output.writeInt32(8, this.wifiCallingMode);
            }
            if (this.isVtOverLteEnabled) {
                output.writeBool(9, this.isVtOverLteEnabled);
            }
            if (this.isVtOverWifiEnabled) {
                output.writeBool(10, this.isVtOverWifiEnabled);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.isAirplaneMode) {
                size += CodedOutputByteBufferNano.computeBoolSize(1, this.isAirplaneMode);
            }
            if (this.isCellularDataEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(2, this.isCellularDataEnabled);
            }
            if (this.isDataRoamingEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(3, this.isDataRoamingEnabled);
            }
            if (this.preferredNetworkMode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(4, this.preferredNetworkMode);
            }
            if (this.isEnhanced4GLteModeEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(5, this.isEnhanced4GLteModeEnabled);
            }
            if (this.isWifiEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(6, this.isWifiEnabled);
            }
            if (this.isWifiCallingEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(7, this.isWifiCallingEnabled);
            }
            if (this.wifiCallingMode != 0) {
                size += CodedOutputByteBufferNano.computeInt32Size(8, this.wifiCallingMode);
            }
            if (this.isVtOverLteEnabled) {
                size += CodedOutputByteBufferNano.computeBoolSize(9, this.isVtOverLteEnabled);
            }
            if (this.isVtOverWifiEnabled) {
                return size + CodedOutputByteBufferNano.computeBoolSize(10, this.isVtOverWifiEnabled);
            }
            return size;
        }

        public TelephonySettings mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                int initialPos;
                int value;
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.isAirplaneMode = input.readBool();
                        break;
                    case 16:
                        this.isCellularDataEnabled = input.readBool();
                        break;
                    case 24:
                        this.isDataRoamingEnabled = input.readBool();
                        break;
                    case 32:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                            case 7:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                            case 12:
                            case 13:
                            case 14:
                            case 15:
                            case 16:
                            case 17:
                            case 18:
                            case 19:
                            case 20:
                            case 21:
                            case 22:
                            case 23:
                                this.preferredNetworkMode = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 40:
                        this.isEnhanced4GLteModeEnabled = input.readBool();
                        break;
                    case 48:
                        this.isWifiEnabled = input.readBool();
                        break;
                    case 56:
                        this.isWifiCallingEnabled = input.readBool();
                        break;
                    case 64:
                        initialPos = input.getPosition();
                        value = input.readInt32();
                        switch (value) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                                this.wifiCallingMode = value;
                                break;
                            default:
                                input.rewindToPosition(initialPos);
                                storeUnknownField(input, tag);
                                break;
                        }
                    case 72:
                        this.isVtOverLteEnabled = input.readBool();
                        break;
                    case RadioNVItems.RIL_NV_LTE_NEXT_SCAN /*80*/:
                        this.isVtOverWifiEnabled = input.readBool();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static TelephonySettings parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (TelephonySettings) MessageNano.mergeFrom(new TelephonySettings(), data);
        }

        public static TelephonySettings parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new TelephonySettings().mergeFrom(input);
        }
    }

    public static final class Time extends ExtendableMessageNano<Time> {
        private static volatile Time[] _emptyArray;
        public long elapsedTimestampMillis;
        public long systemTimestampMillis;

        public static Time[] emptyArray() {
            if (_emptyArray == null) {
                synchronized (InternalNano.LAZY_INIT_LOCK) {
                    if (_emptyArray == null) {
                        _emptyArray = new Time[0];
                    }
                }
            }
            return _emptyArray;
        }

        public Time() {
            clear();
        }

        public Time clear() {
            this.systemTimestampMillis = 0;
            this.elapsedTimestampMillis = 0;
            this.unknownFieldData = null;
            this.cachedSize = -1;
            return this;
        }

        public void writeTo(CodedOutputByteBufferNano output) throws IOException {
            if (this.systemTimestampMillis != 0) {
                output.writeInt64(1, this.systemTimestampMillis);
            }
            if (this.elapsedTimestampMillis != 0) {
                output.writeInt64(2, this.elapsedTimestampMillis);
            }
            super.writeTo(output);
        }

        protected int computeSerializedSize() {
            int size = super.computeSerializedSize();
            if (this.systemTimestampMillis != 0) {
                size += CodedOutputByteBufferNano.computeInt64Size(1, this.systemTimestampMillis);
            }
            if (this.elapsedTimestampMillis != 0) {
                return size + CodedOutputByteBufferNano.computeInt64Size(2, this.elapsedTimestampMillis);
            }
            return size;
        }

        public Time mergeFrom(CodedInputByteBufferNano input) throws IOException {
            while (true) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        return this;
                    case 8:
                        this.systemTimestampMillis = input.readInt64();
                        break;
                    case 16:
                        this.elapsedTimestampMillis = input.readInt64();
                        break;
                    default:
                        if (storeUnknownField(input, tag)) {
                            break;
                        }
                        return this;
                }
            }
        }

        public static Time parseFrom(byte[] data) throws InvalidProtocolBufferNanoException {
            return (Time) MessageNano.mergeFrom(new Time(), data);
        }

        public static Time parseFrom(CodedInputByteBufferNano input) throws IOException {
            return new Time().mergeFrom(input);
        }
    }

    public interface TimeInterval {
        public static final int TI_100_MILLIS = 4;
        public static final int TI_10_MILLIS = 1;
        public static final int TI_10_MINUTES = 14;
        public static final int TI_10_SEC = 10;
        public static final int TI_1_HOUR = 16;
        public static final int TI_1_MINUTE = 12;
        public static final int TI_1_SEC = 7;
        public static final int TI_200_MILLIS = 5;
        public static final int TI_20_MILLIS = 2;
        public static final int TI_2_HOURS = 17;
        public static final int TI_2_SEC = 8;
        public static final int TI_30_MINUTES = 15;
        public static final int TI_30_SEC = 11;
        public static final int TI_3_MINUTES = 13;
        public static final int TI_4_HOURS = 18;
        public static final int TI_500_MILLIS = 6;
        public static final int TI_50_MILLIS = 3;
        public static final int TI_5_SEC = 9;
        public static final int TI_MANY_HOURS = 19;
        public static final int TI_UNKNOWN = 0;
    }
}
