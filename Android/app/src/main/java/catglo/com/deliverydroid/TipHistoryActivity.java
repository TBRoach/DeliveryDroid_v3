package catglo.com.deliverydroid;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import catglo.com.deliveryDatabase.DataBase;
import catglo.com.deliveryDatabase.TipTotalData;
import catglo.com.deliveryDatabase.TipTotalData.PayRatePieriod;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class TipHistoryActivity extends DeliveryDroidBaseActivity {

	boolean	userDates	= false;
	private Button	email;
	private EditText startDateField;
	private EditText endDateField;
	private TextView tipsMade;
	private TextView driverEarnings;
	private TextView bestTip;
	private TextView averageTip;
	private TextView worstTip;
	private TextView totalDeliveries;
	private Button text;
	protected Calendar startDate;
	protected Calendar endDate;
	private TextView milesDriven;
	private CheckBox monday;
	private CheckBox tuesday;
	private CheckBox wendsday;
	private CheckBox thursday;
	private CheckBox friday;
	private CheckBox saturday;
	private CheckBox sunday;
	private ViewGroup dateRangeContent;
	private Editor prefEditor;
	private TextView[] hoursWorked = new TextView[3];
	private TextView[] hoursWorkedTitle = new TextView[3];
	private TextView averagePayRate;
	private View moreClickable;
	private ScrollView scrollView;
	private View filterLayout;
    private DrawerLayout drawerLayout;
    private View menuDrawer;
    private Button filterMenuButton;

    @Override
	public void onPause(){
		super.onPause();
		Editor editor = sharedPreferences.edit();
		editor.putInt("TipScrollPosition", scrollView.getScrollY());
		editor.commit();
	}
	
	//TODO: Save and restore the filter settings like if its this week recalculate to whatever week it is
	//      and if its a custom range those dates exactly.
	@Override
	public void onResume(){
		super.onResume();
		scrollView.scrollTo(0, sharedPreferences.getInt("TipScrollPosition", 0));
	}
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tips_totals);
    
        final Calendar now = Calendar.getInstance();
		now.setTimeInMillis(System.currentTimeMillis());



        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        menuDrawer = findViewById(R.id.menu_drawer);

        findViewById(R.id.email_menu_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                emailResults();
                drawerLayout.closeDrawers();
            }
        });

        findViewById(R.id.text_menu_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                smsResults();
                drawerLayout.closeDrawers();
            }
        });

        filterMenuButton = (Button)findViewById(R.id.toggle_menu_button);
        filterMenuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFilter();
                drawerLayout.closeDrawers();
            }
        });


        startDate = (Calendar) now.clone();
		startDate.setTimeInMillis(sharedPreferences.getLong("tipHistorystartDate", (((Calendar) now.clone()).getTimeInMillis())));
		endDate = (Calendar) now.clone();
		endDate.setTimeInMillis(sharedPreferences.getLong("tipHistoryendDate", (((Calendar) now.clone()).getTimeInMillis())));
	
		moreClickable = findViewById(R.id.moreClickable);
		moreClickable.setOnClickListener(new OnClickListener(){public void onClick(View arg0) {
            if (drawerLayout.isDrawerOpen(menuDrawer)) {
                drawerLayout.closeDrawer(menuDrawer);
            } else {
                drawerLayout.openDrawer(menuDrawer);
            }
		}});
		
		filterLayout = findViewById(R.id.filterLayout);
        if (sharedPreferences.getBoolean("show_tip_totals_day_filter", true)){
            filterLayout.setVisibility(View.VISIBLE);
            filterMenuButton.setText(R.string.Hide_weekday_filters);
        } else {
            filterLayout.setVisibility(View.GONE);
            filterMenuButton.setText(R.string.Show_weekday_filters);
        }
		
		scrollView = (ScrollView)findViewById(R.id.sewidget29);
		
		monday = (CheckBox)findViewById(R.id.monday);
		tuesday = (CheckBox)findViewById(R.id.tuesday);
		wendsday = (CheckBox)findViewById(R.id.wendsday);
		thursday = (CheckBox)findViewById(R.id.thursday);
		friday = (CheckBox)findViewById(R.id.friday);
		saturday = (CheckBox)findViewById(R.id.saturday);
		sunday = (CheckBox)findViewById(R.id.sunday);
		monday.setChecked(sharedPreferences.getBoolean("tipHistoryMonday", true));
		tuesday.setChecked(sharedPreferences.getBoolean("tipHistoryTuesday", true));
		wendsday.setChecked(sharedPreferences.getBoolean("tipHistoryWendsday", true));
		thursday.setChecked(sharedPreferences.getBoolean("tipHistoryThursday", true));
		friday.setChecked(sharedPreferences.getBoolean("tipHistoryFriday", true));
		saturday.setChecked(sharedPreferences.getBoolean("tipHistorySaturday", true));
		sunday.setChecked(sharedPreferences.getBoolean("tipHistorySunday", true));
		
		monday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistoryMonday", isChecked);
			prefEditor.commit();
		}});
		tuesday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistoryTuesday", isChecked);
			prefEditor.commit();
		}});
		wendsday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistoryWendsday", isChecked);
			prefEditor.commit();
		}});
		thursday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistoryThursday", isChecked);
			prefEditor.commit();
		}});
		friday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistoryFriday", isChecked);
			prefEditor.commit();
		}});
		saturday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistorySaturday", isChecked);
			prefEditor.commit();
		}});
		sunday.setOnCheckedChangeListener(new OnCheckedChangeListener(){public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			updateUI();
			prefEditor.putBoolean("tipHistorySunday", isChecked);
			prefEditor.commit();
		}});
		
		
		((View)findViewById(R.id.backButton)).setOnClickListener(new OnClickListener(){public void onClick(View v) {
			finish();
		}});
		
		prefEditor = sharedPreferences.edit();
		
		dateRangeContent = (ViewGroup)findViewById(R.id.dateRangeContent);


		if (sharedPreferences.getBoolean("dateRangeIndicator", true)){
			dateRangeContent.setVisibility(View.VISIBLE);
		} else {
			dateRangeContent.setVisibility(View.GONE);
		}
			
        String[] listValues = {
        		getString(R.string.today),
        		getString(R.string.thisWeek),
        		getString(R.string.ThisMonth),
        		getString(R.string.ThisYear),
        		getString(R.string.CustomDateRange)};
        Spinner dateRangeSpinner = (Spinner) findViewById(R.id.deliveryAreaSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.tip_totals_spinner_in_toolbar, listValues);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		dateRangeSpinner.setAdapter(adapter);
		dateRangeSpinner.setSelection(sharedPreferences.getInt("tipHistorySpinnerDefault", 1));
		dateRangeSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				prefEditor.putInt("tipHistorySpinnerDefault", arg2);
				prefEditor.commit();
				startDate = Calendar.getInstance();
				startDate.setTimeInMillis(System.currentTimeMillis());
				endDate = Calendar.getInstance();
				endDate.setTimeInMillis(System.currentTimeMillis());
					switch (arg2) {
		        case 0: startDate.set(Calendar.HOUR_OF_DAY, 0);
		                endDate.set(Calendar.HOUR_OF_DAY, 0);
		                endDate.add(Calendar.DAY_OF_YEAR, 1);
		                startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
		                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
		                updateUI();
		                break;
		        case 1: tools.getWorkWeekDates(now,startDate,endDate);
				        startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
		                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
		                updateUI(); 
		        break;
		        case 2: startDate.set(Calendar.DATE, 0);
						endDate.add(Calendar.MONTH, 1);
						startDate.set(Calendar.DATE, 1);
						startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
		                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
		                updateUI();
		                break;
		        case 3: startDate.set(Calendar.DAY_OF_YEAR, 1);
						endDate.add(Calendar.YEAR, 1);
						endDate.set(Calendar.DAY_OF_YEAR, 0);
						startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
		                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
		                updateUI();
		                break;
		        case 4: 
		        		tools.getDateRangeDialog(startDate,endDate,new OnDismissListener(){public void onDismiss(DialogInterface dialog) {
		        			startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
			                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
			                updateUI();
		        		}});
		        	break;
			    }
			}
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
        
        startDateField = (EditText) findViewById(R.id.hourlyPayRate);
        endDateField = (EditText) findViewById(R.id.totalCash);
        
        tools.getWorkWeekDates(now,startDate,endDate);
        startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
        endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
      
        OnTouchListener dateTouchListener = new OnTouchListener(){public boolean onTouch(View v, MotionEvent event) {
        	if (event.getAction() == MotionEvent.ACTION_DOWN){
        		tools.getDateRangeDialog(startDate,endDate,new OnDismissListener(){public void onDismiss(DialogInterface dialog) {
        			startDateField.setText((startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR));
	                endDateField.setText((endDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
	                updateUI();
        		}});
        		return true;
        	}
        	return false;
		}};
        
        startDateField.setOnTouchListener(dateTouchListener);
        endDateField.setOnTouchListener(dateTouchListener);
        
        tipsMade = (TextView) findViewById(R.id.tipsMade);
       
        driverEarnings = (TextView) findViewById(R.id.driverEarnings);
        bestTip = (TextView) findViewById(R.id.bestTip);
        averageTip = (TextView) findViewById(R.id.averageTip);
        worstTip = (TextView) findViewById(R.id.worstTip);
        totalDeliveries = (TextView) findViewById(R.id.totalDeliveries);
        milesDriven = (TextView) findViewById(R.id.milesDriven);
        
        hoursWorkedTitle[0] = (TextView) findViewById(R.id.textView20);
        hoursWorkedTitle[1] = (TextView) findViewById(R.id.textView20_b);
        hoursWorkedTitle[2] = (TextView) findViewById(R.id.textView20_c);
        hoursWorked[0] = (TextView) findViewById(R.id.textView19);
        hoursWorked[1] = (TextView) findViewById(R.id.textView19_b);
        hoursWorked[2] = (TextView) findViewById(R.id.textView19_c);
        
        averagePayRate = (TextView)findViewById(R.id.averagePayRate);
         
       
		updateUI();
    }
    
    String getEmailText(TipTotalData tips){
    	 return ""+
			getString(R.string.tipsHistoryExportEmailTop)+"\n"+
			getString(R.string.startDate)+":"+(startDate.get(Calendar.MONTH)+1)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR)+"  "+
			getString(R.string.endDate)+":"+(startDate.get(Calendar.MONTH)+1)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR)+"  "+
			getString(R.string.deliveries) +":"+(tips.deliveries)+"\n"+
			getString(R.string.tipsMade) +":"+ Tools.getFormattedCurrency(tips.payed - tips.cost)+"\n"+
			getString(R.string.DriverEarnings) +":"+ Tools.getFormattedCurrency(tips.total)+"\n"+
			getString(R.string.bestTip) +":"+ Tools.getFormattedCurrency(tips.bestTip)+"\n"+
			getString(R.string.averageTip) +":"+ Tools.getFormattedCurrency(tips.averageTip)+"\n"+
			getString(R.string.worstTip) +":"+ Tools.getFormattedCurrency(tips.worstTip)+ "\n"+
			getString(R.string.hoursWorked) +":"+ Tools.getFormattedCurrency(tips.hours)+ "\n"+
			getString(R.string.milesDriven) +":"+tips.odometerTotal+ "\n";
    }
    
    void updateUI(){
    	//sharedPreferences.getLong("tipHistoryendDate", (((Calendar) now.clone()).getTimeInMillis()))
    	prefEditor.putLong("tipHistoryendDate", endDate.getTimeInMillis());
    	prefEditor.putLong("tipHistorystartDate", startDate.getTimeInMillis());
    	prefEditor.commit();
    	
    	String shiftSqlQuery = ""
    			+ "WHERE shifts.`TimeStart` >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate) 
    			+"' AND shifts.`TimeStart` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"' ";
    			;
    	String sqlQuery = 
    		" Payed > 0 AND `"
			+ DataBase.Time + "` >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate)
			+"' AND `"+ DataBase.Time + "` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"' ";
    	
    	if (sunday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '0'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '0'";
    	}
    	if (monday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '1'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '1'";
    	}
    	if (tuesday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '2'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '2'";
    	}
    	if (wendsday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '3'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '3'";
    	}
    	if (thursday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '4'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '4'";
    	}
    	if (friday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '5'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '5'";
    	}
    	if (saturday.isChecked()==false){
    		sqlQuery = sqlQuery + " AND `weekday` IS NOT '6'";
    		shiftSqlQuery = shiftSqlQuery + " AND `weekday` IS NOT '6'";
    	}
    	
    	
    	TipTotalData tips = dataBase.getTipTotal(getApplicationContext(),sqlQuery,shiftSqlQuery);
    	
    	
    	tipsMade.setText(Tools.getFormattedCurrency(tips.payed - tips.cost));
    	driverEarnings.setText(Tools.getFormattedCurrency(tips.total));
    	bestTip.setText(Tools.getFormattedCurrency(tips.bestTip));
    	if (Float.isNaN(tips.averageTip)==false) averageTip.setText(Tools.getFormattedCurrency(tips.averageTip));
    	worstTip.setText(Tools.getFormattedCurrency(tips.worstTip));
    	totalDeliveries.setText(""+tips.deliveries);
    	milesDriven.setText(""+tips.odometerTotal);
    	
    	hoursWorked[0].setText(String.format("%.2f",tips.hours));
    	
    	float averagePayRateValue = 0;
    	float totalPay = 0;
    	float totalHours = 0;
    	
    	ArrayList<PayRatePieriod> payRates = new ArrayList<PayRatePieriod>();
    	for (String key : tips.payRatePieriods.keySet()) {
    		PayRatePieriod p = tips.payRatePieriods.get(key);
    		payRates.add(p);
    		totalPay += p.hourlyPay*p.hours;
    		totalHours += p.hours;
    	}
    	averagePayRateValue = (totalPay/totalHours)*60;
    	
    	averagePayRate.setText(Tools.getFormattedCurrency(averagePayRateValue));
    	
    	
		//If there was more than 1 pay rate show the breakdown of the top 2
    	if (payRates.size()>1) {
    	
	    	Collections.sort(payRates, new Comparator<PayRatePieriod>(){public int compare(PayRatePieriod rhs, PayRatePieriod lhs) {
	    		return (int)(lhs.hours - rhs.hours);
			}});
	    	
	    	PayRatePieriod prp = payRates.get(0);
			DecimalFormat df = new DecimalFormat("#.##");
			hoursWorked[1].setText(df.format(prp.hours)+"");
			hoursWorkedTitle[1].setText("Hours @ "+ Tools.getFormattedCurrency(prp.hourlyPay * 60));
			
			prp = payRates.get(1);
			hoursWorked[2].setText(df.format(prp.hours)+"");
			hoursWorkedTitle[2].setText("Hours @ "+ Tools.getFormattedCurrency(prp.hourlyPay * 60));
			
			
			hoursWorked[1].setVisibility(View.VISIBLE);
			hoursWorkedTitle[1].setVisibility(View.VISIBLE);
			hoursWorked[2].setVisibility(View.VISIBLE);
			hoursWorkedTitle[2].setVisibility(View.VISIBLE);
			
    	} else {
    		hoursWorked[1].setVisibility(View.GONE);
    		hoursWorked[2].setVisibility(View.GONE);
    		hoursWorkedTitle[1].setVisibility(View.GONE);
    		hoursWorkedTitle[2].setVisibility(View.GONE);	
    	}
    }
    
    final int EMAIL = 1;
    final int SMS = 2;
    final int TOGGLE_FILTER=3;
    
    /* Creates the menu items */
	public boolean onCreateOptionsMenu(final Menu menu) {
		menu.add(0, EMAIL, 0, getString(R.string.email)).setIcon(android.R.drawable.ic_dialog_email);
		menu.add(0, SMS, 0, getString(R.string.textMessage)).setIcon(android.R.drawable.ic_menu_send);
		return true;
	} 
	


    void emailResults(){
        TipTotalData tips = dataBase.getTipTotal(getApplicationContext(),
                " Payed >= 0 AND `"+ DataBase.Time + "` >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate) +
                        "' AND `"+ DataBase.Time + "` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"'",
                "WHERE shifts.TimeStart >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate) +
                        "' AND shifts.`TimeEnd` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"'");

        final Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent .putExtra(Intent.EXTRA_TEXT, getEmailText(tips));
        emailIntent .setType("plain/text");
        emailIntent .putExtra(Intent.EXTRA_EMAIL, new String[]{});
        emailIntent .putExtra(Intent.EXTRA_SUBJECT,  getString(R.string.app_name)+" "+getString(R.string.tipHistory) +
                startDate.get(Calendar.MONTH)+"/"+startDate.get(Calendar.DAY_OF_MONTH)+"/"+startDate.get(Calendar.YEAR)+"..."+startDate.get(Calendar.MONTH)+"/"+endDate.get(Calendar.DAY_OF_MONTH)+"/"+endDate.get(Calendar.YEAR));
        startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }

    void smsResults(){
        //TODO: Update to include new fields like e-mail
        TipTotalData tips = dataBase.getTipTotal(getApplicationContext(),
                " Payed >= 0 AND `"+ DataBase.Time + "` >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate) +
                        "' AND `"+ DataBase.Time + "` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"'",
                "WHERE shifts.TimeStart >= '"+String.format("%3$tY-%3$tm-%3$td", startDate, startDate, startDate) +
                        "' AND shifts.`TimeEnd` <= '" + String.format("%3$tY-%3$tm-%3$td", endDate, endDate, endDate)+"'"
        );

        Intent sendIntent = new Intent(Intent.ACTION_VIEW);
        sendIntent.setData(Uri.parse("sms:"));
        sendIntent.putExtra("sms_body", getEmailText(tips));
        startActivity(Intent.createChooser(sendIntent, "Send text..."));

    }

    void toggleFilter(){
        Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getBoolean("show_tip_totals_day_filter", true)){
            filterLayout.setVisibility(View.GONE);
            editor.putBoolean("show_tip_totals_day_filter", false);
            filterMenuButton.setText(R.string.Show_weekday_filters);
        } else {
            filterLayout.setVisibility(View.VISIBLE);
            editor.putBoolean("show_tip_totals_day_filter", true);
            filterMenuButton.setText(R.string.Hide_weekday_filters);
        }
        editor.commit();
    }

}