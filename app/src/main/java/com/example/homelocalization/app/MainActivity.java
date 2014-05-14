package com.example.homelocalization.app;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends ActionBarActivity {

    String directoryToSave;
    final String FILENAME_TRAINING = "training.txt";
    final String FILENAME_ALLOUTPUTS = "all_outputs.txt";
    final String FILENAME_SAMPLES = "samples.txt";
    final String FILENAME_SAMPLE = "sample.txt";
    final String FILENAME_SINGLEOUTPUT = "sensorStrength.txt";
    final String FILENAME_ALLSENSORS = "all_sensors.txt";
    final String FILENAME_KNOWNAP = "known_ap.txt";

    Button startRecordButton;
    Button resetTrainingButton;
    Button resetAllButton;
    EditText knownApEditText;

    String knownApListString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRecordButton = (Button) findViewById(R.id.startRecordButton);
        resetTrainingButton = (Button) findViewById(R.id.resetTrainingButton);
        resetAllButton = (Button) findViewById(R.id.resetAllButton);
        knownApEditText = (EditText) findViewById(R.id.knownApEditText);

        knownApListString = knownApEditText.getText().toString();

        makeNecessaryInitialSetup();

        startRecordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // save potentially modified all known ap file
                knownApListString = knownApEditText.getText().toString();
                writeToFile(FILENAME_KNOWNAP, knownApEditText.getText().toString());

                Intent intent = new Intent(v.getContext(), SensorActivity.class);

                intent.putExtra("directoryToSave", directoryToSave);
                intent.putExtra("fileNameTraining", FILENAME_TRAINING);
                intent.putExtra("fileNameAllOutputs", FILENAME_ALLOUTPUTS);
                intent.putExtra("fileNameSample", FILENAME_SAMPLE);
                intent.putExtra("fileNameSamples", FILENAME_SAMPLES);
                intent.putExtra("fileNameSingleOutput", FILENAME_SINGLEOUTPUT);
                intent.putExtra("fileNameAllSensors", FILENAME_ALLSENSORS);
                intent.putExtra("knownApListString", knownApListString);

                startActivityForResult(intent, 0);

            }
        });

        resetAllButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // erase all files

            }
        });

        resetTrainingButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // erase the training file
            }
        });
    }

    private void makeNecessaryInitialSetup() {
        //make the folder for this assignment
        String folderPath = "/sdcard/Ubicomp-INFO4120/project";

        File folder = new File(folderPath);
        if(!folder.exists())
        {
            folder.mkdir();
            //directory name to save
            try {
                directoryToSave = folder.getCanonicalPath();
                writeToFile(FILENAME_KNOWNAP, knownApListString);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        else
        {
            //directory name to save
            try {
                directoryToSave = folder.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            File f = new File(directoryToSave + "/" + FILENAME_KNOWNAP);
            if (!f.exists())
            {
                writeToFile(FILENAME_KNOWNAP, knownApListString);
            }
            else
            {
                knownApEditText.setText(readFromFile(FILENAME_KNOWNAP));
            }
        }



    }


    private void writeToFile(String fileName, String content)
    {
        try
        {
            FileOutputStream out = new FileOutputStream(directoryToSave + "/" + fileName);
            out.write(content.trim().getBytes());
            out.close();
        }
        catch(Exception ex)
        {
            System.out.println("writeToFile is wrong!" + ex);

        }
    }

    private String readFromFile(String fileName)
    {
        try
        {
            FileInputStream in = new FileInputStream(directoryToSave + "/" + fileName);
            int k = 0;
            String rtnStr = "";
            while((k = in.read()) != -1)
            {
                rtnStr += (char)k;
            }
            return rtnStr;
        }
        catch(Exception ex)
        {
            System.out.println(ex);
            return "File NOT Found\n";
        }

    }

}
