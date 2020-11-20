package com.example.socialdistancetracker.ui.home.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.LongDef;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialdistancetracker.MainActivity;
import com.example.socialdistancetracker.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.example.socialdistancetracker.App.CHANNEL_ID;

public class LongPollService extends Service {

    private static final String TAG = "LongPollService";
    PendingIntent pendingIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Service Running")
                .setContentText("Your Bluetooth and Location data are used!")
                .setSmallIcon(R.drawable.ic_baseline_android_24)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        discoverDevices();

        return START_NOT_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    public void discoverDevices() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName().length() == 24) {
//                if (device.getName().equals("10.or")) {
                    //notify
                    pollToServer(device.getName());
                    Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setContentTitle("Contact Traced!")
                            .setContentText("Please move away to a safe distance!")
                            .setSmallIcon(R.drawable.ic_baseline_info_24)
                            .setContentIntent(pendingIntent)
                            .build();

                    NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(0, notification);
                }

//                Toast.makeText(getApplicationContext(), device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    void pollToServer(String scannedId) {
        double[] coords = getLocation();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String userId = sharedPref.getString("id", "");
        String token = sharedPref.getString("token", "");
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("scannedId", scannedId);
            jsonBody.put("userId", userId);
            jsonBody.put("lat", coords[0]);
            jsonBody.put("lng", coords[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest locationRequest = prepareLocationRequest(requestBody, token);
        StringRequest blueRequest = prepareBlueRequest(requestBody, token);

        queue.add(locationRequest);
        queue.add(blueRequest);
    }

    StringRequest prepareLocationRequest(String requestBody, String token) {
        StringRequest locationRequest = new StringRequest(Request.Method.POST, "http://3.22.130.81:3300/api/updateLocation",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            int success = Integer.parseInt(obj.getString("success"));
                            switch (success) {
                                case -1:
                                    Log.d(TAG, "onResponse: Server Error");
                                    break;
                                case 1:
                                    Log.d(TAG, "onResponse: Location updated along with score!");
                                    break;
                                    case 0:
                                        Log.d(TAG, "onResponse: Location updated!");
                                        break;
                                default:
                                    Log.d(TAG, "onResponse: Something happened");

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LongPollService.this, "Social Distance Tracker: Network issue!", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //add params <key,value>
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                // add headers <key,value>

                headers.put("Authorization", token);
                return headers;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    // can get more details such as response.headers
                    return super.parseNetworkResponse(response);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        return locationRequest;
    }

    StringRequest prepareBlueRequest(String requestBody, String token) {
        StringRequest blueRequest = new StringRequest(Request.Method.POST, "http://3.22.130.81:3300/api/updateBluetoothScore",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            int success = Integer.parseInt(obj.getString("success"));
                            switch (success) {
                                case -1:
                                    Log.d(TAG, "onResponse: Server Error");
                                    break;
                                case 1:
                                    Log.d(TAG, "onResponse: Bluetooth score increamented");
                                    break;
                                default:
                                    Log.d(TAG, "onResponse: Something happened");

                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LongPollService.this, "Social Distance Tracker: Network issue!", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //add params <key,value>
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                // add headers <key,value>

                headers.put("Authorization", token);
                return headers;
            }

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String responseString = "";
                if (response != null) {
                    responseString = String.valueOf(response.statusCode);
                    // can get more details such as response.headers
                    return super.parseNetworkResponse(response);
                }
                return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
            }
        };

        return blueRequest;
    }

    public double[] getLocation() {
        GpsTrackerService gpsTracker = new GpsTrackerService(this);
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            return new double[]{latitude, longitude};
        } else {
            gpsTracker.showSettingsAlert();
            return new double[]{0, 0};
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
