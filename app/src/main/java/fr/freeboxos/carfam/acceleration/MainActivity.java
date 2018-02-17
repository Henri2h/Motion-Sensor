package fr.freeboxos.carfam.acceleration;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.support.design.widget.Snackbar;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.gson.Gson;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback{


    public static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_WRITE = 0;

    /**
     * Root of the layout of this Activity.
     */
    private View mLayout;


    private final float NOISE = (float) 2.0;
    TextView myTextView;
    TextView helloText;
    ProgressBar xPrg;
    ProgressBar yPrg;
    ProgressBar zPrg;

    LineChart mChart;

    boolean created = false;
    ILineDataSet xData;
    ILineDataSet yData;
    ILineDataSet zData;
    List<MotionEntry> mEntries;
    int count = 0;
    // Set distance to zero at start-up:
    float distance_X = 0;
    float velocity_X = 0;
    float distance_Z = 0;
    float velocity_Z = 0;
    float distance_Y = 0;
    float velocity_Y = 0;
    private long timestamp;
    private float mLastX, mLastY, mLastZ;
    private boolean mInitialized;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    /**
     * Called when the activity is first created.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.main_layout);

        myTextView = (TextView) findViewById(R.id.myTextView);
        helloText = (TextView) findViewById(R.id.helloText);

        mEntries = new ArrayList<>();

        xPrg = (ProgressBar) findViewById(R.id.xProgress);
        yPrg = (ProgressBar) findViewById(R.id.yProgress);
        zPrg = (ProgressBar) findViewById(R.id.zProgress);

        // in this example, a LineChart is initialized from xml
        mChart = (LineChart) findViewById(R.id.chart);

        mInitialized = false;

        //start time
        // First : get time
        timestamp = (long) (new Date().getTime());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);


        mChart.setDrawGridBackground(false);
        mChart.getDescription().setEnabled(false);

        xData = createSet("DataSet x", 255, 0, 0);
        yData = createSet("DataSet y", 0, 255, 0);
        zData = createSet("DataSet z", 0, 0, 255);

        //  xData.setColor(Color.rgb(255, 0, 0));
        //  yData.setColor(Color.rgb(0, 255, 0));
        //   zData.setColor(Color.rgb(0, 0, 255));

        xData.setDrawValues(false);
        yData.setDrawValues(false);
        zData.setDrawValues(false);

        LineData data = new LineData();
        // data.addDataSet(createSet());


        mChart.setData(data);
//        mChart.getXAxis().setDrawLabels(false);
//        mChart.getXAxis().setDrawGridLines(false);

        mChart.invalidate();

        final Button button = findViewById(R.id.btSave);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                saveFile();
            }
        });


    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

// can be safely ignored for this demo

    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        MotionEntry mE = new MotionEntry();

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];


        mE.X_Acceleration = x;
        mE.Y_Acceleration = y;
        mE.Z_Acceleration = z;
        mE.Time = event.timestamp - timestamp;

        mEntries.add(mE);

        xPrg.setProgress((int) x);
        yPrg.setProgress((int) y);
        zPrg.setProgress((int) z);

        xData.addEntry(new Entry(count, x));
        yData.addEntry(new Entry(count, y));
        zData.addEntry(new Entry(count, z));

        count++;

        myTextView.setText("X : " + x + " Y : " + y + " Z : " + z + " " + event.timestamp);

        LineData data = mChart.getData();

        if (created == false) {
            data.addDataSet(xData);
            data.addDataSet(yData);
            data.addDataSet(zData);
            created = true;
        }

        data.notifyDataChanged(); // let the data know a dataSet changed
        mChart.notifyDataSetChanged(); // let the chart know it's data changed


        // mChart.setVisibleYRange(30, AxisDependency.LEFT);
        mChart.setVisibleXRangeMaximum(40);
        // move to the latest entry
        mChart.moveViewToX(data.getEntryCount());


    }

    private LineDataSet createSet(String name, int red, int green, int blue) {
        LineDataSet set = new LineDataSet(null, name);
        set.setLineWidth(2.0f);
        set.setDrawCircles(false);
        set.setColor(Color.rgb(red, green, blue));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }

    private void update_acceleration(float acceleration_X, float acceleration_Y, float acceleration_Z, float time) {
        velocity_X = velocity_X + acceleration_X;
        distance_X = distance_X + velocity_X;

        velocity_Y = velocity_Y + acceleration_X;
        distance_Y = distance_Y + velocity_Y;

        velocity_Z = velocity_Z + acceleration_Z;
        distance_Z = distance_Z + velocity_Z;
    }

    public void saveFile() {
        String json = new Gson().toJson(mEntries);


        Log.i(TAG, "Show camera button pressed. Checking permission.");
        // BEGIN_INCLUDE(camera_permission)
        // Check if the Camera permission is already available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Camera permission has not been granted.

            requestWritePermission();

        } else {

            // Camera permissions is already available, show the camera preview.
            Log.i(TAG,
                    "CAMERA permission has already been granted. Displaying camera preview.");
            saveFileToStorage();
        }


        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

        String filename = "data_" + dateFormat.format(date) + ".json";

        helloText.setText(filename);
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(json.getBytes());
            outputStream.close();
            Log.e("Save", "First saved");
        } catch (Exception e) {
            e.printStackTrace();
            helloText.setText("First : " +e.toString());
        }




    }

    private void requestWritePermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, "Write access",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQUEST_WRITE);
                }
            }).show();
         Log.e("Snackbar", "shouldSchow request");

        } else {
            Snackbar.make(mLayout, "Write access not available", Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_WRITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == PERMISSION_REQUEST_WRITE) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Snackbar.make(mLayout, "Write permission granted",
                        Snackbar.LENGTH_SHORT)
                        .show();
                saveFileToStorage();
            } else {
                // Permission request was denied.
                Snackbar.make(mLayout, "Write permission refused",
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }

    public void saveFileToStorage(){
        String json = new Gson().toJson(mEntries);

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

        String filename = "data_" + dateFormat.format(date) + ".json";

        File docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
        boolean isPresent = true;
        if (!docsFolder.exists()) {
            isPresent = docsFolder.mkdir();
            Log.e("IsPresent", Boolean.toString(isPresent));

        }

        if (isPresent) {
            File file = new File(docsFolder.getAbsolutePath(), filename);
            try {
                helloText.setText(file.getAbsolutePath());
                Log.e("Save", file.getAbsolutePath());
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(json);
                myOutWriter.close();

                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                helloText.setText("Second : " + e.toString());
                Log.e("Exception", "File write failed: " + e.toString());
                e.printStackTrace();
            }
        } else {
            // Failure
        }
    }

}
