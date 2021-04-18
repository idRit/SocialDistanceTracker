package com.example.socialdistancetracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.socialdistancetracker.LoginActivity.isValidEmail;

public class SignupActivity extends AppCompatActivity {
    TextView textView = null;

    private String otp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        Button signup = findViewById(R.id.signup);
        textView = findViewById(R.id.state_signup);
        textView.setText("Password should have:\n1. At least 1 special character\n2. At least 1 number\n3. At least 1 capital character\n4. 8 characters long");

        EditText email = findViewById(R.id.editTextTextEmailAddressSignup);
        EditText password = findViewById(R.id.ETTextPassword);
        EditText passwordRE = findViewById(R.id.ETTextPasswordRe);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailStr = email.getText().toString();
                String passStr = password.getText().toString();
                String passStr2 = passwordRE.getText().toString();
//                if (validatePassword(passStr) && validatePassword(passStr2) && isValidEmail(emailStr) && passStr.equals(passStr2)) {
                    signup(emailStr, passStr);
//                }
//                else
//                    textView.setText("Error: Make sure passwords match and email is correct!");
            }
        });
    }

    public boolean validatePassword(final String password) {
        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{4,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }

    void signup(String username, String password) {
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("username", username);
            jsonBody.put("email", username);
            jsonBody.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final String requestBody = jsonBody.toString();

        StringRequest loginRequest = prepareSignupRequest(requestBody, queue);

        prepareOtpGeneration(queue, username, "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("OTP verification");

        final EditText otpInput = new EditText(this);
        builder.setView(otpInput);

        builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prepareOtpVerification(queue, username, otpInput.getText().toString(), loginRequest);
            }
        });

        builder.show();
    }

    StringRequest prepareSignupRequest(String requestBody, RequestQueue queue) {queue.add(prepareLoginRequest("http://3.22.130.81:3300/api/login", requestBody, queue));
        StringRequest loginRequest = new StringRequest(Request.Method.POST, "http://3.22.130.81:3300/api/signup",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            Boolean success = Boolean.parseBoolean(obj.getString("success"));
                            if (success) {
                                textView.setText("Signed up, now logging in!");
                                queue.add(prepareLoginRequest("http://3.22.130.81:3300/api/login", requestBody, queue));
                            }
                            else textView.setText("Something Happened!");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        }) {
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

    StringRequest prepareLoginRequest(String loginURL, String requestBody, RequestQueue queue) {
        StringRequest loginRequest = new StringRequest(Request.Method.POST, loginURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            Boolean success = Boolean.parseBoolean(obj.getString("success"));
                            if (success){
                                saveDataAndChangeScreen(obj.getString("token"), obj.getString("id"), obj.getString("qr"),queue);
                            }
                            else {
                                textView.setText("Please wait!");
//                                queue.add(prepareSignupRequest(requestBody, queue));
//                                newUser = true;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        }) {
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



    void prepareActivation(RequestQueue queue, String id, String token) {
        StringRequest loginRequest = new StringRequest(Request.Method.GET, "http://3.22.130.81:3300/api/activateProfile/" + id,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            int success = Integer.parseInt(obj.getString("success"));
                            if (success == 0) {
                                Log.d("Activation", "done");
                            }
                            else Log.d("Activation", "not done");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
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

        queue.add(loginRequest);
    }

    void prepareOtpGeneration(RequestQueue queue, String email, String token) {
        StringRequest loginRequest = new StringRequest(Request.Method.POST, "http://3.22.130.81:3300/api/generate-otp/",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            boolean success = Boolean.parseBoolean(obj.getString("success"));
                            if (success == true) {
                                Log.d("OTP", "Sent");
                            }
                            else Log.d("OTP", "not done");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);

                    String requestBody = jsonBody.toString();

                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
//                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                } catch (JSONException jsonException) {
                    return null;
                }
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

        queue.add(loginRequest);
    }

    void prepareOtpVerification(RequestQueue queue, String email, String otp, StringRequest loginRequest) {
        StringRequest verificationRequest = new StringRequest(Request.Method.POST, "http://3.22.130.81:3300/api/verify-otp/",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {
                            JSONObject obj = new JSONObject(response);
                            boolean success = Boolean.parseBoolean(obj.getString("success"));
                            if (success == true) {
                                queue.add(loginRequest);
                                Log.d("OTP", "Sent");
                            }
                            else textView.setText("Please restart the application and restart the sign-up process!");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("That didn't work!");
            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);
                    jsonBody.put("otp", otp);

                    String requestBody = jsonBody.toString();

                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
//                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                } catch (JSONException jsonException) {
                    return null;
                }
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

        queue.add(verificationRequest);
    }

    void saveDataAndChangeScreen(String token, String id, String qr, RequestQueue queue) {
        textView.setText("Logged in: " + id);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().putString("token", token)
                .putString("id", id)
                .putString("qr", qr)
                .apply();

        prepareActivation(queue, id, token);

        this.startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        finish();
    }
}