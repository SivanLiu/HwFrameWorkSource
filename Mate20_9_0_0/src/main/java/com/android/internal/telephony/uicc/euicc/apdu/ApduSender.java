package com.android.internal.telephony.uicc.euicc.apdu;

import android.os.Handler;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccIoResult;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultCallback;
import com.android.internal.telephony.uicc.euicc.async.AsyncResultHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ApduSender {
    private static final int INS_GET_MORE_RESPONSE = 192;
    private static final String LOG_TAG = "ApduSender";
    private static final int STATUS_NO_ERROR = 36864;
    private static final int SW1_MORE_RESPONSE = 97;
    private final String mAid;
    private final Object mChannelLock = new Object();
    private boolean mChannelOpened;
    private final CloseLogicalChannelInvocation mCloseChannel;
    private final OpenLogicalChannelInvocation mOpenChannel;
    private final boolean mSupportExtendedApdu;
    private final TransmitApduLogicalChannelInvocation mTransmitApdu;

    private static void logv(String msg) {
        Rlog.v(LOG_TAG, msg);
    }

    public ApduSender(CommandsInterface ci, String aid, boolean supportExtendedApdu) {
        this.mAid = aid;
        this.mSupportExtendedApdu = supportExtendedApdu;
        this.mOpenChannel = new OpenLogicalChannelInvocation(ci);
        this.mCloseChannel = new CloseLogicalChannelInvocation(ci);
        this.mTransmitApdu = new TransmitApduLogicalChannelInvocation(ci);
    }

    public void send(final RequestProvider requestProvider, final AsyncResultCallback<byte[]> resultCallback, final Handler handler) {
        synchronized (this.mChannelLock) {
            if (this.mChannelOpened) {
                AsyncResultHelper.throwException(new ApduException("Logical channel has already been opened."), resultCallback, handler);
                return;
            }
            this.mChannelOpened = true;
            this.mOpenChannel.invoke(this.mAid, new AsyncResultCallback<IccOpenLogicalChannelResponse>() {
                public void onResult(IccOpenLogicalChannelResponse openChannelResponse) {
                    int channel = openChannelResponse.getChannel();
                    int status = openChannelResponse.getStatus();
                    if (channel == -1 || status != 1) {
                        synchronized (ApduSender.this.mChannelLock) {
                            ApduSender.this.mChannelOpened = false;
                        }
                        AsyncResultCallback asyncResultCallback = resultCallback;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to open logical channel opened for AID: ");
                        stringBuilder.append(ApduSender.this.mAid);
                        stringBuilder.append(", with status: ");
                        stringBuilder.append(status);
                        asyncResultCallback.onException(new ApduException(stringBuilder.toString()));
                        return;
                    }
                    RequestBuilder builder = new RequestBuilder(channel, ApduSender.this.mSupportExtendedApdu);
                    Throwable requestException = null;
                    try {
                        requestProvider.buildRequest(openChannelResponse.getSelectResponse(), builder);
                    } catch (Throwable e) {
                        requestException = e;
                    }
                    Throwable requestException2 = requestException;
                    if (builder.getCommands().isEmpty() || requestException2 != null) {
                        ApduSender.this.closeAndReturn(channel, null, requestException2, resultCallback, handler);
                        return;
                    }
                    ApduSender.this.sendCommand(builder.getCommands(), 0, resultCallback, handler);
                }
            }, handler);
        }
    }

    private void sendCommand(List<ApduCommand> commands, int index, AsyncResultCallback<byte[]> resultCallback, Handler handler) {
        ApduCommand command = (ApduCommand) commands.get(index);
        final ApduCommand apduCommand = command;
        final AsyncResultCallback<byte[]> asyncResultCallback = resultCallback;
        final Handler handler2 = handler;
        final int i = index;
        final List<ApduCommand> list = commands;
        this.mTransmitApdu.invoke(command, new AsyncResultCallback<IccIoResult>() {
            public void onResult(IccIoResult response) {
                ApduSender.this.getCompleteResponse(apduCommand.channel, response, null, new AsyncResultCallback<IccIoResult>() {
                    public void onResult(IccIoResult fullResponse) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Full APDU response: ");
                        stringBuilder.append(fullResponse);
                        ApduSender.logv(stringBuilder.toString());
                        int status = (fullResponse.sw1 << 8) | fullResponse.sw2;
                        if (status != ApduSender.STATUS_NO_ERROR) {
                            ApduSender.this.closeAndReturn(apduCommand.channel, null, new ApduException(status), asyncResultCallback, handler2);
                        } else if (i == list.size() - 1) {
                            ApduSender.this.closeAndReturn(apduCommand.channel, fullResponse.payload, null, asyncResultCallback, handler2);
                        } else {
                            ApduSender.this.sendCommand(list, i + 1, asyncResultCallback, handler2);
                        }
                    }
                }, handler2);
            }
        }, handler);
    }

    private void getCompleteResponse(int channel, IccIoResult lastResponse, ByteArrayOutputStream responseBuilder, AsyncResultCallback<IccIoResult> resultCallback, Handler handler) {
        IccIoResult iccIoResult = lastResponse;
        ByteArrayOutputStream resultBuilder = responseBuilder == null ? new ByteArrayOutputStream() : responseBuilder;
        if (iccIoResult.payload != null) {
            try {
                resultBuilder.write(iccIoResult.payload);
            } catch (IOException e) {
            }
        }
        if (iccIoResult.sw1 != 97) {
            iccIoResult.payload = resultBuilder.toByteArray();
            resultCallback.onResult(iccIoResult);
            return;
        }
        final int i = channel;
        final ByteArrayOutputStream byteArrayOutputStream = resultBuilder;
        final AsyncResultCallback<IccIoResult> asyncResultCallback = resultCallback;
        final Handler handler2 = handler;
        this.mTransmitApdu.invoke(new ApduCommand(channel, 0, 192, 0, 0, iccIoResult.sw2, ""), new AsyncResultCallback<IccIoResult>() {
            public void onResult(IccIoResult response) {
                ApduSender.this.getCompleteResponse(i, response, byteArrayOutputStream, asyncResultCallback, handler2);
            }
        }, handler);
    }

    private void closeAndReturn(int channel, final byte[] response, final Throwable exception, final AsyncResultCallback<byte[]> resultCallback, Handler handler) {
        this.mCloseChannel.invoke(Integer.valueOf(channel), new AsyncResultCallback<Boolean>() {
            public void onResult(Boolean aBoolean) {
                synchronized (ApduSender.this.mChannelLock) {
                    ApduSender.this.mChannelOpened = false;
                }
                if (exception == null) {
                    resultCallback.onResult(response);
                } else {
                    resultCallback.onException(exception);
                }
            }
        }, handler);
    }
}
