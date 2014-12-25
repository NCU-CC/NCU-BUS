package tw.edu.ncu.cc.ncubusdrawerver;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class MainActivity extends FragmentActivity {
    public static final String PREFS_NAME = "MyPrefsFile";

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private String[] drawerListItems = new String[]{"132", "132經高鐵", "133", "172去程", "172返程", "9025往台北","9025往中壢","9025往台北(繞駛中大)","9025往中壢(繞駛中大)"};

    private ActionBarDrawerToggle drawerToggle;
    private CharSequence drawerTitle;

    private static final int NUM_PAGES = 9;
    private ViewPager pager;
    private PagerAdapter pagerAdapter;

    int currentPagePosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        drawerList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, drawerListItems));
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        drawerTitle = getString(R.string.drawer_open_title);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.holo_light_ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getActionBar().setTitle(drawerListItems[currentPagePosition]);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(drawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        //necessary for drawer to be opened by clicking action bar home button
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);


        // Instantiate a ViewPager and a PagerAdapter.
        pager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(pagerAdapter);
        pager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        getActionBar().setTitle(drawerListItems[position]);
                        currentPagePosition = position;
                    }
                });


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        currentPagePosition = settings.getInt("currentPagePosition",0);

        getActionBar().setTitle(drawerListItems[currentPagePosition]);
        pager.setCurrentItem(currentPagePosition);

    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = drawerLayout.isDrawerOpen(drawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            pager.setCurrentItem(position);
            drawerLayout.closeDrawers();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        int id = item.getItemId();
        if (id == R.id.action_time_table) {
            displayTimeTable();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause(){
        super.onPause();
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("currentPagePosition", currentPagePosition);
        editor.commit();
    }


    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DetailFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    public void onButtonClick (View view) {
        Intent intent;
        int viewID = view.getId();
        String sParam = "";

        switch (viewID){
            case R.id.hsr_app:
                sParam = "com.ecom.thsrc";
                break;
            case R.id.hsr_web:
                sParam = "http://www.thsrc.com.tw/index.html?force=1";
                break;
            case R.id.tra_app:
                sParam = "tw.gov.tra.TWeBooking";
                break;
            case R.id.tra_web:
                sParam = "http://twtraffic.tra.gov.tw/twrail/";
                break;
        }

        try{
            PackageManager manager = getPackageManager();
            intent = manager.getLaunchIntentForPackage(sParam);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        }
        catch (Exception e){
            if(viewID == R.id.tra_app || viewID == R.id.hsr_app){
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + sParam));
            }else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sParam));
            }

            startActivity(intent);
        }

    }

    public void displayTimeTable(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;

        switch(currentPagePosition){
            case 0://132
                builder.setTitle("132時刻表");
                builder.setMessage(Constant.timeTables[0]);
                dialog = builder.create();
                dialog.show();
                break;
            case 1://132經高鐵
                builder.setMessage(Constant.timeTables[1]);
                builder.setTitle("132時刻表(經高鐵)");
                dialog = builder.create();
                dialog.show();
                break;
            case 2://133
                builder.setMessage(Constant.timeTables[2]);
                builder.setTitle("133時刻表");
                dialog = builder.create();
                dialog.show();
                break;
            case 3://172去程
                builder.setMessage(Constant.timeTables[3]);
                builder.setTitle("172去程時刻表");
                dialog = builder.create();
                dialog.show();
                break;
            case 4://172返程
                builder.setMessage(Constant.timeTables[3]);
                builder.setTitle("172返程時刻表");
                dialog = builder.create();
                dialog.show();
                break;
            case 5://9025往台北
                builder.setTitle("9025往台北時刻表");
                builder.setMessage(Constant.timeTables[4]);
                dialog = builder.create();
                dialog.show();
                break;
            case 6://9025往中壢
                builder.setTitle("9025往中壢時刻表");
                builder.setMessage(Constant.timeTables[4]);
                dialog = builder.create();
                dialog.show();
                break;
            case 7://9025往台北(繞駛中大)
                builder.setTitle("9025往台北時刻表(繞駛中大)");
                builder.setMessage(Constant.timeTables[5]);
                dialog = builder.create();
                dialog.show();
                break;
            case 8://9025往中壢(繞駛中大)
                builder.setTitle("9025往中壢時刻表(繞駛中大)");
                builder.setMessage(Constant.timeTables[6]);
                dialog = builder.create();
                dialog.show();
                break;
        }
    }

}
