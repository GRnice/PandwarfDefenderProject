package com.example.aventador.protectalarm.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.storage.FileManager;
import com.example.aventador.protectalarm.tools.Logger;

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

    @Nullable
    private String loadNetAddress(Context context) {
        return FileManager.getInstance().fromSharedPrefsGet(context, context.getString(R.string.shared_prefs_url_key));

    }

    public void send(Context context) {
        Logger.d(TAG, "send()");
        String url = loadNetAddress(context);
        if (url == null) {
            Logger.e(TAG, "send: Url not defined");
            return;
        }


        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);

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
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }
}
