package catglo.com.deliverydroid.neworder;

import android.annotation.TargetApi;
import android.app.*;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.View;

import catglo.com.deliveryDatabase.Order;
import catglo.com.deliverydroid.DeliveryDroidBaseActionBarActivity;
import catglo.com.deliverydroid.R;
import android.support.v7.app.ActionBar;
import android.support.v4.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.Locale;

public class NewOrderActivity extends DeliveryDroidBaseActionBarActivity
                                 implements ActionBar.TabListener,
                                            ButtonPadFragment.ButtonPadNextListener {



    SectionsPagerAdapter sectionsPagerAdapter;
    ViewPager viewPager;
    public Order order = new Order();
    private View tabletPane;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onNextButtonPressed() {
        int i = getSupportActionBar().getSelectedNavigationIndex();
        try {
            getSupportActionBar().selectTab(getSupportActionBar().getTabAt(i+1));
        } catch (IndexOutOfBoundsException e){
            //TODO: maybe focus whatever seems like the next logical thing in the last screen frag on tablet
        }
    }

    public enum Pages {
        number,price,phone,address,time,order;
    };
    ArrayList<Pages> viewPagerPages = new ArrayList<Pages>(6);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_new_order_acticity);



        //If one of these exists its a tablet
        tabletPane = findViewById(R.id.orderDetailsContainer);

        boolean useOrderNumberScreen = sharedPreferences.getBoolean("useOrderNumberScreen", true);
        boolean useCostScreen        = sharedPreferences.getBoolean("usePriceScreen", true);
        boolean usePhoneScreen       = sharedPreferences.getBoolean("usePhoneScreen", true);
        boolean useAddressScreen     = sharedPreferences.getBoolean("useAddressScreen", true);
        boolean useTimeScreen        = sharedPreferences.getBoolean("useTimeScreen", false);

        if (useOrderNumberScreen) viewPagerPages.add(Pages.number);
        if (useCostScreen)        viewPagerPages.add(Pages.price);
        if (usePhoneScreen)       viewPagerPages.add(Pages.phone);
        if (useAddressScreen)     viewPagerPages.add(Pages.address);
        if (useTimeScreen)        viewPagerPages.add(Pages.time);
        if (tabletPane==null)     viewPagerPages.add(Pages.order);
        else {
            NewOrderLastScreenFragment lastScreenFragment = new NewOrderLastScreenFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.orderDetailsContainer,lastScreenFragment,"order_details_fragment")
                    .commit();
        }


        // Set up the action bar.
        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.setDisplayHomeAsUpEnabled(true);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(),viewPagerPages);


        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(sectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }



    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        int position = tab.getPosition();
        viewPager.setCurrentItem(position);
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public int getFragmentIndex(Pages fragmentPage){
        int index;
        for (index = 0; index < viewPagerPages.size(); index++){
            if (viewPagerPages.get(index) == fragmentPage){
                return index;
            }
        }
        return -1;
    }

    public DataAwareFragment getFragment(Pages fragmentPage){
        if (   fragmentPage==Pages.order
            && tabletPane!=null){
            return (DataAwareFragment)getSupportFragmentManager().findFragmentByTag("order_details_fragment");
        }
        int index = getFragmentIndex(fragmentPage);
        if (index < 0){
            return null;
        }
        return (DataAwareFragment)getSupportFragmentManager().findFragmentByTag("android:switcher:" + viewPager.getId() + ":" + index);
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/viewPagerPages.
     */
    static public class SectionsPagerAdapter extends FragmentPagerAdapter {

        ArrayList<Pages> viewPagerPages;
        public SectionsPagerAdapter(FragmentManager fm, ArrayList<Pages> viewPagerPages) {
            super(fm);
            this.viewPagerPages = viewPagerPages;

        }

        @Override
        public Fragment getItem(int position) {
            Pages page =  viewPagerPages.get(position);
            switch (page) {
                case number:
                    return new OrderNumberButtonPadFragment();
                case price:
                    return new OrderPriceButtonPadFragment();
                case phone:
                    return new PhoneNumberEntryFragment();
                case address:
                    return new AddressEntryFragment();
                case time:
                    return new TimeFragment();
                case order:
                    return new NewOrderLastScreenFragment();

            }
            throw new IllegalStateException("No fragment for page");
        }

        @Override
        public int getCount() {
           return  viewPagerPages.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            Pages page =  viewPagerPages.get(position);
            switch (page) {
                case number:
                    return  "number".toUpperCase(l);
                case price:
                    return  "price".toUpperCase(l);
                case phone:
                    return  "phone".toUpperCase(l);
                case address:
                    return  "address".toUpperCase(l);
                case time:
                    return  "time".toUpperCase(l);
                case order:
                    return  "order".toUpperCase(l);
            }
            return null;
        }
    }

    void askGoBack(){
        AlertDialog.Builder builder = new AlertDialog.Builder(NewOrderActivity.this);
        builder.setMessage(R.string.ba_to_home_from_new_order)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                }).show();
    }

    @Override
    public void onBackPressed(){
        askGoBack();
    }


}
