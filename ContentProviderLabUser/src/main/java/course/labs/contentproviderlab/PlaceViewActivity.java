package course.labs.contentproviderlab;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import course.labs.contentproviderlab.provider.PlaceBadgesContract;

public class PlaceViewActivity extends ListActivity implements
        LocationListener, LoaderCallbacks<Cursor> {
    private static final long FIVE_MINS = 5 * 60 * 1000;

    private static String TAG = "Lab-ContentProvider";

    // The last valid location reading
    private Location mLastLocationReading;

    // The ListView's adapter
    // private PlaceViewAdapter mAdapter;
    private PlaceViewAdapter mCursorAdapter;

    // Reference to the LocationManager
    private LocationManager mLocationManager;

    // A fake location provider used for testing
    private MockLocationProvider mMockLocationProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getListView().setFooterDividersEnabled(true);

        TextView footerView = (TextView) getLayoutInflater().inflate(R.layout.footer_view, null);

        getListView().addFooterView(footerView);

        assert footerView != null;
        footerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                log("Entered footerView.OnClickListener.onClick()");

                mLastLocationReading = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if(mLastLocationReading == null) {
                    log("Location data is not available");
                } else {
                    if(mCursorAdapter.intersects(mLastLocationReading)) {
                        //location viewed
                        log("You already have this location badge");
                        Toast.makeText(getApplicationContext(), "You already have this location badge", Toast.LENGTH_SHORT).show();
                    } else {
                        //location new
                        log("Starting Place Download");
                        new PlaceDownloaderTask(PlaceViewActivity.this).execute(mLastLocationReading);
                    }
                }
            }
        });

        mCursorAdapter = new PlaceViewAdapter(this, null, 0);
        setListAdapter(mCursorAdapter);

        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mMockLocationProvider = new MockLocationProvider(
                LocationManager.NETWORK_PROVIDER, this);

        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if(mLastLocationReading != null && age(mLastLocationReading) > FIVE_MINS) {
            mLastLocationReading = null;
        }

        long mMinTime = 5000;
        float mMinDistance = 1000.0f;

        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);
    }

    @Override
    protected void onPause() {

        mMockLocationProvider.shutdown();

        mLocationManager.removeUpdates(this);

        super.onPause();
    }

    public void addNewPlace(PlaceRecord place) {

        log("Entered addNewPlace()");

        mCursorAdapter.add(place);

    }

    @Override
    public void onLocationChanged(Location currentLocation) {

        if(mLastLocationReading == null) {
            mLastLocationReading = currentLocation;
        }

        if(currentLocation.getTime() > mLastLocationReading.getTime()) {
            mLastLocationReading = currentLocation;
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // not implemented
    }

    @Override
    public void onProviderEnabled(String provider) {
        // not implemented
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // not implemented
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        log("Entered onCreateLoader()");

        final String columnsToExtract[] = new String[] {
                PlaceBadgesContract._ID, PlaceBadgesContract.FLAG_BITMAP_PATH,
                PlaceBadgesContract.COUNTRY_NAME, PlaceBadgesContract.PLACE_NAME,
                PlaceBadgesContract.LAT, PlaceBadgesContract.LON};

        String select = "((" + PlaceBadgesContract.COUNTRY_NAME + " NOTNULL) AND ("
                + PlaceBadgesContract.COUNTRY_NAME + " != '' )";


        return new CursorLoader(this, PlaceBadgesContract.CONTENT_URI, columnsToExtract,
                select, null, PlaceBadgesContract._ID + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> newLoader, Cursor newCursor) {
        if(mCursorAdapter != null && newCursor != null) {
            mCursorAdapter.swapCursor(newCursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> newLoader) {
        if( mCursorAdapter != null ) {
            mCursorAdapter.swapCursor(null);
        }
    }

    private long age(Location location) {
        return System.currentTimeMillis() - location.getTime();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.print_badges:
                ArrayList<PlaceRecord> currData = mCursorAdapter.getList();
                for (PlaceRecord aCurrData : currData) {
                    log(aCurrData.toString());
                }
                return true;
            case R.id.delete_badges:
                mCursorAdapter.removeAllViews();
                return true;
            case R.id.place_one:
                mMockLocationProvider.pushLocation(37.422, -122.084);
                return true;
            case R.id.place_invalid:
                mMockLocationProvider.pushLocation(0, 0);
                return true;
            case R.id.place_two:
                mMockLocationProvider.pushLocation(38.996667, -76.9275);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private static void log(String msg) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, msg);
    }
}