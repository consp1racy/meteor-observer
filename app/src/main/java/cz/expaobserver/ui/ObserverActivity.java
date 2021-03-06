package cz.expaobserver.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import cz.expaobserver.R;
import cz.expaobserver.background.ObserverLogic;
import cz.expaobserver.model.Vector3;
import cz.expaobserver.util.ActivityUtils;
import cz.expaobserver.util.DateUtils;
import cz.expaobserver.util.Util;

public class ObserverActivity extends AppCompatActivity implements ObserverLogic.Callbacks,
    ConfirmIntentDialogFragment.ConfirmIntentClient {

    private static final long DELAY_DIM = 2000;

    @Bind(R.id.time)
    TextView mTimeText;
    @Bind(R.id.loc)
    TextView mLocationText;
    @Bind(R.id.instruct)
    TextView mInstructionsText;
    @Bind(R.id.ori)
    TextView mOrientationText;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

//    private SensorManager mSensorManager;
//    private Sensor mLightSensor;
    private boolean mIntentNeedsConfirmation = true;

    private final Handler mHandler = new Handler();
    private final Runnable mDelayedDim = new Runnable() {
        @Override
        public void run() {
            dimSystemUi();
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_observer);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

//    ActionBar ab = getSupportActionBar();
//    ab.setDisplayHomeAsUpEnabled(false);
//    ab.setHomeButtonEnabled(false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(ObserverFragment.newInstance(), ObserverFragment.TAG).commit();
        }

//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
//        if (mLightSensor == null) {
//            Timber.d("No light sensor available.");
//        }
    }

    protected void onPause() {
//        if (mLightSensor != null) {
//            mSensorManager.unregisterListener(this);
//        }

        super.onPause();
    }

    protected void onResume() {
        super.onResume();

        dimSystemUi();

//        if (mLightSensor != null) {
//            mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_FASTEST);
//        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void dimSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

            mHandler.removeCallbacks(mDelayedDim);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            toggleSystemUi();
        }

        return super.onTouchEvent(event);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.observer_options, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        super.onPrepareOptionsMenu(menu);
//    MenuItemCompat.setShowAsAction(menu.findItem(R.id.dim_screen), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        Util.Material.tintMenu(menu, getSupportActionBar().getThemedContext());
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_records:
                startActivity(new Intent(this, RecordListActivity.class));
                return true;

            case R.id.gps_settings:
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return true;

            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.dim_screen:
                ActivityUtils.setBrightness(this, 25);
                dimSystemUi();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void toggleSystemUi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return;
        }

        if (getWindow().getDecorView().getSystemUiVisibility() == View.SYSTEM_UI_FLAG_VISIBLE) {
            dimSystemUi();
        } else {
            showSystemUi();
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void showSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            mHandler.postDelayed(mDelayedDim, DELAY_DIM);
        }
    }

    @Override
    public void onStateChanged(@ObserverLogic.TrailMeasureState int state) {
        switch (state) {
            case ObserverLogic.TrailMeasureState.IDLE:
                mInstructionsText.setText(getString(R.string.mo_instruction_init));
                break;

            case ObserverLogic.TrailMeasureState.TRAIL_START:
                mInstructionsText.setText(getString(R.string.mo_instruction_point_to_start));
                break;

            case ObserverLogic.TrailMeasureState.TRAIL_END:
                mInstructionsText.setText(getString(R.string.mo_instruction_point_to_end));
                break;
        }
    }

    @Override
    public void onTimeChanged(long time) {
        mTimeText.setText(DateUtils.DATETIME_FORMATTER.print(time));
    }

    @Override
    public void onOrientationChanged(Vector3 v) {
        String vecString = String.format(Locale.getDefault(), "[%.2f %.2f %.2f]", v.x(), v.y(), v.z());
        mOrientationText.setText(vecString);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLocationText.setText("Lat "
            + Location.convert(location.getLatitude(), Location.FORMAT_SECONDS) + "\nLong "
            + Location.convert(location.getLongitude(), Location.FORMAT_SECONDS));
    }

    @Override
    public void startActivity(final Intent intent) {
        if (mIntentNeedsConfirmation) {
            ConfirmIntentDialogFragment.validateIntent(this, intent);
        } else {
            superStartActivity(intent);
        }
    }

    @Override
    public void superStartActivity(final Intent intent) {
        super.startActivity(intent);
    }

//    @Override
//    public void onSensorChanged(final SensorEvent event) {
//        Timber.d("timestamp=" + event.timestamp + ", values=" + printArray(event.values));
//    }
//
//    @Override
//    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
//        Timber.d("accuracy=" + accuracy);
//    }
//
//    private static String printArray(float[] array) {
//        String out = "[";
//        if (array.length > 0) {
//            out += array[0];
//        }
//        for (int i = 1; i < array.length; i++) {
//            out += ", " + array[i];
//        }
//        out += "]";
//        return out;
//    }
}
