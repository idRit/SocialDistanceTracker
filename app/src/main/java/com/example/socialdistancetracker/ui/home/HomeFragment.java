package com.example.socialdistancetracker.ui.home;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialdistancetracker.MainActivity;
import com.example.socialdistancetracker.R;
import com.example.socialdistancetracker.ui.home.services.GpsTrackerService;
import com.example.socialdistancetracker.ui.home.services.LongPollService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    GpsTrackerService gpsTracker = null;
    BluetoothAdapter bluetoothAdapter;

    TextView hscore, bluescore;

    final long lTimeToGiveUp_ms = System.currentTimeMillis() + 10000;

    WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Cannot find physical bluetooth!", Toast.LENGTH_SHORT).show();
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        final Boolean[] backgroundService = {sharedPref.getBoolean("backgroundService", false)};

        Button startStop = root.findViewById(R.id.startserv);
        if (backgroundService[0]) {
            startStop.setText("Stop Service");
        } else startStop.setText("Start Service");
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backgroundService[0]) {
                    backgroundService[0] = false;
                    startStop.setText("Start Service");
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                    sharedPref.edit().putBoolean("backgroundService", false).apply();
                    //disable bluetooth
                    tryChangingName("Nokia 6.1 Plus", true);
                    Toast.makeText(getContext(), "Service Deactivated", Toast.LENGTH_SHORT).show();
                    //stop the service
                    Intent serviceIntent = new Intent(getContext(), LongPollService.class);
                    getActivity().stopService(serviceIntent);
                } else {
                    backgroundService[0] = true;
                    startStop.setText("Stop Service");
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                    sharedPref.edit().putBoolean("backgroundService", true).apply();
                    //Enable bluetooth
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(bluetoothIntent, 11);
                    } else {
                        String sOldName = bluetoothAdapter.getName();
                        sharedPref.edit().putString("blName", sOldName).apply();
                        tryChangingName(sharedPref.getString("id", "null"), false);

                        //start the service
                        Intent serviceIntent = new Intent(getContext(), LongPollService.class);
                        getActivity().startService(serviceIntent);
                    }
                }
            }
        });

        webView = (WebView) root.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(0);

        try {
            if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        getLocation();


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!this.isInterrupted()) {
                        Thread.sleep(10000);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getLocation();
                            }
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        // thread.start();

        hscore = root.findViewById(R.id.hscore);
        bluescore = root.findViewById(R.id.blc);

        hscore.setText(sharedPref.getString("hsc", "0"));
        bluescore.setText(sharedPref.getString("bsc", "0"));

        loadValues();

        return root;
    }

    void loadValues() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String userId = sharedPref.getString("id", "");
        String token = sharedPref.getString("token", "");

        RequestQueue queue = Volley.newRequestQueue(getContext());

        StringRequest req = prepareScoresRequest(userId, token);
        queue.add(req);
    }

    StringRequest prepareScoresRequest(String id, String token) {
        StringRequest loginRequest = new StringRequest(Request.Method.GET, "http://3.22.130.81:3300/api/getProfileCurrentScores/" + id,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getInt("success") == 1) {
//                                Toast.makeText(getContext(), obj.getString("bluetoothScore"), Toast.LENGTH_SHORT).show();
                                bluescore.setText(obj.getString("bluetoothScore"));
                                hscore.setText(obj.getString("score"));

                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
                                sharedPref.edit()
                                        .putString("bsc", obj.getString("bluetoothScore"))
                                        .putString("hsc", obj.getString("score"))
                                        .apply();
                            } else {
                                Toast.makeText(getContext(), "Internal Server Error", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getContext(), "Network issue, will try again later!", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //add params <key,value>
                return params;
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

        return loginRequest;
    }

    public void getLocation() {
        gpsTracker = new GpsTrackerService(getActivity());
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            webView.loadUrl("https://idrit.github.io/map-profile/?lat=" + latitude + "&lng=" + longitude);
        } else {
            gpsTracker.showSettingsAlert();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Toast.makeText(getContext(), "Bluetooth Turned On!", Toast.LENGTH_SHORT).show();
            String sOldName = bluetoothAdapter.getName();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPref.edit().putString("blName", sOldName).apply();
            tryChangingName(sharedPref.getString("id", "null"), false);

            //start the service
            Intent serviceIntent = new Intent(getContext(), LongPollService.class);
            getActivity().startService(serviceIntent);
        }
        if (resultCode == RESULT_CANCELED) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
            sharedPref.edit().putBoolean("backgroundService", false).apply();

            Toast.makeText(getContext(), "Bluetooth Operation Cancelled!", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(getContext(), LongPollService.class);
            getActivity().stopService(serviceIntent);
        }
    }

    public void tryChangingName(String sNewName, Boolean turnOff) {
        final Handler myTimerHandler = new Handler();
        myTimerHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.setName(sNewName);
                            if (sNewName.equalsIgnoreCase(bluetoothAdapter.getName())) {
                                Log.i("HomeFragment", "Updated BT Name to " + bluetoothAdapter.getName());
                                if (turnOff) {
                                    bluetoothAdapter.disable();
                                }
                            }
                        }
                        if ((!sNewName.equalsIgnoreCase(bluetoothAdapter.getName())) && (System.currentTimeMillis() < lTimeToGiveUp_ms)) {
                            myTimerHandler.postDelayed(this, 500);
                            if (bluetoothAdapter.isEnabled())
                                Log.i("HomeFragment", "Update BT Name: waiting on BT Enable");
                            else
                                Log.i("HomeFragment", "Update BT Name: waiting for Name (" + sNewName + ") to set in");
                        }
                    }
                }, 500);

    }

}