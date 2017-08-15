package cy.ac.unic.pitchlium;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        SensorEventListener {

    // Initialize
    GoogleApiClient mGoogleApiClient;
    long lastSampleTime = 0L;
    String status = "idle";
    boolean nodeConnected = false;
    long tStart = 0;
    double presentationDuration = 0;

    // Data Routes
    final String PATH = "/sensors";
    final String STATUS = "/status";
    final String SCRIPT = "/script";

    // Initialize View Elements
    TextView tx, tmx, ty, tTotalAccel, tmy, tz, tmz, tmTotalAccel, theartrate, tgr1, tgr2, tgr3, tc,
             tsteps, tms, tt, thu, tp, tl, tm1, tm2, tm3, hours, minutes, seconds,
             tTotalChange, tmTotalChange, tDetectMove, tmDetectMove;
    Button startPresentationBtn;

    // Initialize Sensors
    SensorManager mSensorManager;
    Sensor mAccelerometer, mOrientation, mStepCounter, mTemperature, mHumidity, mPressure, mMagnetic;

    // Phone Sensor Data
    float stepCount = 0, totalAcceleration = 0, lastTotalAcceleration = 0;
    List<Float> temperature = new ArrayList<>(), humidity = new ArrayList<>(),
            pressure = new ArrayList<>(), light = new ArrayList<>(), degrees = new ArrayList<>();
    double[] movements = {0,0,0,0,0,0,0};

    // Wear Sensor Data
    float steps = 0;
    List<Float> heartrate = new ArrayList<>();
    float averageHeartrate = 0;
    double[] handMovements = {0,0,0,0,0};

    // Results
    String recordedScript = "";

    // Movement Thresholds
    final float THRESHOLD_STEADY = (float) 0.15;
    final float THRESHOLD_STEADY_MOTION = (float) 0.40;
    final float THRESHOLD_SLIGHT_MOVEMENT = (float) 0.90;
    final float THRESHOLD_SLOW_WALK = (float) 1.50;
    final float THRESHOLD_MODERATE_WALK = (float) 2.50;
    final float THRESHOLD_FAST_WALK = (float) 4.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
                = new BottomNavigationView.OnNavigationItemSelectedListener() {

            FrameLayout presentation = (FrameLayout) findViewById(R.id.content_presentation);
            FrameLayout data = (FrameLayout) findViewById(R.id.content_data);
            FrameLayout calculations = (FrameLayout) findViewById(R.id.content_calculations);

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_presentation:
                        presentation.setVisibility(View.VISIBLE);
                        data.setVisibility(View.GONE);
                        calculations.setVisibility(View.GONE);
                        return true;
                    case R.id.navigation_data:
                        presentation.setVisibility(View.GONE);
                        data.setVisibility(View.VISIBLE);
                        calculations.setVisibility(View.GONE);
                        return true;
                    case R.id.navigation_calc:
                        presentation.setVisibility(View.GONE);
                        data.setVisibility(View.GONE);
                        calculations.setVisibility(View.VISIBLE);
                        return true;
                }
                return false;
            }

        };

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        tx = (TextView) findViewById(R.id.x);
        ty = (TextView) findViewById(R.id.y);
        tz = (TextView) findViewById(R.id.z);
        tTotalAccel = (TextView) findViewById(R.id.totalAccel);
        tTotalChange = (TextView) findViewById(R.id.totalAccelChange);
        tDetectMove = (TextView) findViewById(R.id.detectMov);
        tgr1 = (TextView) findViewById(R.id.gr1);
        tgr2 = (TextView) findViewById(R.id.gr2);
        tgr3 = (TextView) findViewById(R.id.gr3);
        tm1 = (TextView) findViewById(R.id.m1);
        tm2 = (TextView) findViewById(R.id.m2);
        tm3 = (TextView) findViewById(R.id.m3);
        theartrate = (TextView) findViewById(R.id.h);
        tsteps = (TextView) findViewById(R.id.s);
        tmx = (TextView) findViewById(R.id.mx);
        tmy = (TextView) findViewById(R.id.my);
        tmz = (TextView) findViewById(R.id.mz);
        tmTotalAccel = (TextView) findViewById(R.id.mTotalAccel);
        tmTotalChange = (TextView) findViewById(R.id.mtotalAccelChange);
        tmDetectMove = (TextView) findViewById(R.id.mdetectMov);
        tms = (TextView) findViewById(R.id.ms);
        tt = (TextView) findViewById(R.id.t);
        thu = (TextView) findViewById(R.id.hu);
        tp = (TextView) findViewById(R.id.p);
        tl = (TextView) findViewById(R.id.l);
        tc = (TextView) findViewById(R.id.c);
        hours = (TextView) findViewById(R.id.hour);
        minutes = (TextView) findViewById(R.id.minute);
        seconds = (TextView) findViewById(R.id.second);

        startPresentationBtn = (Button) findViewById(R.id.present);
        startPresentationBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPresentation();
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        // Check If All Sensors are Compatible
        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (mOrientation != null) {
            mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mMagnetic != null) {
            mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_GAME);
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
            measureTimer();
            if (lastSampleTime == 0L)
                lastSampleTime = System.currentTimeMillis();
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                tmx.setText("X: " + sensorEvent.values[0] + " m/s^2");
                tmy.setText("Y: " + sensorEvent.values[1] + " m/s^2");
                tmz.setText("Z: " + sensorEvent.values[2] + " m/s^2");
                totalAcceleration = (float) Math.sqrt(
                        sensorEvent.values[0] * sensorEvent.values[0] +
                        sensorEvent.values[1] * sensorEvent.values[1] +
                        sensorEvent.values[2] * sensorEvent.values[2]
                );
                tmTotalAccel.setText("" + totalAcceleration + " m/s^2");
                if (lastSampleTime != 0L)
                    recordMovement();
                lastTotalAcceleration = totalAcceleration;
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                degrees.add((float) Math.round(sensorEvent.values[0]));
                tc.setText("" + Math.round(sensorEvent.values[0]) + " Degrees");
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                tm1.setText("M1: " + sensorEvent.values[0] + " uT");
                tm2.setText("M2: " + sensorEvent.values[1] + " uT");
                tm3.setText("M3: " + sensorEvent.values[2] + " uT");
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                stepCount = sensorEvent.values[0];
                tms.setText("" + sensorEvent.values[0] + " steps");
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                temperature.add(sensorEvent.values[0]);
                tt.setText("" + sensorEvent.values[0] + "C");
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                humidity.add(sensorEvent.values[0]);
                thu.setText("" + sensorEvent.values[0] + "%");
            }
            else if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE) {
                pressure.add(sensorEvent.values[0]);
                tp.setText("" + sensorEvent.values[0] + " hPa");
            }
            lastSampleTime = System.currentTimeMillis();
        }
    }

    public void recordMovement() {
        float diff = Math.abs(lastTotalAcceleration - totalAcceleration);
        tmTotalChange.setText("" + diff + " m/s^2");
        if (diff <= THRESHOLD_STEADY) {
            movements[0]++;
            tmDetectMove.setText("" + "Steady" + "");
        } else if (diff > THRESHOLD_STEADY && diff <= THRESHOLD_STEADY_MOTION) {
            movements[1]++;
            tmDetectMove.setText("" + "Steady Motion" + "");
        } else if (diff > THRESHOLD_STEADY_MOTION && diff <= THRESHOLD_SLIGHT_MOVEMENT) {
            movements[2]++;
            tmDetectMove.setText("" + "Steady Movement" + "");
        } else if (diff > THRESHOLD_SLIGHT_MOVEMENT && diff <= THRESHOLD_SLOW_WALK) {
            movements[3]++;
            tmDetectMove.setText("" + "Slow Walk" + "");
        } else if (diff > THRESHOLD_SLOW_WALK && diff <= THRESHOLD_MODERATE_WALK) {
            movements[4]++;
            tmDetectMove.setText("" + "Moderate Walk" + "");
        } else if (diff > THRESHOLD_MODERATE_WALK && diff <= THRESHOLD_FAST_WALK) {
            movements[5]++;
            tmDetectMove.setText("" + "Fast Walk" + "");
        } else {
            movements[6]++;
            tmDetectMove.setText("" + "Crazy Walk!" + "");
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
                        try {
                            final JSONObject data = new JSONObject(dataMap.getString("SensorData"));
                            handMovements[0] = (float) data.getDouble("move1");
                            handMovements[1] = (float) data.getDouble("move2");
                            handMovements[2] = (float) data.getDouble("move3");
                            handMovements[3] = (float) data.getDouble("move4");
                            handMovements[4] = (float) data.getDouble("move5");
                            heartrate.add((float) data.getDouble("heartrate"));
                            steps = (float) data.getDouble("steps");
                            light.add((float) data.getDouble("light"));

                            tx.setText("X: " + data.getDouble("x") + " m/s^2");
                            ty.setText("Y: " + data.getDouble("y") + " m/s^2");
                            tz.setText("Z: " + data.getDouble("z") + " m/s^2");
                            tTotalAccel.setText("" + data.getDouble("total") + " m/s^2");
                            tTotalChange.setText("" + data.getDouble("diff") + "");
                            tDetectMove.setText("" + data.getString("movement") + "");
                            tgr1.setText("G1: " + data.getDouble("gr1") + " m/s^2");
                            tgr2.setText("G2: " + data.getDouble("gr2") + " m/s^2");
                            tgr3.setText("G3: " + data.getDouble("gr3") + " m/s^2");
                            theartrate.setText("" + data.getDouble("heartrate") + " bpm");
                            tsteps.setText("" + steps + " steps");
                            tl.setText("" + data.getDouble("light") + " SI lux");
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

    public void initializeData() {
        temperature = new ArrayList<>();
        pressure = new ArrayList<>();
        humidity = new ArrayList<>();
        recordedScript = "";
        Arrays.fill(movements, 0);
        tStart = System.currentTimeMillis();
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
                        initializeData();
                        status = "presenting";
                    } else if (status.equals("presenting"))
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
        final JSONObject finalData = new JSONObject();
        Gson gson = new Gson();
        try {
            finalData.put("duration", (System.currentTimeMillis() - tStart) / 1000.0);
            finalData.put("movementPercentages", gson.toJson(calculateMovementPercentage(movements)));
            finalData.put("handMovementPercentages", gson.toJson(calculateMovementPercentage(handMovements)));
            finalData.put("facingCheck", checkDirectionFacing());
            finalData.put("stressCheck", gson.toJson(checkAverageThreshold(heartrate, 0, 80)));
            finalData.put("temperatureCheck", gson.toJson(checkAverageThreshold(temperature, 20, 26)));
            finalData.put("humidityCheck", gson.toJson(checkAverageThreshold(humidity, 30, 60)));
            finalData.put("pressureCheck", gson.toJson(checkAverageThreshold(pressure, 990, 1020)));
            finalData.put("script", recordedScript);
            // Instantiate the RequestQueue.
            RequestQueue requstQueue = Volley.newRequestQueue(this);

            // Request a string response from the provided URL.
            JsonObjectRequest jsonobj = new JsonObjectRequest(
                    Request.Method.POST,
                    "https://pitchlium.herokuapp.com/",
                    finalData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.e("Results: ", response.toString());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Error: ", error.toString());
                        }
                    }
            );
            requstQueue.add(jsonobj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<Double> calculateMovementPercentage(double[] thisMovements) {
        double sum = 0;
        List<Double> results = new ArrayList<>();
        if (thisMovements.length > 0) {
            for (double move : thisMovements)
                sum += move;
            for (double move : thisMovements)
                if (sum != 0)
                    results.add(RoundTo2Decimals((move / sum) * 100));
                else
                    results.add(0d);
        }
        else
            results.add(0d);
        return results;
    }

    public boolean checkDirectionFacing() {
        boolean valid = true;
        float min = degrees.get(0), max = degrees.get(0);
        for (float degree: degrees) {
            if (degree > max)
                max = degree;
            else if (degree < min)
                min = degree;
        }
        if (max - min > 150)
            valid = false;
        return valid;
    }

    public String[] checkAverageThreshold(List<Float> list, float min, float max) {
        boolean valid = true;
        float sum = 0;
        for (float item: list) {
            sum += item;
            if (item < min || item > max)
                valid = false;
        }
        String[] results = new String[2];
        results[0] = (valid ? "true" : "false");
        results[1] = "" + RoundTo2Decimals(((double) sum / (double) list.size()));
        return results;
    }

    public double RoundTo2Decimals(double val) {
        DecimalFormat df2 = new DecimalFormat("###.##");
        return Double.valueOf(df2.format(val));
    }

    public void measureTimer() {
        double time = (System.currentTimeMillis() - tStart) / 1000.0;
        int hour = (int) time / 3600;
        int remainder = (int) time - hour * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;
        int[] currentTime = {hour, mins, secs};

        hours.setText("" + (currentTime[0] < 10 ? "0" : "") + currentTime[0]);
        minutes.setText("" + (currentTime[1] < 10 ? "0" : "") + currentTime[1]);
        seconds.setText("" + (currentTime[2] < 10 ? "0" : "") + currentTime[2]);
    }

    @Override
    public void onConnectionSuspended(int i) {
        nodeConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        nodeConnected = false;
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
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
