package com.example.aventador.protectalarm.customViews.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.customViews.HistoryLog;

import java.util.List;

/**
 * Created by Aventador on 03/10/2017.
 */

/**
 * Used by HistorySubView.class {@link com.example.aventador.protectalarm.customViews.HistorySubView }
 * LogAdapter store many HistoryLog, where each HistoryLog contains differentes informations "date", "message"...
 * {@link HistoryLog}
 *
 * This adapter is associated to the listView of guardian_history.xml
 */

public class LogAdapter extends ArrayAdapter<HistoryLog> {

    public LogAdapter(Context context, int resource) {
        super(context, resource);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public LogAdapter(Context context, int resource, HistoryLog[] objects) {
        super(context, resource, objects);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId, HistoryLog[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public LogAdapter(Context context, int resource, List<HistoryLog> objects) {
        super(context, resource, objects);
    }

    public LogAdapter(Context context, int resource, int textViewResourceId, List<HistoryLog> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_history_listview, parent, false);
        }

        HistoryLog historyLog = getItem(position);
        /**
         * Now we check the warning level of the given historyLog.
         */
        if (historyLog.getWarningLevel().equals(HistoryLog.WARNING_LEVEL.HIGH)) {
            ImageView imageView = (ImageView) convertView.findViewById(R.id.log_warning_imageView);
            Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_error_outline, null);
            imageView.setImageDrawable(drawable);
        } else if (historyLog.getWarningLevel().equals(HistoryLog.WARNING_LEVEL.LOW)) {
            ImageView imageView = (ImageView) convertView.findViewById(R.id.log_warning_imageView);
            Drawable drawable = getContext().getResources().getDrawable(R.drawable.ic_info_outline, null);
            imageView.setImageDrawable(drawable);
        } else {

        }
        TextView textView = (TextView) convertView.findViewById(R.id.message_log_textview);
        textView.setText(historyLog.getDate() + " : " + historyLog.getMessage()); // set the message of the given historyLog.

        return convertView;
    }
}
