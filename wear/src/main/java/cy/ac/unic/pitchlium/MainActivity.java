package cy.ac.unic.pitchlium;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private BoxInsetLayout mContainerView;
    private SensorManager mSensorManager;
    private GoogleApiClient mGoogleApiClient;
    private boolean nodeConnected = false;

    final private String[] PATHS = {"/heart-beat"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mContainerView = findViewById(R.id.container);

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor rotVector = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor heartrate = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        TextView accelText = findViewById(R.id.accelerometer);
        TextView gravityText = findViewById(R.id.gravity);
        TextView rotVectorText = findViewById(R.id.rotation);
        TextView gyroscopeText = findViewById(R.id.gyro);
        TextView heartrateText = findViewById(R.id.heart);

        if (accelerometer != null) {
            accelText.setTextColor(this.getResources().getColor(R.color.green));
        }
        if (gravity != null) {
            gravityText.setTextColor(this.getResources().getColor(R.color.green));
        }
        if (rotVector != null) {
            rotVectorText.setTextColor(this.getResources().getColor(R.color.green));
        }
        if (gyroscope != null) {
            gyroscopeText.setTextColor(this.getResources().getColor(R.color.green));
        }
        if (heartrate != null) {
            heartrateText.setTextColor(this.getResources().getColor(R.color.green));
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    private void sendData(final String dataToSend, final int sensor) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!nodeConnected) {
                    mGoogleApiClient.blockingConnect(15000, TimeUnit.SECONDS);
                }
                if (!nodeConnected) {
                    Log.e("WEAR APP", "Failed to connect to mGoogleApiClient within " + 15000 + " seconds");
                    return;
                }

                if (mGoogleApiClient.isConnected()) {
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHS[sensor]);
                    putDataMapRequest.getDataMap().putString("data", dataToSend);
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();

                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(mGoogleApiClient, request);

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
    public void onSensorChanged(final SensorEvent sensorEvent) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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
