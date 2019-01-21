package gov.nist.javax.sip.parser;

import gov.nist.core.Debug;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.message.SIPMessage;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public final class PipelinedMsgParser implements Runnable {
    private static int uid = 0;
    private int maxMessageSize;
    private Thread mythread;
    private Pipeline rawInputStream;
    protected SIPMessageListener sipMessageListener;
    private int sizeCounter;

    protected PipelinedMsgParser() {
    }

    private static synchronized int getNewUid() {
        int i;
        synchronized (PipelinedMsgParser.class) {
            i = uid;
            uid = i + 1;
        }
        return i;
    }

    public PipelinedMsgParser(SIPMessageListener sipMessageListener, Pipeline in, boolean debug, int maxMessageSize) {
        this();
        this.sipMessageListener = sipMessageListener;
        this.rawInputStream = in;
        this.maxMessageSize = maxMessageSize;
        this.mythread = new Thread(this);
        Thread thread = this.mythread;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PipelineThread-");
        stringBuilder.append(getNewUid());
        thread.setName(stringBuilder.toString());
    }

    public PipelinedMsgParser(SIPMessageListener mhandler, Pipeline in, int maxMsgSize) {
        this(mhandler, in, false, maxMsgSize);
    }

    public PipelinedMsgParser(Pipeline in) {
        this(null, in, false, 0);
    }

    public void processInput() {
        this.mythread.start();
    }

    protected Object clone() {
        PipelinedMsgParser p = new PipelinedMsgParser();
        p.rawInputStream = this.rawInputStream;
        p.sipMessageListener = this.sipMessageListener;
        new Thread(p).setName("PipelineThread");
        return p;
    }

    public void setMessageListener(SIPMessageListener mlistener) {
        this.sipMessageListener = mlistener;
    }

    private String readLine(InputStream inputStream) throws IOException {
        StringBuffer retval = new StringBuffer("");
        while (true) {
            int i = inputStream.read();
            if (i != -1) {
                char ch = (char) i;
                if (this.maxMessageSize > 0) {
                    this.sizeCounter--;
                    if (this.sizeCounter <= 0) {
                        throw new IOException("Max size exceeded!");
                    }
                }
                if (ch != 13) {
                    retval.append(ch);
                }
                if (ch == 10) {
                    return retval.toString();
                }
            } else {
                throw new IOException("End of stream");
            }
        }
    }

    public void run() {
        Pipeline inputStream = this.rawInputStream;
        while (true) {
            try {
                String line1;
                String line2;
                this.sizeCounter = this.maxMessageSize;
                StringBuffer inputBuffer = new StringBuffer();
                if (Debug.parserDebug) {
                    Debug.println("Starting parse!");
                }
                SIPMessage line22 = null;
                while (true) {
                    try {
                        line1 = readLine(inputStream);
                        if (!line1.equals(Separators.RETURN)) {
                            break;
                        } else if (Debug.parserDebug) {
                            Debug.println("Discarding blank line. ");
                        }
                    } catch (IOException ex) {
                        Debug.printStackTrace(ex);
                        this.rawInputStream.stopTimer();
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            InternalErrorHandler.handleException(e);
                        }
                        return;
                    }
                }
                inputBuffer.append(line1);
                this.rawInputStream.startTimer();
                Debug.println("Reading Input Stream");
                while (true) {
                    try {
                        line2 = readLine(inputStream);
                        inputBuffer.append(line2);
                        if (line2.trim().equals("")) {
                            break;
                        }
                    } catch (IOException ex2) {
                        this.rawInputStream.stopTimer();
                        Debug.printStackTrace(ex2);
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                            InternalErrorHandler.handleException(e2);
                        }
                        return;
                    }
                }
                this.rawInputStream.stopTimer();
                inputBuffer.append(line2);
                StringMsgParser smp = new StringMsgParser(this.sipMessageListener);
                int nread = 0;
                smp.readBody = false;
                try {
                    if (Debug.debug) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("About to parse : ");
                        stringBuilder.append(inputBuffer.toString());
                        Debug.println(stringBuilder.toString());
                    }
                    SIPMessage sipMessage = smp.parseSIPMessage(inputBuffer.toString());
                    if (sipMessage == null) {
                        this.rawInputStream.stopTimer();
                    } else {
                        int contentLength;
                        if (Debug.debug) {
                            Debug.println("Completed parsing message");
                        }
                        ContentLength cl = (ContentLength) sipMessage.getContentLength();
                        if (cl != null) {
                            contentLength = cl.getContentLength();
                        } else {
                            contentLength = 0;
                        }
                        if (Debug.debug) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("contentLength ");
                            stringBuilder2.append(contentLength);
                            Debug.println(stringBuilder2.toString());
                        }
                        if (contentLength == 0) {
                            sipMessage.removeContent();
                        } else if (this.maxMessageSize == 0 || contentLength < this.sizeCounter) {
                            byte[] message_body = new byte[contentLength];
                            while (nread < contentLength) {
                                this.rawInputStream.startTimer();
                                Pipeline pipeline;
                                try {
                                    int readlength = inputStream.read(message_body, nread, contentLength - nread);
                                    if (readlength <= 0) {
                                        pipeline = this.rawInputStream;
                                        pipeline.stopTimer();
                                        break;
                                    }
                                    nread += readlength;
                                    this.rawInputStream.stopTimer();
                                } catch (IOException ex3) {
                                    Debug.logError("Exception Reading Content", ex3);
                                    pipeline = this.rawInputStream;
                                }
                            }
                            sipMessage.setMessageContent(message_body);
                        }
                        if (this.sipMessageListener != null) {
                            try {
                                this.sipMessageListener.processMessage(sipMessage);
                            } catch (Exception e3) {
                                try {
                                    inputStream.close();
                                } catch (IOException inputBuffer2) {
                                    InternalErrorHandler.handleException(inputBuffer2);
                                }
                                return;
                            }
                        }
                    }
                } catch (ParseException ex4) {
                    Debug.logError("Detected a parse error", ex4);
                }
            } catch (Throwable th) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                    InternalErrorHandler.handleException(e4);
                }
            }
        }
    }

    public void close() {
        try {
            this.rawInputStream.close();
        } catch (IOException e) {
        }
    }
}
