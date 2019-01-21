package com.android.internal.telephony.uicc.euicc;

import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Registrant;
import android.os.RegistrantList;
import android.service.carrier.CarrierIdentifier;
import android.service.euicc.EuiccProfileInfo;
import android.service.euicc.EuiccProfileInfo.Builder;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.UiccAccessRule;
import android.telephony.euicc.EuiccNotification;
import android.telephony.euicc.EuiccRulesAuthTable;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.asn1.Asn1Decoder;
import com.android.internal.telephony.uicc.asn1.Asn1Node;
import com.android.internal.telephony.uicc.asn1.InvalidAsn1DataException;
import com.android.internal.telephony.uicc.asn1.TagNotFoundException;
import com.android.internal.telephony.uicc.euicc.apdu.ApduException;
import com.android.internal.telephony.uicc.euicc.apdu.ApduSender;
import com.android.internal.telephony.uicc.euicc.apdu.RequestBuilder;
import com.android.internal.telephony.uicc.euicc.apdu.RequestProvider;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultHelper;
import com.google.android.mms.pdu.PduHeaders;
import java.util.Arrays;
import java.util.List;

public class EuiccCard extends UiccCard {
    private static final int APDU_ERROR_SIM_REFRESH = 28416;
    private static final int CODE_NOTHING_TO_DELETE = 1;
    private static final int CODE_NO_RESULT_AVAILABLE = 1;
    private static final int CODE_OK = 0;
    private static final int CODE_PROFILE_NOT_IN_EXPECTED_STATE = 2;
    private static final boolean DBG = true;
    private static final String DEV_CAP_CDMA_1X = "cdma1x";
    private static final String DEV_CAP_CRL = "crl";
    private static final String DEV_CAP_EHRPD = "ehrpd";
    private static final String DEV_CAP_EUTRAN = "eutran";
    private static final String DEV_CAP_GSM = "gsm";
    private static final String DEV_CAP_HRPD = "hrpd";
    private static final String DEV_CAP_NFC = "nfc";
    private static final String DEV_CAP_UTRAN = "utran";
    private static final int ICCID_LENGTH = 20;
    private static final String ISD_R_AID = "A0000005591010FFFFFFFF8900000100";
    private static final String LOG_TAG = "EuiccCard";
    private static final EuiccSpecVersion SGP_2_0 = new EuiccSpecVersion(2, 0, 0);
    private final ApduSender mApduSender;
    private volatile String mEid;
    private RegistrantList mEidReadyRegistrants;
    private final Object mLock = new Object();
    private EuiccSpecVersion mSpecVersion;

    private interface ApduExceptionHandler {
        void handleException(Throwable th);
    }

    private interface ApduRequestBuilder {
        void build(RequestBuilder requestBuilder) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException;
    }

    private interface ApduResponseHandler<T> {
        T handleResult(byte[] bArr) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException;
    }

    public EuiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, Object lock) {
        super(c, ci, ics, phoneId, lock);
        this.mApduSender = new ApduSender(ci, ISD_R_AID, false);
        loadEidAndNotifyRegistrants();
    }

    public void registerForEidReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        if (this.mEid != null) {
            r.notifyRegistrant(new AsyncResult(null, null, null));
            return;
        }
        if (this.mEidReadyRegistrants == null) {
            this.mEidReadyRegistrants = new RegistrantList();
        }
        this.mEidReadyRegistrants.add(r);
    }

    public void unregisterForEidReady(Handler h) {
        if (this.mEidReadyRegistrants != null) {
            this.mEidReadyRegistrants.remove(h);
        }
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected void loadEidAndNotifyRegistrants() {
        getEid(new AsyncResultCallback<String>() {
            public void onResult(String result) {
                if (EuiccCard.this.mEidReadyRegistrants != null) {
                    EuiccCard.this.mEidReadyRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                }
            }

            public void onException(Throwable e) {
                if (EuiccCard.this.mEidReadyRegistrants != null) {
                    EuiccCard.this.mEidReadyRegistrants.notifyRegistrants(new AsyncResult(null, null, null));
                }
                EuiccCard.this.mEid = "";
                EuiccCard.this.mCardId = "";
                Rlog.e(EuiccCard.LOG_TAG, "Failed loading eid", e);
            }
        }, new Handler());
    }

    public void getSpecVersion(AsyncResultCallback<EuiccSpecVersion> callback, Handler handler) {
        if (this.mSpecVersion != null) {
            AsyncResultHelper.returnResult(this.mSpecVersion, callback, handler);
        } else {
            sendApdu(newRequestProvider(-$$Lambda$EuiccCard$HgCDP54gCppk81aqhuCG0YGJWEc.INSTANCE), new -$$Lambda$EuiccCard$U1ORE3W_o_HdXWc6N59UnRQmLQI(this), callback, handler);
        }
    }

    static /* synthetic */ void lambda$getSpecVersion$0(RequestBuilder requestBuilder) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
    }

    protected void updateCardId() {
        if (TextUtils.isEmpty(this.mEid)) {
            super.updateCardId();
        } else {
            this.mCardId = this.mEid;
        }
    }

    public void getAllProfiles(AsyncResultCallback<EuiccProfileInfo[]> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$toN63DWLt72dzp0WCl28UOMSmzE.INSTANCE), -$$Lambda$EuiccCard$B99bQ-FkeD9OwB8_qTcKScitlrM.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccProfileInfo[] lambda$getAllProfiles$3(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        List<Asn1Node> profileNodes = new Asn1Decoder(response).nextNode().getChild(160, new int[0]).getChildren(227);
        int size = profileNodes.size();
        EuiccProfileInfo[] profiles = new EuiccProfileInfo[size];
        int profileCount = 0;
        for (int i = 0; i < size; i++) {
            Asn1Node profileNode = (Asn1Node) profileNodes.get(i);
            if (profileNode.hasChild(90, new int[0])) {
                Builder profileBuilder = new Builder(stripTrailingFs(profileNode.getChild(90, new int[0]).asBytes()));
                buildProfile(profileNode, profileBuilder);
                int profileCount2 = profileCount + 1;
                profiles[profileCount] = profileBuilder.build();
                profileCount = profileCount2;
            } else {
                loge("Profile must have an ICCID.");
            }
        }
        return profiles;
    }

    public final void getProfile(String iccid, AsyncResultCallback<EuiccProfileInfo> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$QGtQZCF6KEnI-x59_tp1eo8mWew(iccid)), -$$Lambda$EuiccCard$TTvsStUIyUFrPpvGTlsjBCy3NyM.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccProfileInfo lambda$getProfile$5(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        List<Asn1Node> profileNodes = new Asn1Decoder(response).nextNode().getChild(160, new int[0]).getChildren(227);
        if (profileNodes.isEmpty()) {
            return null;
        }
        Asn1Node profileNode = (Asn1Node) profileNodes.get(0);
        Builder profileBuilder = new Builder(stripTrailingFs(profileNode.getChild(90, new int[0]).asBytes()));
        buildProfile(profileNode, profileBuilder);
        return profileBuilder.build();
    }

    public void disableProfile(String iccid, boolean refresh, AsyncResultCallback<Void> callback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new -$$Lambda$EuiccCard$0N6_V0pqmnTfKxVMU5IUj_svXDA(iccid, refresh)), new -$$Lambda$EuiccCard$Rc41c7zRLip3RrHuKqZ-Sv7h8wI(iccid), callback, handler);
    }

    static /* synthetic */ Void lambda$disableProfile$7(String iccid, byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0) {
            return null;
        }
        if (result == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Profile is already disabled, iccid: ");
            stringBuilder.append(SubscriptionInfo.givePrintableIccid(iccid));
            logd(stringBuilder.toString());
            return null;
        }
        throw new EuiccCardErrorException(11, result);
    }

    public void switchToProfile(String iccid, boolean refresh, AsyncResultCallback<Void> callback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new -$$Lambda$EuiccCard$AYHfF2w_VlO00s9p-djcPJl_1no(iccid, refresh)), new -$$Lambda$EuiccCard$fcz5l0a6JlSxs8MXCst7wXG4bUc(iccid), callback, handler);
    }

    static /* synthetic */ Void lambda$switchToProfile$9(String iccid, byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0) {
            return null;
        }
        if (result == 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Profile is already enabled, iccid: ");
            stringBuilder.append(SubscriptionInfo.givePrintableIccid(iccid));
            logd(stringBuilder.toString());
            return null;
        }
        throw new EuiccCardErrorException(10, result);
    }

    public String getEid() {
        return this.mEid;
    }

    public void getEid(AsyncResultCallback<String> callback, Handler handler) {
        if (this.mEid != null) {
            AsyncResultHelper.returnResult(this.mEid, callback, handler);
        } else {
            sendApdu(newRequestProvider(-$$Lambda$EuiccCard$HBn5KBGylwjLqIEm3rBhXnUU_8U.INSTANCE), new -$$Lambda$EuiccCard$okradEAowCk8rNBK1OaJIA6l6eA(this), callback, handler);
        }
    }

    public static /* synthetic */ String lambda$getEid$11(EuiccCard euiccCard, byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        String eid = IccUtils.bytesToHexString(parseResponse(response).getChild(90, new int[0]).asBytes());
        synchronized (euiccCard.mLock) {
            euiccCard.mEid = eid;
            euiccCard.mCardId = eid;
        }
        return eid;
    }

    public void setNickname(String iccid, String nickname, AsyncResultCallback<Void> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$_VOB5FQfE7RUMgpmr8bK-j3CsUA(iccid, nickname)), -$$Lambda$EuiccCard$4gL9ssytVrnit44qHJ-7-Uy6ZOQ.INSTANCE, callback, handler);
    }

    static /* synthetic */ Void lambda$setNickname$13(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0) {
            return null;
        }
        throw new EuiccCardErrorException(7, result);
    }

    public void deleteProfile(String iccid, AsyncResultCallback<Void> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$MoRNAw8O6kYG_c2AJkozlJwO2NM(iccid)), -$$Lambda$EuiccCard$6M0Cvkh43ith8i9YF2YZNZ-YvOM.INSTANCE, callback, handler);
    }

    static /* synthetic */ Void lambda$deleteProfile$15(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0) {
            return null;
        }
        throw new EuiccCardErrorException(12, result);
    }

    public void resetMemory(int options, AsyncResultCallback<Void> callback, Handler handler) {
        sendApduWithSimResetErrorWorkaround(newRequestProvider(new -$$Lambda$EuiccCard$Wx9UmYdMwRy23Rf6Vd7b2aSx6S8(options)), -$$Lambda$EuiccCard$0NUjmK32-r6146hGb0RCJUAfiOg.INSTANCE, callback, handler);
    }

    static /* synthetic */ Void lambda$resetMemory$17(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0 || result == 1) {
            return null;
        }
        throw new EuiccCardErrorException(13, result);
    }

    public void getDefaultSmdpAddress(AsyncResultCallback<String> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$3LRPBN7jGieBA4qKqsiYoON1xT0.INSTANCE), -$$Lambda$EuiccCard$Qej04bOzl5rj_T7NIjvbnJX7b2s.INSTANCE, callback, handler);
    }

    public void getSmdsAddress(AsyncResultCallback<String> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$tPSWjOKtm9yQg21kHmLX49PPf_4.INSTANCE), -$$Lambda$EuiccCard$u2-6zCuoZP9CLxIS2g4BREHHECI.INSTANCE, callback, handler);
    }

    public void setDefaultSmdpAddress(String defaultSmdpAddress, AsyncResultCallback<Void> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$FbRMt6fKnYLkYt6oi5qhs1ZyEvc(defaultSmdpAddress)), -$$Lambda$EuiccCard$wgj93ukgzqjttFzrDLqGFk_Sd5A.INSTANCE, callback, handler);
    }

    static /* synthetic */ Void lambda$setDefaultSmdpAddress$23(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0) {
            return null;
        }
        throw new EuiccCardErrorException(14, result);
    }

    public void getRulesAuthTable(AsyncResultCallback<EuiccRulesAuthTable> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$AWltG4uFbHn2Xq7ZPpU3U1qOqVM.INSTANCE), -$$Lambda$EuiccCard$IMmMA3gSh1g8aaHsYtCih61EKmo.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccRulesAuthTable lambda$getRulesAuthTable$25(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        List<Asn1Node> nodes = parseResponse(response).getChildren(160);
        EuiccRulesAuthTable.Builder builder = new EuiccRulesAuthTable.Builder(nodes.size());
        int size = nodes.size();
        for (int i = 0; i < size; i++) {
            Asn1Node node = (Asn1Node) nodes.get(i);
            List<Asn1Node> opIdNodes = node.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[0]).getChildren();
            int opIdSize = opIdNodes.size();
            CarrierIdentifier[] opIds = new CarrierIdentifier[opIdSize];
            for (int j = 0; j < opIdSize; j++) {
                opIds[j] = buildCarrierIdentifier((Asn1Node) opIdNodes.get(j));
            }
            builder.add(node.getChild(128, new int[0]).asBits(), Arrays.asList(opIds), node.getChild(130, new int[0]).asBits());
        }
        return builder.build();
    }

    public void getEuiccChallenge(AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$8wofF-Li1V6a8rJQc-M2IGeJ26E.INSTANCE), -$$Lambda$EuiccCard$AGpR_ArLREPF7xVOCf0sgHwbDtA.INSTANCE, callback, handler);
    }

    public void getEuiccInfo1(AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$WE7TDTe507w4dBh1UvCgBgp3xVk.INSTANCE), -$$Lambda$EuiccCard$hCCBghNOkOgvjeYe8LWQml6I9Ow.INSTANCE, callback, handler);
    }

    static /* synthetic */ byte[] lambda$getEuiccInfo1$29(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        return response;
    }

    public void getEuiccInfo2(AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(-$$Lambda$EuiccCard$UxQlywWQ3cqQ7G7vS2KuMEwtYro.INSTANCE), -$$Lambda$EuiccCard$X8OWFy8Bi7TMh117x6vCBqzSqVY.INSTANCE, callback, handler);
    }

    static /* synthetic */ byte[] lambda$getEuiccInfo2$31(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        return response;
    }

    public void authenticateServer(String matchingId, byte[] serverSigned1, byte[] serverSignature1, byte[] euiccCiPkIdToBeUsed, byte[] serverCertificate, AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$dXiSnJocvC7r6HwRUJlZI7Qnleo(this, matchingId, serverSigned1, serverSignature1, euiccCiPkIdToBeUsed, serverCertificate)), -$$Lambda$EuiccCard$MRlmz2j6osUyi5hGvD3j9D4Tsrg.INSTANCE, callback, handler);
    }

    public static /* synthetic */ void lambda$authenticateServer$32(EuiccCard euiccCard, String matchingId, byte[] serverSigned1, byte[] serverSignature1, byte[] euiccCiPkIdToBeUsed, byte[] serverCertificate, RequestBuilder requestBuilder) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        byte[] imeiBytes = euiccCard.getDeviceId();
        byte[] tacBytes = new byte[4];
        int i = 0;
        System.arraycopy(imeiBytes, 0, tacBytes, 0, 4);
        Asn1Node.Builder devCapsBuilder = Asn1Node.newBuilder(PduHeaders.PREVIOUSLY_SENT_DATE);
        String[] devCapsStrings = euiccCard.getResources().getStringArray(17236039);
        if (devCapsStrings != null) {
            int length = devCapsStrings.length;
            while (i < length) {
                euiccCard.addDeviceCapability(devCapsBuilder, devCapsStrings[i]);
                i++;
            }
        } else {
            logd("No device capabilities set.");
        }
        requestBuilder.addStoreData(Asn1Node.newBuilder(48952).addChild(new Asn1Decoder(serverSigned1).nextNode()).addChild(new Asn1Decoder(serverSignature1).nextNode()).addChild(new Asn1Decoder(euiccCiPkIdToBeUsed).nextNode()).addChild(new Asn1Decoder(serverCertificate).nextNode()).addChild(Asn1Node.newBuilder(160).addChildAsString(128, matchingId).addChild(Asn1Node.newBuilder(PduHeaders.PREVIOUSLY_SENT_DATE).addChildAsBytes(128, tacBytes).addChild(devCapsBuilder).addChildAsBytes(130, imeiBytes))).build().toHex());
    }

    public void prepareDownload(byte[] hashCc, byte[] smdpSigned2, byte[] smdpSignature2, byte[] smdpCertificate, AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$5wK_r0z9fLtA1ZRVlbk3WfOYXJI(smdpSigned2, smdpSignature2, hashCc, smdpCertificate)), -$$Lambda$EuiccCard$v0S5B6MBAksDVSST9c1nk2Movvk.INSTANCE, callback, handler);
    }

    static /* synthetic */ void lambda$prepareDownload$34(byte[] smdpSigned2, byte[] smdpSignature2, byte[] hashCc, byte[] smdpCertificate, RequestBuilder requestBuilder) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        Asn1Node.Builder builder = Asn1Node.newBuilder(48929).addChild(new Asn1Decoder(smdpSigned2).nextNode()).addChild(new Asn1Decoder(smdpSignature2).nextNode());
        if (hashCc != null) {
            builder.addChildAsBytes(4, hashCc);
        }
        requestBuilder.addStoreData(builder.addChild(new Asn1Decoder(smdpCertificate).nextNode()).build().toHex());
    }

    static /* synthetic */ byte[] lambda$prepareDownload$35(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        Asn1Node root = parseResponse(response);
        if (!root.hasChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[]{2})) {
            return root.toBytes();
        }
        throw new EuiccCardErrorException(2, root.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[]{2}).asInteger());
    }

    public void loadBoundProfilePackage(byte[] boundProfilePackage, AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$XDNTzAU-9I92HztVAJQr4NXR3DU(boundProfilePackage)), -$$Lambda$EuiccCard$g0LHcTcRLtF0WE8Tyv2BvipGgrM.INSTANCE, callback, handler);
    }

    static /* synthetic */ void lambda$loadBoundProfilePackage$36(byte[] boundProfilePackage, RequestBuilder requestBuilder) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        Asn1Node bppNode = new Asn1Decoder(boundProfilePackage).nextNode();
        int i = 0;
        Asn1Node initialiseSecureChannelRequest = bppNode.getChild(48931, new int[0]);
        Asn1Node firstSequenceOf87 = bppNode.getChild(160, new int[0]);
        Asn1Node sequenceOf88 = bppNode.getChild(PduHeaders.PREVIOUSLY_SENT_DATE, new int[0]);
        List<Asn1Node> metaDataSeqs = sequenceOf88.getChildren(136);
        Asn1Node sequenceOf86 = bppNode.getChild(PduHeaders.MM_STATE, new int[0]);
        List<Asn1Node> elementSeqs = sequenceOf86.getChildren(134);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(bppNode.getHeadAsHex());
        stringBuilder.append(initialiseSecureChannelRequest.toHex());
        requestBuilder.addStoreData(stringBuilder.toString());
        requestBuilder.addStoreData(firstSequenceOf87.toHex());
        requestBuilder.addStoreData(sequenceOf88.getHeadAsHex());
        int size = metaDataSeqs.size();
        for (int i2 = 0; i2 < size; i2++) {
            requestBuilder.addStoreData(((Asn1Node) metaDataSeqs.get(i2)).toHex());
        }
        if (bppNode.hasChild(PduHeaders.STORE, new int[0])) {
            requestBuilder.addStoreData(bppNode.getChild(PduHeaders.STORE, new int[0]).toHex());
        }
        requestBuilder.addStoreData(sequenceOf86.getHeadAsHex());
        size = elementSeqs.size();
        while (i < size) {
            requestBuilder.addStoreData(((Asn1Node) elementSeqs.get(i)).toHex());
            i++;
        }
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x001a, element type: int, insn element type: null */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static /* synthetic */ byte[] lambda$loadBoundProfilePackage$37(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        Asn1Node root = parseResponse(response);
        if (!root.hasChild(48935, new int[]{PduHeaders.STORE, PduHeaders.PREVIOUSLY_SENT_DATE, 129})) {
            return root.toBytes();
        }
        Asn1Node errorNode = root.getChild(48935, new int[]{PduHeaders.STORE, PduHeaders.PREVIOUSLY_SENT_DATE, 129});
        throw new EuiccCardErrorException(5, errorNode.asInteger(), errorNode);
    }

    public void cancelSession(byte[] transactionId, int reason, AsyncResultCallback<byte[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$mf0dWT4hLdKlsLFFHVfdGFxHyX0(transactionId, reason)), -$$Lambda$EuiccCard$ItNER0v0H8BgPYIx3JhINdI9slE.INSTANCE, callback, handler);
    }

    public void listNotifications(int events, AsyncResultCallback<EuiccNotification[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$Ktn9yHrkajo1XOdBNZaiNkYG4vA(events)), -$$Lambda$EuiccCard$nNX2R6O4UzJoFix96ifwgIx0npQ.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccNotification[] lambda$listNotifications$41(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int i = 0;
        List<Asn1Node> nodes = parseResponseAndCheckSimpleError(response, 6).getChild(160, new int[0]).getChildren();
        EuiccNotification[] notifications = new EuiccNotification[nodes.size()];
        while (i < notifications.length) {
            notifications[i] = createNotification((Asn1Node) nodes.get(i));
            i++;
        }
        return notifications;
    }

    public void retrieveNotificationList(int events, AsyncResultCallback<EuiccNotification[]> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$w7krlzKo4ZhEQOPUsWoy_EH6S6w(events)), -$$Lambda$EuiccCard$47aYJh9ifWZ2OFC8SQNq0T5EePE.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccNotification[] lambda$retrieveNotificationList$43(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        Asn1Node root = parseResponse(response);
        int i = 0;
        if (root.hasChild(129, new int[0])) {
            int error = root.getChild(129, new int[0]).asInteger();
            if (error == 1) {
                return new EuiccNotification[0];
            }
            throw new EuiccCardErrorException(8, error);
        }
        List<Asn1Node> nodes = root.getChild(160, new int[0]).getChildren();
        EuiccNotification[] notifications = new EuiccNotification[nodes.size()];
        while (i < notifications.length) {
            notifications[i] = createNotification((Asn1Node) nodes.get(i));
            i++;
        }
        return notifications;
    }

    public void retrieveNotification(int seqNumber, AsyncResultCallback<EuiccNotification> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$KOXfsx-q_NGiOmoDdBfBkea98mo(seqNumber)), -$$Lambda$EuiccCard$ICeXAGilnO8X0GNWbK6HW7brq-s.INSTANCE, callback, handler);
    }

    static /* synthetic */ EuiccNotification lambda$retrieveNotification$45(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        List<Asn1Node> nodes = parseResponseAndCheckSimpleError(response, 8).getChild(160, new int[0]).getChildren();
        if (nodes.size() > 0) {
            return createNotification((Asn1Node) nodes.get(0));
        }
        return null;
    }

    public void removeNotificationFromList(int seqNumber, AsyncResultCallback<Void> callback, Handler handler) {
        sendApdu(newRequestProvider(new -$$Lambda$EuiccCard$7VXOA-y5ZAskOFBWhqVLPntT7K0(seqNumber)), -$$Lambda$EuiccCard$7T_o46uJcfxyJtjGMX_0X0kMk84.INSTANCE, callback, handler);
    }

    static /* synthetic */ Void lambda$removeNotificationFromList$47(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        int result = parseSimpleResult(response);
        if (result == 0 || result == 1) {
            return null;
        }
        throw new EuiccCardErrorException(9, result);
    }

    /* JADX WARNING: Missing block: B:31:0x009d, code skipped:
            if (r3.equals(DEV_CAP_CDMA_1X) != false) goto L_0x00a1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting(visibility = Visibility.PRIVATE)
    public void addDeviceCapability(Asn1Node.Builder devCapBuilder, String devCapItem) {
        String[] split = devCapItem.split(",");
        int i = 2;
        StringBuilder stringBuilder;
        if (split.length != 2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid device capability item: ");
            stringBuilder.append(Arrays.toString(split));
            loge(stringBuilder.toString());
            return;
        }
        String devCap = split[0].trim();
        try {
            byte[] versionBytes = new byte[]{Integer.valueOf(Integer.parseInt(split[1].trim())).byteValue(), (byte) 0, (byte) 0};
            switch (devCap.hashCode()) {
                case -1364987172:
                    break;
                case -1291802661:
                    if (devCap.equals(DEV_CAP_EUTRAN)) {
                        i = 5;
                        break;
                    }
                case 98781:
                    if (devCap.equals(DEV_CAP_CRL)) {
                        i = 7;
                        break;
                    }
                case 102657:
                    if (devCap.equals(DEV_CAP_GSM)) {
                        i = 0;
                        break;
                    }
                case 108971:
                    if (devCap.equals(DEV_CAP_NFC)) {
                        i = 6;
                        break;
                    }
                case 3211390:
                    if (devCap.equals(DEV_CAP_HRPD)) {
                        i = 3;
                        break;
                    }
                case 96487011:
                    if (devCap.equals(DEV_CAP_EHRPD)) {
                        i = 4;
                        break;
                    }
                case 111620384:
                    if (devCap.equals(DEV_CAP_UTRAN)) {
                        i = 1;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    devCapBuilder.addChildAsBytes(128, versionBytes);
                    break;
                case 1:
                    devCapBuilder.addChildAsBytes(129, versionBytes);
                    break;
                case 2:
                    devCapBuilder.addChildAsBytes(130, versionBytes);
                    break;
                case 3:
                    devCapBuilder.addChildAsBytes(131, versionBytes);
                    break;
                case 4:
                    devCapBuilder.addChildAsBytes(132, versionBytes);
                    break;
                case 5:
                    devCapBuilder.addChildAsBytes(133, versionBytes);
                    break;
                case 6:
                    devCapBuilder.addChildAsBytes(134, versionBytes);
                    break;
                case 7:
                    devCapBuilder.addChildAsBytes(135, versionBytes);
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid device capability name: ");
                    stringBuilder.append(devCap);
                    loge(stringBuilder.toString());
                    break;
            }
        } catch (NumberFormatException e) {
            loge("Invalid device capability version number.", e);
        }
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected byte[] getDeviceId() {
        byte[] imeiBytes = new byte[8];
        Phone phone = PhoneFactory.getPhone(getPhoneId());
        if (phone != null) {
            IccUtils.bcdToBytes(phone.getDeviceId(), imeiBytes);
        }
        return imeiBytes;
    }

    @VisibleForTesting(visibility = Visibility.PRIVATE)
    protected Resources getResources() {
        return Resources.getSystem();
    }

    private RequestProvider newRequestProvider(ApduRequestBuilder builder) {
        return new -$$Lambda$EuiccCard$7vWsDgJ3RMY6kHsGeBw-CxIKViI(this, builder);
    }

    public static /* synthetic */ void lambda$newRequestProvider$48(EuiccCard euiccCard, ApduRequestBuilder builder, byte[] selectResponse, RequestBuilder requestBuilder) throws Throwable {
        EuiccSpecVersion ver = euiccCard.getOrExtractSpecVersion(selectResponse);
        if (ver != null) {
            try {
                if (ver.compareTo(SGP_2_0) >= 0) {
                    builder.build(requestBuilder);
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("eUICC spec version is unsupported: ");
                stringBuilder.append(ver);
                throw new EuiccCardException(stringBuilder.toString());
            } catch (InvalidAsn1DataException | TagNotFoundException e) {
                throw new EuiccCardException("Cannot parse ASN1 to build request.", e);
            }
        }
        throw new EuiccCardException("Cannot get eUICC spec version.");
    }

    private EuiccSpecVersion getOrExtractSpecVersion(byte[] selectResponse) {
        if (this.mSpecVersion != null) {
            return this.mSpecVersion;
        }
        EuiccSpecVersion ver = EuiccSpecVersion.fromOpenChannelResponse(selectResponse);
        if (ver != null) {
            synchronized (this.mLock) {
                if (this.mSpecVersion == null) {
                    this.mSpecVersion = ver;
                }
            }
        }
        return ver;
    }

    private <T> void sendApdu(RequestProvider requestBuilder, ApduResponseHandler<T> responseHandler, AsyncResultCallback<T> callback, Handler handler) {
        sendApdu(requestBuilder, responseHandler, new -$$Lambda$EuiccCard$L4YPgLjdI8c0_VHmXQ199X1DICE(callback), callback, handler);
    }

    private void sendApduWithSimResetErrorWorkaround(RequestProvider requestBuilder, ApduResponseHandler<Void> responseHandler, AsyncResultCallback<Void> callback, Handler handler) {
        sendApdu(requestBuilder, responseHandler, new -$$Lambda$EuiccCard$jFzxc6nh3bkdLVyMXzM3mlnBqrA(callback), callback, handler);
    }

    static /* synthetic */ void lambda$sendApduWithSimResetErrorWorkaround$50(AsyncResultCallback callback, Throwable e) {
        if ((e instanceof ApduException) && ((ApduException) e).getApduStatus() == APDU_ERROR_SIM_REFRESH) {
            logi("Sim is refreshed after disabling profile, no response got.");
            callback.onResult(null);
            return;
        }
        callback.onException(new EuiccCardException("Cannot send APDU.", e));
    }

    private <T> void sendApdu(RequestProvider requestBuilder, final ApduResponseHandler<T> responseHandler, final ApduExceptionHandler exceptionHandler, final AsyncResultCallback<T> callback, Handler handler) {
        this.mApduSender.send(requestBuilder, new AsyncResultCallback<byte[]>() {
            public void onResult(byte[] response) {
                try {
                    callback.onResult(responseHandler.handleResult(response));
                } catch (EuiccCardException e) {
                    callback.onException(e);
                } catch (InvalidAsn1DataException | TagNotFoundException e2) {
                    AsyncResultCallback asyncResultCallback = callback;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot parse response: ");
                    stringBuilder.append(IccUtils.bytesToHexString(response));
                    asyncResultCallback.onException(new EuiccCardException(stringBuilder.toString(), e2));
                }
            }

            public void onException(Throwable e) {
                exceptionHandler.handleException(e);
            }
        }, handler);
    }

    private static void buildProfile(Asn1Node profileNode, Builder profileBuilder) throws TagNotFoundException, InvalidAsn1DataException {
        if (profileNode.hasChild(144, new int[0])) {
            profileBuilder.setNickname(profileNode.getChild(144, new int[0]).asString());
        }
        if (profileNode.hasChild(145, new int[0])) {
            profileBuilder.setServiceProviderName(profileNode.getChild(145, new int[0]).asString());
        }
        if (profileNode.hasChild(146, new int[0])) {
            profileBuilder.setProfileName(profileNode.getChild(146, new int[0]).asString());
        }
        if (profileNode.hasChild(PduHeaders.APPLIC_ID, new int[0])) {
            profileBuilder.setCarrierIdentifier(buildCarrierIdentifier(profileNode.getChild(PduHeaders.APPLIC_ID, new int[0])));
        }
        if (profileNode.hasChild(40816, new int[0])) {
            profileBuilder.setState(profileNode.getChild(40816, new int[0]).asInteger());
        } else {
            profileBuilder.setState(0);
        }
        if (profileNode.hasChild(149, new int[0])) {
            profileBuilder.setProfileClass(profileNode.getChild(149, new int[0]).asInteger());
        } else {
            profileBuilder.setProfileClass(2);
        }
        if (profileNode.hasChild(153, new int[0])) {
            profileBuilder.setPolicyRules(profileNode.getChild(153, new int[0]).asBits());
        }
        if (profileNode.hasChild(49014, new int[0])) {
            UiccAccessRule[] rules = buildUiccAccessRule(profileNode.getChild(49014, new int[0]).getChildren(226));
            List<UiccAccessRule> rulesList = null;
            if (rules != null) {
                rulesList = Arrays.asList(rules);
            }
            profileBuilder.setUiccAccessRule(rulesList);
        }
    }

    private static CarrierIdentifier buildCarrierIdentifier(Asn1Node node) throws InvalidAsn1DataException, TagNotFoundException {
        String gid1 = null;
        if (node.hasChild(129, new int[0])) {
            gid1 = IccUtils.bytesToHexString(node.getChild(129, new int[0]).asBytes());
        }
        String gid2 = null;
        if (node.hasChild(130, new int[0])) {
            gid2 = IccUtils.bytesToHexString(node.getChild(130, new int[0]).asBytes());
        }
        return new CarrierIdentifier(node.getChild(128, new int[0]).asBytes(), gid1, gid2);
    }

    private static UiccAccessRule[] buildUiccAccessRule(List<Asn1Node> nodes) throws InvalidAsn1DataException, TagNotFoundException {
        if (nodes.isEmpty()) {
            return null;
        }
        int count = nodes.size();
        UiccAccessRule[] rules = new UiccAccessRule[count];
        for (int i = 0; i < count; i++) {
            Asn1Node node = (Asn1Node) nodes.get(i);
            Asn1Node refDoNode = node.getChild(225, new int[0]);
            byte[] signature = refDoNode.getChild(193, new int[0]).asBytes();
            String packageName = null;
            if (refDoNode.hasChild(202, new int[0])) {
                packageName = refDoNode.getChild(202, new int[0]).asString();
            }
            long accessType = 0;
            if (node.hasChild(227, new int[]{219})) {
                accessType = node.getChild(227, new int[]{219}).asRawLong();
            }
            rules[i] = new UiccAccessRule(signature, packageName, accessType);
        }
        return rules;
    }

    private static EuiccNotification createNotification(Asn1Node node) throws TagNotFoundException, InvalidAsn1DataException {
        Asn1Node metadataNode;
        if (node.getTag() == 48943) {
            metadataNode = node;
        } else if (node.getTag() == 48951) {
            metadataNode = node.getChild(48935, new int[]{48943});
        } else {
            metadataNode = node.getChild(48943, new int[0]);
        }
        return new EuiccNotification(metadataNode.getChild(128, new int[0]).asInteger(), metadataNode.getChild(12, new int[0]).asString(), metadataNode.getChild(129, new int[0]).asBits(), node.getTag() == 48943 ? null : node.toBytes());
    }

    private static int parseSimpleResult(byte[] response) throws EuiccCardException, TagNotFoundException, InvalidAsn1DataException {
        return parseResponse(response).getChild(128, new int[0]).asInteger();
    }

    private static Asn1Node parseResponse(byte[] response) throws EuiccCardException, InvalidAsn1DataException {
        Asn1Decoder decoder = new Asn1Decoder(response);
        if (decoder.hasNextNode()) {
            return decoder.nextNode();
        }
        throw new EuiccCardException("Empty response", null);
    }

    private static Asn1Node parseResponseAndCheckSimpleError(byte[] response, int opCode) throws EuiccCardException, InvalidAsn1DataException, TagNotFoundException {
        Asn1Node root = parseResponse(response);
        if (!root.hasChild(129, new int[0])) {
            return root;
        }
        throw new EuiccCardErrorException(opCode, root.getChild(129, new int[0]).asInteger());
    }

    private static String stripTrailingFs(byte[] iccId) {
        return IccUtils.stripTrailingFs(IccUtils.bchToString(iccId, 0, iccId.length));
    }

    private static String padTrailingFs(String iccId) {
        if (TextUtils.isEmpty(iccId) || iccId.length() >= 20) {
            return iccId;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(iccId);
        stringBuilder.append(new String(new char[(20 - iccId.length())]).replace(0, 'F'));
        return stringBuilder.toString();
    }

    private static void loge(String message) {
        Rlog.e(LOG_TAG, message);
    }

    private static void loge(String message, Throwable tr) {
        Rlog.e(LOG_TAG, message, tr);
    }

    private static void logi(String message) {
        Rlog.i(LOG_TAG, message);
    }

    private static void logd(String message) {
        Rlog.d(LOG_TAG, message);
    }
}
