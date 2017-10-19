package com.example.aventador.protectalarm.customViews;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.customViews.adapters.LogAdapter;

import java.util.ArrayList;

/**
 * Created by Aventador on 03/10/2017.
 */

public class HistorySubView extends GuardianSubView {

    private ListView listViewLogs;
    private View layout;
    private Button clearButton;
    private Context context;

    public HistorySubView(Context context, String title, int layoutResId) {
        super(title, layoutResId);
        this.context = context; // for generate LogAdapter
    }

    public void addLog(HistoryLog log) {
        clearButton.setVisibility(View.VISIBLE);
        LogAdapter logAdapter = (LogAdapter) listViewLogs.getAdapter();
        logAdapter.add(log);
        logAdapter.notifyDataSetChanged();
    }

    @Override
    public View instantiate(LayoutInflater inflater, ViewGroup viewGroup) {
        layout = inflater.inflate(getLayoutResId(), viewGroup, false);
        listViewLogs = (ListView) layout.findViewById(R.id.history_listview);
        listViewLogs.setAdapter(new LogAdapter(context, 0, new ArrayList<HistoryLog>()));
        clearButton = (Button) layout.findViewById(R.id.clear_button_history);
        clearButton.setOnClickListener(this);
        clearButton.setVisibility(View.GONE);
        return layout;
    }

    @Override
    public void onClick(View view) {
        if (view == clearButton) {
            LogAdapter logAdapter = (LogAdapter) listViewLogs.getAdapter();
            logAdapter.clear();
            logAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
