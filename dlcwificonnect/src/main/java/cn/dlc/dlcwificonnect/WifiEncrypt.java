package cn.dlc.dlcwificonnect;

import android.net.wifi.ScanResult;

/**
 * wifi加密方式
 */
public enum WifiEncrypt {

    NONE, WEP, WPA, EAP;

    /**
     * 辨别加密类型
     *
     * @param scanResult
     * @return
     */
    public static WifiEncrypt distinguish(ScanResult scanResult) {
        return distinguish(scanResult.capabilities);
    }

    /**
     * 辨别加密类型
     *
     * @param scanResult_capabilities
     * @return
     */
    public static WifiEncrypt distinguish(String scanResult_capabilities) {

        if (scanResult_capabilities.contains("WPA")) {
            return WPA;
        } else if (scanResult_capabilities.contains("WEP")) {
            return WEP;
        } else if (scanResult_capabilities.contains("EAP")) {
            return EAP;
        } else {
            return NONE;
        }
    }

}
