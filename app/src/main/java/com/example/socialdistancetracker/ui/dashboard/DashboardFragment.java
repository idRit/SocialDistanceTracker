package com.example.socialdistancetracker.ui.dashboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.socialdistancetracker.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    TextView hscore, bluescore;
    CalendarView calender;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        hscore = root.findViewById(R.id.hscore);
        bluescore = root.findViewById(R.id.blc);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        hscore.setText(sharedPref.getString("hsc", "0"));
        bluescore.setText(sharedPref.getString("bsc", "0"));

        calender = root.findViewById(R.id.calendarView);

        calender.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                String date
                        = year + "-"
                        + (month + 1) + "-" + dayOfMonth;

                getScoreOfDay(date);
            }
        });

        return root;
    }

    void getScoreOfDay(String date) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        String userId = sharedPref.getString("id", "");
        String token = sharedPref.getString("token", "");

        RequestQueue queue = Volley.newRequestQueue(getContext());

        StringRequest req = prepareScoresRequest(userId, token, date);
        queue.add(req);
    }

    StringRequest prepareScoresRequest(String id, String token, String date) {
        StringRequest loginRequest = new StringRequest(Request.Method.GET,
                "http://3.22.130.81:3300/api/score/getAllScores/" + id + "/" + date,
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
                            } else {
                                Toast.makeText(getContext(), "Invalid Date!", Toast.LENGTH_SHORT).show();
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
}