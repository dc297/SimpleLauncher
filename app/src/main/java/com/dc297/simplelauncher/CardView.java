package com.dc297.simplelauncher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.BaseCardView;

public class CardView extends BaseCardView {

    private final TextView mTitleView;
    private final ImageView mImageView;

    public CardView(Context context) {
        super(context, null, androidx.leanback.R.style.Widget_Leanback_BaseCardViewStyle);
        setFocusable(true);
        setFocusableInTouchMode(true);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.card_view, this);
        mTitleView = findViewById(R.id.textView);
        mImageView = findViewById(R.id.imageView);
    }

    public void setText(CharSequence text) {
        if (mTitleView == null) {
            return;
        }
        mTitleView.setText(text);
    }

    public void setImage(Drawable drawable) {
        if (mImageView == null) {
            return;
        }

        mImageView.setImageDrawable(drawable);
    }

    public void setCardSelected(boolean selected){
        if (mTitleView == null) {
            return;
        }
        mTitleView.setVisibility(selected ? View.VISIBLE : View.GONE);
    }
}
