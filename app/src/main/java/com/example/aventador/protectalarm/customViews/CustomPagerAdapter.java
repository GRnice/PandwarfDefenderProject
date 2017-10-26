package com.example.aventador.protectalarm.customViews;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.tools.Logger;

import java.util.ArrayList;

/**
 * Created by Aventador on 02/10/2017.
 */


/**
 * This PagerAdapter is used by Guardian fragment
 *
 * This object store all sub views (SettingsSubView & HistorySubView)
 */
public class CustomPagerAdapter extends PagerAdapter {

    private static final String TAG = "CustomPagerAdapter";
    private Context context;
    private ArrayList<GuardianSubView> allSubGardians;

    public CustomPagerAdapter(Context context, ViewPager viewPager) {
        this.context = context;
        this.allSubGardians = new ArrayList<>(); // contains all subviews of Guardian fragment.
        init(viewPager);
    }

    /**
     * Returns the SettingsSubView object.
     * @return
     */
    @Nullable
    public SettingsSubView getSettingsSubView() {
        for (int i = 0; i < allSubGardians.size(); i++) {
            if (allSubGardians.get(i) instanceof SettingsSubView) {
                return (SettingsSubView) allSubGardians.get(i);
            }
        }

        return null;
    }

    /**
     * Returns the HistorySubView object.
     *
     * @return
     */
    @Nullable
    public HistorySubView getHistorySubView() {
        for (int i = 0; i < allSubGardians.size(); i++) {
            if (allSubGardians.get(i) instanceof HistorySubView) {
                return (HistorySubView) allSubGardians.get(i);
            }
        }

        return null;
    }

    /**
     * Init the PagerAdapter.
     * SettingsSubView & HistorySubView are Created.
     * @param viewPager
     */
    private void init(ViewPager viewPager) {
        this.allSubGardians.add(new SettingsSubView(context, "Settings", R.layout.guardian_settings, viewPager));
        this.allSubGardians.add(new HistorySubView(context, "History", R.layout.guardian_history));
    }

    /**
     * instantiateItem call "instantiate" method of the selected object. This object extends GuardianSubView.
     * @param collection
     * @param position
     * @return
     */
    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        Logger.d(TAG, "instantiateItem");
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = allSubGardians.get(position).instantiate(inflater, collection);
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
