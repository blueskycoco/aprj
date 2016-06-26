package com.example.a20_prj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Random;

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
import android.widget.LinearLayout;
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
	static boolean duoci_flag=false;
	int jifen_time=100;
	Context g_ctx=null;
	ArrayList<String> xRawDatas;
	LineGraphicView tu;  
	ArrayList<Double> yList;  
	byte[] cmd_check_fpga=	{(byte) 0xaa,0x55,0x00,0x00,(byte) 0xff,0x00,0x0d};
	byte[] cmd_danci=		{(byte) 0xaa,0x01,0x00,0x00,(byte) 0xac,0x00,0x0d};
	byte[] cmd_duoci=		{(byte) 0xaa,0x02,0x00,0x00,(byte) 0xad,0x00,0x0d};
	byte[] cmd_stop=		{(byte) 0xaa,0x03,0x00,0x00,(byte) 0xae,0x00,0x0d};
	byte[] cmd_jiguang_k=	{(byte) 0xaa,0x04,0x00,0x00,0x00,0x00,0x0d};
	byte[] cmd_jiguang_g=	{(byte) 0xaa,0x05,0x00,0x00,0x00,0x00,0x0d};	
	byte[] cmd_xianzhen=	{(byte) 0xaa,0x06,0x00,0x00,(byte) 0xac,0x00,0x0d};
	int[] fpga_data=new int[30];
	private final Handler handler = new Handler();
	private final Handler handlerUI = new Handler();
	final Runnable mUpdateResults = new Runnable() {
		public void run() {

			draw_cuve(fpga_data);
		}
		};
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
				//send_485();
				if(duoci_flag)
				{
					try {
						Thread.sleep(jifen_time);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					HardwareControl.wrSPI();
					//Log.i("20_prj", "Spi "+byte2HexStr(HardwareControl.wrSPI(),2068*72));
					handlerUI.post(mUpdateResults); 
				}
				else
				{
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
						if((buffer[0]&0xff)==0xaa && (buffer[6]&0xff)==0x0d)
						{
							if(((int)(buffer[0]&0xff)+(int)(buffer[1]&0xff)+(int)(buffer[2]&0xff)+(int)(buffer[3]&0xff)*256) 
									== ((int)(buffer[4]&0xff)+(int)(buffer[5]&0xff)*256))
							{
								switch(buffer[1]&0xff)
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
			Log.i("20_prj", "Spi "+byte2HexStr(HardwareControl.wrSPI(),5));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void send_cmd(byte[] cmd)
	{
		try {
			int tmp=((int)(cmd[0]&0xff)+(int)(cmd[1]&0xff)+(int)(cmd[2]&0xff)+(int)(cmd[3]&0xff)*256);
			cmd[4]=(byte) (tmp&0xff);
			cmd[5]=(byte) ((tmp>>8)&0xff);
			mOutputStream.write(cmd);			
			Log.i("20_prj", "sned cmd "+byte2HexStr(cmd,7));
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
				btnDuoci.setEnabled(false);
				btnStop.setEnabled(false);
				int jf=Integer.valueOf(editJifen.getText().toString());
				jifen_time=jf;
				cmd_danci[2]=(byte) (jf&0xff);
				cmd_danci[3]=(byte) ((jf>>8)&0xff);
				send_cmd(cmd_danci);
				try {
					Thread.sleep(jifen_time);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				HardwareControl.wrSPI();
				//Log.i("20_prj", "Spi "+byte2HexStr(HardwareControl.wrSPI(),2068*72));
				draw_cuve(fpga_data);
				btnDuoci.setEnabled(true);
				btnStop.setEnabled(true);
			}
		});
	}
	public void setButtonDuoci() {
		btnDuoci.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {				
				Log.i("20_prj","btnDuoci");
				btnDuoci.setEnabled(false);
				btnDanci.setEnabled(false);
				int jf=Integer.valueOf(editJifen.getText().toString());
				jifen_time=jf;
				cmd_duoci[2]=(byte) (jf&0xff);
				cmd_duoci[3]=(byte) ((jf>>8)&0xff);
				send_cmd(cmd_duoci);
				duoci_flag=true;				
			}
		});
	}
	public void setButtonStop() {
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnStop");
				send_cmd(cmd_stop);
				duoci_flag=false;
				btnDuoci.setEnabled(true);
				btnDanci.setEnabled(true);
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
					send_cmd(cmd_jiguang_g);
				}
				else
				{
					btnJiguang.setText(g_ctx.getString(R.string.jiguangg));
					jiguang_flag=true;
					int gl=Integer.valueOf(editGonglv.getText().toString());
					cmd_jiguang_k[2]=(byte) (gl&0xff);
					cmd_jiguang_k[3]=(byte) ((gl>>8)&0xff);
					send_cmd(cmd_jiguang_k);
				}
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
					cmd_xianzhen[2]=0x00;
				}
				else
				{
					btnXianzhen.setText(g_ctx.getString(R.string.xianzhen));
					xianzhen_flag=true;
					cmd_xianzhen[2]=0x01;
				}
				send_cmd(cmd_xianzhen);
			}
		});
	}
	void init_draw()
	{
		LinearLayout layout=(LinearLayout) findViewById(R.id.draw);  
        final DrawView view=new DrawView(this);  
        view.setMinimumHeight(500);  
        view.setMinimumWidth(300);  
        //通知view组件重绘    
        view.invalidate();  
        layout.addView(view);  
	}
	void init_draw2()
	{		 
		tu = (LineGraphicView) findViewById(R.id.line_graphic);		  
        yList = new ArrayList<Double>();        
        xRawDatas = new ArrayList<String>();
        for(int i=0;i<30;i++)
        xRawDatas.add(String.valueOf(i));  
        draw_cuve(fpga_data);
	}
	void create_fpga_data(int[] cuve)
	{
		Random random=new Random();
		for(int i=0;i<30;i++)
			cuve[i]=random.nextInt(73727);
	}
	void draw_cuve(int[] cuve)
	{
		create_fpga_data(cuve);
		yList.clear();
		for(int j=0;j<30;j++)
        {
        	yList.add((double) cuve[j]);
        }
        tu.setData(yList, xRawDatas, 73727, 8192);
        tu.invalidate();
	}
	public void Init()
	{
		//for(int i=0;i<2068*72;i++)
		//fpga_data[i]=0;
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
		new TestThread().start();
		init_draw2();
	}
}
