package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Message;
import android.os.SystemProperties;
import android.text.TextUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.internal.telephony.HwTelephonyFactory;
import com.android.internal.telephony.cat.AppInterface.CommandType;
import com.android.internal.telephony.cat.BearerDescription.BearerType;
import com.android.internal.telephony.cat.InterfaceTransportLevel.TransportProtocol;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class CommandParamsFactory extends AbstractCommandParamsFactory {
    static final int DTTZ_SETTING = 3;
    static final int LANGUAGE_SETTING = 4;
    static final int LOAD_MULTI_ICONS = 2;
    static final int LOAD_NO_ICON = 0;
    static final int LOAD_SINGLE_ICON = 1;
    private static final int MAX_GSM7_DEFAULT_CHARS = 239;
    private static final int MAX_UCS2_CHARS = 118;
    static final int MSG_ID_LOAD_ICON_DONE = 1;
    static final int NON_SPECIFIC_LANGUAGE = 0;
    static final int REFRESH_FILE_CHANGE_NOTIFICATION = 1;
    static final int REFRESH_NAA_INIT = 3;
    static final int REFRESH_NAA_INIT_AND_FILE_CHANGE = 2;
    static final int REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE = 0;
    static final int REFRESH_UICC_RESET = 4;
    static final int SPECIFIC_LANGUAGE = 1;
    private static CommandParamsFactory sInstance = null;
    private RilMessageDecoder mCaller = null;
    private CommandParams mCmdParams = null;
    private int mIconLoadState = 0;
    private IconLoader mIconLoader;
    private String mRequestedLanguage;
    private String mSavedLanguage;
    private boolean mloadIcon = false;
    private boolean stkSupportIcon = SystemProperties.getBoolean("ro.config.hw_stk_icon", false);

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller, IccFileHandler fh) {
        synchronized (CommandParamsFactory.class) {
            CommandParamsFactory commandParamsFactory;
            if (sInstance != null) {
                commandParamsFactory = sInstance;
                return commandParamsFactory;
            } else if (fh != null) {
                commandParamsFactory = new CommandParamsFactory(caller, fh);
                return commandParamsFactory;
            } else {
                return null;
            }
        }
    }

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh) {
        this.mCaller = caller;
        this.mIconLoader = IconLoader.getInstance(this, fh);
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> ctlvs) {
        if (ctlvs == null) {
            return null;
        }
        ComprehensionTlv ctlvCmdDet = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, ctlvs);
        if (ctlvCmdDet == null) {
            return null;
        }
        try {
            return ValueParser.retrieveCommandDetails(ctlvCmdDet);
        } catch (ResultException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processCommandDetails: Failed to procees command details e=");
            stringBuilder.append(e);
            CatLog.d((Object) this, stringBuilder.toString());
            return null;
        }
    }

    void make(BerTlv berTlv) {
        if (berTlv != null) {
            this.mCmdParams = null;
            this.mIconLoadState = 0;
            if (berTlv.getTag() != BerTlv.BER_PROACTIVE_COMMAND_TAG) {
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            boolean cmdPending = false;
            List<ComprehensionTlv> ctlvs = berTlv.getComprehensionTlvs();
            CommandDetails cmdDet = processCommandDetails(ctlvs);
            if (cmdDet == null) {
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            CommandType cmdType = CommandType.fromInt(cmdDet.typeOfCommand);
            if (cmdType == null) {
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
            } else if (berTlv.isLengthValid()) {
                try {
                    switch (cmdType) {
                        case SET_UP_MENU:
                            cmdPending = processSelectItem(cmdDet, ctlvs);
                            break;
                        case SELECT_ITEM:
                            cmdPending = processSelectItem(cmdDet, ctlvs);
                            break;
                        case DISPLAY_TEXT:
                            cmdPending = processDisplayText(cmdDet, ctlvs);
                            break;
                        case SET_UP_IDLE_MODE_TEXT:
                            cmdPending = processSetUpIdleModeText(cmdDet, ctlvs);
                            break;
                        case GET_INKEY:
                            cmdPending = processGetInkey(cmdDet, ctlvs);
                            break;
                        case GET_INPUT:
                            cmdPending = processGetInput(cmdDet, ctlvs);
                            break;
                        case SEND_DTMF:
                        case SEND_SMS:
                        case SEND_SS:
                        case SEND_USSD:
                            cmdPending = processEventNotify(cmdDet, ctlvs);
                            break;
                        case GET_CHANNEL_STATUS:
                            if (!HuaweiTelephonyConfigs.isModemBipEnable()) {
                                cmdPending = processGetChannelStatus(cmdDet, ctlvs);
                                break;
                            }
                        case SET_UP_CALL:
                            cmdPending = processSetupCall(cmdDet, ctlvs);
                            break;
                        case REFRESH:
                            processRefresh(cmdDet, ctlvs);
                            cmdPending = false;
                            break;
                        case LAUNCH_BROWSER:
                            cmdPending = processLaunchBrowser(cmdDet, ctlvs);
                            break;
                        case PLAY_TONE:
                            cmdPending = processPlayTone(cmdDet, ctlvs);
                            break;
                        case SET_UP_EVENT_LIST:
                            cmdPending = processSetUpEventList(cmdDet, ctlvs);
                            break;
                        case PROVIDE_LOCAL_INFORMATION:
                            cmdPending = processProvideLocalInfo(cmdDet, ctlvs);
                            break;
                        case LANGUAGE_NOTIFICATION:
                            cmdPending = processLanguageNotification(cmdDet, ctlvs);
                            break;
                        case OPEN_CHANNEL:
                        case CLOSE_CHANNEL:
                        case RECEIVE_DATA:
                        case SEND_DATA:
                            if (!HuaweiTelephonyConfigs.isModemBipEnable()) {
                                if (cmdType != CommandType.OPEN_CHANNEL) {
                                    if (cmdType != CommandType.CLOSE_CHANNEL) {
                                        if (cmdType != CommandType.RECEIVE_DATA) {
                                            if (cmdType == CommandType.SEND_DATA) {
                                                cmdPending = processSendData(cmdDet, ctlvs);
                                                break;
                                            }
                                        }
                                        cmdPending = processReceiveData(cmdDet, ctlvs);
                                        break;
                                    }
                                    cmdPending = processCloseChannel(cmdDet, ctlvs);
                                    break;
                                }
                                cmdPending = processOpenChannel(cmdDet, ctlvs);
                                break;
                            }
                            cmdPending = processBIPClient(cmdDet, ctlvs);
                            break;
                            break;
                        default:
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                    }
                    if (!cmdPending) {
                        sendCmdParams(ResultCode.OK);
                    }
                } catch (ResultException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("make: caught ResultException e=");
                    stringBuilder.append(e);
                    CatLog.d((Object) this, stringBuilder.toString());
                    this.mCmdParams = new CommandParams(cmdDet);
                    sendCmdParams(e.result());
                }
            } else {
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
    }

    public void handleMessage(Message msg) {
        if (msg.what == 1 && this.mIconLoader != null) {
            sendCmdParams(setIcons(msg.obj));
        }
    }

    private ResultCode setIcons(Object data) {
        int i = 0;
        if (data == null) {
            CatLog.d((Object) this, "Optional Icon data is NULL");
            this.mCmdParams.mLoadIconFailed = true;
            this.mloadIcon = false;
            return ResultCode.OK;
        }
        switch (this.mIconLoadState) {
            case 1:
                this.mCmdParams.setIcon((Bitmap) data);
                break;
            case 2:
                Bitmap[] icons = (Bitmap[]) data;
                int length = icons.length;
                while (i < length) {
                    Bitmap icon = icons[i];
                    this.mCmdParams.setIcon(icon);
                    if (icon == null && this.mloadIcon) {
                        CatLog.d((Object) this, "Optional Icon data is NULL while loading multi icons");
                        this.mCmdParams.mLoadIconFailed = true;
                    }
                    i++;
                }
                break;
        }
        return ResultCode.OK;
    }

    private void sendCmdParams(ResultCode resCode) {
        if (this.mCaller != null) {
            this.mCaller.sendMsgParamsDecoded(resCode, this.mCmdParams);
        }
    }

    private ComprehensionTlv searchForTag(ComprehensionTlvTag tag, List<ComprehensionTlv> ctlvs) {
        return searchForNextTag(tag, ctlvs.iterator());
    }

    private ComprehensionTlv searchForNextTag(ComprehensionTlvTag tag, Iterator<ComprehensionTlv> iter) {
        int tagValue = tag.value();
        while (iter.hasNext()) {
            ComprehensionTlv ctlv = (ComprehensionTlv) iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    private boolean processDisplayText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process DisplayText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        if (textMsg.text != null) {
            if (searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, ctlvs) != null) {
                textMsg.responseNeeded = false;
            }
            ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv != null) {
                iconId = ValueParser.retrieveIconId(ctlv);
                textMsg.iconSelfExplanatory = iconId.selfExplanatory;
            }
            ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
            if (ctlv != null) {
                textMsg.duration = ValueParser.retrieveDuration(ctlv);
            }
            textMsg.isHighPriority = (cmdDet.commandQualifier & 1) != 0;
            textMsg.userClear = (cmdDet.commandQualifier & 128) != 0;
            this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
            if (iconId != null) {
                if (this.stkSupportIcon) {
                    this.mloadIcon = true;
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                    return true;
                }
                CatLog.d((Object) this, "Close load icon feature.");
                this.mCmdParams.mLoadIconFailed = true;
            }
            return false;
        }
        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
    }

    private boolean processSetUpIdleModeText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process SetUpIdleModeText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        if (textMsg.text != null || iconId == null || textMsg.iconSelfExplanatory) {
            this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
            if (iconId != null) {
                if (this.stkSupportIcon) {
                    this.mloadIcon = true;
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                    return true;
                }
                CatLog.d((Object) this, "Close load icon feature.");
                this.mCmdParams.mLoadIconFailed = true;
            }
            return false;
        }
        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
    }

    private boolean processGetInkey(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process GetInkey");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv != null) {
                iconId = ValueParser.retrieveIconId(ctlv);
                input.iconSelfExplanatory = iconId.selfExplanatory;
            }
            ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
            if (ctlv != null) {
                input.duration = ValueParser.retrieveDuration(ctlv);
            }
            input.minLen = 1;
            input.maxLen = 1;
            input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
            input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
            input.yesNo = (cmdDet.commandQualifier & 4) != 0;
            input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
            input.echo = true;
            this.mCmdParams = new GetInputParams(cmdDet, input);
            if (iconId != null) {
                if (this.stkSupportIcon) {
                    this.mloadIcon = true;
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                    return true;
                }
                CatLog.d((Object) this, "Close load icon feature.");
                this.mCmdParams.mLoadIconFailed = true;
            }
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processGetInput(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process GetInput");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ctlv = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, ctlvs);
            if (ctlv != null) {
                try {
                    byte[] rawValue = ctlv.getRawValue();
                    int valueIndex = ctlv.getValueIndex();
                    input.minLen = rawValue[valueIndex] & 255;
                    input.maxLen = rawValue[valueIndex + 1] & 255;
                    ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
                    if (ctlv != null) {
                        input.duration = ValueParser.retrieveDuration(ctlv);
                    }
                    ctlv = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, ctlvs);
                    if (ctlv != null) {
                        input.defaultText = ValueParser.retrieveTextString(ctlv);
                    }
                    ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
                    if (ctlv != null) {
                        iconId = ValueParser.retrieveIconId(ctlv);
                        input.iconSelfExplanatory = iconId.selfExplanatory;
                    }
                    ctlv = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
                    if (ctlv != null) {
                        input.duration = ValueParser.retrieveDuration(ctlv);
                    }
                    input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
                    input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
                    input.echo = (cmdDet.commandQualifier & 4) == 0;
                    input.packed = (cmdDet.commandQualifier & 8) != 0;
                    input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
                    StringBuilder stringBuilder;
                    if (input.ucs2 && input.maxLen > 118) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("UCS2: received maxLen = ");
                        stringBuilder.append(input.maxLen);
                        stringBuilder.append(", truncating to ");
                        stringBuilder.append(118);
                        CatLog.d((Object) this, stringBuilder.toString());
                        input.maxLen = 118;
                    } else if (!input.packed && input.maxLen > MAX_GSM7_DEFAULT_CHARS) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("GSM 7Bit Default: received maxLen = ");
                        stringBuilder.append(input.maxLen);
                        stringBuilder.append(", truncating to ");
                        stringBuilder.append(MAX_GSM7_DEFAULT_CHARS);
                        CatLog.d((Object) this, stringBuilder.toString());
                        input.maxLen = MAX_GSM7_DEFAULT_CHARS;
                    }
                    this.mCmdParams = new GetInputParams(cmdDet, input);
                    if (iconId != null) {
                        if (this.stkSupportIcon) {
                            this.mloadIcon = true;
                            this.mIconLoadState = 1;
                            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                            return true;
                        }
                        CatLog.d((Object) this, "Close load icon feature.");
                        this.mCmdParams.mLoadIconFailed = true;
                    }
                    return false;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processRefresh(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) {
        CatLog.d((Object) this, "process Refresh");
        switch (cmdDet.commandQualifier) {
            case 0:
            case 2:
            case 3:
            case 4:
                this.mCmdParams = new DisplayTextParams(cmdDet, null);
                break;
            case 1:
                processFileChangeNotification(cmdDet, ctlvs);
                break;
        }
        return false;
    }

    private boolean processSelectItem(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process SelectItem");
        Menu menu = new Menu();
        IconId titleIconId = null;
        ItemsIconId itemsIconId = null;
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        CommandType cmdType = CommandType.fromInt(cmdDet.typeOfCommand);
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            menu.title = ValueParser.retrieveAlphaId(ctlv);
        } else if (cmdType == CommandType.SET_UP_MENU) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        while (true) {
            ctlv = searchForNextTag(ComprehensionTlvTag.ITEM, iter);
            if (ctlv == null) {
                break;
            }
            menu.items.add(ValueParser.retrieveItem(ctlv));
        }
        if (menu.items.size() != 0) {
            ctlv = searchForTag(ComprehensionTlvTag.ITEM_ID, ctlvs);
            if (ctlv != null) {
                menu.defaultItem = ValueParser.retrieveItemId(ctlv) - 1;
            }
            ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv != null) {
                this.mIconLoadState = 1;
                titleIconId = ValueParser.retrieveIconId(ctlv);
                menu.titleIconSelfExplanatory = titleIconId.selfExplanatory;
            }
            ctlv = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, ctlvs);
            if (ctlv != null) {
                this.mIconLoadState = 2;
                itemsIconId = ValueParser.retrieveItemsIconId(ctlv);
                menu.itemsIconSelfExplanatory = itemsIconId.selfExplanatory;
            }
            if ((cmdDet.commandQualifier & 1) != 0) {
                if ((2 & cmdDet.commandQualifier) == 0) {
                    menu.presentationType = PresentationType.DATA_VALUES;
                } else {
                    menu.presentationType = PresentationType.NAVIGATION_OPTIONS;
                }
            }
            menu.softKeyPreferred = (cmdDet.commandQualifier & 4) != 0;
            menu.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
            this.mCmdParams = new SelectItemParams(cmdDet, menu, titleIconId != null);
            switch (this.mIconLoadState) {
                case 0:
                    return false;
                case 1:
                    if (this.stkSupportIcon) {
                        this.mloadIcon = true;
                        this.mIconLoader.loadIcon(titleIconId.recordNumber, obtainMessage(1));
                        break;
                    }
                    CatLog.d((Object) this, "Close load icon feature.");
                    this.mCmdParams.mLoadIconFailed = true;
                    return false;
                case 2:
                    if (this.stkSupportIcon) {
                        int[] recordNumbers = itemsIconId.recordNumbers;
                        if (titleIconId != null) {
                            recordNumbers = new int[(itemsIconId.recordNumbers.length + 1)];
                            recordNumbers[0] = titleIconId.recordNumber;
                            System.arraycopy(itemsIconId.recordNumbers, 0, recordNumbers, 1, itemsIconId.recordNumbers.length);
                        }
                        this.mloadIcon = true;
                        this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
                        break;
                    }
                    CatLog.d((Object) this, "Close load icon feature.");
                    this.mCmdParams.mLoadIconFailed = true;
                    return false;
            }
            return true;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processEventNotify(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process EventNotify");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        textMsg.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        textMsg.responseNeeded = false;
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId != null) {
            if (this.stkSupportIcon) {
                this.mloadIcon = true;
                this.mIconLoadState = 1;
                this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                return true;
            }
            CatLog.d((Object) this, "Close load icon feature.");
            this.mCmdParams.mLoadIconFailed = true;
        }
        return false;
    }

    private boolean processSetUpEventList(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) {
        CatLog.d((Object) this, "process SetUpEventList");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                int[] eventList = new int[valueLen];
                int eventValue = -1;
                int valueIndex2 = valueIndex;
                valueIndex = 0;
                while (valueLen > 0) {
                    eventValue = rawValue[valueIndex2] & 255;
                    valueIndex2++;
                    valueLen--;
                    if (eventValue != 15) {
                        switch (eventValue) {
                            case 4:
                            case 5:
                                break;
                            default:
                                switch (eventValue) {
                                    case 7:
                                    case 8:
                                    case 9:
                                    case 10:
                                        break;
                                    default:
                                        continue;
                                        continue;
                                }
                        }
                    }
                    eventList[valueIndex] = eventValue;
                    valueIndex++;
                }
                this.mCmdParams = new SetEventListParams(cmdDet, eventList);
            } catch (IndexOutOfBoundsException e) {
                CatLog.e((Object) this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    private boolean processLaunchBrowser(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        LaunchBrowserMode mode;
        CatLog.d((Object) this, "process LaunchBrowser");
        TextMessage confirmMsg = new TextMessage();
        IconId iconId = null;
        String url = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.URL, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                if (valueLen > 0) {
                    url = HwTelephonyFactory.getHwTelephonyBaseManager().gsm8BitUnpackedToString(rawValue, valueIndex, valueLen, true);
                } else {
                    url = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        confirmMsg.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs));
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            confirmMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        switch (cmdDet.commandQualifier) {
            case 2:
                mode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 3:
                mode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
            default:
                mode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
        }
        this.mCmdParams = new LaunchBrowserParams(cmdDet, confirmMsg, url, mode);
        if (iconId != null) {
            if (this.stkSupportIcon) {
                this.mIconLoadState = 1;
                this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                return true;
            }
            CatLog.d((Object) this, "Close load icon feature.");
            this.mCmdParams.mLoadIconFailed = true;
        }
        return false;
    }

    private boolean processPlayTone(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        List<ComprehensionTlv> list = ctlvs;
        CatLog.d(this, "process PlayTone");
        Tone tone = null;
        TextMessage textMsg = new TextMessage();
        Duration duration = null;
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TONE, list);
        if (ctlv != null && ctlv.getLength() > 0) {
            try {
                tone = Tone.fromInt(ctlv.getRawValue()[ctlv.getValueIndex()]);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (ctlv2 != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv2);
            if (textMsg.text == null) {
                textMsg.text = "";
            }
        }
        ctlv2 = searchForTag(ComprehensionTlvTag.DURATION, list);
        if (ctlv2 != null) {
            duration = ValueParser.retrieveDuration(ctlv2);
        }
        Duration duration2 = duration;
        ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        IconId iconId2 = iconId;
        CommandDetails commandDetails = cmdDet;
        boolean vibrate = (commandDetails.commandQualifier & 1) != 0;
        textMsg.responseNeeded = false;
        CommandParams commandParams = r4;
        CommandParams playToneParams = new PlayToneParams(commandDetails, textMsg, tone, duration2, vibrate);
        this.mCmdParams = commandParams;
        if (iconId2 != null) {
            if (this.stkSupportIcon) {
                this.mIconLoadState = 1;
                this.mIconLoader.loadIcon(iconId2.recordNumber, obtainMessage(1));
                return true;
            }
            CatLog.d(this, "Close load icon feature.");
            this.mCmdParams.mLoadIconFailed = true;
        }
        return false;
    }

    private boolean processSetupCall(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process SetupCall");
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        TextMessage confirmMsg = new TextMessage();
        TextMessage callMsg = new TextMessage();
        IconId confirmIconId = null;
        IconId callIconId = null;
        TextMessage TempMsg = new TextMessage();
        IconId tempIconId = null;
        ComprehensionTlv ctlv = searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter);
        if (ctlv != null) {
            TempMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            tempIconId = ValueParser.retrieveIconId(ctlv);
            TempMsg.iconSelfExplanatory = tempIconId.selfExplanatory;
        }
        if (searchForNextTag(ComprehensionTlvTag.ADDRESS, iter) != null) {
            CatLog.d((Object) this, "ADDRESS_ID parse entered");
            confirmMsg.text = TempMsg.text;
            confirmIconId = tempIconId;
            if (confirmIconId != null) {
                confirmMsg.iconSelfExplanatory = confirmIconId.selfExplanatory;
            }
            ctlv = searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter);
            if (ctlv != null) {
                callMsg.text = ValueParser.retrieveAlphaId(ctlv);
            }
            ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv != null) {
                callIconId = ValueParser.retrieveIconId(ctlv);
                callMsg.iconSelfExplanatory = callIconId.selfExplanatory;
            }
        } else {
            callMsg.text = TempMsg.text;
            callIconId = tempIconId;
            if (callIconId != null) {
                callMsg.iconSelfExplanatory = callIconId.selfExplanatory;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("callMsg.text");
        stringBuilder.append(callMsg.text);
        CatLog.d((Object) this, stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("confirmMsg.text");
        stringBuilder.append(confirmMsg.text);
        CatLog.d((Object) this, stringBuilder.toString());
        this.mCmdParams = new CallSetupParams(cmdDet, confirmMsg, callMsg);
        if (!(confirmIconId == null && callIconId == null)) {
            if (this.stkSupportIcon) {
                this.mIconLoadState = 2;
                int[] recordNumbers = new int[2];
                int i = -1;
                recordNumbers[0] = confirmIconId != null ? confirmIconId.recordNumber : -1;
                if (callIconId != null) {
                    i = callIconId.recordNumber;
                }
                recordNumbers[1] = i;
                this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
                return true;
            }
            CatLog.d((Object) this, "Close load icon feature.");
            this.mCmdParams.mLoadIconFailed = true;
        }
        return false;
    }

    private boolean processProvideLocalInfo(CommandDetails cmdDet, List<ComprehensionTlv> list) throws ResultException {
        CatLog.d((Object) this, "process ProvideLocalInfo");
        switch (cmdDet.commandQualifier) {
            case 3:
                CatLog.d((Object) this, "PLI [DTTZ_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                break;
            case 4:
                CatLog.d((Object) this, "PLI [LANGUAGE_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PLI[");
                stringBuilder.append(cmdDet.commandQualifier);
                stringBuilder.append("] Command Not Supported");
                CatLog.d((Object) this, stringBuilder.toString());
                this.mCmdParams = new CommandParams(cmdDet);
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }
        return false;
    }

    public boolean processLanguageNotification(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process Language Notification");
        String desiredLanguage = null;
        String currentLanguage = Locale.getDefault().getLanguage();
        StringBuilder stringBuilder;
        switch (cmdDet.commandQualifier) {
            case 0:
                if (!(TextUtils.isEmpty(this.mSavedLanguage) || TextUtils.isEmpty(this.mRequestedLanguage) || !this.mRequestedLanguage.equals(currentLanguage))) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Non-specific language notification changes the language setting back to ");
                    stringBuilder.append(this.mSavedLanguage);
                    CatLog.d((Object) this, stringBuilder.toString());
                    desiredLanguage = this.mSavedLanguage;
                }
                this.mSavedLanguage = null;
                this.mRequestedLanguage = null;
                break;
            case 1:
                ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.LANGUAGE, ctlvs);
                if (ctlv != null) {
                    if (ctlv.getLength() == 2) {
                        desiredLanguage = GsmAlphabet.gsm8BitUnpackedToString(ctlv.getRawValue(), ctlv.getValueIndex(), 2);
                        if (TextUtils.isEmpty(this.mSavedLanguage) || !(TextUtils.isEmpty(this.mRequestedLanguage) || this.mRequestedLanguage.equals(currentLanguage))) {
                            this.mSavedLanguage = currentLanguage;
                        }
                        this.mRequestedLanguage = desiredLanguage;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Specific language notification changes the language setting to ");
                        stringBuilder2.append(this.mRequestedLanguage);
                        CatLog.d((Object) this, stringBuilder2.toString());
                        break;
                    }
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
                break;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("LN[");
                stringBuilder.append(cmdDet.commandQualifier);
                stringBuilder.append("] Command Not Supported");
                CatLog.d((Object) this, stringBuilder.toString());
                break;
        }
        this.mCmdParams = new LanguageParams(cmdDet, desiredLanguage);
        return false;
    }

    private boolean processBIPClient(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CommandType commandType = CommandType.fromInt(cmdDet.typeOfCommand);
        if (commandType != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("process ");
            stringBuilder.append(commandType.name());
            CatLog.d((Object) this, stringBuilder.toString());
        }
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        boolean has_alpha_id = false;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("alpha TLV text=");
            stringBuilder2.append(textMsg.text);
            CatLog.d((Object) this, stringBuilder2.toString());
            has_alpha_id = true;
        }
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        textMsg.responseNeeded = false;
        this.mCmdParams = new BIPClientParams(cmdDet, textMsg, has_alpha_id);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    public void dispose() {
        this.mIconLoader.dispose();
        this.mIconLoader = null;
        this.mCmdParams = null;
        this.mCaller = null;
        sInstance = null;
    }

    private boolean processOpenChannel(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        List<ComprehensionTlv> list = ctlvs;
        CatLog.d(this, "process OpenChannel");
        TextMessage confirmMsg = new TextMessage();
        IconId confirmIconId = null;
        InterfaceTransportLevel itl = null;
        byte[] destinationAddress = null;
        confirmMsg.responseNeeded = false;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (ctlv != null) {
            confirmMsg.text = ValueParser.retrieveBIPAlphaId(ctlv);
            if (confirmMsg.text != null) {
                confirmMsg.responseNeeded = true;
            }
        }
        ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (ctlv != null) {
            confirmIconId = ValueParser.retrieveIconId(ctlv);
            confirmMsg.iconSelfExplanatory = confirmIconId.selfExplanatory;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.BUFFER_SIZE, list);
        if (ctlv2 != null) {
            BearerDescription bearerDescription;
            String networkAccessName;
            String userLogin;
            String userPassword;
            String iter;
            String str;
            int bufSize = ValueParser.retrieveBufferSize(ctlv2);
            Iterator<ComprehensionTlv> iter2 = ctlvs.iterator();
            ctlv2 = searchForNextTag(ComprehensionTlvTag.IF_TRANS_LEVEL, iter2);
            if (ctlv2 != null) {
                itl = ValueParser.retrieveInterfaceTransportLevel(ctlv2);
                ctlv2 = searchForNextTag(ComprehensionTlvTag.OTHER_ADDRESS, iter2);
                if (ctlv2 != null) {
                    destinationAddress = ValueParser.retrieveOtherAddress(ctlv2);
                }
            }
            InterfaceTransportLevel itl2 = itl;
            byte[] destinationAddress2 = destinationAddress;
            ctlv2 = searchForTag(ComprehensionTlvTag.BEARER_DESC, list);
            if (ctlv2 != null) {
                BearerDescription bearerDescription2 = ValueParser.retrieveBearerDescription(ctlv2);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processOpenChannel bearer: ");
                stringBuilder.append(bearerDescription2.type.value());
                stringBuilder.append(" param.len: ");
                stringBuilder.append(bearerDescription2.parameters.length);
                CatLog.d(this, stringBuilder.toString());
                bearerDescription = bearerDescription2;
            } else {
                bearerDescription = null;
            }
            ctlv2 = searchForNextTag(ComprehensionTlvTag.NETWORK_ACCESS_NAME, ctlvs.iterator());
            if (ctlv2 != null) {
                networkAccessName = ValueParser.retrieveNetworkAccessName(ctlv2);
            } else {
                networkAccessName = null;
            }
            String networkAccessName2 = ctlvs.iterator();
            ctlv2 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, networkAccessName2);
            if (ctlv2 != null) {
                userLogin = ValueParser.retrieveTextString(ctlv2);
            } else {
                userLogin = null;
            }
            String userLogin2 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, networkAccessName2);
            if (userLogin2 != null) {
                userPassword = ValueParser.retrieveTextString(userLogin2);
            } else {
                userPassword = null;
            }
            if (itl2 == null || bearerDescription != null) {
                BearerDescription bearerDescription3;
                InterfaceTransportLevel interfaceTransportLevel;
                if (bearerDescription == null) {
                    iter = networkAccessName2;
                    str = userLogin2;
                    bearerDescription3 = bearerDescription;
                    interfaceTransportLevel = itl2;
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                } else if (bearerDescription.type != BearerType.DEFAULT_BEARER && bearerDescription.type != BearerType.MOBILE_PS && bearerDescription.type != BearerType.MOBILE_PS_EXTENDED_QOS && bearerDescription.type != BearerType.E_UTRAN) {
                    throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                } else if (itl2 == null) {
                    iter = networkAccessName2;
                    str = userLogin2;
                    bearerDescription3 = bearerDescription;
                    interfaceTransportLevel = itl2;
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                } else if (itl2.protocol != TransportProtocol.TCP_CLIENT_REMOTE && itl2.protocol != TransportProtocol.UDP_CLIENT_REMOTE) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                } else if (destinationAddress2 == null) {
                    iter = networkAccessName2;
                    str = userLogin2;
                    bearerDescription3 = bearerDescription;
                    interfaceTransportLevel = itl2;
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                }
            } else if (!(itl2.protocol == TransportProtocol.TCP_SERVER || itl2.protocol == TransportProtocol.TCP_CLIENT_LOCAL || itl2.protocol == TransportProtocol.UDP_CLIENT_LOCAL)) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processOpenChannel bufSize=");
            stringBuilder2.append(bufSize);
            stringBuilder2.append(" protocol=");
            stringBuilder2.append(itl2.protocol);
            stringBuilder2.append(" APN=");
            stringBuilder2.append(networkAccessName != null ? networkAccessName : "undefined");
            stringBuilder2.append(" user/password=");
            stringBuilder2.append(userLogin != null ? userLogin : "---");
            stringBuilder2.append("/");
            stringBuilder2.append(userPassword != null ? userPassword : "---");
            String msg = stringBuilder2.toString();
            CatLog.d(this, msg);
            CommandParams commandParams = r3;
            iter = networkAccessName2;
            str = userLogin2;
            CommandParams openChannelParams = new OpenChannelParams(cmdDet, confirmMsg, bufSize, itl2, destinationAddress2, bearerDescription, networkAccessName, userLogin, userPassword);
            this.mCmdParams = commandParams;
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processCloseChannel(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process CloseChannel");
        TextMessage alertMsg = new TextMessage();
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            channel &= 15;
            ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
            if (ctlv != null) {
                alertMsg.text = ValueParser.retrieveBIPAlphaId(ctlv);
            }
            ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv != null) {
                alertMsg.iconSelfExplanatory = ValueParser.retrieveIconId(ctlv).selfExplanatory;
            }
            this.mCmdParams = new CloseChannelParams(cmdDet, alertMsg, channel);
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processReceiveData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process ReceiveData");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid Channel number given: ");
                stringBuilder.append(channel);
                CatLog.d((Object) this, stringBuilder.toString());
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            channel &= 15;
            TextMessage textMsg = null;
            ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
            if (ctlv != null) {
                textMsg = new TextMessage();
                textMsg.text = ValueParser.retrieveBIPAlphaId(ctlv);
                textMsg.responseNeeded = false;
            }
            ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA_LENGTH, ctlvs);
            if (ctlv != null) {
                this.mCmdParams = new ReceiveDataParams(cmdDet, channel, ValueParser.retrieveChannelDataLength(ctlv), textMsg);
                return false;
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processSendData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d((Object) this, "process SendData");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            channel &= 15;
            TextMessage textMsg = null;
            ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
            if (ctlv != null) {
                textMsg = new TextMessage();
                textMsg.text = ValueParser.retrieveBIPAlphaId(ctlv);
                textMsg.responseNeeded = false;
            }
            ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA, ctlvs);
            if (ctlv != null) {
                this.mCmdParams = new SendDataParams(cmdDet, channel, ValueParser.retrieveChannelData(ctlv), textMsg);
                return false;
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processGetChannelStatus(CommandDetails cmdDet, List<ComprehensionTlv> list) throws ResultException {
        CatLog.d((Object) this, "process GetChannelStatus");
        this.mCmdParams = new GetChannelStatusParams(cmdDet);
        return false;
    }
}
