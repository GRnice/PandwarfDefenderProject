package com.example.aventador.protectalarm.customViews;

import android.content.Context;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
    private Context context;

    public HistorySubView(Context context, String title, int layoutResId) {
        super(title, layoutResId);
        this.context = context; // for generate LogAdapter
    }

    public void addLog(HistoryLog log) {
        LogAdapter logAdapter = (LogAdapter) listViewLogs.getAdapter();
        logAdapter.add(log);
        logAdapter.notifyDataSetChanged();
    }

    @Override
    public View instantiate(LayoutInflater inflater, ViewGroup viewGroup) {
        layout = inflater.inflate(getLayoutResId(), viewGroup, false);
        listViewLogs = (ListView) layout.findViewById(R.id.history_listview);
        listViewLogs.setAdapter(new LogAdapter(context, 0, new ArrayList<HistoryLog>()));
        return layout;
    }

    @Override
    public void onClick(View view) {

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
