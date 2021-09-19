package com.voxeldev.customswiperefresh;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.animation.Animation;

import androidx.core.view.ViewCompat;

class CircleImageView extends androidx.appcompat.widget.AppCompatImageView {
    private static final int SHADOW_ELEVATION = 4;

    private Animation.AnimationListener mListener;

    @SuppressLint("WrongConstant")
    public CircleImageView(Context context, int color) {
        super(context);
        final float density = getContext().getResources().getDisplayMetrics().density;

        ShapeDrawable circle;
        circle = new ShapeDrawable(new OvalShape());
        ViewCompat.setElevation(this, SHADOW_ELEVATION * density);
        circle.getPaint().setColor(color);
        setBackgroundDrawable(circle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setAnimationListener(Animation.AnimationListener listener) {
        mListener = listener;
    }

    @Override
    public void onAnimationStart() {
        super.onAnimationStart();
        if (mListener != null) {
            mListener.onAnimationStart(getAnimation());
        }
    }

    @Override
    public void onAnimationEnd() {
        super.onAnimationEnd();
        if (mListener != null) {
            mListener.onAnimationEnd(getAnimation());
        }
    }

    /**
     * Update the background color of the circle image view.
     */
    @SuppressLint("ResourceType")
    public void setBackgroundColor(int colorRes) {
        if (getBackground() instanceof ShapeDrawable) {
            final Resources res = getResources();
            ((ShapeDrawable) getBackground()).getPaint().setColor(res.getColor(colorRes, getContext().getTheme()));
        }
    }
}