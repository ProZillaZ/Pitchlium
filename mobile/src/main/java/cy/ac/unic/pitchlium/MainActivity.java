package cy.ac.unic.pitchlium;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener {

    GoogleApiClient mGoogleApiClient;
    long lastSampleTime = 0L;
    boolean nodeConnected = false;
    final String PATH = "/sensors";
    final String STATUS = "/status";
    final String SCRIPT = "/script";
    String status = "idle";

    float x; TextView tx;
    float y; TextView ty;
    float z; TextView tz;
    float g1; TextView tg1;
    float g2; TextView tg2;
    float g3; TextView tg3;
    float heartrate; TextView theartrate;
    float gr1; TextView tgr1;
    float gr2; TextView tgr2;
    float gr3; TextView tgr3;
    float r1; TextView tr1;
    float r2; TextView tr2;
    float r3; TextView tr3;
    float steps; TextView tsteps;
    float accRange;
    float watchtime;
    float sessionStartTime;

    Button startPresentationBtn;
    String recordedScript = "";

    SensorManager mSensorManager;
    float[] acceleration = new float[3]; Sensor mAccelerometer;
    float stepCount = 0; Sensor mStepCounter;
    float temperature = 0; Sensor mTemperature;
    float humidity = 0; Sensor mHumidity;
    float pressure = 0; Sensor mPressure;

    List<String> wearReadings = new ArrayList<>();
    List<String> mobileReadings =  new ArrayList<>();

    long tStart = 0;
    double presentationDuration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tx = findViewById(R.id.x);
        ty = findViewById(R.id.y);
        tz = findViewById(R.id.z);
        tg1 = findViewById(R.id.g1);
        tg2 = findViewById(R.id.g2);
        tg3 = findViewById(R.id.g3);
        tgr1 = findViewById(R.id.gr1);
        tgr2 = findViewById(R.id.gr2);
        tgr3 = findViewById(R.id.gr3);
        tr1 = findViewById(R.id.r1);
        tr2 = findViewById(R.id.r2);
        tr3 = findViewById(R.id.r3);
        theartrate = findViewById(R.id.h);
        tsteps = findViewById(R.id.s);

        startPresentationBtn = findViewById(R.id.present);
        startPresentationBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPresentation();
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        // Check If All Sensors are Compatible
        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mStepCounter != null) {
            mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mTemperature != null) {
            mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mHumidity != null) {
            mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mPressure != null) {
            mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }
    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
        if (status.equals("presenting")) {
            if (lastSampleTime == 0L) {
                lastSampleTime = System.currentTimeMillis();
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                acceleration[0] = sensorEvent.values[0];
                acceleration[1] = sensorEvent.values[1];
                acceleration[2] = sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                stepCount = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                temperature = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                humidity = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
                pressure = sensorEvent.values[0];
            }
            if (lastSampleTime == 0L || lastSampleTime + 50 < System.currentTimeMillis()) {
                final JSONObject data = new JSONObject();
                try {
                    data.put("x", acceleration[0]);
                    data.put("y", acceleration[1]);
                    data.put("z", acceleration[2]);
                    data.put("steps", stepCount);
                    data.put("temp", temperature);
                    data.put("humidity", humidity);
                    data.put("pressure", pressure);
                    data.put("accRange", mAccelerometer.getMaximumRange());
                    data.put("mobiletime", System.currentTimeMillis());
                    lastSampleTime = System.currentTimeMillis();
                    mobileReadings.add(data.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (status.equals("presenting")) {
            for (DataEvent event : dataEvents) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo(PATH) == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        wearReadings.add(dataMap.getString("SensorData"));
                        try {
                            final JSONObject data = new JSONObject(dataMap.getString("SensorData"));
                            x = (float) data.getDouble("x");
                            y = (float) data.getDouble("y");
                            z = (float) data.getDouble("z");
                            g1 = (float) data.getDouble("g1");
                            g2 = (float) data.getDouble("g2");
                            g3 = (float) data.getDouble("g3");
                            heartrate = (float) data.getDouble("heartrate");
                            gr1 = (float) data.getDouble("gr1");
                            gr2 = (float) data.getDouble("gr2");
                            gr3 = (float) data.getDouble("gr3");
                            r1 = (float) data.getDouble("r1");
                            r2 = (float) data.getDouble("r2");
                            r3 = (float) data.getDouble("r3");
                            steps = (float) data.getDouble("steps");
                            accRange = (float) data.getDouble("accRange");
                            watchtime = (float) data.getDouble("watchtime");
                            sessionStartTime = (float) data.getDouble("sessionStartTime");

                            tx.setText("X: " + x);
                            ty.setText("Y: " + y);
                            tz.setText("Z: " + z);
                            tg1.setText("G1: " + g1);
                            tg2.setText("G2: " + g2);
                            tg3.setText("G3: " + g3);
                            tgr1.setText("Gr1: " + gr1);
                            tgr2.setText("Gr2: " + gr2);
                            tgr3.setText("Gr3: " + gr3);
                            tr1.setText("R1: " + r1);
                            tr2.setText("R2: " + r2);
                            tr3.setText("R3: " + r3);
                            theartrate.setText("Heart Rate: " + heartrate);
                            tsteps.setText("Steps Counter: " + steps);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (item.getUri().getPath().compareTo(SCRIPT) == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        recordedScript = dataMap.getString("Script");
                    }
                }
            }
        }
    }

    public void startPresentation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Check Node Connection
                if (!nodeConnected) {
                    mGoogleApiClient.blockingConnect(15000, TimeUnit.SECONDS);
                }
                // If Node not Connected
                if (!nodeConnected) {
                    Log.e("WEAR APP", "Failed to connect to mGoogleApiClient within " + 15000 + " seconds");
                    return;
                }
                // If Everything is Connected
                if (mGoogleApiClient.isConnected()) {
                    // Set App Status
                    if (!status.equals("presenting")) {
                        wearReadings = new ArrayList<>();
                        mobileReadings =  new ArrayList<>();
                        recordedScript = "";
                        tStart = System.currentTimeMillis();
                        status = "presenting";
                    }
                    else if (status.equals("presenting"))
                        stopPresentation();
                    // Set Data Transfer Path
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(STATUS);
                    // Map Sensor Data
                    putDataMapRequest.getDataMap().putString("Status", status);
                    // Request Data Transfer
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();
                    // Send Data
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(mGoogleApiClient, request);

                } else {
                    Log.e("WEAR APP", "No Google API Client connection");
                }
            }
        }).start();
    }

    public void stopPresentation() {
        status = "analyzing";
        presentationDuration = (System.currentTimeMillis() - tStart) / 1000.0;
        final JSONObject finalData = new JSONObject();
        Gson gson = new Gson();
        try {
            finalData.put("duration", presentationDuration);
            finalData.put("mobileData", gson.toJson(mobileReadings));
            finalData.put("wearData", gson.toJson(wearReadings));
            finalData.put("script", recordedScript);
            Log.e("DATA: ", finalData.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {nodeConnected = false;}
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {nodeConnected = false;}
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onConnected(Bundle bundle) {
        nodeConnected = true;
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        mSensorManager.unregisterListener(this);
    }
}
