package com.example.a20_prj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
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
	EditText editJifen;
	EditText editGonglv;
	ImageView imageviewdianchi;
	static boolean jiguang_flag=false;
	static boolean xianzhen_flag=true;
	Context g_ctx=null;
	byte[] cmd_check_fpga=	{(byte) 0xaa,0x55,0x00,0x00,(byte) 0xff,0x00,0x0d};
	byte[] cmd_jifen=		{(byte) 0xaa,0x01,0x00,0x00,0x00,0x00,0x0d};
	byte[] cmd_danci=		{(byte) 0xaa,0x02,0x00,0x00,(byte) 0xac,0x00,0x0d};
	byte[] cmd_duoci=		{(byte) 0xaa,0x03,0x00,0x00,(byte) 0xad,0x00,0x0d};
	byte[] cmd_stop=		{(byte) 0xaa,0x04,0x00,0x00,(byte) 0xae,0x00,0x0d};
	byte[] cmd_jiguang=		{(byte) 0xaa,0x05,0x00,0x00,0x00,0x00,0x0d};
	byte[] cmd_gonglv=		{(byte) 0xaa,0x07,0x00,0x00,(byte) 0xac,0x00,0x0d};
	byte[] cmd_xianzhen=	{(byte) 0xaa,0x08,0x00,0x00,(byte) 0xac,0x00,0x0d};
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
				//Log.i("20_prj", "Battery "+level2+"Level "+level);
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
		g_ctx=(Context)this;
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
						if(buffer[0]==0xaa && buffer[1]==0x0d)
						{
							if((buffer[0]+buffer[1]+buffer[2]+buffer[3]*256) == (buffer[4]+buffer[5]*256))
							{
								switch(buffer[1])
								{
									case 0x01:
										Log.i("RCV", "Jifen Time");
										break;
									case 0x02:
										Log.i("RCV", "Dan ci");
										break;
									case 0x03:
										Log.i("RCV", "Duo ci");
										break;
									case 0x04:
										Log.i("RCV", "Stop");
										break;
									case 0x05:
										Log.i("RCV", "K/G jiguang");
										break;
									case 0x07:
										Log.i("RCV", "Jiguang GL");
										break;
									case 0x08:
										Log.i("RCV", "XianZhen/MianZhen");
										break;
									default:
										Log.i("RCV", "Unknown");
										break;
								}
							}
							else
								Log.i("20_prj", "CRC error");
						}
						else
							Log.i("20_prj", "invalid packet "+ret);
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
	void send_cmd(byte[] cmd)
	{
		try {
			cmd[4]=(byte) ((cmd[0]+cmd[1]+cmd[2]+cmd[3]*256)&0xff);
			cmd[5]=(byte) (((cmd[0]+cmd[1]+cmd[2]+cmd[3]*256)>>8)&0xff);
			mOutputStream.write(cmd);
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
				send_cmd(cmd_danci);
			}
		});
	}
	public void setButtonDuoci() {
		btnDuoci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnDuoci");
				send_cmd(cmd_duoci);
			}
		});
	}
	public void setButtonStop() {
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnStop");
				send_cmd(cmd_stop);
			}
		});
	}
	public void setButtonJiguang() {
		btnJiguang.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnJiguang");
				if(jiguang_flag)
				{
					btnJiguang.setText(g_ctx.getString(R.string.jiguangk));
					jiguang_flag=false;
					cmd_jiguang[2]=0x00;
				}
				else
				{
					btnJiguang.setText(g_ctx.getString(R.string.jiguangg));
					jiguang_flag=true;
					cmd_jiguang[2]=0x01;
				}
				send_cmd(cmd_jiguang);
			}
		});
	}
	public void setButtonXianzhen() {
		btnXianzhen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnXianzhen");
				if(xianzhen_flag)
				{
					btnXianzhen.setText(g_ctx.getString(R.string.mianzhen));
					xianzhen_flag=false;
					cmd_xianzhen[2]=0x01;
				}
				else
				{
					btnXianzhen.setText(g_ctx.getString(R.string.xianzhen));
					xianzhen_flag=true;
					cmd_xianzhen[2]=0x00;
				}
				send_cmd(cmd_xianzhen);
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
		editJifen=(EditText)findViewById(R.id.Tjifentime);
		editGonglv=(EditText)findViewById(R.id.Tgonglv);
		/*
		editJifen.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {//输入数据之后监听

				Log.i("20_prj", "Send cmd jifen");
				cmd_jifen[2]=(byte) (Integer.parseInt(editJifen.getText().toString())&0xff);
				cmd_jifen[3]=(byte) ((Integer.parseInt(editJifen.getText().toString())>>8)&0xff);
				send_cmd(cmd_jifen);
				}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
			}
		 	}
				);
		editGonglv=(EditText)findViewById(R.id.Tgonglv);
		editGonglv.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {//输入数据之后监听

				Log.i("20_prj", "Send cmd gonglv");
				cmd_gonglv[2]=(byte) (Integer.parseInt(editGonglv.getText().toString())&0xff);
				cmd_gonglv[3]=(byte) ((Integer.parseInt(editGonglv.getText().toString())>>8)&0xff);
				send_cmd(cmd_gonglv);
				}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				}
		 	}
				);*/
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
		//new TestThread().start();
	}
}
