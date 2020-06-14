package com.smartsay.qbuster;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.smartsay.qbuster.LibUtils.RadarScanView;
import com.smartsay.qbuster.LibUtils.RandomTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RadarScanView radarScanView;
    private Switch discoverSwitch;
    private RelativeLayout radar_rl;
    private LinearLayout timer_gate_ll;
    private EditText wait_time_et,gate_num_et;
    private RandomTextView random_textview;
    private Button five_min,ten_min,fifteen_min,twenty_min,gate_a,gate_b,gate_c,gate_f,id_send,id_send_time,id_send_gate;
    private ConnectionsClient connectionsClient;
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String PACKAGE_NAME = "com.smartsay.qbuster";
    private static final String EndPointName = "Ground Staff";
    private String PassengerEndPointId;

    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    Log.e("Payload Received",payload.toString()+" EPID"+endpointId);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        Log.e("Payload Transfer",update.getStatus()+" EPID"+endpointId);
                    }else{
                        Log.e("Payload Error",update.getStatus()+"");
                    }
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.e("Endpoint Found",info.getEndpointName()+" EPID"+endpointId);
                    connectionsClient.requestConnection(EndPointName, endpointId, connectionLifecycleCallback);
                    PassengerEndPointId = endpointId;
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.e("Endpoint Lost",endpointId);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.e("Connection Initiated",connectionInfo.getAuthenticationToken());
                    PassengerEndPointId = endpointId;
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    random_textview.addKeyWord((connectionInfo.getEndpointName()));
                    random_textview.show();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.e("Connected","Start Sending and Receiving data");
                                radarScanView.StopScanAnimation();
                                connectionsClient.stopAdvertising();
                                id_send.setVisibility(View.VISIBLE);
                            break;

                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.e("Connection Rejected",result.toString());
                            break;

                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.e("Connection Error",result.toString());
                            break;

                        default:
                            // Unknown status code
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.e("Connection Lost",endpointId);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        init();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission Missing", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    private void init() {
        radar_rl = findViewById(R.id.radar_rl);
        timer_gate_ll = findViewById(R.id.time_gate_ll);
        radarScanView = findViewById(R.id.radarScanView);
        random_textview = findViewById(R.id.random_textview);
        discoverSwitch = findViewById(R.id.discoverSwitch);
        wait_time_et = findViewById(R.id.wait_time_et);
        gate_num_et = findViewById(R.id.gate_num_et);
        id_send_time = findViewById(R.id.id_send_time);
        id_send_time.setOnClickListener(this);
        id_send_gate = findViewById(R.id.id_send_gate);
        id_send_gate.setOnClickListener(this);
        id_send = findViewById(R.id.id_send);
        id_send.setOnClickListener(this);
        five_min = findViewById(R.id.five_min);
        five_min.setOnClickListener(this);
        ten_min = findViewById(R.id.ten_min);
        ten_min.setOnClickListener(this);
        fifteen_min = findViewById(R.id.fifteen_min);
        fifteen_min.setOnClickListener(this);
        twenty_min = findViewById(R.id.twenty_min);
        twenty_min.setOnClickListener(this);
        gate_a = findViewById(R.id.gate_a);
        gate_a.setOnClickListener(this);
        gate_b = findViewById(R.id.gate_b);
        gate_b.setOnClickListener(this);
        gate_c = findViewById(R.id.gate_c);
        gate_c.setOnClickListener(this);
        gate_f = findViewById(R.id.gate_f);
        gate_f.setOnClickListener(this);

        connectionsClient = Nearby.getConnectionsClient(MainActivity.this);
        discoverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    radarScanView.setVisibility(View.VISIBLE);
                    startAdvertising();
                    //startDiscovery();
                    radarScanView.StartScanAnimation();
                }else{
                    random_textview.hideAll();
                    radarScanView.setVisibility(View.GONE);
                    connectionsClient.stopAdvertising();
                    radarScanView.StopScanAnimation();
                }
            }
        });

    }

    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                PACKAGE_NAME, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build();

        connectionsClient
                .startAdvertising(EndPointName, PACKAGE_NAME, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        advertising -> {
                            Log.e("Adv Started", "advertising Started"+" :Thread Name " +Thread.currentThread().getName());
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Log.e("Adv Exception", "advertising Exception" + e.getMessage());
                        });

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.five_min:
                //sendStringPayload("Timer~5");
                try {
                    sendMyPayload(false,true,false,300000,"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.ten_min:
                //sendStringPayload("Timer~10");
                try {
                    sendMyPayload(false,true,false,600000,"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.fifteen_min:
                //sendStringPayload("Timer~15");
                try {
                    sendMyPayload(false,true,false,900000,"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.twenty_min:
                //sendStringPayload("Timer~20");
                try {
                    sendMyPayload(false,true,false,1200000,"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.gate_a:
                //sendStringPayload("Gate~1A");
                try {
                    sendMyPayload(false,false,true,0,gate_a.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.gate_b:
                //sendStringPayload("Gate~5B");
                try {
                    sendMyPayload(false,false,true,0,gate_b.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.gate_c:
                try {
                    sendMyPayload(false,false,true,0,gate_c.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.gate_f:
                try {
                    sendMyPayload(false,false,true,0,gate_f.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.id_send:
                radar_rl.setVisibility(View.GONE);
                timer_gate_ll.setVisibility(View.VISIBLE);
                try {
                    sendMyPayload(true,false,false,0,"");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.id_send_time:
                if(!wait_time_et.getText().toString().equals("")){
                    long t = Long.parseLong(wait_time_et.getText().toString());
                    try {
                        sendMyPayload(false,true,false,TimeUnit.MINUTES.toMillis(t),"");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case R.id.id_send_gate:
                if(!gate_num_et.getText().toString().equals("")){
                    try {
                        sendMyPayload(false,false,true,0,gate_num_et.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }

    }

    public void sendStringPayload(String message){
        byte[] bytes = message.getBytes();
        Payload bytesPayload = Payload.fromBytes(bytes);
        //Nearby.getConnectionsClient(MainActivity.this).sendPayload(PassengerEndPointId, bytesPayload);
        connectionsClient.sendPayload(PassengerEndPointId, bytesPayload);
    }

    @SuppressLint("NewApi")
    public void sendMyPayload(boolean showTimer, boolean timer, boolean gate, long time, String gateNum)throws JSONException{

        JSONObject object = new JSONObject();
        object.put("screen",showTimer);
        object.put("timer",timer);
        object.put("gate",gate);
        object.put("time",time);
        object.put("gate_num",gateNum);

        connectionsClient.sendPayload(PassengerEndPointId, Payload.fromBytes(object.toString().getBytes(UTF_8)));
        id_send.setVisibility(View.GONE);

    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }
}
