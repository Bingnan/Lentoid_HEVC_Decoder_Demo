package pku.shengbin.hevdecoder;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import pku.shengbin.hevdecoder.LocalExploreActivity;
import pku.shengbin.utils.MessageBox;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

public class LocalExploreActivity extends ListActivity {
	
	private String 			mRoot = Environment.getExternalStorageDirectory().getPath();
	private String			mCurrentDir = mRoot;
	private TextView 		mTextViewLocation;
	private File[]			mFiles;
	
	static String[] exts = {"hevc", "hm91", "hm10", "bit", "hvc", "h265", "265"};

	// for native debug
   static {
    	System.loadLibrary("lenthevcdec");
    	System.loadLibrary("jniplayer"); 
    }
   // end
    
   @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.movie_explorer);
		mTextViewLocation = (TextView) findViewById(R.id.textview_path);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LocalExploreActivity.this);  
		String currentDir = prefs.getString("current_dir", "");
		if (currentDir.isEmpty()) {
			getDirectory(mRoot);
		} else {
			getDirectory(currentDir);
		}
	}
   
	
	protected static boolean checkExtension(File file) {
		for (int i = 0; i < exts.length; i++) {
			if (file.getName().indexOf(exts[i]) > 0) {
				return true;
			}
		}
		return false; // true if not check!!
	}
	
	private void sortFiles(File[] files) {
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				if (f1.isDirectory() && f2.isFile()) return -1;
				else if (f2.isDirectory() && f1.isFile()) return 1;
				else return f1.getName().compareToIgnoreCase(f2.getName());
			}
		});
	}

	private void getDirectory(String dirPath) {
		try {
			mTextViewLocation.setText("Location: " + dirPath);
			mCurrentDir = dirPath;
			
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LocalExploreActivity.this);  
	    	prefs.edit().putString("current_dir", mCurrentDir).commit();
	
			class MyFilter implements FileFilter {
	            
	            // android maintains the preferences for us, so use directly
	            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(LocalExploreActivity.this); 
	            boolean showHidden = settings.getBoolean("show_hidden_switch", false);  
	            boolean showMediaOnly = settings.getBoolean("only_media_switch", false);
	            
	            private boolean isMedia(String name) {
	            	for (int i = 0; i < LocalExploreActivity.exts.length; i++) {
	            		if (name.endsWith(LocalExploreActivity.exts[i])) return true;
	            	}
					return false; 	
	            }

				public boolean accept(File file) {
					// TODO Auto-generated method stub
					if (!showHidden && file.getName().startsWith(".")) return false;
					else if (file.isDirectory()) return true;
					else if (showMediaOnly && !isMedia(file.getName())) return false;
					else return true;
				}
			}
            
			File f = new File(dirPath);
			File[] temp = f.listFiles(new MyFilter());
			
			sortFiles(temp);
			
			File[] files = null;
			if (!dirPath.equals(mRoot)) {
				files = new File[temp.length + 1];
				System.arraycopy(temp, 0, files, 1, temp.length);
				files[0] = new File(f.getParent());
			} else {
				files = temp;
			}
			
			mFiles = files;
			setListAdapter(new LocalExplorerAdapter(this, files, dirPath.equals(mRoot)));
			
		} catch (Exception ex) {
			MessageBox.show(this, "Sorry", "Get directory failed! Please check your SD card state.");
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String moviePath = new String();
		
		File file = mFiles[position];
		if (file.isDirectory()) {
			if (file.canRead()) {
				getDirectory(file.getAbsolutePath());
			}
			else {
				MessageBox.show(this, "Error", "[" + file.getName() + "] folder can't be read!");
				return;
			}
		} else {
			if (!checkExtension(file)) {
				StringBuilder strBuilder = new StringBuilder();
				for (int i = 0; i < exts.length; i++)
					strBuilder.append(exts[i] + " ");
				MessageBox.show(this, "Sorry", "Only these file extensions are supported:" + strBuilder.toString());
				return;
			}
			
			moviePath = file.getAbsolutePath();
			startPlayer(moviePath, 0);
		}
	}
	
	private void startPlayer(String filePath, int mediaType) {
    	Intent i = new Intent(this, GLPlayActivity.class);
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(LocalExploreActivity.this);  
        boolean useGL = settings.getBoolean("opengl_switch", true);
        if (!useGL) {
        	i = new Intent(this, PlayActivity.class);
        	Log.i("hevdecoder", "will play without OpenGL rendering");
        }
    	i.putExtra("pku.shengbin.hevdecoder.strMediaPath", filePath);
    	i.putExtra("pku.shengbin.hevdecoder.intMediaType", mediaType);
    	
    	startActivity(i);
    }
	
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {  	
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // do something on back
        	if (mCurrentDir.equals(mRoot)) {
        		new AlertDialog.Builder(this)
            	.setMessage("The application will exit. Are you sure?")
            	.setPositiveButton("Conform", new OnClickListener() {
            		public void onClick(DialogInterface arg0, int arg1) {
            			// user exits our application, so we may clear the current_dir state
            			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LocalExploreActivity.this);  
            			prefs.edit().putString("current_dir", "").commit();
            			finish();
            		}
            		})
            	.setNegativeButton("Cancel", new OnClickListener() {
    				public void onClick(DialogInterface arg0, int arg1) {
    				}
            		})
            	.show();
        	} else {
        		File f = new File(mCurrentDir);
        		getDirectory(f.getParent());
        	}
        	
            return true;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    private static final int MENU1 = Menu.FIRST;
    private static final int REQ_SYSTEM_SETTINGS = 0;
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // group id, item id, order id(0 stands for adding order)
        menu.add(0, MENU1, 1, "Settings");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        
	        case MENU1:
	        {
	        	startActivityForResult(new Intent(this, SettingsActivity.class), REQ_SYSTEM_SETTINGS);  
	        	break;
	        }
	        
        };
        return super.onOptionsItemSelected(item);
    }
    
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	if (requestCode == REQ_SYSTEM_SETTINGS) {   
	    	//restart activity to apply the setting changes
            this.finish();
            startActivity(new Intent(this, this.getClass()));
        }
    }
     
}
