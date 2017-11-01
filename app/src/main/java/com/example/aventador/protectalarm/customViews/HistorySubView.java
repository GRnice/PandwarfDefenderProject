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
 * Created by Giangrasso on 03/10/2017.
 */

/**
 * HistorySubView is responsible for displaying the history of differents events 'attack detected, protection started..."
 * HistorySubView is the controller of the history sub view. It extend GuardianSubView
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

    /**
     * Add a log the history.
     * clear button is displayed, and the log is added to the LogAdapter
     * after that the log is displayed into the listview.
     * @param log
     */
    public void addLog(HistoryLog log) {
        clearButton.setVisibility(View.VISIBLE);
        LogAdapter logAdapter = (LogAdapter) listViewLogs.getAdapter();
        logAdapter.add(log);
        logAdapter.notifyDataSetChanged();
    }

    /**
     * Like onCreateView, this method inflate the view and gets all widgets.
     * @param inflater
     * @param viewGroup
     * @return
     */
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
        // if clear button is pressed -> flush the history.
        if (view == clearButton) {
            LogAdapter logAdapter = (LogAdapter) listViewLogs.getAdapter();
            logAdapter.clear();
            logAdapter.notifyDataSetChanged();
        }
    }
}
