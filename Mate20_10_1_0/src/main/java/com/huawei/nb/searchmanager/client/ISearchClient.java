package com.huawei.nb.searchmanager.client;

import android.database.Cursor;
import android.os.Bundle;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.searchmanager.client.listener.IIndexChangeListener;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.IndexForm;
import java.util.List;
import java.util.Map;

public interface ISearchClient {
    SearchSession beginSearch(String str, String str2);

    int clearIndex(String str, String str2, Map<String, List<String>> map);

    int clearIndexForm(String str);

    void clearUserIndexSearchData(String str, int i);

    List<IndexData> delete(String str, String str2, List<IndexData> list);

    int deleteByQuery(String str, String str2, String str3);

    List<String> deleteByTerm(String str, String str2, String str3, List<String> list);

    void endSearch(String str, String str2, SearchSession searchSession);

    List<Word> executeAnalyzeText(String str, String str2);

    void executeDBCrawl(String str, List<String> list, int i);

    int executeDeleteIndex(String str, List<String> list, List<Attributes> list2);

    void executeFileCrawl(String str, String str2, boolean z, int i);

    int executeInsertIndex(String str, List<SearchIndexData> list, List<Attributes> list2);

    List<SearchIntentItem> executeIntentSearch(String str, String str2, List<String> list, String str3);

    Cursor executeSearch(String str, Bundle bundle);

    BulkCursorDescriptor executeSearch(String str, String str2, List<String> list, List<Attributes> list2);

    int executeUpdateIndex(String str, List<SearchIndexData> list, List<Attributes> list2);

    int getApiVersionCode();

    List<IndexForm> getIndexForm(String str);

    int getIndexFormVersion(String str);

    String grantFilePermission(String str, String str2, String str3, int i);

    List<IndexData> insert(String str, String str2, List<IndexData> list);

    void registerIndexChangeListener(String str, String str2, IIndexChangeListener iIndexChangeListener);

    String revokeFilePermission(String str, String str2, String str3, int i);

    int setIndexForm(String str, int i, List<IndexForm> list);

    void setSearchSwitch(String str, boolean z);

    void unRegisterIndexChangeListener(String str, String str2, IIndexChangeListener iIndexChangeListener);

    List<IndexData> update(String str, String str2, List<IndexData> list);
}
