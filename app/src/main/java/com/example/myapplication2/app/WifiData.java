package com.example.myapplication2.app;

/**
 * Created by rujiezhou on 5/1/14.
 */
public class WifiData {

    public String BSSID = "";
    public String SSID = "";
    public String frequency = "";
    public String capabilities = "";
    public double level = -999.0;
    public String timestamp = "";

    public WifiData(){

    }

    public WifiData(String BSSID, String SSID, String frequency, String capabilities, double level, String timestamp) {
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.frequency = frequency;
        this.capabilities = capabilities;
        this.level = level;
        this.timestamp = timestamp;
    }

    public WifiData(String BSSID)
    {
        this.BSSID = BSSID;
        SSID = "";
        frequency = "";
        capabilities = "";
        level = -999.9;
        timestamp = "";
    }

    public String printWifiData(int verbose)
    {
        String str = "";

        switch(verbose)
        {
            case 3:
            {
                str += BSSID + "," + SSID + "," + capabilities + "," + frequency + "," + level + ";";
                break;
            }
            case 0:
            {
                str += BSSID  + "," + level + ";";
                break;
            }
            case 1:
            {
                str += BSSID  + "," + SSID + "," + level + ";";
                break;
            }
            default:
                str += "nothing to print";
        }
        return str;
    }

}
