package com.example.homelocalization.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by rujiezhou on 4/12/14.
 */


public class SensorActivity extends Activity implements SensorEventListener {

    private Context sensorContext;
    private Map<String, Double> sensorValuesMap;
    private Sensor sensorPressure;
    private Sensor sensorMagnetic;
    private SensorManager sensorManager;
    private WifiManager wifiManager;
    private TextView sensorInfoTextView;
    private TextView wifiInfoTextView;
    private Button saveButton;
    private Button sendButton;
    private ToggleButton trainingToggleButton;
    private ToggleButton filterKnownApsToggleButton;
    private ToggleButton autoSampleToggleButton;
    private Button scanWifiApButton;
    private EditText trainingNumberEditText;

    private static HashMap<String, WifiData> knownApWifiDataMap;
    private static ArrayList<WifiData> foundWifiDataList;
    private static HashMap<String, WifiData> foundWifiDataMap;

    private static boolean ifFilterKnownAP;
    private static ArrayList<String> knownApArrayList;

    private String directoryToSave;
    private String fileNameTraining;
    private String fileNameAllOutputs;
    private String fileNameSamples;
    private String fileNameSample;
    private String fileNameSingleOutput;
    private String fileNameAllSensors;
    private String knownApListString;

    TimerTask timerTask;
    final Handler handler = new Handler();
    Timer t = new Timer();

    private static boolean ifAutoSample;

    static final int INTERVAL = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        sensorContext = this.getApplicationContext();

        Toast.makeText(this,"Started",500).show();

        sensorInfoTextView = (TextView) findViewById(R.id.sensorInfoTextView);
        wifiInfoTextView = (TextView) findViewById(R.id.wifiInfoTextView);
        saveButton = (Button) findViewById(R.id.saveButton);
        sendButton = (Button) findViewById(R.id.sendButton);
        trainingToggleButton = (ToggleButton) findViewById(R.id.trainingToggleButton);
        filterKnownApsToggleButton = (ToggleButton) findViewById(R.id.filterKnownApsToggleButton);
        autoSampleToggleButton = (ToggleButton) findViewById(R.id.autoSampleToggleButton);
        scanWifiApButton = (Button) findViewById(R.id.scanWifiApButton);
        trainingNumberEditText = (EditText) findViewById(R.id.trainingNumberEditText);

        getValuesFromMainActivity();

        sensorManager = (SensorManager) getApplication().getSystemService(
                Context.SENSOR_SERVICE);
        sensorPressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, sensorMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorPressure, SensorManager.SENSOR_DELAY_NORMAL);

        sensorValuesMap = new HashMap<String, Double>();
        sensorValuesMap.put("Magnetometer", -1.0);
        sensorValuesMap.put("Barometer", -1.0);

        populateKnownApList();

        ifFilterKnownAP = filterKnownApsToggleButton.isChecked();
        ifAutoSample = autoSampleToggleButton.isChecked();

        foundWifiDataList = new ArrayList<WifiData>();
        foundWifiDataMap = new HashMap<String, WifiData>();
        knownApWifiDataMap = new HashMap<String, WifiData>();
        for(String knownAp : knownApArrayList) knownApWifiDataMap.put(knownAp, new WifiData(knownAp));

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        this.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                handleWifiScanResult();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();


        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveToFile();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new PostDataAsyncTask().execute();
            }
        });

        scanWifiApButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiManager.startScan();
            }
        });

        filterKnownApsToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ifFilterKnownAP = filterKnownApsToggleButton.isChecked();
            }
        });

        autoSampleToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    scanWifiApButton.setEnabled(false);
                    saveButton.setEnabled(false);
                    sendButton.setEnabled(false);
                    trainingToggleButton.setChecked(false);
                    trainingToggleButton.setEnabled(false);
                    trainingNumberEditText.setText("0");
                    trainingNumberEditText.setEnabled(false);
                    filterKnownApsToggleButton.setChecked(true);
                    filterKnownApsToggleButton.setEnabled(false);
                    // start the auto scan process

                } else {
                    scanWifiApButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    sendButton.setEnabled(true);
                    trainingToggleButton.setEnabled(true);
                    trainingNumberEditText.setText("1");
                    trainingNumberEditText.setEnabled(true);
                    filterKnownApsToggleButton.setEnabled(true);
                }

                ifAutoSample = isChecked;
            }
        });


        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(ifAutoSample) autoSample();
                        //Toast.makeText(sensorContext, "timer event", 500).show();
                    }
                });
            }
        };
        t.schedule(timerTask, 300, INTERVAL);

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {

        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_PRESSURE)
        {
            sensorValuesMap.put("Barometer", (double) event.values[0]);
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            sensorValuesMap.put("Magnetometer", Math.sqrt(event.values[0]*event.values[0] +
                    event.values[1]*event.values[1] + event.values[2]*event.values[2]));
        }

        String strB = new DecimalFormat("##.##").format(sensorValuesMap.get("Barometer"));
        String strM = new DecimalFormat("##.##").format(sensorValuesMap.get("Magnetometer"));
        String str = "magnetometer," + strM + ";\n" + "barometer," + strB + ";";

        sensorInfoTextView.setText(str);

    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    private void handleWifiScanResult(){

        StringBuffer sb = new StringBuffer();

        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);

        for (ScanResult e1 : wifiManager.getScanResults()){
            WifiData w = new WifiData(e1.BSSID, e1.SSID, e1.frequency+"", e1.capabilities, e1.level*1.0, "");

            foundWifiDataMap.put(e1.BSSID, w);
        }

        foundWifiDataList = new ArrayList<WifiData>(foundWifiDataMap.values());

        Collections.sort(foundWifiDataList, new CustomComparator());

        if (!ifFilterKnownAP)
        {
            for(WifiData w : foundWifiDataList)
            {
                sb.append(w.printWifiData(1) + "\n");
            }
            wifiInfoTextView.setText(sb.toString());
        }
        else
        {

            Iterator it = knownApWifiDataMap.entrySet().iterator();

            while(it.hasNext())
            {

                Map.Entry knownApWifiDataPairs = (Map.Entry) it.next();

                if(foundWifiDataMap.containsKey(knownApWifiDataPairs.getKey()))
                {
                    WifiData w = foundWifiDataMap.get(knownApWifiDataPairs.getKey());
                    knownApWifiDataMap.get(w.BSSID).level = w.level*1.0;
                    knownApWifiDataMap.get(w.BSSID).SSID = w.SSID;
                    knownApWifiDataMap.get(w.BSSID).capabilities = w.capabilities;
                    knownApWifiDataMap.get(w.BSSID).frequency = w.frequency+"";

                    sb.append(knownApWifiDataMap.get(w.BSSID).printWifiData(0) + "\n");

                }
                else
                { //cannot find a known AP in range
                    WifiData w = (WifiData)knownApWifiDataPairs.getValue();
                    w.level = -618.0;
                    sb.append(w.printWifiData(0) + "\n");

                }
            }
            wifiInfoTextView.setText(sb.toString());

        }
   }

    private void saveToFile(){

        Calendar c = Calendar.getInstance();
        String timestamp = c.get(Calendar.MONTH) + "-" + c.get(Calendar.DATE) + "-" + c.get(Calendar.HOUR) +
                "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND);

        BufferedWriter bw;
        try {

            String label = trainingNumberEditText.getText().toString();
            boolean ifTraining = trainingToggleButton.isChecked();

            String barometerValueStr = new DecimalFormat("##.##").format(sensorValuesMap.get("Barometer"));
            String magnetometerValueStr = new DecimalFormat("##.##").format(sensorValuesMap.get("Magnetometer"));

            if (ifTraining)
            {
                //// record all training wifi data to send to webserver
                bw = new BufferedWriter(new FileWriter(new File(directoryToSave + "/" + fileNameTraining), true));

                for(String knownAp : knownApArrayList) // save all data from known wifi map
                {
                    bw.write(knownApWifiDataMap.get(knownAp).level+",");
                }

                bw.write(label+"\n");

                bw.close();

            }
            else //isSample
            {
                bw = new BufferedWriter(new FileWriter(new File(directoryToSave + "/" + fileNameSamples), true));

                // record all sample wifi data
                for(String knownAp : knownApArrayList) // save all data from known wifi map
                {
                    bw.write(knownApWifiDataMap.get(knownAp).level+",");
                }
                bw.write(label+"\n");

                bw.close();

                // record single sample wifi data to send to webserver
                bw = new BufferedWriter(new FileWriter(new File(directoryToSave + "/" + fileNameSample), false));

                for(String knownAp : knownApArrayList) // save all data from known wifi map
                {
                    bw.write(knownApWifiDataMap.get(knownAp).level+",");
                }
                bw.write(label+"\n");

                bw.close();
            }


            // record all history wifi outputs
            bw = new BufferedWriter(new FileWriter(new File(directoryToSave + "/" + fileNameAllOutputs), true));
            bw.write(timestamp+";\n");
            bw.write(ifTraining? "t," : "s,");
            bw.write(";\n");
            bw.write("magnetometer," + magnetometerValueStr + ";\n"
                    + "barometer," + barometerValueStr + ";\n");

            for(String knownAp : knownApArrayList) // save all data from known wifi map
            {
                bw.write(knownApWifiDataMap.get(knownAp).printWifiData(3) + "\n");
            }

            bw.write("\n");
            bw.close();

            // record single wifi output to send to webserver
            FileOutputStream out = new FileOutputStream(directoryToSave + "/" + fileNameSingleOutput);
            String str = "";
            str += timestamp + ";\n";
            str += ifTraining? "t," : "s,";
            str += label + ";\n";
            str += "magnetometer," + magnetometerValueStr + ";\n"
                    + "barometer," + barometerValueStr + ";\n";
            for(String knownAp : knownApArrayList) // save all data from known wifi map
            {
                str += knownApWifiDataMap.get(knownAp).printWifiData(3) + "\n";
            }
            out.write(str.trim().getBytes());
            out.close();

            // record all sensor info
            bw = new BufferedWriter(new FileWriter(new File(directoryToSave + "/" + fileNameAllSensors), true));
            bw.write(magnetometerValueStr + "," + barometerValueStr + "," + label + "\n");
            bw.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public class PostDataAsyncTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();
            // do stuff before posting data
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String str = "";

                postDataFile(fileNameSingleOutput, "upload_stream.php");
                postDataFile(fileNameSample, "upload_stream_sample.php");
                postDataFile(fileNameSamples, "upload_stream_samples.php");
                str = postDataFile(fileNameTraining, "upload_stream_training.php");

                return str;

            } catch (Exception e) {
                e.printStackTrace();
                return "Exception Caught";
            }
        }


        @Override
        protected void onPostExecute(String result) {

            if(result.contains("Exception")){
                Toast.makeText(sensorContext, "Exception! "+result, 500).show();
            }
            else if (result==null){
                Toast.makeText(sensorContext, "Success!", 500).show();
            }
            else {
                Toast.makeText(sensorContext, "Response! "+result, 500).show();
            }
        }
    }

    private String postDataFile(String fileName, String phpScriptName) {
        // Create a new HttpClient and Post Header
        // HttpClient
        DefaultHttpClient httpClient = new DefaultHttpClient();

        // http 401 authentication requirement
        Credentials creds = new UsernamePasswordCredentials("OurFamily", "tylwyth");
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);

        httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httpPost = new HttpPost("http://www.taidsaccount.com/indoor_localization_system/" + phpScriptName);
        File file = new File(directoryToSave + "/" + fileName);
        //File file = new File(db_path);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new FileBody(file, "binary/octet-stream");
        mpEntity.addPart("userfile", cbFile);
        try {
            httpPost.setEntity(mpEntity);
        }
        catch(Exception ex)
        {//do nothing
        }


        String str="";
        try {

            str = str + " " + "executing request " + httpPost.getRequestLine();

            //System.out.println("executing request " + httpPost.getRequestLine());
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity resEntity = response.getEntity();

            if (resEntity != null) {

                str = EntityUtils.toString(resEntity);
                resEntity.consumeContent();
            }

            httpClient.getConnectionManager().shutdown();

            return "Finish postDataFile(), response " + str;
        }
        catch(Exception e)
        {
            str = str + " "+e.toString();
            return "Exception in postDataFile(): "+str;
        }

    }

    private void autoSample()
    {
        wifiManager.startScan();
        saveToFile();
        new PostDataAsyncTask().execute();
    }

    //
    private void populateKnownApList()
    {
        String strs[] = knownApListString.split("\n");
        knownApArrayList = new ArrayList<String>(Arrays.asList(strs));

        // sunday used APS carpenter first floor
        // "9c:1c:12:e0:d0:81","9c:1c:12:e0:dd:d0","9c:1c:12:e0:dd:d2","9c:1c:12:e0:dc:10","9c:1c:12:e0:dc:11","9c:1c:12:e0:dc:12"

        //third time used APS
        // "9c:1c:12:e0:c8:d0", "9c:1c:12:e0:c8:d1", "9c:1c:12:e0:c8:d2", "9c:1c:12:e0:c8:d3"

        //second time used APs
        // "9c:1c:12:e0:c7:72","9c:1c:12:e0:c7:62","9c:1c:12:e0:c7:71"

        //first time used APs
        //"00:0b:86:54:a0:08","00:24:6c:72:cd:d3","00:24:6c:72:cd:d0"

        // THOMAS home wifi aps
        // "64:66:b3:85:3b:70","64:66:b3:85:3f:6a","e8:94:f6:84:b1:82","e8:94:f6:84:b1:68","00:30:44:12:8d:b5","dc:9f:db:6a:e8:62"

    }

    private void getValuesFromMainActivity()
    {
        directoryToSave = getIntent().getExtras().getString("directoryToSave");;
        fileNameTraining = this.getIntent().getExtras().getString("fileNameTraining");
        fileNameAllOutputs = this.getIntent().getExtras().getString("fileNameAllOutputs");
        fileNameSamples = this.getIntent().getExtras().getString("fileNameSamples");
        fileNameSample = this.getIntent().getExtras().getString("fileNameSample");
        fileNameSingleOutput = this.getIntent().getExtras().getString("fileNameSingleOutput");
        fileNameAllSensors = this.getIntent().getExtras().getString("fileNameAllSensors");
        knownApListString = this.getIntent().getExtras().getString("knownApListString");

    }

    //*******************************************************************
    //********************** comparators *********************
    //*******************************************************************
    public class CustomComparator implements Comparator<WifiData>
    {
        public int compare(WifiData e1, WifiData e2)
        {
            return (int)(e2.level - e1.level);
        }
    }


    public class CustomComparator2 implements Comparator<Map.Entry<String,ScanResult>>
    {
        public int compare(Map.Entry<String,ScanResult> m1, Map.Entry<String, ScanResult> m2)
        {
            if (m1.getValue().level > m2.getValue().level)
                return 1;
            else if (m1.getValue().level < m2.getValue().level)
                return -1;
            else
                return 0;
        }
    }
}