package com.example.a20_prj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
	TextView texttimedetail;
	TextView textdatedetail;
	Button	btnDanci;
	Button	btnDuoci;
	Button	btnStop;
	Button	btnJiguang;
	Button	btnXianzhen;
	ImageView imageviewdianchi;
	private final Handler handler = new Handler();
	private final Runnable task = new Runnable() {

		public void run() {
				SimpleDateFormat sDateFormat_time = new SimpleDateFormat("HH:mm:ss");
				String time = sDateFormat_time.format(new java.util.Date());
				texttimedetail.setText(time);
				SimpleDateFormat sDateFormat_date = new SimpleDateFormat("yyyy-MM-dd");
				String date = sDateFormat_date.format(new java.util.Date());
				textdatedetail.setText(date);
				int level = 0;
				float max=32767;
				float level2=0;
				level2=(float)HardwareControl.getBattery();
				level=(int) ((level2/max)*((float)100));
				Log.i("20_prj", "Battery "+level2+"Level "+level);
				if (20 > level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt0));
				} else if (40 > level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt1));
				} else if (60 > level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt2));
				} else if (80 > level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt3));
				} else if (100 > level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt4));
				}

				if (100 == level) {
					imageviewdianchi.setBackground(getResources().getDrawable(
							R.drawable.batt5));
				}
				handler.postDelayed(this, 1000);
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	private class TestThread extends Thread {

		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				send_485();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	private class ReadThread extends Thread {

		@Override
		public void run() {
			super.run();
			while (!isInterrupted()) {
				int size;
				try {
					byte[] buffer = new byte[128];
					if (mInputStream == null)
						return;

					
					size = mInputStream.read(buffer);
					if (size > 0) {
						onDataReceived(buffer, size);
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	public static String byte2HexStr(byte[] b,int len)    
	{    
	    String stmp="";    
	    StringBuilder sb = new StringBuilder("");    
	    for (int n=0;n<len;n++)    
	    {    
	        stmp = Integer.toHexString(b[n] & 0xFF);    
	        sb.append((stmp.length()==1)? "0"+stmp : stmp);    
	        sb.append(" ");    
	    }    
	    return sb.toString().toUpperCase().trim();    
	}
	public static float byte2float(byte[] b, int index) {    
	    int l;                                             
	    l = b[index + 3];                                  
	    l &= 0xff;                                         
	    l |= ((long) b[index + 2] << 8);                   
	    l &= 0xffff;                                       
	    l |= ((long) b[index + 1] << 16);                  
	    l &= 0xffffff;                                     
	    l |= ((long) b[index + 0] << 24);                  
	    return Float.intBitsToFloat(l);                    
	}
	protected void onDataReceived(final byte[] buffer, final int size) {
		
        runOnUiThread(new Runnable() {
                public void run() {
                		String ret=byte2HexStr(buffer,size);
						Log.i("485",size + "==>"+ret);						
                }
        });
	}
	public void send_485()
	{
		byte[] cmd={0x24,0x32,(byte)0xff,0x23,0x0a};
		try {
			mOutputStream.write(cmd);
			Log.i("20_prj", "Spi "+byte2HexStr(HardwareControl.wrSPI(cmd),5));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void setButtonDanci() {
		btnDanci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnDanci");
			}
		});
	}
	public void setButtonDuoci() {
		btnDuoci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnDuoci");
			}
		});
	}
	public void setButtonStop() {
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnStop");
			}
		});
	}
	public void setButtonJiguang() {
		btnJiguang.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnJiguang");
			}
		});
	}
	public void setButtonXianzhen() {
		btnXianzhen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnXianzhen");
			}
		});
	}
	public void Init()
	{
		textdatedetail = (TextView) findViewById(R.id.date);
		texttimedetail = (TextView) findViewById(R.id.time);
		btnDanci =(Button)findViewById(R.id.Bdanci);
		btnDuoci =(Button)findViewById(R.id.Bduoci);
		btnStop =(Button)findViewById(R.id.Bstop);
		btnJiguang =(Button)findViewById(R.id.Bjiguang);
		btnXianzhen =(Button)findViewById(R.id.Bxianzhen);
		setButtonDanci();
		setButtonDuoci();
		setButtonStop();
		setButtonJiguang();
		setButtonXianzhen();
		imageviewdianchi = (ImageView) findViewById(R.id.imageviewpower);
		HardwareControl.init();
		mInputStream = HardwareControl.getInputStream();
		mOutputStream = HardwareControl.getOutputStream();
		mReadThread = new ReadThread();
		mReadThread.start();
		handler.postDelayed(task, 1000);
		new TestThread().start();
	}
}
