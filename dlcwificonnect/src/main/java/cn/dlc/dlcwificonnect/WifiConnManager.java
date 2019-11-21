package cn.dlc.dlcwificonnect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

/**
 * wifi连接管理器
 */
public class WifiConnManager
    implements WifiConnector.WifiStateChangeListener, WifiConnector.NetworkStateChangeListener {

    private static final String TAG = "WifiConnManager";

    private Context mContext;
    private WifiManager mWifiManager;
    private ConnectivityManager mConnectivityManager;

    private final WifiHelper mWifiHelper;
    private final WifiConnector mWifiConnector;

    private boolean mShouldBindToNetwork;

    private String mToConnectSSID;

    private ConnectNetworkListener mConnectNetworkListener;
    private NetworkInfoListener mNetworkInfoListener;

    /**
     * 网路信息监听器
     */
    public interface NetworkInfoListener {

        void onConnecting(NetworkInfo networkInfo, NetworkInfo.State state,
            NetworkInfo.DetailedState detailedState);
    }

    /**
     * 连接网络监听
     */
    public interface ConnectNetworkListener {

        /**
         * 当有网络连接成功是，注意这里不一定是自己要连的那个网络，可以用{@link WifiHelper#isConnectedToSSID(String)} 去判断
         */
        void onConnected();

        /**
         * 网络连接失败
         *
         * @param passwordError
         * @param reason
         */
        void onConnectFailure(boolean passwordError, String reason);
    }

    /**
     * wifi连接管理器
     *
     * @param context
     */
    public WifiConnManager(Context context) {

        mContext = context;

        mWifiManager =
            (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mConnectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        mWifiHelper = new WifiHelper(mContext, mWifiManager, mConnectivityManager);
        mWifiConnector = new WifiConnector(mContext, mWifiHelper);
    }

    public WifiHelper getHelper() {
        return mWifiHelper;
    }

    public WifiConnector getConnector() {
        return mWifiConnector;
    }

    public void setBindingEnabled(boolean shouldBindToNetwork) {
        mShouldBindToNetwork = shouldBindToNetwork;
    }

    public void abort() {
        mWifiConnector.removeNetworkStateChangeListener();
        mWifiConnector.removeWifiStateChangeListener();
        mWifiConnector.clearNetworkBinding();
    }

    /**
     * 开启wifi
     */
    public void enableWifi() {
        // 先移除上一个监听器
        mWifiConnector.removeWifiStateChangeListener();
        mWifiConnector.setWifiStateChangeListener(this);
        mWifiConnector.enableWifi(true);
    }

    /**
     * 监控网络连接状态变化
     */
    public void listenNetworkInfo(NetworkInfoListener listener) {

        mNetworkInfoListener = listener;
        // 先移除上一个监听器
        mWifiConnector.removeNetworkStateChangeListener();
        mWifiConnector.setNetworkStateChangeListener(this);
    }

    /**
     * 移除网络连接状态监听
     */
    public void removeListenNetworkInfo() {
        mNetworkInfoListener = null;
        mWifiConnector.removeNetworkStateChangeListener();
        mWifiConnector.clearNetworkBinding();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        mWifiHelper.disconnect();
    }

    /**
     * 连接wifi
     * @param scanResult
     * @param password
     * @param listener
     */
    public void connect(ScanResult scanResult, String password, ConnectNetworkListener listener) {
        connect(WifiEncrypt.distinguish(scanResult), scanResult.SSID, password, listener);
    }

    public void connect(WifiEncrypt wifiEncrypt, String SSID, String password,
        ConnectNetworkListener listener) {

        mConnectNetworkListener = listener;

        mToConnectSSID = SSID;

        if (mWifiHelper.isConnectedToSSID(SSID)) {
            listener.onConnected();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mShouldBindToNetwork) {
            mWifiConnector.bindToNetwork(SSID, this);
        }

        boolean connectingToSSID = mWifiConnector.connect(wifiEncrypt, SSID, password);
        if (!connectingToSSID) {
            String reason = "Error while enabling network.";
            listener.onConnectFailure(false, reason);
        }
    }

    public Network getBoundNetworkForProcess() {
        return mWifiConnector.getBoundNetworkForProcess();
    }

    public void checkBoundNetworkConnectivity() {
        mWifiConnector.reportBoundNetworkConnectivity();
    }

    @Override
    public void onWifiEnabled(boolean initialStickyBroadcast) {
        mWifiConnector.removeWifiStateChangeListener();
        scheduleWifiScan();
    }

    @Override
    public void onWifiDisabled(boolean initialStickyBroadcast) {
        if (initialStickyBroadcast) {
            mWifiConnector.enableWifi(true);
        } else {
            abort();
        }
    }

    @Override
    public void onNetworkStateChange(NetworkInfo networkInfo, NetworkInfo.State state,
        NetworkInfo.DetailedState detailedState) {

        mNetworkInfoListener.onConnecting(networkInfo, state, detailedState);

        if (mConnectNetworkListener != null) {
            if (state == NetworkInfo.State.CONNECTED) {
                mConnectNetworkListener.onConnected();
                mConnectNetworkListener = null;
            }
        }
    }

    @Override
    public void onErrorAuthenticating() {

        Log.e(TAG, "wifi密码验证失败");

        if (mConnectNetworkListener != null) {
            mConnectNetworkListener.onConnectFailure(true, "password error!");
            mConnectNetworkListener = null;
        }
    }

    @Override
    public void onNetworkBound() {
        if (mConnectNetworkListener != null) {
            mConnectNetworkListener.onConnected();
            mConnectNetworkListener = null;
        }
    }

    /**
     * 延迟执行扫描
     */
    private void scheduleWifiScan() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mWifiHelper.startScan();
            }
        }, 200);
    }
}