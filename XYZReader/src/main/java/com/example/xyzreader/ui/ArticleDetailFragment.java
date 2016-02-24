package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;

    private View mRootView;
    private LinearLayout mMetabar;

    private TextView mTitleView;
    private TextView mBylineView;
    private TextView mBodyView;

    private boolean mIsCard = false;

    /* Appbar views */
    private CoordinatorLayout mCoordinatorLayour;
    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mCollapsingToolbar;
    private Toolbar mToolbar;

    private ImageView mPhotoView;
    private FloatingActionButton mFab;

    /* Used in the override of the offset change listener  for collapsing toolbar */
    private int mCurrentOffset;

    private int mThemeDark;
    private int mThemePrimary;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mThemePrimary = ContextCompat.getColor(getActivity(), R.color.theme_primary);
        mThemeDark = ContextCompat.getColor(getActivity(), R.color.theme_primary_dark);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        ViewCompat.setTransitionName(mRootView.findViewById(R.id.meta_bar), "transition appbar");

        mCoordinatorLayour = (CoordinatorLayout) mRootView.findViewById(R.id.detail_coordinator_layout);
        mAppBar = (AppBarLayout) mRootView.findViewById(R.id.detail_app_bar_layout);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mCollapsingToolbar =
                (CollapsingToolbarLayout) mRootView.findViewById(R.id.detail_collapsing_toolbar);

        mMetabar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
        mToolbar = (Toolbar) mRootView.findViewById(R.id.detail_toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        mFab = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        bindViews();
        return mRootView;
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mTitleView = (TextView) mRootView.findViewById(R.id.article_title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTitleView.setTransitionName(getActivity().getString(R.string.shared_element_transition)
                    + mItemId);
        }

        mBylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView = (TextView) mRootView.findViewById(R.id.article_body);
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            mBylineView.setText(Html.fromHtml(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by <font color='#ffffff'>"
                            + mCursor.getString(ArticleLoader.Query.AUTHOR)
                            + "</font>"));
            mBodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null && !bitmap.isRecycled()) {
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                Palette.from(bitmap).generate(paletteListener);
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

            mCollapsingToolbar.setTitle(mCursor.getString(ArticleLoader.Query.TITLE));
            mCollapsingToolbar.setExpandedTitleColor(
                    ActivityCompat.getColor(getActivity(), android.R.color.transparent));
        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A");
            mBodyView.setText("N/A");
        }
    }

    Palette.PaletteAsyncListener paletteListener = new Palette.PaletteAsyncListener() {
        public void onGenerated(Palette palette) {
            applyPalette(palette);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scheduleStartPostponedTransition(mTitleView);
        }
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sharedElement.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                            getActivity().startPostponedEnterTransition();
                            return true;
                        }
                    });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    private void applyPalette(Palette palette) {

        mCollapsingToolbar.setStatusBarScrimColor(
                palette.getDarkMutedColor(mThemeDark));

        mCollapsingToolbar.setContentScrimColor(
                palette.getMutedColor(mThemePrimary));

        mMetabar.setBackgroundColor(palette.getDarkMutedColor(0xFF333333));

        if (isAdded()) {
            mFab.setRippleColor(
                    palette.getLightVibrantColor(
                            ContextCompat.getColor(getActivity(), R.color.ltgray)));

            mFab.setBackgroundTintList(
                    ColorStateList.valueOf(
                            palette.getVibrantColor(
                                    ContextCompat.getColor(getActivity(), R.color.theme_accent))));

            mBodyView.setLinkTextColor(palette.getVibrantColor(
                    ContextCompat.getColor(getActivity(), R.color.theme_accent)));
        }
    }
}
