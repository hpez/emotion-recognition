package com.hpezeshk.emotionrecognition;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.Complex;
import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";
    private static final double threshold = 0.001;
    private Handler handler;
    private static final double ALPHA = 0.001;
    private static final double TARGETS[] = {2, 10}; //for ANN Happy Angry
    private static final double THRESHOLD[] = {300, 500, 0}; //for ANN
    private static final int FACTORCOUNT = 6;
    private static final int NEURONCOUNT = 2;
    private MediaRecorder mRecorder;
    private static String mFileName = null;
    private static String testRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public double mean(ArrayList<Double> array){
        double sum = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) != null)
                sum += array.get(i);
            else
                Log.d("Zad Birun", "!!!!!!1");
        }
        return sum / array.size();
    }
    public double min(ArrayList<Double> array){
        double Min = 1000000;
        for (int i = 0; i < array.size(); i++)
            if (array.get(i) < Min)
                Min = array.get(i);
            
        return Min;
    }
    public double max(ArrayList<Double> array){
        double Max = 0;
        for (int i = 0; i < array.size(); i++)
            if (array.get(i) > Max)
                Max = array.get(i);
        return Max;
    }

    public boolean getNeuronResult(double cof[], double input[], int thresholdInd)
    {
        int res = 0;
        for (int i = 0; i < FACTORCOUNT; i++)
            res += cof[i] * input[i];
        return (res > THRESHOLD[thresholdInd]);
    }

    public void test(double input[], String files[])
    {
        for (int i = 0; i < NEURONCOUNT; i++)
            if (getNeuronResult(readFromFile(files[i]), input, i))
                testRes = files[i];
    }

    public double[] train(double input[], int target, String file, int targetInd, double cof[]) //input: frqMean ampMean freqMin ampMin freqMax ampMax
    {
//        double cof[] = readFromFile(file);
        if (cof[0] == 0)
            Log.d(TAG, "train: asdasd");
        double res = 0;
        for (int i = 0; i < FACTORCOUNT; i++)
            res += cof[i] * input[i];
        int ans = -1;
        if (res > THRESHOLD[targetInd])
            ans = 1;
        double error = (target - ans) / 2.0;
        if (error != 0)
            for (int i = 0; i < FACTORCOUNT; i++)
                cof[i] += error * input[i] * ALPHA; // da faq??
        return cof;
    }

    public double[] readFromFile(String filename)
    {
        File sdcard = Environment.getExternalStorageDirectory();

//Get the text file
        File file = new File(sdcard,filename + "Cofs");
//        Log.d("Input", String.valueOf(file.exists()));

//Read text from file
        double ans[] = new double[FACTORCOUNT];

        try {
//            Scanner scan = new Scanner(file).useDelimiter("\\s*\\n");
            int i = 0;
//            while (scan.hasNextDouble() && i < FACTORCOUNT) {
//
//            }
            FileInputStream is = new FileInputStream(file);
            Log.d(TAG, "readFromFile: " + file.exists());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            try {
                String line = reader.readLine();
                while (line != null && i < FACTORCOUNT) {
                    Log.d("StackOverflow", line);
                    ans[i++] = Double.parseDouble(line);
                    line = reader.readLine();
                }
            }
            catch (java.io.IOException e2)
            {

            }
        }
        catch (FileNotFoundException e1) {
        }
        return ans;
    }

    public void writeToFile(double[] cof, String filename)
    {
        File folder = new File(Environment.getExternalStorageDirectory().toString());
        File file = new File(folder, filename + "Cofs");
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.flush();
            for (int i = 0; i < FACTORCOUNT; i++) {
                String endl = "\n";
                writer.print(String.format("%.2f", cof[i]) + endl);
                writer.flush();
                //Log.d("Fuckin", String.format("%.2f", cof[i]) + endl);
            }
            writer.close();
        }
        catch (FileNotFoundException e) {

        }
    }

    public File[] getAllSampleFiles()
    {
        String path = Environment.getExternalStorageDirectory().toString()+"/Samples";
        File directory = new File(path);
        return directory.listFiles();
    }

    public File[] getAllTestFiles()
    {
        String path = Environment.getExternalStorageDirectory().toString()+"/Tests";
        File directory = new File(path);
        return directory.listFiles();
    }

    private void startRecording(String filename) { // filename = /Samples/Folan n.3gp
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//
//// 2. Chain together various setter methods to set the dialog characteristics
//        builder.setMessage("Recording")
//                .setTitle("Voice");
//        builder.setPositiveButton("Stop", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                stopRecording();
//            }
//        });
//
//// 3. Get the AlertDialog from create()
//        AlertDialog dialog = builder.create();
//        dialog.show();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName + filename);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final MotionEvent fevent = event;
        final Map<Integer, ArrayList> windowsFreq = new HashMap<>();
        final ArrayList<Double> windowFreq = new ArrayList();
        final Map<Integer, ArrayList> windowsAmp = new HashMap<>();
        final ArrayList<Double> windowAmp = new ArrayList();
        new AndroidFFMPEGLocator(this);
        Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
                    Display display = wm.getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    boolean isTest = ((int)fevent.getX() > width/2);
                    File[] files = isTest ? getAllTestFiles() : getAllSampleFiles();
                    for (int t = 0; t < (isTest ? 1 : 50); t++) {
                        for (File file : files) {
                            Log.d("FILE:", String.valueOf(file.exists()));

                            final int bufferSize = 4096;
                            final int fftSize = bufferSize / 2;
                            final int sampleRate = 44100;

                            AudioDispatcher audioDispatcher;
                            audioDispatcher = AudioDispatcherFactory.fromPipe(file.getAbsolutePath(), sampleRate, bufferSize, 0);
                            audioDispatcher.addAudioProcessor(new AudioProcessor() {

                                FFT fft = new FFT(bufferSize);
                                final float[] amplitudes = new float[fftSize];

                                @Override
                                public boolean process(AudioEvent audioEvent) {
                                    float[] audioBuffer = audioEvent.getFloatBuffer();
                                    fft.forwardTransform(audioBuffer);
                                    fft.modulus(audioBuffer, amplitudes);
                                    double sum = 0;
                                    int k = 0;
                                    int j = -1;
                                    boolean isWindow = false;
                                    for (int i = 0; i < amplitudes.length; i++) {
                                        if (amplitudes[i] > threshold) {
                                            if (!isWindow) {
                                                windowFreq.clear();
                                                windowAmp.clear();
                                                j++;
                                            }
                                            windowFreq.add((double) ((int) fft.binToHz(i, sampleRate)));
                                            windowAmp.add((double) amplitudes[i]);
                                            isWindow = true;
                                        } else if (amplitudes[i] < threshold) {
                                            if (isWindow) {
                                                windowsFreq.put(j, windowFreq);
                                                windowsAmp.put(j, windowAmp);
                                            }
                                            isWindow = false;
                                        }
                                        //Log.d(TAG, String.format(Locale.getDefault()
                                        //       , "Amplitude at %3d Hz: %8.3f", (int) fft.binToHz(i, sampleRate), amplitudes[i]));
                                    }
                                    //                            Log.d("JJJJJ:", Integer.toString(j));
                                    return true;
                                }

                                @Override
                                public void processingFinished() {
                                /*File folder = new File(Environment.getExternalStorageDirectory() + "/yourDirectoryName");
                                boolean success = true;
                                if (!folder.exists()) {
                                    success = folder.mkdir();
                                }
                                File file = new File(folder, "my-file-name.txt");
                                try {
                                    FileOutputStream stream = new FileOutputStream(file);
                                    try {
                                        stream.write(windowsFreq.toString().getBytes());
                                    }
                                    catch (IOException e) {

                                    }
                                    try {
                                        stream.close();
                                    }
                                    catch (IOException e)
                                    {}
                                }
                                catch (FileNotFoundException e) {

                                }*/
                                    Log.d("DONE", "done");
                                }
                            });
                            Log.d("1", "111");
                            audioDispatcher.run();
                            final double Mean[][] = new double[500][], Max[][] = new double[500][], Min[][] = new double[500][];
                            for (int i = 0; i < 500; i++) {
                                Mean[i] = new double[2];
                                Max[i] = new double[2];
                                Min[i] = new double[2];
                            }
                            Iterator it = windowsFreq.entrySet().iterator();
                            Iterator it2 = windowsAmp.entrySet().iterator();
                            double happyCofs[] = readFromFile("Happy"), angryCofs[] = readFromFile("Angry");
                            int k = 0;
                            while (it.hasNext()) {
                                Map.Entry pair = (Map.Entry) it.next();
                                ArrayList<Double> freq = (ArrayList<Double>) pair.getValue();
                                Map.Entry pair2 = (Map.Entry) it2.next();
                                ArrayList<Double> amp = (ArrayList<Double>) pair2.getValue();
                                //                        Log.d("T esh!", Integer.toString(k));
                                //                        Log.d("Size esh!", Integer.toString(freq.size()));
                                double input[] = new double[FACTORCOUNT];
                                input[0] = Mean[k][0] = mean(freq);
                                input[1] = Mean[k][1] = mean(amp);
                                input[2] = Min[k][0] = min(freq);
                                input[3] = Min[k][1] = min(amp);
                                input[4] = Max[k][0] = max(freq);
                                input[5] = Max[k][1] = max(amp);
                                k++;
                                if (isTest)
                                {
                                    Log.d("Test File Name", file.getName());
                                    String testFiles[] = {"Happy", "Angry", "Neutral"};
                                    test(input, testFiles);
                                }
                                else if (file.getName().contains("Happy")) {
                                    Log.d("Happy", "Happy");
                                    double res[] = train(input, 1, "Happy", 0, happyCofs);
                                    for (int i = 0; i < FACTORCOUNT; i++)
                                        happyCofs[i] = res[i];
                                    res = train(input, -1, "Angry", 1, angryCofs);
                                    for (int i = 0; i < FACTORCOUNT; i++)
                                        angryCofs[i] = res[i];
                                    //                            train(input, -1, "Neutral");
                                    //                            train(input, -1, "Angry", 1);
                                } else if (file.getName().contains("Neutral")) {
                                    //                            Log.d("Neutral", "Neutral");
                                    //                            train(input, -1, "Happy");
                                    //                            train(input, 1, "Neutral");
                                    //                            train(input, -1, "Angry");
                                } else if (file.getName().contains("Angry")) {
                                    Log.d("Angry", "Angry");
                                    double res[] = train(input, -1, "Happy", 0, happyCofs);
                                    for (int i = 0; i < FACTORCOUNT; i++)
                                        happyCofs[i] = res[i];
                                    res = train(input, 1, "Angry", 1, angryCofs);
                                    for (int i = 0; i < FACTORCOUNT; i++)
                                        angryCofs[i] = res[i];
                                    //                            train(input, -1, "Neutral");
                                    //                            train(input, 1, "Angry", 1);
                                }

                                //String files[] = {"Happy, Sad"};
                                //test(input, files);

                                //it.remove(); // avoids a ConcurrentModificationException
                            }
                            if (!isTest) {
                                writeToFile(happyCofs, "Happy");
                                writeToFile(angryCofs, "Angry");
                            }
                        }
                    }
                }
            });
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

// 2. Chain together various setter methods to set the dialog characteristics
        WindowManager wm = (WindowManager) MainActivity.this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        boolean isTest = ((int)fevent.getX() > width/2);
        if (isTest)
            builder.setMessage(testRes)
                .setTitle("Test");
        else
            builder.setMessage("done")
                    .setTitle("Training");

// 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
        return super.onTouchEvent(event);
    }
}
