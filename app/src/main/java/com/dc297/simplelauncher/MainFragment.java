package com.dc297.simplelauncher;

import static androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_LARGE;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.room.Room;

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
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainFragment extends RowsSupportFragment {
    private static final String TAG = "MainFragment";

    private static final String RECENT_HEADING = "Recent Apps";

    AppUsageDao appUsageDao;
    List<AppDto> apps;
    ArrayObjectAdapter recentAppsAdapter;
    Disposable recentAppsFlowable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        appUsageDao = Room.databaseBuilder(getContext(), AppDatabase.class, "app-database").build().appUsageDao();

        loadRows();

        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private void loadRows() {
        ListRowPresenter listRowPresenter = new ListRowPresenter(ZOOM_FACTOR_LARGE);
        listRowPresenter.setShadowEnabled(false);
        listRowPresenter.setSelectEffectEnabled(false);

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(listRowPresenter);
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
        mainIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        PackageManager pm = getContext().getPackageManager();
        apps = pm.queryIntentActivities( mainIntent, 0).stream()
                .map(r -> AppDto.from(r, pm)).collect(Collectors.toList());
        return apps.stream()
                .collect(Collectors.groupingBy(
                        a -> String.valueOf(a.getTitle().charAt(0)),
                        TreeMap::new,
                        Collectors.toList()));
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
                Intent intent = context.getPackageManager().getLeanbackLaunchIntentForPackage(app.getPackageName());
                if (intent == null) {
                    return;
                    //throw new ActivityNotFoundException();
                }
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                context.startActivity(intent);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recentAppsFlowable != null && !recentAppsFlowable.isDisposed()) {
            recentAppsFlowable.dispose();
        }
    }
}