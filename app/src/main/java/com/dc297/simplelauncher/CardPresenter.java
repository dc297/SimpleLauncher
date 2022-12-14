package com.dc297.simplelauncher;

import androidx.leanback.widget.Presenter;

import android.graphics.Color;
import android.util.Log;
import android.view.ViewGroup;

import com.dc297.simplelauncher.domain.AppDto;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static void updateSelected(CardView view, boolean selected) {
        // Both background colors should be set because the view"s background is temporarily visible
        // during animations.
        view.setCardSelected(selected);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        CardView cardView =
                new CardView(parent.getContext()) {
                    @Override
                    public void setSelected(boolean selected) {
                        updateSelected(this, selected);
                        super.setSelected(selected);
                    }
                };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.setBackgroundColor(Color.TRANSPARENT);
        cardView.setPadding(0, 0, 0, 0);
        updateSelected(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        AppDto app = (AppDto) item;
        CardView cardView = (CardView) viewHolder.view;

        Log.d(TAG, "onBindViewHolder");
        cardView.setText(app.getTitle());
        cardView.setImage(app.getLogo());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
        CardView cardView = (CardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setImage(null);
    }
}