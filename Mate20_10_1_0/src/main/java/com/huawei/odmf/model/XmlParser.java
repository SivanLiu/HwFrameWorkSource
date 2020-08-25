package com.huawei.odmf.model;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFXmlParserException;
import com.huawei.odmf.model.api.ObjectModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public final class XmlParser {
    private XmlParser() {
    }

    public static ObjectModel parseToModel(String fileDir, String fileName) {
        if (TextUtils.isEmpty(fileDir) || TextUtils.isEmpty(fileName)) {
            throw new ODMFIllegalArgumentException("parameter fileDir or fileName is null");
        }
        try {
            ObjectModel model = new XmlParserHelper(new FileInputStream(new File(fileDir, fileName)), fileName).getModel();
            if (model != null) {
                return model;
            }
            throw new ODMFXmlParserException("xml parser exception");
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found");
        }
    }

    public static ObjectModel parseToModel(File file) {
        if (file == null) {
            throw new ODMFIllegalArgumentException("parameter file error");
        }
        try {
            ObjectModel model = new XmlParserHelper(new FileInputStream(file), file.getName()).getModel();
            if (model != null) {
                return model;
            }
            throw new ODMFXmlParserException("xml parser exception");
        } catch (FileNotFoundException e) {
            throw new ODMFIllegalArgumentException("The xml file not found");
        }
    }

    public static ObjectModel parseToModel(Context context, String assetsFileName) {
        if (TextUtils.isEmpty(assetsFileName) || context == null) {
            throw new ODMFIllegalArgumentException("parameter assetsFileName error");
        }
        ObjectModel model = new XmlParserHelper(context, assetsFileName).getModel();
        if (model != null) {
            return model;
        }
        throw new ODMFXmlParserException("xml parser exception");
    }
}
