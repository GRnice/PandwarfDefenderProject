package com.example.aventador.protectalarm.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.storage.FileManager;
import com.example.aventador.protectalarm.tools.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Aventador on 06/11/2017.
 */

public class SenderNet {
    private static final String TAG = "SenderNet";
    private static SenderNet instance;
    public static SenderNet getInstance() {
        if (instance == null) {
            instance = new SenderNet();
        }
        return instance;
    }

    private SenderNet() {

    }

    public void send(@NonNull Context context, @Nullable String url, @NonNull String message) {
        Logger.d(TAG, "send()");

        if (url == null) {
            Logger.e(TAG, "send: Url not defined");
            return;
        }


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String requestBody = jsonBody.toString();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Logger.d(TAG, "send: onResponse:" + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.e(TAG, "send: onErrorResponse: " + error.getMessage());
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
                    Logger.e(TAG, "Unsupported Encoding while trying to get the bytes of "+ requestBody + "utf-8");
                    return null;
                }
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
        queue.start();

    }
}
