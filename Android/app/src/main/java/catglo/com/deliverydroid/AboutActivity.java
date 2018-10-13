package catglo.com.deliverydroid;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;


public class AboutActivity extends Activity  {

      @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            //int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            ((TextView)findViewById(R.id.version)).setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String legalString = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
        ((TextView)findViewById(R.id.legal)).setText(legalString);



    }


}
