package cy.ac.unic.pitchlium;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.WindowManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

// Activity Start
public class MainActivity extends WearableActivity implements SensorEventListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Initialize Data
    private GoogleApiClient mGoogleApiClient;
    private boolean nodeConnected = false;
    private long lastSampleTime = 0L;
    private String status = "idle";

    // Set Data Paths for the Sensors
    final private String PATH = "/sensors";
    final private String STATUS = "/status";
    final private String SCRIPT = "/script";

    // Sensor Data Global Variable
    float[] acceleration = new float[3], gravity = new float[3];
    float heartRate = 0, stepCount = 0, light = 0, totalAcceleration = 0, lastTotalAcceleration = 0;
    double[] movements = {0,0,0,0,0};
    String movementLabel = "";
    float movementChange = 0F;
    long tStart = 0;

    // Initial Global Sensors
    Sensor accelerometerS;
    Sensor gravityS;
    Sensor heartrateS;
    Sensor stepCounterS;
    Sensor lightS;

    // View Initialize
    TextView hours, minutes, seconds, tStatus;

    // Voice Recognition Initialize
    private SpeechRecognizer sr = null;
    private Intent speechIntent = null;
    StringBuilder script = new StringBuilder();

    // Movement Thresholds
    final float THRESHOLD_STEADY = (float) 0.10;
    final float THRESHOLD_SLOW_MOVEMENT = (float) 0.15;
    final float THRESHOLD_MODERATE_MOVEMENT = (float) 0.30;
    final float THRESHOLD_FAST_MOVEMENT = (float) 0.80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        // Initialize Sensor Manager
        SensorManager mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        hours = (TextView) findViewById(R.id.hour);
        minutes = (TextView) findViewById(R.id.minute);
        seconds = (TextView) findViewById(R.id.second);
        tStatus = (TextView) findViewById(R.id.status);

        // Setting Up Separate Sensors
        accelerometerS = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravityS = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        heartrateS = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        stepCounterS = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        lightS = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Check If All Sensors are Compatible
        if (accelerometerS != null) {
            mSensorManager.registerListener(this, accelerometerS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (gravityS != null) {
            mSensorManager.registerListener(this, gravityS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (heartrateS != null) {
            mSensorManager.registerListener(this, heartrateS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (stepCounterS != null) {
            mSensorManager.registerListener(this, stepCounterS, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (lightS != null) {
            mSensorManager.registerListener(this, lightS, SensorManager.SENSOR_DELAY_NORMAL);
        }

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(recognitionListener);

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
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(STATUS) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    status = dataMap.getString("Status");
                    if (status.equals("presenting")) startPresenting(); else stopPresenting();
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                status = "idle";
            }
        }
    }

    public void startPresenting() {
        tStart = System.currentTimeMillis();
        script = new StringBuilder();
        sr.startListening(speechIntent);
        tStatus.setText("Presenting...");
    }
    public void stopPresenting() {
        sr.stopListening();
        sendScript();
        tStatus.setText("Analyzing...");
    }
    public void sendScript() {
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
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(SCRIPT);
                    // Map Sensor Data
                    Log.e("Wear Script: ", script.toString());
                    putDataMapRequest.getDataMap().putString("Script", script.toString());
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

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        if (status.equals("presenting")) {
            measureTimer();
            if (lastSampleTime == 0L) {
                lastSampleTime = System.currentTimeMillis();
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                acceleration[0] = sensorEvent.values[0];
                acceleration[1] = sensorEvent.values[1];
                acceleration[2] = sensorEvent.values[2];
                totalAcceleration = (float) Math.sqrt(
                                sensorEvent.values[0] * sensorEvent.values[0] +
                                sensorEvent.values[1] * sensorEvent.values[1] +
                                sensorEvent.values[2] * sensorEvent.values[2]
                );
                movementChange = Math.abs(lastTotalAcceleration - totalAcceleration);
                if (lastSampleTime != 0L)
                    movementLabel = recordMovement();
                lastTotalAcceleration = totalAcceleration;
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                heartRate = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravity[0] = sensorEvent.values[0];
                gravity[1] = sensorEvent.values[1];
                gravity[2] = sensorEvent.values[2];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                stepCount = sensorEvent.values[0];
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
                light = sensorEvent.values[0];
            }

            if (lastSampleTime == 0L || lastSampleTime + 100 < System.currentTimeMillis()) {
                lastSampleTime = System.currentTimeMillis();
                final JSONObject data = new JSONObject();
                try {
                    data.put("x", acceleration[0]);
                    data.put("y", acceleration[1]);
                    data.put("z", acceleration[2]);
                    data.put("total", totalAcceleration);
                    data.put("movement", movementLabel);
                    data.put("diff", movementChange);
                    data.put("move1", movements[0]);
                    data.put("move2", movements[1]);
                    data.put("move3", movements[2]);
                    data.put("move4", movements[3]);
                    data.put("move5", movements[4]);
                    data.put("heartrate", heartRate);
                    data.put("gr1", gravity[0]);
                    data.put("gr2", gravity[1]);
                    data.put("gr3", gravity[2]);
                    data.put("steps", stepCount);
                    data.put("light", light);
                    data.put("accRange", accelerometerS.getMaximumRange());
                    data.put("watchtime", System.currentTimeMillis());
                    sendData(data.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String recordMovement() {
        if (movementChange <= THRESHOLD_STEADY) {
            movements[0]++;
            return "Steady";
        } else if (movementChange > THRESHOLD_STEADY && movementChange <= THRESHOLD_SLOW_MOVEMENT) {
            movements[1]++;
            return "Slow Movements";
        } else if (movementChange > THRESHOLD_SLOW_MOVEMENT && movementChange <= THRESHOLD_MODERATE_MOVEMENT) {
            movements[2]++;
            return "Moderate Movements";
        } else if (movementChange > THRESHOLD_MODERATE_MOVEMENT && movementChange <= THRESHOLD_FAST_MOVEMENT) {
            movements[3]++;
            return "Fast Movements";
        } else {
            movements[4]++;
            return "Crazy Movements!";
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
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
                    // Map Sensor Data
                    putDataMapRequest.getDataMap().putString("SensorData", data);
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

    public void measureTimer() {
        double time = (System.currentTimeMillis() - tStart) / 1000.0;
        int hour = (int) time / 3600;
        int remainder = (int) time - hour * 3600;
        int mins = remainder / 60;
        remainder = remainder - mins * 60;
        int secs = remainder;
        int[] currentTime = {hour, mins, secs};

        hours.setText((currentTime[0] < 10 ? "0" : "") + currentTime[0]);
        minutes.setText((currentTime[1] < 10 ? "0" : "") + currentTime[1]);
        seconds.setText((currentTime[2] < 10 ? "0" : "") + currentTime[2]);
    }

    public RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
        }
        @Override
        public void onBeginningOfSpeech() {
        }
        @Override
        public void onRmsChanged(float v) {
        }
        @Override
        public void onBufferReceived(byte[] bytes) {
        }
        @Override
        public void onEndOfSpeech() {
        }
        @Override
        public void onError(int i) {
        }
        @Override
        public void onResults(Bundle results) {
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            script.append(data.get(0));
            sr.startListening(speechIntent);
        }
        @Override
        public void onPartialResults(Bundle bundle) {
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onDestroy() {
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
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
