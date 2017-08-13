package cy.ac.unic.pitchlium;

import android.app.Activity;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private TextView textViewToChange;
    private boolean nodeConnected = false;
    final private String PATH = "/sensors";
    final private String STATUS = "/status";
    private String status = "idle";

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
    float significant; TextView tsignificant;
    float accRange; TextView taccRange;
    float watchtime; TextView twatchtime;
    float sessionStartTime; TextView tsession;

    Button startPresentationBtn;

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
        tsignificant = findViewById(R.id.sig);

        tsession = findViewById(R.id.status);
        startPresentationBtn = findViewById(R.id.present);
        startPresentationBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startPresentation();
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
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
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo(PATH) == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
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
                        significant = (float) data.getDouble("significant");
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
                        tsignificant.setText("Significant Motion: " + significant);
                    } catch (JSONException e) {
                        e.printStackTrace();
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
                    if (status.equals("idle"))
                        status = "presenting";
                    else if (status.equals("presenting"))
                        status = "analyzing";
                    else
                        status = "idle";
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

    @Override
    public void onConnectionSuspended(int i) {nodeConnected = false;}
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {nodeConnected = false;}
}
