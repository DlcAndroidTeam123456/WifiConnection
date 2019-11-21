package cn.dlc.wifilistdemo;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.view.View;
import android.widget.TextView;
import cn.dlc.commonlibrary.ui.adapter.BaseRecyclerAdapter;
import cn.dlc.dlcwificonnect.WifiEncrypt;
import cn.dlc.dlcwificonnect.WifiHelper;

/**
 * Created by liuwenzhuo on 2017/11/6.
 */

public class WifiAdapter extends BaseRecyclerAdapter<ScanResult> {

    private String connectedSSID = "";

    @Override
    public int getItemLayoutId(int viewType) {
        return R.layout.item_wifi;
    }

    @Override
    public void onBindViewHolder(CommonHolder holder, int position) {

        ScanResult item = getItem(position);

        TextView mTvWifiName = holder.getView(R.id.tv_wifi_name);
        mTvWifiName.setText(item.SSID);

        View mark = holder.getView(R.id.mark);

        holder.setText(R.id.tv_type, WifiEncrypt.distinguish(item).name());

        boolean isConnet = WifiHelper.areEqual(connectedSSID, item.SSID);

        if (isConnet) {
            mark.setVisibility(View.VISIBLE);
        } else {
            mark.setVisibility(View.INVISIBLE);
        }
    }

    public void setConnectedSSID(String connectedSSID) {

        if (connectedSSID == null) {
            connectedSSID = "";
        }
        this.connectedSSID = connectedSSID;
        notifyDataSetChanged();
    }

    public void setConnected(WifiInfo wifiInfo) {
        if (wifiInfo == null) {
            setConnectedSSID(null);
        } else {
            setConnectedSSID(wifiInfo.getSSID());
        }
    }
}
