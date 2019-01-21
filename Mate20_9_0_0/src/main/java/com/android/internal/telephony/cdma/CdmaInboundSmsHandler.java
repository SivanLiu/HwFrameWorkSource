package com.android.internal.telephony.cdma;

import android.content.Context;
import android.content.res.Resources;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.SmsCbMessage;
import com.android.internal.telephony.CellBroadcastHandler;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.InboundSmsHandler;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsConstants.MessageClass;
import com.android.internal.telephony.SmsMessageBase;
import com.android.internal.telephony.SmsStorageMonitor;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.util.HexDump;
import java.util.Arrays;

public class CdmaInboundSmsHandler extends InboundSmsHandler {
    private final boolean mCheckForDuplicatePortsInOmadmWapPush = Resources.getSystem().getBoolean(17956944);
    private byte[] mLastAcknowledgedSmsFingerprint;
    private byte[] mLastDispatchedSmsFingerprint;
    private final CdmaServiceCategoryProgramHandler mServiceCategoryProgramHandler;
    private final CdmaSMSDispatcher mSmsDispatcher;

    private CdmaInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone, CdmaSMSDispatcher smsDispatcher) {
        super("CdmaInboundSmsHandler", context, storageMonitor, phone, CellBroadcastHandler.makeCellBroadcastHandler(context, phone));
        this.mSmsDispatcher = smsDispatcher;
        this.mServiceCategoryProgramHandler = CdmaServiceCategoryProgramHandler.makeScpHandler(context, phone.mCi);
        phone.mCi.setOnNewCdmaSms(getHandler(), 1, null);
    }

    protected void onQuitting() {
        this.mPhone.mCi.unSetOnNewCdmaSms(getHandler());
        this.mCellBroadcastHandler.dispose();
        log("unregistered for 3GPP2 SMS");
        super.onQuitting();
    }

    public static CdmaInboundSmsHandler makeInboundSmsHandler(Context context, SmsStorageMonitor storageMonitor, Phone phone, CdmaSMSDispatcher smsDispatcher) {
        CdmaInboundSmsHandler handler = new CdmaInboundSmsHandler(context, storageMonitor, phone, smsDispatcher);
        handler.start();
        return handler;
    }

    protected boolean is3gpp2() {
        return true;
    }

    protected int dispatchMessageRadioSpecific(SmsMessageBase smsb) {
        SmsMessage sms = (SmsMessage) smsb;
        if (1 == sms.getMessageType()) {
            log("Broadcast type message");
            SmsCbMessage cbMessage = sms.parseBroadcastSms();
            if (cbMessage != null) {
                this.mCellBroadcastHandler.dispatchSmsMessage(cbMessage);
            } else {
                loge("error trying to parse broadcast SMS");
            }
            return 1;
        }
        this.mLastDispatchedSmsFingerprint = sms.getIncomingSmsFingerprint();
        if (this.mLastAcknowledgedSmsFingerprint == null || !Arrays.equals(this.mLastDispatchedSmsFingerprint, this.mLastAcknowledgedSmsFingerprint)) {
            sms.parseSms();
            int teleService = sms.getTeleService();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("teleService: 0x");
            stringBuilder.append(Integer.toHexString(teleService));
            log(stringBuilder.toString());
            if (teleService != 65002) {
                if (teleService != 65005) {
                    if (teleService != InboundSmsTracker.DEST_PORT_FLAG_3GPP2) {
                        switch (teleService) {
                            case 4098:
                            case 4101:
                                if (sms.isStatusReportMessage()) {
                                    this.mSmsDispatcher.sendStatusReportMessage(sms);
                                    return 1;
                                }
                                break;
                            case 4099:
                                break;
                            case 4100:
                                break;
                            case 4102:
                                this.mServiceCategoryProgramHandler.dispatchSmsMessage(sms);
                                return 1;
                            default:
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("unsupported teleservice 0x");
                                stringBuilder2.append(Integer.toHexString(teleService));
                                loge(stringBuilder2.toString());
                                return 4;
                        }
                    }
                    handleVoicemailTeleservice(sms);
                    return 1;
                } else if ((92 == SystemProperties.getInt("ro.config.hw_opta", 0) || ServiceStateTracker.CS_NOTIFICATION == SystemProperties.getInt("ro.config.hw_opta", 0)) && 156 == SystemProperties.getInt("ro.config.hw_optb", 0)) {
                    log("CT's AutoRegSms notification!");
                    this.mSmsDispatcher.dispatchCTAutoRegSmsPdus(sms);
                    return 1;
                }
            }
            if (!this.mStorageMonitor.isStorageAvailable() && sms.getMessageClass() != MessageClass.CLASS_0) {
                log("No storage available, return.");
                return 3;
            } else if (4100 == teleService) {
                return processCdmaWapPdu(sms.getUserData(), sms.mMessageRef, sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), sms.getTimestampMillis());
            } else if (65002 != teleService || !HwTelephonyFactory.getHwInnerSmsManager().currentSubIsChinaTelecomSim(this.mPhone.getPhoneId())) {
                return dispatchNormalMessage(smsb);
            } else {
                log("CT's MMS notification");
                BearerData mCTBearerData = BearerData.decode(sms.getUserData());
                if (mCTBearerData == null) {
                    log("Decode user data failed");
                    return 1;
                }
                return processCdmaWapPdu(mCTBearerData.userData.payload, 31, sms.getOriginatingAddress(), sms.getDisplayOriginatingAddress(), sms.getTimestampMillis());
            }
        }
        log("Receives network duplicate SMS by fingerprint, return.");
        return 1;
    }

    protected void acknowledgeLastIncomingSms(boolean success, int result, Message response) {
        int causeCode = resultToCause(result);
        this.mPhone.mCi.acknowledgeLastIncomingCdmaSms(success, causeCode, response);
        if (causeCode == 0) {
            this.mLastAcknowledgedSmsFingerprint = this.mLastDispatchedSmsFingerprint;
        }
        this.mLastDispatchedSmsFingerprint = null;
    }

    protected void onUpdatePhoneObject(Phone phone) {
        super.onUpdatePhoneObject(phone);
        this.mCellBroadcastHandler.updatePhoneObject(phone);
    }

    private static int resultToCause(int rc) {
        if (rc == -1 || rc == 1) {
            return 0;
        }
        switch (rc) {
            case 3:
                return 35;
            case 4:
                return 4;
            default:
                return 39;
        }
    }

    private void handleVoicemailTeleservice(SmsMessage sms) {
        int voicemailCount = sms.getNumOfVoicemails();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Voicemail count=");
        stringBuilder.append(voicemailCount);
        log(stringBuilder.toString());
        if (voicemailCount < 0) {
            voicemailCount = -1;
        } else if (voicemailCount > 99) {
            voicemailCount = 99;
        }
        this.mPhone.setVoiceMessageCount(voicemailCount);
    }

    private int processCdmaWapPdu(byte[] pdu, int referenceNumber, String address, String dispAddr, long timestamp) {
        byte[] bArr = pdu;
        int index = 0 + 1;
        int msgType = bArr[0] & 255;
        if (msgType != 0) {
            log("Received a WAP SMS which is not WDP. Discard.");
            return 1;
        }
        int index2 = index + 1;
        index = bArr[index] & 255;
        int index3 = index2 + 1;
        index2 = bArr[index2] & 255;
        if (index2 >= index) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WDP bad segment #");
            stringBuilder.append(index2);
            stringBuilder.append(" expecting 0-");
            stringBuilder.append(index - 1);
            loge(stringBuilder.toString());
            return 1;
        }
        int destinationPort;
        int index4;
        int sourcePort = 0;
        if (index2 == 0) {
            int index5 = index3 + 1;
            sourcePort = (bArr[index3] & 255) << 8;
            index3 = index5 + 1;
            sourcePort |= bArr[index5] & 255;
            index5 = index3 + 1;
            int destinationPort2 = index5 + 1;
            destinationPort = (255 & bArr[index5]) | ((bArr[index3] & 255) << 8);
            index4 = (this.mCheckForDuplicatePortsInOmadmWapPush && checkDuplicatePortOmadmWapPush(bArr, destinationPort2)) ? destinationPort2 + 4 : destinationPort2;
        } else {
            index4 = index3;
            destinationPort = 0;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Received WAP PDU. Type = ");
        stringBuilder2.append(msgType);
        stringBuilder2.append(", originator = ");
        String str = address;
        stringBuilder2.append(str);
        stringBuilder2.append(", src-port = ");
        stringBuilder2.append(sourcePort);
        stringBuilder2.append(", dst-port = ");
        stringBuilder2.append(destinationPort);
        stringBuilder2.append(", ID = ");
        stringBuilder2.append(referenceNumber);
        stringBuilder2.append(", segment# = ");
        stringBuilder2.append(index2);
        stringBuilder2.append('/');
        stringBuilder2.append(index);
        log(stringBuilder2.toString());
        byte[] userData = new byte[(bArr.length - index4)];
        System.arraycopy(bArr, index4, userData, 0, bArr.length - index4);
        return addTrackerToRawTableAndSendMessage(TelephonyComponentFactory.getInstance().makeInboundSmsTracker(userData, timestamp, destinationPort, true, str, dispAddr, referenceNumber, index2, index, true, HexDump.toHexString(userData)), false);
    }

    private static boolean checkDuplicatePortOmadmWapPush(byte[] origPdu, int index) {
        index += 4;
        byte[] omaPdu = new byte[(origPdu.length - index)];
        System.arraycopy(origPdu, index, omaPdu, 0, omaPdu.length);
        WspTypeDecoder pduDecoder = HwTelephonyFactory.getHwInnerSmsManager().createHwWspTypeDecoder(omaPdu);
        if (!pduDecoder.decodeUintvarInteger(2) || !pduDecoder.decodeContentType(2 + pduDecoder.getDecodedDataLength())) {
            return false;
        }
        return WspTypeDecoder.CONTENT_TYPE_B_PUSH_SYNCML_NOTI.equals(pduDecoder.getValueString());
    }
}
