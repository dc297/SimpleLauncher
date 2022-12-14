package com.dc297.simplelauncher.domain;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Optional;

public class AppDto implements Serializable {
    static final long serialVersionUID = 727566175075960653L;
    private CharSequence title;
    private String packageName;
    private Drawable logo;

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = title;
    }

    public Drawable getLogo() {
        return logo;
    }

    public void setLogo(Drawable logo) {
        this.logo = logo;
    }

    @NonNull
    @Override
    public String toString() {
        return "App{" +
                "title='" + title + '\'' +
                ", logo='" + logo + '\'' +
                ", packageName='" + packageName + '\'' +
                '}';
    }

    public static AppDto from(ResolveInfo resolveInfo, PackageManager pm) {
        AppDto app = new AppDto();
        app.setTitle(resolveInfo.loadLabel(pm));
        try {
            app.setLogo(Optional.ofNullable(
                    Optional.ofNullable(pm.getApplicationBanner(resolveInfo.activityInfo.packageName))
                            .orElse(pm.getApplicationIcon(resolveInfo.activityInfo.packageName)))
                    .orElse(pm.getApplicationLogo(resolveInfo.activityInfo.packageName)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        app.setPackageName(resolveInfo.activityInfo.packageName);
        return app;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}