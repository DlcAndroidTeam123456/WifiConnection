package cn.dlc.dlcwificonnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wifi帮助类
 */
public class WifiHelper implements WifiScanner {

    private final Context mContext;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnectivityManager;

    private ScanResultFilter mScanResultFilter;
    private ScanListener mScanListener;
    private BroadcastReceiver mScanReceiver;

    /**
     * Wifi帮助类
     *
     * @param context
     * @param wifiManager
     * @param connectivityManager
     */
    public WifiHelper(Context context, WifiManager wifiManager,
        ConnectivityManager connectivityManager) {
        mContext = context;
        mWifiManager = wifiManager;
        mConnectivityManager = connectivityManager;
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public ConnectivityManager getConnectivityManager() {
        return mConnectivityManager;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.toString().isEmpty();
    }

    /**
     * 去除双引号
     */
    @NonNull
    public static String trimQuotes(String ssid) {
        if (!isEmpty(ssid)) {
            return ssid.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }
        return ssid;
    }

    /**
     * 添加双引号
     *
     * @param ssid
     * @return
     */
    public static String addQuotes(String ssid) {
        if (isEmpty(ssid)) {
            return "\"\"";
        }
        return "\"" + ssid + "\"";
    }

    /**
     * 判断两个ssid是否一样
     *
     * @param SSID
     * @param anotherSSID
     * @return
     */
    public static boolean areEqual(String SSID, String anotherSSID) {
        return TextUtils.equals(trimQuotes(SSID), trimQuotes(anotherSSID));
    }

    /**
     * 判断指定的ssid为当前连接的wifi
     *
     * @param SSID
     * @return
     */
    public boolean isConnectedToSSID(String SSID) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            return areEqual(wifiInfo.getSSID(), SSID)
                && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED;
        }
        return false;
    }

    /**
     * 获取已连接信息
     *
     * @return
     */
    public WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 判断当前网路是否为wifi
     *
     * @return
     */
    public boolean isWifiActive() {
        NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null
            && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 判断wifi是否已经开启
     *
     * @return WIFI的可用状态
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * 开启/关闭 wifi
     *
     * @param enable
     */
    public void enableWifi(boolean enable) {
        mWifiManager.setWifiEnabled(enable);
    }

    /**
     * 断开wifi连接
     */
    public void disconnect() {
        mWifiManager.disconnect();
    }

    /**
     * 获取已经存在的网络id
     *
     * @param SSID
     * @return
     */
    public int getExistingNetworkId(String SSID) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (areEqual(trimQuotes(existingConfig.SSID), trimQuotes(SSID))) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    /**
     * 获取已经存在的网络
     *
     * @param SSID
     * @return
     */
    public WifiConfiguration getExistingNetworkConfig(String SSID) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (areEqual(trimQuotes(existingConfig.SSID), trimQuotes(SSID))) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    /**
     * 提高新的优先级
     *
     * @param config
     */
    public void assignHighestPriority(WifiConfiguration config) {
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (config.priority <= existingConfig.priority) {
                    config.priority = existingConfig.priority + 1;
                }
            }
        }
    }

    /**
     * 通过热点名获取热点配置
     *
     * @param SSID
     * @return 返回热点配置，可能为null
     */
    public WifiConfiguration getConfigBySSID(@NonNull String SSID) {

        // 下面比较要先加个双引号
        SSID = addQuotes(SSID);

        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (null != existingConfigs) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (SSID.equals(existingConfig.SSID)) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    /**
     * 修改wifi配置
     *
     * @param config 要修改的配置
     * @param wifiEncrypt 加密方式
     * @param SSID SSID
     * @param password 新密码
     * @return
     */
    public WifiConfiguration editWifiConfig(WifiConfiguration config, WifiEncrypt wifiEncrypt,
        String SSID, String password) {

        config.SSID = addQuotes(SSID);
        config.status = WifiConfiguration.Status.DISABLED;
        assignHighestPriority(config);

        switch (wifiEncrypt) {
            case NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.clear();
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
            case EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.wepKeys[0] = addQuotes(password);
                config.wepTxKeyIndex = 0;
                break;
            case WPA:
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.preSharedKey = addQuotes(password);
                break;
            default:
                break;
        }

        return config;
    }

    /**
     * 创建配置
     *
     * @param wifiEncrypt
     * @param SSID
     * @param password
     * @return
     */
    public WifiConfiguration createWifiConfig(WifiEncrypt wifiEncrypt, String SSID,
        String password) {

        Log.w("AAA", "SSID = " + SSID + "password " + password + "type =" + wifiEncrypt);

        WifiConfiguration config = new WifiConfiguration();
        editWifiConfig(config, wifiEncrypt, SSID, password);
        return config;
    }

    /**
     * 按照信号强度排序
     *
     * @param scanResults
     */
    public static void sortBySignalStrength(List<ScanResult> scanResults) {

        if (scanResults == null) {
            return;
        }

        Collections.sort(scanResults, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult resultOne, ScanResult resultTwo) {
                return resultTwo.level - resultOne.level;
            }
        });
    }

    @Override
    public void setScanListener(ScanListener listener) {

        if (mScanListener == null) {
            mScanListener = listener;
            IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

            mScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (mScanListener != null) {
                        mScanListener.onScanResults(WifiHelper.this);
                    }
                    removeScanListener();
                }
            };

            mContext.registerReceiver(mScanReceiver, intentFilter);
        }
    }

    @Override
    public void removeScanListener() {
        mScanListener = null;
        if (mScanReceiver != null) {
            mContext.unregisterReceiver(mScanReceiver);
            mScanReceiver = null;
        }
    }

    @Override
    public void startScan() {
        mWifiManager.startScan();
    }

    @Override
    public List<ScanResult> getScanResults(boolean filterEmpty) {
        return getScanResults(filterEmpty, mScanResultFilter);
    }

    @Override
    public List<ScanResult> getScanResults(boolean filterEmpty, @Nullable ScanResultFilter filter) {
        List<ScanResult> results = null;

        try {
            results = mWifiManager.getScanResults();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (filterEmpty) {
            return filterScanResult(results, filter);
        } else {
            return results;
        }
    }

    /**
     * 过滤结果
     *
     * @param scanResults 待处理的数据
     * @return 去重数据
     */
    public static List<ScanResult> filterScanResult(List<ScanResult> scanResults,
        @Nullable ScanResultFilter filter) {

        ArrayList<ScanResult> results = new ArrayList<>();

        if (scanResults != null) {
            HashMap<String, ScanResult> hashMap = new HashMap<>();

            for (ScanResult scanResult : scanResults) {
                String ssid = scanResult.SSID;

                if (TextUtils.isEmpty(ssid)) {
                    continue;
                }

                // 使用自定义过滤器自定义
                if (filter != null && !filter.letItGo(scanResult)) {
                    continue;
                }

                ScanResult tempResult = hashMap.get(ssid);
                if (null == tempResult) {
                    hashMap.put(ssid, scanResult);
                    continue;
                }

                if (WifiManager.calculateSignalLevel(tempResult.level, 100)
                    < WifiManager.calculateSignalLevel(scanResult.level, 100)) {
                    hashMap.put(ssid, scanResult);
                }
            }

            for (Map.Entry<String, ScanResult> entry : hashMap.entrySet()) {
                results.add(entry.getValue());
            }
        }

        return results;
    }

    @Override
    public void setScanResultFilter(ScanResultFilter scanResultFilter) {
        mScanResultFilter = scanResultFilter;
    }
}
