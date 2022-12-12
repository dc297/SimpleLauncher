package com.dc297.simplelauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dc297.simplelauncher.domain.AppDatabase;
import com.dc297.simplelauncher.domain.AppDto;
import com.dc297.simplelauncher.domain.entity.AppUsage;
import com.dc297.simplelauncher.domain.repository.AppUsageDao;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainFragment extends RowsSupportFragment {
    private static final String TAG = "MainFragment";

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final String RECENT_HEADING = "Recent Apps";

    AppUsageDao appUsageDao;
    List<AppDto> apps;
    ArrayObjectAdapter recentAppsAdapter;
    Disposable recentAppsFlowable;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);
        appUsageDao = Room.databaseBuilder(getContext(), AppDatabase.class, "app-database").build().appUsageDao();

        prepareBackgroundManager();

        loadRows();

        setupEventListeners();
    }

    private void loadRows() {
        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(rowsAdapter);
        CardPresenter cardPresenter = new CardPresenter();
        Map<String, List<AppDto>> apps = getApps();

        int i = 0;
        for(Map.Entry<String, List<AppDto>> entry : apps.entrySet()) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            listRowAdapter.addAll(0, entry.getValue());
            rowsAdapter.add(new ListRow(new HeaderItem(i, entry.getKey()), listRowAdapter));
            i++;
        }

        HeaderItem gridHeader = new HeaderItem(i, "PREFERENCES");

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.grid_view));
        gridRowAdapter.add(getString(R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        rowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        getRecentAppsFromDatabase(rowsAdapter, cardPresenter);
    }

    private void getRecentAppsFromDatabase(ArrayObjectAdapter rowsAdapter, CardPresenter cardPresenter) {
        recentAppsFlowable = appUsageDao.getAll()
                .defaultIfEmpty(new ArrayList<>())
                .map(x -> {
                    x.sort(Comparator.comparing(a -> ((AppUsage)a).lastLaunchTime).reversed());
                    List<AppDto> recentApps = new ArrayList<>();
                    for(AppUsage recentApp : x) {
                        Optional<AppDto> optionalApp = apps.stream().filter(a -> a.getPackageName().equals(recentApp.packageName)).findAny();
                        optionalApp.ifPresent(recentApps::add);
                    }
                    return recentApps;
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(recentApps -> {
                    if (CollectionUtils.isNotEmpty(recentApps)) {
                        if (recentAppsAdapter == null){
                            recentAppsAdapter = new ArrayObjectAdapter(cardPresenter);
                            recentAppsAdapter.addAll(0, recentApps);
                            rowsAdapter.add(0, new ListRow(new HeaderItem(0, RECENT_HEADING), recentAppsAdapter));
                        }
                        else {
                            recentAppsAdapter.clear();
                            recentAppsAdapter.addAll(0, recentApps);
                        }
                    }
                }, e -> Log.e("sl-err", "Error while loading recent apps", e));
    }

    private Map<String, List<AppDto>> getApps() {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getContext().getPackageManager();
        apps = pm.queryIntentActivities( mainIntent, 0).stream()
                .map(r -> AppDto.from(r, pm)).collect(Collectors.toList());
        return apps.stream()
                .collect(Collectors.groupingBy(
                        a -> String.valueOf(a.getTitle().charAt(0)),
                        TreeMap::new,
                        Collectors.toList()));
    }

    private void prepareBackgroundManager() {
        BackgroundManager mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        DisplayMetrics mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof AppDto) {
                AppDto app = (AppDto) item;
                saveUsageDetails(app.getPackageName());
                Log.d(TAG, "Item: " + item);
                Context context = getContext();
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
                if (intent == null) {
                    return;
                    //throw new ActivityNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                context.startActivity(intent);
            } else if (item instanceof String) {
                if (((String) item).contains(getString(R.string.error_fragment))) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveUsageDetails(String packageName) {
        appUsageDao.getByPackageName(packageName)
                .defaultIfEmpty(new AppUsage(packageName))
                .flatMapCompletable(value -> appUsageDao.save(value.incrementCount()))
                .subscribeOn(Schedulers.newThread())
                .subscribe();
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(
                    ContextCompat.getColor(getContext(), R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}