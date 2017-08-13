package cy.ac.unic.pitchlium;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

// Activity Start
public class MainActivity extends WearableActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Initialize Data
    private SensorManager mSensorManager;
    private GoogleApiClient mGoogleApiClient;
    private boolean nodeConnected = false;
    private long lastSampleTime = 0L;
    private long session = 0L;

    // Set Data Paths for the Sensors
    final private String[] PATHS = {"/heart-rate"};

    // Sensor Data Global Variable
    private float[] acceleration = new float[3];
    private float[] gyroscope = new float[3];
    private float[] gravity = new float[3];
    private float[] rotVector = new float[3];
    private float heartRate = 0;
    private float stepCount = 0;
    private float significant = 0;

    // Initial Global Sensors
    Sensor accelerometerS;
    Sensor gravityS;
    Sensor rotVectorS;
    Sensor gyroscopeS;
    Sensor heartrateS;
    Sensor stepCounterS;
    Sensor significantS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // Initialize Sensor Manager
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));

        // Setting Up Separate Sensors
        accelerometerS = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        rotVectorS = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        gyroscopeS = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        heartrateS = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        stepCounterS = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        significantS = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);

        // Setting the TexViews to Show Data
        TextView accelText = findViewById(R.id.accelerometer);
        TextView gravityText = findViewById(R.id.gravity);
        TextView rotVectorText = findViewById(R.id.rotation);
        TextView gyroscopeText = findViewById(R.id.gyro);
        TextView heartrateText = findViewById(R.id.heart);
        TextView stepCounterText = findViewById(R.id.step);
        TextView significantText = findViewById(R.id.significant);

        // Check If All Sensors are Compatible
        if (accelerometerS != null) {
            accelText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, accelerometerS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gravityS != null) {
            gravityText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, gravityS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (rotVectorS != null) {
            rotVectorText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, rotVectorS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gyroscopeS != null) {
            gyroscopeText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, gyroscopeS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (heartrateS != null) {
            heartrateText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, heartrateS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (stepCounterS != null) {
            stepCounterText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, stepCounterS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (significantS != null) {
            significantText.setTextColor(this.getResources().getColor(R.color.green));
            mSensorManager.registerListener(this, significantS, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Initialize Data Layer API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (lastSampleTime == 0L) {
            lastSampleTime = System.currentTimeMillis();
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            acceleration[0] = sensorEvent.values[0];
            acceleration[1] = sensorEvent.values[1];
            acceleration[2] = sensorEvent.values[2];
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRate = sensorEvent.values[0];
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscope[0] = sensorEvent.values[0];
            gyroscope[1] = sensorEvent.values[1];
            gyroscope[2] = sensorEvent.values[2];
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravity[0] = sensorEvent.values[0];
            gravity[1] = sensorEvent.values[1];
            gravity[2] = sensorEvent.values[2];
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotVector[0] = sensorEvent.values[0];
            rotVector[1] = sensorEvent.values[1];
            rotVector[2] = sensorEvent.values[2];
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = sensorEvent.values[0];
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_SIGNIFICANT_MOTION) {
            significant = sensorEvent.values[0];
        }

        if (lastSampleTime == 0L || lastSampleTime + 300 < System.currentTimeMillis()) {
            final JSONObject data = new JSONObject();
            try {
                data.put("x", acceleration[0]);
                data.put("y", acceleration[1]);
                data.put("z", acceleration[2]);
                data.put("g1", gyroscope[0]);
                data.put("g2", gyroscope[1]);
                data.put("g3", gyroscope[2]);
                data.put("heartrate",heartRate);
                data.put("grav1", gravity[0]);
                data.put("grav2", gravity[1]);
                data.put("grav3", gravity[2]);
                data.put("r1", rotVector[0]);
                data.put("r2", rotVector[1]);
                data.put("r3", rotVector[2]);
                data.put("steps", stepCount);
                data.put("significant", significant);
                data.put("accRange", accelerometerS.getMaximumRange());
                data.put("watchtime", System.currentTimeMillis());
                data.put("sessionStartTime", session);
                lastSampleTime = System.currentTimeMillis();

                sendData(data.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Send Sensor Data From Wearable to Phone
    private void sendData(final String data) {
        // Start a New Runnable
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
                    // Set Data Transfer Path
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/heart-rate");
                    // Map Sensor Data
                    putDataMapRequest.getDataMap().putString("SensorData", data);
                    // Request Data Transfer
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();
                    // Send Data
                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    // Callback
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.e("WEAR APP", "APPLICATION Result has come");
                        }
                    });

                } else {
                    Log.e("WEAR APP", "No Google API Client connection");
                }
            }
        }).start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
    }

    @Override
    public void onConnected(Bundle bundle) {
        nodeConnected = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        nodeConnected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        nodeConnected = false;
    }
}
