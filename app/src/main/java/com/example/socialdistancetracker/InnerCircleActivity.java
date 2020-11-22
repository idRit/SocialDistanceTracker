package com.example.socialdistancetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.socialdistancetracker.adapter.ListAdapter;
import com.example.socialdistancetracker.adapter.RecyclerItemClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InnerCircleActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    List<String> listOfEmails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inner_circle);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {}

                    @Override public void onLongItemClick(View view, int position) {
                        TextView email = view.findViewById(R.id.txtName);
                        String emailStr = email.getText().toString();
                        removeUser(emailStr);
                    }
                })
        );

        getAllInnerCircleEmails();
    }

    void getAllInnerCircleEmails() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String userId = sharedPref.getString("id", "");
        String token = sharedPref.getString("token", "");

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = prepareGetAllInnerCircleEmails(userId, token);
        queue.add(req);
    }

    void removeUser(String scannedId) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String userId = sharedPref.getString("id", "");
        String token = sharedPref.getString("token", "");

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest scanRequest = prepareRemovalRequest(userId, scannedId, token);

        queue.add(scanRequest);
    }

    StringRequest prepareRemovalRequest(String id, String refId, String token) {
        StringRequest loginRequest = new StringRequest(Request.Method.GET,
                "http://3.22.130.81:3300/api/innerCircle/removeInnerCircle/" + id + "/" + refId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            switch (obj.getInt("success")) {
                                case 1:
                                    Toast.makeText(getApplicationContext(), "User removed!", Toast.LENGTH_SHORT).show();
                                    getAllInnerCircleEmails();
                                    break;
                                case 0:
                                    Toast.makeText(getApplicationContext(), "User not Found!", Toast.LENGTH_SHORT).show();
                                    getAllInnerCircleEmails();
                                    break;
                                case -1:
                                    Toast.makeText(getApplicationContext(), "Something Happened!", Toast.LENGTH_SHORT).show();
                                    getAllInnerCircleEmails();
                                    break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Network issue, will try again later!", Toast.LENGTH_SHORT).show();
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

    StringRequest prepareGetAllInnerCircleEmails(String id, String token) {
        StringRequest loginRequest = new StringRequest(Request.Method.GET,
                "http://3.22.130.81:3300/api/innerCircle/getAllInnerCircleEmails/" + id,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            if (obj.getInt("success") == 1) {
                                JSONArray arr =  obj.getJSONArray("listOfEmails");
                                List<String> list = new ArrayList<String>();
                                if (arr.length() == 0) {
                                    Toast.makeText(getApplicationContext(), "No one added!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                                for(int i = 0; i < arr.length(); i++){
                                    list.add(arr.getString(i));
                                }
                                mAdapter = new ListAdapter(list);
                                recyclerView.setAdapter(mAdapter);
                            } else {
                                Toast.makeText(getApplicationContext(), "No one added!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Network issue, will try again later!", Toast.LENGTH_SHORT).show();
                finish();
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