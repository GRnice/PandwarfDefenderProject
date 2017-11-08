package com.example.aventador.protectalarm.fragments.settings;

import android.support.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Aventador on 08/11/2017.
 */

public class SettingsHub {
    @Expose
    @SerializedName("call_api_enabled")
    private boolean callApiEnabled = false;

    @Expose
    @SerializedName("url_api")
    @Nullable
    private String urlApi = null;

    public SettingsHub() {

    }

    public boolean callApiEnabled() {
        return callApiEnabled;
    }

    public void setCallApiEnabled(boolean enabled) {
        this.callApiEnabled = enabled;
    }

    @Nullable
    public String getUrlApi() {
        return urlApi;
    }

    public void setUrlApi(String urlApi) {
        this.urlApi = urlApi;
    }
}
