package com.smartsay.qbuster;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

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

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class Discover extends AppCompatActivity {

    private RadarScanView radarScanView;
    private Switch discoverSwitch;
    private TextView searchingInfoTextView, endPointName;
    private RandomTextView random_textview;
    private TextView boardingGate_number, textViewTime, boardingTimetv, titleMsg_TV;
    private ConnectionsClient connectionsClient;
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private final String PACKAGE_NAME = "com.smartsay.qbuster";
    private final String EndPointName = "Digital Pumpkin";
    private RelativeLayout timer_rl, radar_rl;
    private ProgressBar progressBarCircle;
    private long def_time_millisec = 300000;
    private String receivedMsg;
    private CountDownTimer countDownTimer;

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {

                    Log.e("Pass Payload Received", payload.toString() + " EPID" + endpointId);

                    byte[] receivedBytes = payload.asBytes();
                    if (receivedBytes != null) {
                        receivedMsg = new String(receivedBytes);

                        try {
                            JSONObject object = new JSONObject(receivedMsg);

                            Long timerValue = object.getLong("time");
                            String gateNumber = object.getString("gate_num");
                            if (object.getBoolean("screen")) {
                                startCountDownTimer(def_time_millisec);
                            } else {
                                if (object.getBoolean("timer") || object.getBoolean("gate")) {
                                    if (object.getBoolean("timer")) {
                                        startCountDownTimer(object.getLong("time"));
                                    }
                                    if (object.getBoolean("gate")) {
                                        boardingGate_number.setText(object.getString("gate_num"));
                                    }
                                } else {
                                    onBackPressed();
                                }

                            }
                        } catch (Exception ex) {
                            Log.i("JSON Error", ex.getMessage());
                        }


                    } else {
                        Log.e("Payload Received", "Received NULL!!!");
                    }

                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        Log.e("Pass Transfer Update", update.getStatus() + " EPID" + endpointId);
                    }
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    Log.e("Endpoint Found", info.getEndpointName() + " EPID" + endpointId);
                    connectionsClient.requestConnection(EndPointName, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    Log.e("Endpoint Lost", endpointId);
                }
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.e("Connection Initiated", connectionInfo.getAuthenticationToken());
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    random_textview.addKeyWord(connectionInfo.getEndpointName());
                    random_textview.show();
                    searchingInfoTextView.setText(getResources().getString(R.string.connectionEstablished));
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            Log.e("Connected", "Start Sending and Receiving data");
                            radarScanView.StopScanAnimation();
                            connectionsClient.stopDiscovery();
                            break;

                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            Log.e("Connection Rejected", result.toString());
                            break;

                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            Log.e("Connection Error", result.toString());
                            break;

                        default:
                            // Unknown status code
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.e("Connection Lost", endpointId);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radar_scan_timer_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        init();

    }

    private void init() {

        radarScanView = findViewById(R.id.radarScanView);
        discoverSwitch = findViewById(R.id.discoverSwitch);
        searchingInfoTextView = findViewById(R.id.searchingInfoTextView);
        endPointName = findViewById(R.id.endPointName);
        random_textview = findViewById(R.id.random_textview);
        boardingTimetv = findViewById(R.id.boardingTimetv);
        boardingGate_number = findViewById(R.id.boardingGate_number);
        progressBarCircle = findViewById(R.id.progressBarCircle);
        titleMsg_TV = findViewById(R.id.titleMsg_TV);
        textViewTime = findViewById(R.id.textViewTime);
        timer_rl = findViewById(R.id.timer_rl);
        timer_rl.setVisibility(View.GONE);
        radar_rl = findViewById(R.id.radar_rl);
        radar_rl.setVisibility(View.VISIBLE);

        connectionsClient = Nearby.getConnectionsClient(Discover.this);
        initScreenTimes();
        setProgressandTimerInitValues();

        discoverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    radarScanView.setVisibility(View.VISIBLE);
                    //startAdvertising();
                    startDiscovery();
                    searchingInfoTextView.setText(getResources().getString(R.string.tryingConnection));
                    radarScanView.StartScanAnimation();
                } else {
                    radarScanView.setVisibility(View.GONE);
                    connectionsClient.stopDiscovery();
                    radarScanView.StopScanAnimation();
                    searchingInfoTextView.setText(getResources().getString(R.string.switchOn));
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Discover", "On pause");
    }

    private void initScreenTimes() {
        Date d1 = new Date();
        Calendar cl = Calendar.getInstance();
        cl.setTime(d1);
        new SimpleDateFormat("hh:mm a zzz");
        cl.add(10, 1);
    }

    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();

        connectionsClient
                .startDiscovery(PACKAGE_NAME, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        discover -> {
                            Log.i("Disc Started", "Discovery Started" + " :Thread Name " + Thread.currentThread().getName());
                        })
                .addOnFailureListener(
                        e -> {
                            Log.i("Disc Exception", "Discovery Exception");
                        });

    }

    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                EndPointName, PACKAGE_NAME, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    @Override
    protected void onStop() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }

    @SuppressLint("DefaultLocale")
    private String hmsTimeFormatter(long milliSeconds) {
        return String.format("%02d:%02d:%02d", new Object[]{Long.valueOf(TimeUnit.MILLISECONDS.toHours(milliSeconds)), Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds))), Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)))});
    }

    public void setProgressandTimerInitValues() {
        progressBarCircle.setMax(((int) def_time_millisec) / 1000);
        progressBarCircle.setProgress(((int) def_time_millisec) / 1000);
    }

    public void startCountDownTimer(long timeCountInMilliSeconds) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        radar_rl.setVisibility(View.GONE);
        timer_rl.setVisibility(View.VISIBLE);
        final CountDownTimer cdt = new CountDownTimer(timeCountInMilliSeconds, 1000) {
            public void onTick(long millisUntilFinished) {
                textViewTime.setText(hmsTimeFormatter(millisUntilFinished));
                progressBarCircle.setProgress((int) (millisUntilFinished / 1000));
            }

            public void onFinish() {
                textViewTime.setText(hmsTimeFormatter(0));
                progressBarCircle.setProgress(0);
                titleMsg_TV.setText("Times Up");
                countDownTimer.cancel();
            }
        };
        countDownTimer = cdt;
        countDownTimer.start();
    }

    @Override
    public void onBackPressed() {
        if (radar_rl.getVisibility() == View.VISIBLE) {
            finish();
        }
        if (radar_rl.getVisibility() == View.GONE && timer_rl.getVisibility() == View.VISIBLE) {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            timer_rl.setVisibility(View.GONE);
            radar_rl.setVisibility(View.VISIBLE);
            setProgressandTimerInitValues();
        } else {
            super.onBackPressed();
        }
    }
}
