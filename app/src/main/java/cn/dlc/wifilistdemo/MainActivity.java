package cn.dlc.wifilistdemo;

import android.content.DialogInterface;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import cn.dlc.commonlibrary.ui.adapter.BaseRecyclerAdapter;
import cn.dlc.commonlibrary.ui.base.BaseCommonActivity;
import cn.dlc.dlcwificonnect.WifiConnManager;
import cn.dlc.dlcwificonnect.WifiEncrypt;
import cn.dlc.dlcwificonnect.WifiHelper;
import cn.dlc.dlcwificonnect.WifiScanner;
import com.licheedev.myutils.LogPlus;
import java.util.List;

public class MainActivity extends BaseCommonActivity {

    private WifiConnManager mWifiConnManager;
    private WifiHelper mWifiHelper;
    private WifiAdapter mWifiAdapter;
    private RecyclerView mRecyclerView;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    private void initRecyclerView() {

        mRecyclerView = findViewById(R.id.recycler_view);

        mWifiAdapter = new WifiAdapter();
        mWifiAdapter.setOnItemClickListener(new BaseRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ViewGroup parent, BaseRecyclerAdapter.CommonHolder holder,
                int position) {
                LogPlus.e("连接WiFi");

                ScanResult scanResult = mWifiAdapter.getItem(position);
                if (mWifiHelper.isConnectedToSSID(scanResult.SSID)) {
                    // 已连接，显示断开对话框，虽说没啥卵用，断了会又会自动连上
                    showDisconnectDialog(scanResult);
                } else {
                    // 获取加密方式
                    WifiEncrypt encrypt = WifiEncrypt.distinguish(scanResult);

                    switch (encrypt) {
                        case WEP:
                        case WPA: {
                            showPasswordDialog(scanResult);
                        }
                        break;
                        case EAP:
                            showToast("不支持的设备");
                            break;
                        default:
                            connectWifi(scanResult, "");
                            break;
                    }
                }
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mWifiAdapter);
    }

    /**
     * 初始化wifi工具
     */
    private void initWifiTools() {
        mWifiConnManager = new WifiConnManager(this);
        mWifiHelper = mWifiConnManager.getHelper();

        mWifiConnManager.setBindingEnabled(true); // 给Android高版本用的
        // 监听网络信息
        mWifiConnManager.listenNetworkInfo(new WifiConnManager.NetworkInfoListener() {
            @Override
            public void onConnecting(NetworkInfo networkInfo, NetworkInfo.State state,
                NetworkInfo.DetailedState detailedState) {

                if (state == NetworkInfo.State.DISCONNECTED) {
                    mWifiAdapter.setConnectedSSID(null);
                    LogPlus.e("连接已断开");
                } else if (state == NetworkInfo.State.CONNECTED) {

                    WifiInfo wifiInfo = mWifiHelper.getConnectionInfo();

                    mWifiAdapter.setConnected(wifiInfo);

                    LogPlus.e("已连接到网络:" + wifiInfo.getSSID());
                } else {
                    if (detailedState == NetworkInfo.DetailedState.CONNECTING) {
                        LogPlus.e("连接中...");
                    } else if (detailedState == NetworkInfo.DetailedState.AUTHENTICATING) {
                        LogPlus.e("正在验证身份信息...");
                    } else if (detailedState == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        LogPlus.e("正在获取IP地址...");
                    } else if (detailedState == NetworkInfo.DetailedState.FAILED) {
                        LogPlus.e("连接失败");
                    }
                }
            }
        });

        // 自定义扫描结果过滤器
        WifiScanner.ScanResultFilter resultFilter = new WifiScanner.ScanResultFilter() {
            @Override
            public boolean letItGo(ScanResult scanResult) {
                // 只要包含"usr"的
                return scanResult.SSID.toUpperCase().contains("USR");
            }
        };

        // todo 设置顾虑器，可选
        //mWifiHelper.setScanResultFilter(resultFilter);

        if (mWifiHelper.isWifiEnabled()) {
            mWifiAdapter.setNewData(mWifiHelper.getScanResults(true));
            mWifiAdapter.setConnected(mWifiHelper.getConnectionInfo());
        } else {
            // 先设定监听器，开启wifi之后会扫描列表，回调后会取消监听
            mWifiHelper.setScanListener(new WifiScanner.ScanListener() {
                @Override
                public void onScanResults(WifiScanner wifiScanner) {
                    List<ScanResult> scanResults = wifiScanner.getScanResults(true);

                    mWifiAdapter.setNewData(scanResults);
                }
            });
            mWifiConnManager.enableWifi();
        }
    }

    /**
     * 显示断开连接对话框
     *
     * @param scanResult
     */
    private void showDisconnectDialog(ScanResult scanResult) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("断开 " + scanResult.SSID)
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 隐藏对话框
                    dialog.dismiss();
                }
            })
            .setPositiveButton("断开", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 断开
                    mWifiConnManager.disconnect();
                }
            })
            .show();
    }

    /**
     * 显示密码对话框
     *
     * @param scanResult
     */
    private void showPasswordDialog(final ScanResult scanResult) {
        PasswordDialog passwordDialog = new PasswordDialog(this);
        passwordDialog.show(new PasswordDialog.OnClickConnectListener() {
            @Override
            public void toConnect(PasswordDialog dialog, String password) {
                dialog.dismiss();
                connectWifi(scanResult, password);
            }
        });
    }

    /**
     * 连接WIFI
     *
     * @param scanResult
     */
    private void connectWifi(final ScanResult scanResult, String password) {

        showWaitingDialog("正在连接", false);

        mWifiConnManager.connect(scanResult, password,
            new WifiConnManager.ConnectNetworkListener() {
                @Override
                public void onConnected() {

                    dismissWaitingDialog();

                    if (mWifiHelper.isConnectedToSSID(scanResult.SSID)) {
                        LogPlus.e("*********连接成功*************");
                        showToast("连接成功！");
                    } else {
                        LogPlus.e(
                            "*********连接失败，连到" + mWifiHelper.getConnectionInfo() + "*************");
                        showToast("连接失败，请重新连接！");
                    }
                }

                @Override
                public void onConnectFailure(boolean passwordError, String reason) {
                    dismissWaitingDialog();

                    if (passwordError) {
                        showToast("密码错误！");
                    } else {
                        showToast("连接失败！");
                    }
                }
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRecyclerView();
        initWifiTools();
    }
}
