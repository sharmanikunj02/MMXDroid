package com.rey.material.demo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mmx.service.ISensorService;
import com.rey.material.app.ToolbarManager;
import com.rey.material.util.ThemeUtil;
import com.rey.material.widget.SnackBar;
import com.rey.material.widget.TabPageIndicator;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class MainActivity extends ActionBarActivity implements ToolbarManager.OnToolbarGroupChangedListener {

    private DrawerLayout dl_navigator;
    private FrameLayout fl_drawer;
    private ListView lv_drawer;
    private CustomViewPager vp;
    private TabPageIndicator tpi;

    private DrawerAdapter mDrawerAdapter;
    private PagerAdapter mPagerAdapter;

    private Toolbar mToolbar;
    private ToolbarManager mToolbarManager;
    private SnackBar mSnackBar;

    private Tab[] mTabItems = new Tab[]{Tab.HOME, Tab.STATISTICS};

    private Tab[] mItems = new Tab[]{Tab.HOME, Tab.WALKTHROUGH, Tab.ABOUTUS, Tab.STATISTICS};


    private Button mBindServiceBtn;
    private Button mUnbindServiceBtn;

    private ISensorService mSensorService;
    private Bundle mThreadArgs;
    private Message mThreadMsg;
    private TextView mTextView;

    private final ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v("SERVICE CONNECTED", "SERVICE CONNECTED");
            mSensorService = ISensorService.Stub.asInterface(service);
            Bundle data = new Bundle();
            data.putString("KEY_APP_ID", "");
            data.putString("KEY_AUTH_KEY", "");
            data.putString("KEY_DEVICE_ID", "");
            data.putString("KEY_APP_VERSION", "");

            try {
                mSensorService.sendRequiredDataToService(data);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Service Connected !!!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v("SERVICE DISCONNECTED", "SERVICE DISCONNECTED");
            mSensorService = null;
            Toast.makeText(getApplicationContext(), "Service Disconnected !!!", Toast.LENGTH_SHORT).show();
        }
    };

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle args = (Bundle) msg.obj;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        dl_navigator = (DrawerLayout) findViewById(R.id.main_dl);
        fl_drawer = (FrameLayout) findViewById(R.id.main_fl_drawer);
        lv_drawer = (ListView) findViewById(R.id.main_lv_drawer);
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        vp = (CustomViewPager) findViewById(R.id.main_vp);
        tpi = (TabPageIndicator) findViewById(R.id.main_tpi);
        mSnackBar = (SnackBar) findViewById(R.id.main_sn);

        mToolbarManager = new ToolbarManager(this, mToolbar, 0, R.style.ToolbarRippleStyle, R.anim.abc_fade_in, R.anim.abc_fade_out);
        mToolbarManager.setNavigationManager(new ToolbarManager.BaseNavigationManager(R.style.NavigationDrawerDrawable, this, mToolbar, dl_navigator) {
            @Override
            public void onNavigationClick() {
                if (mToolbarManager.getCurrentGroup() != 0)
                    mToolbarManager.setCurrentGroup(0);
                else
                    dl_navigator.openDrawer(GravityCompat.START);
            }

            @Override
            public boolean isBackState() {
                return super.isBackState() || mToolbarManager.getCurrentGroup() != 0;
            }

            @Override
            protected boolean shouldSyncDrawerSlidingProgress() {
                return super.shouldSyncDrawerSlidingProgress() && mToolbarManager.getCurrentGroup() == 0;
            }

        });
        mToolbarManager.registerOnToolbarGroupChangedListener(this);

        mDrawerAdapter = new DrawerAdapter();
        lv_drawer.setAdapter(mDrawerAdapter);

        mPagerAdapter = new PagerAdapter(getSupportFragmentManager(), mTabItems);
        vp.setAdapter(mPagerAdapter);
        tpi.setViewPager(vp);
        tpi.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                mDrawerAdapter.setSelected(mTabItems[position]);
                mSnackBar.dismiss();
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

        });

        mDrawerAdapter.setSelected(Tab.HOME);
        vp.setCurrentItem(0);
        initilizingServiceComponent();
    }

    private void initilizingServiceComponent() {
 /*       View v = getLayoutInflater().inflate(R.layout.b, null);
        mBindServiceBtn = (Button) v.findViewById(R.id.btnBindService);
        mBindServiceBtn.setOnClickListener(this);

        mUnbindServiceBtn = (Button) v.findViewById(R.id.btnUnbindService);
        mUnbindServiceBtn.setOnClickListener(this);
        mUnbindServiceBtn.setEnabled(false);
*/
        HandlerThread hthr = new HandlerThread("StartedSensorServiceThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        hthr.start();
        mServiceLooper = hthr.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mThreadArgs = new Bundle();

        IntentFilter filter = new IntentFilter("com.mmx.service");
        SensorReceiver receiver = new SensorReceiver();
        registerReceiver(receiver, filter);

    }

    private void appendText(String text) {
        String currentText = mTextView.getText() + System.getProperty("line.separator") + text;
        mTextView.setText(currentText);
        final int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            mTextView.scrollTo(0, scrollAmount);
        else
            mTextView.scrollTo(0, 0);
    }

    public class SensorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }


    public void onBind(View v) {

        if (!bindService(new Intent(ISensorService.class.getName()), mServiceConn, Context.BIND_AUTO_CREATE)) {
            Toast.makeText(getApplicationContext(), "Kindly Install the service", Toast.LENGTH_LONG).show();
            return;
        }
//       mBindServiceBtn.setEnabled(false);
        //      mUnbindServiceBtn.setEnabled(true);
    }

    public void onUnBind(View v) {
        unbindService(mServiceConn);
        //      mBindServiceBtn.setEnabled(true);
        //     mUnbindServiceBtn.setEnabled(false);

    }

    private void startWalkthroughActivity() {
        Intent intent = new Intent(this, WalkthroughActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mToolbarManager.createMenu(R.menu.menu_main);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mToolbarManager.onPrepareMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tb_contextual:
                mToolbarManager.setCurrentGroup(R.id.tb_group_contextual);
                break;
            case R.id.tb_done:
            case R.id.tb_done_all:
                mToolbarManager.setCurrentGroup(0);
                break;
        }
        return true;
    }

    @Override
    public void onToolbarGroupChanged(int oldGroupId, int groupId) {
        mToolbarManager.notifyNavigationStateChanged();
    }

    public SnackBar getSnackBar() {
        return mSnackBar;
    }

    public enum Tab {
        HOME("HOME"),
        WALKTHROUGH("WALKTHROUGH"),
        ABOUTUS("ABOUT US"),
        STATISTICS("STATISTICS");
        private final String name;

        private Tab(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return (otherName != null) && name.equals(otherName);
        }

        public String toString() {
            return name;
        }

    }

    class DrawerAdapter extends BaseAdapter implements View.OnClickListener {

        private Tab mSelectedTab;

        public void setSelected(Tab tab) {
            if (tab != mSelectedTab) {
                mSelectedTab = tab;
                notifyDataSetInvalidated();
            }
        }

        public Tab getSelectedTab() {
            return mSelectedTab;
        }

        @Override
        public int getCount() {
            return mTabItems.length;
        }

        @Override
        public Object getItem(int position) {
            return mTabItems[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_drawer, null);
                v.setOnClickListener(this);
            }

            v.setTag(position);
            Tab tab = (Tab) getItem(position);
            ((TextView) v).setText(tab.toString());

            if (tab == mSelectedTab) {
                v.setBackgroundColor(ThemeUtil.colorPrimary(MainActivity.this, 0));
                ((TextView) v).setTextColor(0xFFFFFFFF);
            } else {
                v.setBackgroundResource(0);
                ((TextView) v).setTextColor(0xFF000000);
            }

            return v;
        }

        @Override
        public void onClick(View v) {
            int position = (Integer) v.getTag();
            vp.setCurrentItem(position);
            dl_navigator.closeDrawer(fl_drawer);
        }
    }

    private static class PagerAdapter extends FragmentStatePagerAdapter {

        Fragment[] mFragments;
        Tab[] mTabs;

        private static final Field sActiveField;

        static {
            Field f = null;
            try {
                Class<?> c = Class.forName("android.support.v4.app.FragmentManagerImpl");
                f = c.getDeclaredField("mActive");
                f.setAccessible(true);
            } catch (Exception e) {
            }

            sActiveField = f;
        }

        public PagerAdapter(FragmentManager fm, Tab[] tabs) {
            super(fm);
            mTabs = tabs;
            mFragments = new Fragment[mTabs.length];


            //dirty way to get reference of cached fragment
            try {
                ArrayList<Fragment> mActive = (ArrayList<Fragment>) sActiveField.get(fm);
                if (mActive != null) {
                    for (Fragment fragment : mActive) {
                        if (fragment instanceof ProgressFragment)
                            setFragment(Tab.HOME, fragment);
/*    					else if(fragment instanceof ButtonFragment)
                            setFragment(Tab.WALKTHROUGH, fragment);
                        else if(fragment instanceof FabFragment)
                            setFragment(Tab.ABOUTUS, fragment);
                            */
                        else if (fragment instanceof SwitchesFragment)
                            setFragment(Tab.STATISTICS, fragment);
/*                        else if(fragment instanceof SliderFragment)
                            setFragment(Tab.SLIDERS, fragment);
                        else if(fragment instanceof SpinnersFragment)
                            setFragment(Tab.SPINNERS, fragment);
    					else if(fragment instanceof TextfieldFragment)
    						setFragment(Tab.TEXTFIELDS, fragment);
    					else if(fragment instanceof SnackbarFragment)
    						setFragment(Tab.SNACKBARS, fragment);
                        else if(fragment instanceof DialogsFragment)
                            setFragment(Tab.DIALOGS, fragment);
                            */
                    }
                }
            } catch (Exception e) {
            }
        }

        private void setFragment(Tab tab, Fragment f) {
            for (int i = 0; i < mTabs.length; i++)
                if (mTabs[i] == tab) {
                    mFragments[i] = f;
                    break;
                }
        }

        @Override
        public Fragment getItem(int position) {
            if (mFragments[position] == null) {
                switch (mTabs[position]) {
                    case HOME:
                        mFragments[position] = ProgressFragment.newInstance();
                        break;
                    case WALKTHROUGH:
                        mFragments[position] = ProgressFragment.newInstance();
                        break;
                    case ABOUTUS:
                        mFragments[position] = FabFragment.newInstance();
                        break;
                    case STATISTICS:
                        mFragments[position] = SwitchesFragment.newInstance();
                        break;
/*                    case SLIDERS:
                        mFragments[position] = SliderFragment.newInstance();
                        break;
                    case SPINNERS:
                        mFragments[position] = SpinnersFragment.newInstance();
                        break;
					case TEXTFIELDS:
						mFragments[position] = TextfieldFragment.newInstance();
						break;
					case SNACKBARS:
						mFragments[position] = SnackbarFragment.newInstance();
						break;
                    case DIALOGS:
                        mFragments[position] = DialogsFragment.newInstance();
                        break;
                    */
                }
            }

            return mFragments[position];
        }


        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs[position].toString().toUpperCase();
        }

        @Override
        public int getCount() {
            return mFragments.length;
        }
    }
}
