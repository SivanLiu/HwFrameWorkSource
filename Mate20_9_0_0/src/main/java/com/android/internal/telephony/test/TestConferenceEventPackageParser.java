package com.android.internal.telephony.test;

import android.os.Bundle;
import android.telephony.ims.ImsConferenceState;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class TestConferenceEventPackageParser {
    private static final String LOG_TAG = "TestConferenceEventPackageParser";
    private static final String PARTICIPANT_TAG = "participant";
    private InputStream mInputStream;

    public TestConferenceEventPackageParser(InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    public ImsConferenceState parse() {
        ImsConferenceState conferenceState = new ImsConferenceState();
        XmlPullParser parser;
        try {
            parser = Xml.newPullParser();
            parser.setInput(this.mInputStream, null);
            parser.nextTag();
            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                if (parser.getName().equals(PARTICIPANT_TAG)) {
                    Log.v(LOG_TAG, "Found participant.");
                    Bundle participant = parseParticipant(parser);
                    conferenceState.mParticipants.put(participant.getString("endpoint"), participant);
                }
            }
            return conferenceState;
        } catch (IOException | XmlPullParserException e) {
            parser = e;
            Log.e(LOG_TAG, "Failed to read test conference event package from XML file", parser);
            return null;
        } finally {
            try {
                this.mInputStream.close();
            } catch (IOException e2) {
                Log.e(LOG_TAG, "Failed to close test conference event package InputStream", e2);
                return null;
            }
        }
    }

    private Bundle parseParticipant(XmlPullParser parser) throws IOException, XmlPullParserException {
        Bundle bundle = new Bundle();
        String user = "";
        String displayText = "";
        String endpoint = "";
        String status = "";
        int outerDepth = parser.getDepth();
        while (XmlUtils.nextElementWithin(parser, outerDepth)) {
            if (parser.getName().equals("user")) {
                parser.next();
                user = parser.getText();
            } else if (parser.getName().equals("display-text")) {
                parser.next();
                displayText = parser.getText();
            } else if (parser.getName().equals("endpoint")) {
                parser.next();
                endpoint = parser.getText();
            } else if (parser.getName().equals("status")) {
                parser.next();
                status = parser.getText();
            }
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User: ");
        stringBuilder.append(user);
        Log.v(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("DisplayText: ");
        stringBuilder.append(displayText);
        Log.v(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Endpoint: ");
        stringBuilder.append(endpoint);
        Log.v(str, stringBuilder.toString());
        str = LOG_TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Status: ");
        stringBuilder.append(status);
        Log.v(str, stringBuilder.toString());
        bundle.putString("user", user);
        bundle.putString("display-text", displayText);
        bundle.putString("endpoint", endpoint);
        bundle.putString("status", status);
        return bundle;
    }
}
