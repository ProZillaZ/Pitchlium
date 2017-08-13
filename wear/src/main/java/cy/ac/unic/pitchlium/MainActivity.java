package cy.ac.unic.pitchlium;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.widget.TextView;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private BoxInsetLayout mContainerView;

    private SensorManager mSensorManager;

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
}
