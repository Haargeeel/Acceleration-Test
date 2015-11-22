package de.beuth.ema.beschleunigungstest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Godclass
 *
 * by Ray
 */
public class MainActivityFragment extends Fragment implements SensorEventListener{

    private SensorManager sensorManager;
    private TextView x,y,z, result;
    private ProgressBar xProgress, yProgress, zProgress, resultProgress;
    private Button startButton;
    private static final String TAG = "Fragment";
    private long timer;
    private ArrayList<float[]> results;
    private int fertig = 0;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        x = (TextView) view.findViewById(R.id.xResult);
        y = (TextView) view.findViewById(R.id.yResult);
        z = (TextView) view.findViewById(R.id.zResult);
        result = (TextView) view.findViewById(R.id.result);
        xProgress = (ProgressBar) view.findViewById(R.id.xProgress);
        xProgress.setMax(100);
        yProgress = (ProgressBar) view.findViewById(R.id.yProgress);
        yProgress.setMax(100);
        zProgress = (ProgressBar) view.findViewById(R.id.zProgress);
        zProgress.setMax(100);
        resultProgress = (ProgressBar) view.findViewById(R.id.resultProgress);
        resultProgress.setMax(100);
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        results = new ArrayList<>();
        final SensorEventListener sensorEventListener = this;
        startButton = (Button) view.findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
                timer = System.currentTimeMillis();
            }
        });
        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();
        if (System.currentTimeMillis() - timer < 10000) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                x.setText(String.valueOf(event.values[0]));
                y.setText(String.valueOf(event.values[1]));
                z.setText(String.valueOf(event.values[2]));
                double xRel = event.values[0] * 100 / 9.81;
                double yRel = event.values[1] * 100 / 9.81;
                double zRel = event.values[2] * 100 / 9.81;
                double r = Math.sqrt(event.values[0] * event.values[0] +
                                   event.values[1] * event.values[1] +
                                   event.values[2] * event.values[2]);
                result.setText(String.valueOf(r));
                double rRel = r * 100 / 9.81;
                xProgress.setProgress((int)xRel);
                yProgress.setProgress((int)yRel);
                zProgress.setProgress((int)zRel);
                resultProgress.setProgress((int)rRel);
                float[] tmp = new float[5];
                tmp[0] = (float) (now - timer);
                tmp[1] = event.values[0];
                tmp[2] = event.values[1];
                tmp[3] = event.values[2];
                tmp[4] = (float) r;
                results.add(tmp);
                Log.i(TAG, "time: " + (now - timer));
            }
        } else {
            if (fertig == 0) {
//                for (int i = 0; i < results.size(); i++) {
//                    Log.i(TAG, "x: " + results.get(i)[0]);
//                    Log.i(TAG, "y: " + results.get(i)[1]);
//                    Log.i(TAG, "z: " + results.get(i)[2]);
//                }
                fertig = 1;
                try {
                    saveCSV();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveCSV() throws IOException{
        // stop the sensor listener
        final SensorEventListener sensorEventListener = this;
        sensorManager.unregisterListener(sensorEventListener);

        // create the directory if it's not there now
        Log.i(TAG, "Environment: " + Environment.getExternalStorageDirectory());
        File folder = new File(Environment.getExternalStorageDirectory() + "/EMA");
        boolean var = false;
        if (!folder.exists()) {
            var = folder.mkdir();
        }
        if (var)
            Log.i(TAG, "folder created");
        else
            Log.i(TAG, "folder not created haha!");

        // create the csv file
        String suffix = folder.listFiles().length + ".csv";
        final String filename = folder.toString() + "/Tabelle" + suffix;
        new Thread() {
            public void run() {
                try {
                    FileWriter fw = new FileWriter(filename);
                    fw.append("Zeit,x,y,z,Betrag\n");
                    for (int i = 0; i < results.size(); i++) {
                        fw.append(String.valueOf(results.get(i)[0]));
                        fw.append(',');
                        fw.append(String.valueOf(results.get(i)[1]));
                        fw.append(',');
                        fw.append(String.valueOf(results.get(i)[2]));
                        fw.append(',');
                        fw.append(String.valueOf(results.get(i)[3]));
                        fw.append(',');
                        fw.append(String.valueOf(results.get(i)[4]));
                        fw.append("\n");

                    }
                    fw.close();
                    // reset all the data
                    results.clear();
                    fertig = 0;

                    // show the user that we are done
                    Handler h = new Handler(getContext().getMainLooper());
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(), "File created: " + filename, Toast.LENGTH_LONG).show();
                        }
                    });
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
