package com.example.a20_prj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	protected OutputStream mOutputStream;
	private InputStream mInputStream;
	private ReadThread mReadThread;
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
			Log.i("20_prj", "Battery "+HardwareControl.getBattery());
			Log.i("20_prj", "Spi "+String.valueOf(HardwareControl.wrSPI(cmd)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void Init()
	{
		HardwareControl.init();
		mInputStream = HardwareControl.getInputStream();
		mOutputStream = HardwareControl.getOutputStream();
		mReadThread = new ReadThread();
		mReadThread.start();
		new TestThread().start();
	}
}
