package com.example.aventador.protectalarm.customViews;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;

/**
 * Created by Aventador on 02/10/2017.
 */

public class CustomPagerAdapter extends PagerAdapter {

    private static final String TAG = "CustomPagerAdapter";
    private Context context;
    private ArrayList<GuardianSubView> allSubGardians;

    public CustomPagerAdapter(Context context) {
        this.context = context;
        this.allSubGardians = new ArrayList<>();
        init();
    }

    private void init() {
        this.allSubGardians.add(new GuardianSubView("Settings", R.layout.guardian_settings));
        this.allSubGardians.add(new GuardianSubView("History", R.layout.guardian_history));
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        Logger.d(TAG, "instantiateItem");
        GuardianSubView subView = allSubGardians.get(position);
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(subView.getLayoutResId(), collection, false);
        collection.addView(layout);
        return layout;
    }

    @Override
    public int getCount() {
        return allSubGardians.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        Logger.d(TAG, "isViewFromObject");
        return (view == object);

    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
        this.allSubGardians.remove(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        GuardianSubView subView = allSubGardians.get(position);
        return subView.getTitle();
    }
}