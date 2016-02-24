package com.example.xyzreader.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.WindowInsets;

/**
 * Created by Toty on 2/20/2016.
 */
public class WindowInsetsViewPager extends ViewPager {

    public WindowInsetsViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WindowInsetsViewPager(Context context) {
        super(context);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        int childCount = getChildCount();
        for (int index = 0; index < childCount; index++)
            getChildAt(index).dispatchApplyWindowInsets(insets);

        return insets;
    }
}
