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

import java.util.zip.Inflater;

/**
 * Created by Aventador on 08/11/2017.
 */

public class SettingsFragment extends Fragment {

    private ImageButton backButton;

    private Switch enableCallApi;
    private EditText urlApi;

    private SettingsHub settingsHub;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View bodyView = inflater.inflate(R.layout.fragment_settings, container, false);
        settingsHub = new SettingsHub();
        backButton = (ImageButton) bodyView.findViewById(R.id.back_button_settings);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        return bodyView;
    }
}
