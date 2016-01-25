package net.ddns.dwaraka.yaftp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.Stack;

public class ClientActivity extends AppCompatActivity {

    public static  MyPagerAdapter adapterViewPager;
    public static  ViewPager vpPager;
    public static Stack<String> stackPage0 = new Stack<>();
    public static Stack<String> stackPage1 = new Stack<>();
    public static Toolbar myToolbar;

    public static String SERVER_NAME,HOST_NAME,USERNAME,PASSWORD,LOCAL_DIRECTORY,REMOTE_DIRECTORY;
    public static int PORT;

    public void initializeCredentials(Bundle bundle){
        SERVER_NAME = bundle.getString("SERVER_NAME");
        HOST_NAME = bundle.getString("HOST_NAME");
        PORT=bundle.getInt("PORT");
        USERNAME=bundle.getString("USERNAME");
        PASSWORD=bundle.getString("PASSWORD");
        LOCAL_DIRECTORY=bundle.getString("LOCAL_DIRECTORY");
        REMOTE_DIRECTORY=bundle.getString("REMOTE_DIRECTORY");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeCredentials(getIntent().getExtras());
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(SERVER_NAME);
        setSupportActionBar(myToolbar);
        vpPager = (ViewPager) findViewById(R.id.pager);
        Bundle bundle = new Bundle();
        bundle.putString("path",REMOTE_DIRECTORY);
        bundle.putString("pageType","remote");
        Bundle bundle2 = new Bundle();
        bundle2.putString("path", LOCAL_DIRECTORY);
        bundle2.putString("pageType","local");
        MyPagerAdapter.setRootFirstPage(bundle);
        MyPagerAdapter.setRootSecondPage(bundle2);
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(adapterViewPager);
        vpPager.setPageTransformer(true,new ZoomOutPageTransformer());
    }

    @Override
    public void onBackPressed() {

        if (vpPager.getCurrentItem() == 0){
            if(stackPage0.size() == 1) {
               exitHandler();
            }
            else {
                stackPage0.pop();
                Bundle bundle = new Bundle();
                bundle.putString("path", stackPage0.peek());
                bundle.putString("pageType","remote");
                MyPagerAdapter.setRootFirstPage(bundle);
                MyPagerAdapter.changedFragment=MyPagerAdapter.remoteFragment;
                stackPage0.pop();
                adapterViewPager.notifyDataSetChanged();
            }
        }
        else {
            if(stackPage1.size() == 1) {
                exitHandler();
            }
            else {
                stackPage1.pop();
                Bundle bundle = new Bundle();
                bundle.putString("path", stackPage1.peek());
                bundle.putString("pageType","local");
                MyPagerAdapter.setRootSecondPage(bundle);
                MyPagerAdapter.changedFragment=MyPagerAdapter.localFragment;
                stackPage1.pop();
                adapterViewPager.notifyDataSetChanged();
            }
        }

    }

    public static class MyPagerAdapter extends FragmentStatePagerAdapter {
        private static int NUM_ITEMS = 2;
        public static Bundle bundle1,bundle2;
        public static ListFragment remoteFragment,localFragment;
        public static ListFragment changedFragment;

        public MyPagerAdapter(android.support.v4.app.FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        public static void setRootFirstPage(Bundle b){
            bundle1 = b;
            stackPage0.push(bundle1.getString("path"));
        }

        public static void setRootSecondPage(Bundle b){
            bundle2 = b;
            stackPage1.push(bundle2.getString("path"));
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    remoteFragment = ListFragment.newInstance(bundle1, adapterViewPager, vpPager);
                    remoteFragment.setChangedFragmentID(remoteFragment.toString());
                    return remoteFragment;
                case 1:
                    localFragment = ListFragment.newInstance(bundle2, adapterViewPager, vpPager);
                    localFragment.setChangedFragmentID(localFragment.toString());
                    return localFragment;
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            if(object.toString().equals(remoteFragment.toString())) {
                if(remoteFragment==changedFragment)
                    return POSITION_NONE;
            }
            if(object.toString().equals(localFragment.toString())) {
                if(localFragment==changedFragment)
                    return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if( position == 0)
                return "Remote";
            else
                return "Local";
        }
    }


    public static  class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    public void exitHandler(){

        new AlertDialog.Builder(this)
                .setIcon(R.drawable.alert)
                .setTitle("Disconnect?")
                .setMessage("Are you sure you want to disconnect from the server?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        while (stackPage0.size() > 0)
                            stackPage0.pop();
                        while (stackPage1.size() > 0)
                            stackPage1.pop();
                        finish();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }
}
