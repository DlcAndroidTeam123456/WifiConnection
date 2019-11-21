package cn.dlc.dlcwificonnect;

import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;
import java.util.List;

/**
 * Wifi扫描器
 */

public interface WifiScanner {

    /**
     * 扫描结果监听器
     */
    interface ScanListener {
        void onScanResults(WifiScanner wifiScanner);
    }

    /**
     * 扫描结果过滤器
     */
    interface ScanResultFilter {

        boolean letItGo(ScanResult scanResult);
    }

    /**
     * 设置扫描监听器
     *
     * @param listener
     */
    void setScanListener(ScanListener listener);

    /**
     * 移除扫描
     */
    void removeScanListener();

    /**
     * 开始扫描
     */
    void startScan();

    /**
     * 获取最近扫描的WIFI热点
     *
     * @return WIFI热点列表
     */
    List<ScanResult> getScanResults(boolean filterEmpty);

    /**
     * 获取最近扫描的WIFI热点
     *
     * @param filterEmpty
     * @param filter
     * @return
     */
    List<ScanResult> getScanResults(boolean filterEmpty, @Nullable ScanResultFilter filter);

    /**
     * 设置扫描过滤器
     *
     * @param scanResultFilter
     */
    void setScanResultFilter(ScanResultFilter scanResultFilter);
}
