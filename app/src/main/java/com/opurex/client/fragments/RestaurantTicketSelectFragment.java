package com.opurex.client.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.opurex.client.R;
import com.opurex.client.RestaurantTicketSelect;
import com.opurex.client.data.Data;
import com.opurex.client.models.Floor;
import com.opurex.client.models.Place;
import com.opurex.client.widgets.FloorView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by svirch_n on 23/05/16
 * Last edited at 12:13.
 */
public class RestaurantTicketSelectFragment extends Fragment implements FloorView.FloorOnClickListener {

    private static String currentTabTag;
    private TabHost.OnTabChangeListener onTabChangedListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            currentTabTag = tabId;
        }
    };

    private List<FloorView> floorViewList = new LinkedList<>();
    private RestaurantTicketSelect restaurantTicketSelect;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.restaurant_ticket_fragment, container, false);
        TabHost tabHost = (TabHost) result.findViewById(R.id.tabhost);
        tabHost.setup();
        FrameLayout frameLayout = (FrameLayout) tabHost.findViewById(android.R.id.tabcontent);
        TabWidget tabWidget = tabHost.getTabWidget();
        for (int i = 0; i < Data.Place.floors.size(); i++) {
            Floor floor = Data.Place.floors.get(i);
            FloorView floorView = new FloorView(getContext(), floor);
            floorViewList.add(floorView);
            floorView.setOnPlaceClickListener(this);
            final int id = i;
            floorView.setId(id);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            frameLayout.addView(floorView, params);
            TabHost.TabSpec tabSpec = tabHost.newTabSpec(floor.getId()).setIndicator(floor.getName());
            tabSpec.setContent(id);
            tabHost.addTab(tabSpec);
            View childView = tabWidget.getChildTabViewAt(i);
            childView.setBackgroundResource(R.drawable.tab_selector);
            TextView tabTitle = (TextView) childView.findViewById(android.R.id.title);
            if (childView != null) {
                tabTitle.setTextColor(getResources().getColor(R.color.popup_outer_txt));
                tabTitle.setTypeface(null, Typeface.BOLD);
            }
        }

        if (currentTabTag != null) {
            tabHost.setCurrentTabByTag(currentTabTag);
        }
        tabHost.setOnTabChangedListener(onTabChangedListener);
        return result;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RestaurantTicketSelect) {
            this.restaurantTicketSelect = (RestaurantTicketSelect) context;
        }
    }

    public void refreshView() {
        for (FloorView each : floorViewList) {
            each.update();
        }
    }

    @Override
    public void onClick(Floor floor, Place place) {
        if (this.restaurantTicketSelect != null) {
            this.restaurantTicketSelect.accessPlace(place);
        }
    }
}
