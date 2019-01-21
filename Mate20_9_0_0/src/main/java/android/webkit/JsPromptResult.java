package android.webkit;

import android.annotation.SystemApi;
import android.webkit.JsResult.ResultReceiver;

public class JsPromptResult extends JsResult {
    private String mStringResult;

    public void confirm(String result) {
        this.mStringResult = result;
        confirm();
    }

    @SystemApi
    public JsPromptResult(ResultReceiver receiver) {
        super(receiver);
    }

    @SystemApi
    public String getStringResult() {
        return this.mStringResult;
    }
}
