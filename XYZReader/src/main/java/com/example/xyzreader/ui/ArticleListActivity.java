package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xyzreader.R;
import com.example.xyzreader.Utility;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {

    private boolean mIsAppStart;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private View.OnClickListener mSnackbarOnClickListener;
    private Snackbar mSnackbar;
    public static String SELECTED_ITEM_POSITION = "selected item position";

    private AppBarLayout mAppbarLayout;
    private int mMaxAppbarScrollRange;

    private ImageView mLogo;
    private boolean mLogoShown;
    private static int PERCENT_TO_ANIMATE_LOGO = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mIsAppStart = true;

        initLogo();
        initAppbarLayout();
        initSwipeRefresh();
        initRecyclerview();
        setSnackbarListener();

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void initLogo() {
        mLogo = (ImageView) findViewById(R.id.main_logo);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.logo_slide_down);
            mLogo.setAnimation(animation);
        }
    }

    private void setSnackbarListener() {
        mSnackbarOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        };
    }

    private void initSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    private void initRecyclerview() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    }

    private void initAppbarLayout() {
        mAppbarLayout = (AppBarLayout) findViewById(R.id.main_appbar);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMaxAppbarScrollRange = mAppbarLayout.getTotalScrollRange();
            mAppbarLayout.addOnOffsetChangedListener(this);
        }
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!mIsAppStart) {
                if (mMaxAppbarScrollRange == 0)
                    mMaxAppbarScrollRange = appBarLayout.getTotalScrollRange();

                int percentage = (Math.abs(verticalOffset) * 100) / mMaxAppbarScrollRange;

                if (percentage >= PERCENT_TO_ANIMATE_LOGO && mLogoShown) {
                    mLogoShown = false;
                    mLogo.animate().scaleX(0).scaleY(0).setDuration(200).start();
                }

                if (percentage < PERCENT_TO_ANIMATE_LOGO && !mLogoShown) {
                    mLogoShown = true;
                    mLogo.animate().scaleX(1).scaleY(1).setDuration(200).start();
                }
            }
        }
    }


    @Override public void onEnterAnimationComplete() {
        mIsAppStart = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mIsAppStart) {
                mRecyclerView.scheduleLayoutAnimation();
            }
        }
        super.onEnterAnimationComplete();
    }

    /* This method shows snackbar if no connectivity found, or launch the update
     * the update service otherwise
     * */
    private void refresh() {
        if (!Utility.isNetworkAvailable(getApplicationContext())) {
            if (mSwipeRefreshLayout.isRefreshing()) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            showSnackbar();
        } else {
            hideSnackbar();
            startService(new Intent(this, UpdaterService.class));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
        //setResult(Activity.RESULT_OK);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    /* Shows or hides the loading circle when user swipes to refresh
     * depending on the update service state
     * */
    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    /* Creates a loader that shall get a cursor of all the found articles */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        setRecyclerAdapter(cursor);
        setRecyclerGrid();
    }

    private void setRecyclerAdapter(Cursor cursor) {
        Adapter adapter = new Adapter(cursor);

        // This option optimizes the recyclerview, it means the items ids isn't going to change
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
    }

    private void setRecyclerGrid() {
        // The number of columns for the grid (depends on the device)
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mIsRefreshing) {
                        Toast.makeText(ArticleListActivity.this,
                                "Please wait till loading completes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class)
                            .putExtra(SELECTED_ITEM_POSITION, vh.getAdapterPosition());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // Adding a transition
                        TextView title = (TextView) view.findViewById(R.id.article_title);
                        String transitionName = getString(R.string.shared_element_transition)
                                + String.valueOf(getItemId(vh.getAdapterPosition()));
                        title.setTransitionName(transitionName);

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ArticleListActivity.this,
                                title,
                                transitionName).toBundle();
                        ActivityCompat.startActivity(ArticleListActivity.this, intent, bundle);
                    } else {
                        startActivity(intent);
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }

    /* Snackbar showing no connectivity */
    private void showSnackbar() {
        mSnackbar = Snackbar
                .make(findViewById(R.id.activity_list_container), "No connection found", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry", mSnackbarOnClickListener);
        mSnackbar.setActionTextColor(getResources().getColor(R.color.theme_accent));
        View snackbarView = mSnackbar.getView();
        snackbarView.setBackgroundColor(Color.DKGRAY);
        TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        mSnackbar.show();
    }

    private void hideSnackbar() {
        if (mSnackbar != null) {
            mSnackbar.dismiss();
        }
    }
}
