package pku.shengbin.hevdecoder;

import pku.shengbin.utils.MessageBox;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener,
		OnPreferenceClickListener
{
		@Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.settings);
	        
	        Preference pref1=findPreference("about_this_app");
	        pref1.setOnPreferenceClickListener(this);
	    }

		public boolean onPreferenceChange(Preference arg0, Object arg1) {
			// TODO Auto-generated method stub
			
			return true; // return true so the preference can be changed, false otherwise
		}

		public boolean onPreferenceClick(Preference arg0) {
			// TODO Auto-generated method stub
			if(arg0.getKey().equals("about_this_app")){
				MessageBox.show(this, "About", "This app is to demonstrate HEVC decoding.\n\nSuggestions and bug reports may be sent to: shengbinmeng@gmail.com ");
			};
			return false;
		}
}