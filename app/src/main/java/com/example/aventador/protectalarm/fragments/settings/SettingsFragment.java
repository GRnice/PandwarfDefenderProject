package com.example.aventador.protectalarm.fragments.settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;

import com.example.aventador.protectalarm.Main2Activity;
import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.storage.FileManager;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.zip.Inflater;

/**
 * Created by Aventador on 08/11/2017.
 */

public class SettingsFragment extends Fragment {

    private static final String TAG = "SettingsFragment";

    private ImageButton backButton;

    private Switch enableCallApi;
    private EditText urlApi;

    private SettingsHub settingsHub;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bodyView = inflater.inflate(R.layout.fragment_settings, container, false);


        backButton = (ImageButton) bodyView.findViewById(R.id.back_button_settings);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
                ((Main2Activity) getActivity()).closeSettings();
            }
        });

        urlApi = (EditText) bodyView.findViewById(R.id.url_to_call_edittext);
        urlApi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                settingsHub.setUrlApi(editable.toString());
            }
        });

        enableCallApi = (Switch) bodyView.findViewById(R.id.contact_api_switch);
        enableCallApi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                settingsHub.setCallApiEnabled(enabled);
                urlApi.setEnabled(enabled);
            }
        });
        loadSettings();
        return bodyView;
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.d(TAG, "onStop()");
        saveSettings();
    }

    private void saveSettings() {
        Logger.d(TAG, "saveSettings():");
        FileManager.getInstance().saveSettings(getContext(), getString(R.string.settings_app_file), settingsHub);
    }

    private void loadSettings() {
        Logger.d(TAG, "loadSettings():");
        settingsHub = FileManager.getInstance().loadSettings(getContext(), getString(R.string.settings_app_file));
        if (settingsHub ==  null) {
            settingsHub = new SettingsHub();
            return;
        }
        enableCallApi.setChecked(settingsHub.callApiEnabled());
        urlApi.setText(settingsHub.getUrlApi());
    }
}
