package cn.dlc.dlcwificonnect;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static java.lang.String.format;

class WifiConnector {

    private static final String TAG = "WifiConnector";

    private Context mContext;
    private WifiHelper mWifiHelper;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    private WifiStateChangeListener mWifiStateChangeListener;
    private BroadcastReceiver mWifiStateReceiver;

    private NetworkStateChangeListener mNetworkStateChangeListener;
    private BroadcastReceiver mNetworkStateReceiver;

    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * wifi状态更改（开启/关闭）监听
     */
    interface WifiStateChangeListener {

        /**
         * 当wifi开启时
         *
         * @param initialStickyBroadcast
         */
        void onWifiEnabled(boolean initialStickyBroadcast);

        /**
         * 当wifi关闭时
         *
         * @param initialStickyBroadcast
         */
        void onWifiDisabled(boolean initialStickyBroadcast);
    }

    /**
     * 网络状态更改监听
     */
    interface NetworkStateChangeListener {

        /**
         * 当网络状态更改
         *
         * @param networkInfo
         * @param state
         * @param detailedState
         */
        void onNetworkStateChange(NetworkInfo networkInfo, NetworkInfo.State state,
            NetworkInfo.DetailedState detailedState);

        /**
         * 密码验证失败
         */
        void onErrorAuthenticating();

        /**
         * 当绑定网络时
         */
        void onNetworkBound();
    }

    public WifiConnector(Context context, WifiHelper wifiHelper) {

        mContext = context;

        mWifiManager = wifiHelper.getWifiManager();
        mConnectivityManager = wifiHelper.getConnectivityManager();
        mWifiHelper = wifiHelper;
    }

    /**
     * 判断当前网路是否为wifi
     *
     * @return
     */
    public boolean isWifiActive() {
        return mWifiHelper.isWifiActive();
    }

    /**
     * 判断wifi是否已经开启
     *
     * @return WIFI的可用状态
     */
    public boolean isWifiEnabled() {
        return mWifiHelper.isWifiEnabled();
    }

    /**
     * 开启/关闭 wifi
     *
     * @param enable
     */
    public void enableWifi(boolean enable) {
        mWifiHelper.enableWifi(enable);
    }

    /**
     * 断开wifi连接
     */
    public void disconnect() {
        mWifiHelper.disconnect();
    }

    /**
     * 监听wifi状态变化
     *
     * @param listener
     */
    public void setWifiStateChangeListener(@NonNull WifiStateChangeListener listener) {

        mWifiStateChangeListener = listener;

        mWifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int wifiState = intent.getExtras().getInt(WifiManager.EXTRA_WIFI_STATE);

                if (mWifiStateChangeListener != null) {
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        mWifiStateChangeListener.onWifiEnabled(isInitialStickyBroadcast());
                    } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        mWifiStateChangeListener.onWifiDisabled(isInitialStickyBroadcast());
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mContext.registerReceiver(mWifiStateReceiver, intentFilter);
    }

    /**
     * 移除wifi状态监听器
     */
    public void removeWifiStateChangeListener() {

        if (mWifiStateReceiver != null) {
            mContext.unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
        mWifiStateChangeListener = null;
    }

    /**
     * 连接接入点
     *
     * @param scanResult
     * @param password
     * @return
     */
    public boolean connect(ScanResult scanResult, String password) {
        WifiEncrypt wifiEncrypt = WifiEncrypt.distinguish(scanResult);
        return connect(wifiEncrypt, scanResult.SSID, password);
    }

    /**
     * 连接接入点
     *
     * @param wifiEncrypt
     * @param SSID
     * @param password
     * @return
     */
    public boolean connect(WifiEncrypt wifiEncrypt, String SSID, String password) {

        WifiConfiguration configuration = mWifiHelper.createWifiConfig(wifiEncrypt, SSID, password);

        int networkId = mWifiManager.addNetwork(configuration);
        if (networkId == -1) {
            networkId = mWifiHelper.getExistingNetworkId(SSID);

            Log.i(TAG, "networkId of existing network is " + networkId);

            if (networkId == -1) {
                Log.e(TAG, "Couldn't add network with SSID: " + SSID);
                return false;
            }
        }

        mWifiManager.disconnect();
        boolean bool = mWifiManager.enableNetwork(networkId, true);
        mWifiManager.reconnect();

        return bool;
    }

    /**
     * 设置网路状态变化监听
     *
     * @param listener
     */
    public void setNetworkStateChangeListener(@NonNull NetworkStateChangeListener listener) {

        mNetworkStateChangeListener = listener;
        mNetworkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                final String action = intent.getAction();

                if (mNetworkStateChangeListener != null) {
                    if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                        NetworkInfo info =
                            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        NetworkInfo.State state = info.getState();
                        NetworkInfo.DetailedState detailedState = info.getDetailedState();
                        mNetworkStateChangeListener.onNetworkStateChange(info, state,
                            detailedState);
                    } else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                        int error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
                        if (error == WifiManager.ERROR_AUTHENTICATING) {
                            // 密码错误
                            mNetworkStateChangeListener.onErrorAuthenticating();
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);// 密码校验

        mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
    }

    /**
     * 移除网络状态监听器
     */
    public void removeNetworkStateChangeListener() {

        if (mNetworkStateReceiver != null) {
            mContext.unregisterReceiver(mNetworkStateReceiver);
            mNetworkStateReceiver = null;
        }

        mNetworkStateChangeListener = null;
    }

    /**
     * 绑定网络
     *
     * @param SSID
     * @param listener
     */
    @TargetApi(LOLLIPOP)
    public void bindToNetwork(final String SSID, final NetworkStateChangeListener listener) {
        if (SDK_INT < LOLLIPOP) {
            return;
        }

        Log.i(TAG, "绑定网络到：" + SSID);

        NetworkRequest request =
            new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        networkCallback = networkCallback(SSID, listener);
        mConnectivityManager.registerNetworkCallback(request, networkCallback);
    }

    @TargetApi(LOLLIPOP)
    ConnectivityManager.NetworkCallback networkCallback(final String SSID,
        final NetworkStateChangeListener listener) {
        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {

                NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
                Log.i(TAG, "当前可以网络: " + networkInfo);

                if (WifiHelper.areEqual(networkInfo.getExtraInfo(), SSID)) {
                    mConnectivityManager.unregisterNetworkCallback(this);
                    networkCallback = null;

                    bindToRequiredNetwork(network);

                    Log.i(TAG, format("应用绑定到网络：%s", SSID));

                    listener.onNetworkBound();
                }
            }
        };
    }

    @TargetApi(LOLLIPOP)
    void bindToRequiredNetwork(Network network) {
        if (SDK_INT >= M) {
            mConnectivityManager.bindProcessToNetwork(network);
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network);
        }
    }

    /**
     * 清除网络绑定
     */
    public @TargetApi(LOLLIPOP)
    void clearNetworkBinding() {

        if (networkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(networkCallback);
            Log.i(TAG, "解除网路绑定回调");
        }

        if (SDK_INT < LOLLIPOP || getBoundNetworkForProcess() == null) {
            // 没绑定到网络，不用管
            return;
        }

        // 清空绑定的网络
        bindToRequiredNetwork(null);
    }

    /**
     * 获取一下绑定的网络
     *
     * @return
     */
    public @TargetApi(LOLLIPOP)
    Network getBoundNetworkForProcess() {
        if (SDK_INT >= M) {
            return mConnectivityManager.getBoundNetworkForProcess();
        } else {
            return ConnectivityManager.getProcessDefaultNetwork();
        }
    }

    /**
     * 告诉系统网络绑定完成
     */
    public @TargetApi(LOLLIPOP)
    void reportBoundNetworkConnectivity() {
        if (SDK_INT < LOLLIPOP) {
            return;
        }

        if (SDK_INT >= M) {
            Network defaultNetwork = mConnectivityManager.getBoundNetworkForProcess();
            mConnectivityManager.reportNetworkConnectivity(defaultNetwork, true);
        } else {
            Network defaultNetwork = ConnectivityManager.getProcessDefaultNetwork();
            mConnectivityManager.reportBadNetwork(defaultNetwork);
        }
    }
}