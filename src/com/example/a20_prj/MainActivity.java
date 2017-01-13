package com.example.a20_prj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
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
import android.widget.Toast;

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
	Button	btnBodong;
	EditText editJifen;
	EditText editGonglv;
	ImageView imageviewdianchi;
	FileOutputStream outStream=null;
	File file=null;
	String date;
	static boolean jiguang_flag=false;
	static boolean xianzhen_flag=true;
	static boolean bodong_flag=true;
	static boolean duoci_flag=false;
	int jifen_time=100;
	Context g_ctx=null;
	ArrayList<String> xRawDatas;
	LineGraphicView tu;  
	ArrayList<Double> yList;
	int Y_Max=70;
	byte[] cmd_check_fpga	 =		{(byte) 0xaa,0x55,0x00,0x00,(byte) 0xff,0x00,0x0d};
	byte[] cmd_e			 =		{(byte) 0xab,(byte)0xff};
	byte[] cmd_real_data			 =		{(byte) 0xab,0x00};
	byte[] cmd_switch_to_spi =		{(byte) 0xaa,0x01};
	byte[] cmd_switch_to_usb =		{(byte) 0xaa,0x00};
	byte[] cmd_switch_to_xian=		{(byte) 0xaa,(byte)0xde};
	byte[] cmd_switch_to_mian=		{(byte) 0xaa,(byte)0xab};
	byte[] cmd_jiguang_k	 =		{(byte) 0xaa,0x01,0x00,0x00,0x00,0x00,0x0d};
	byte[] cmd_jiguang_g	 =		{(byte) 0xaa,0x01,0x00,0x00,0x00,0x00,0x0d};
	int[] fpga_data=new int[Y_Max];
	private final Handler handler = new Handler();
	private Handler handlerUI = null;
	private final Handler handlerMain = new Handler(){
		public void handleMessage(Message msg) {  
            // TODO Auto-generated method stub  
            int tmp = msg.arg1;  
            System.out.println(Thread.currentThread().getId() + "::::::::::::" + tmp);  
            if(tmp==1123)
            	draw_cuve(fpga_data);
            return ;  
        }  		
	};
	final Runnable mUpdateResults = new Runnable() {
		public void run() {
			if(duoci_flag)
			{		
			double tmp=0;
			int j=0;
			int bei=1;
			synchronized (this) {
			if(xianzhen_flag)
				HardwareControl.wrSPI(cmd_switch_to_xian);
			else
				HardwareControl.wrSPI(cmd_switch_to_mian);
			try {
				Thread.sleep(jifen_time);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte[] data = HardwareControl.wrSPI(null);
			//if(xianzhen_flag)
			//	Log.i("20_prj", "Spi "+byte2HexStr(data,2068*2));
			try {		
				
				file = new File("/mnt/extsd0");
				if(file.canWrite())
				{
					file = new File("/mnt/extsd0/ad_"+date+".dat");
					outStream = new FileOutputStream(file,true);
					outStream.write(data);
					outStream.flush();
					outStream.close();				
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(!xianzhen_flag)
				bei=70;
			for(int i=0;i<Y_Max;i++)
			{
				/*for(j=i*130*bei;j<(i+1)*130*bei;j=j+2)
				{
					Log.i("LOCAL", String.valueOf((int)((data[j]&0xff)<<8|(data[j+1]&0xff))));
					tmp=tmp+(int)((data[j]&0xff)<<8|(data[j+1]&0xff));
				}
				Log.i("PJ", String.valueOf(tmp));
				fpga_data[i]=(int)(tmp/(130*bei));
				Log.i("JG", String.valueOf(fpga_data[i]));
				tmp=0;*/
				if((int)((data[i*50*bei]&0xff)<<8|(data[i*50*bei+1]&0xff))!=0)
					fpga_data[i]=(int)((data[i*50*bei]&0xff)<<8|(data[i*50*bei+1]&0xff));
				else
					fpga_data[i]=(int)((data[(i+1)*50*bei]&0xff)<<8|(data[(i+1)*50*bei+1]&0xff));
			}
			//draw_cuve(fpga_data);
			Message msg = handlerMain.obtainMessage();  
            msg.arg1 = 1123;  
            handlerMain.sendMessage(msg);  
			}
			}
			handlerUI.postDelayed(this,100);
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
				float max=42964;
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
				if(duoci_flag)
				{
					handlerUI.postDelayed(mUpdateResults,1000);
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
		if(b == null)
			return null;
		
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
							if(((int)(buffer[0]&0xff)+(int)(buffer[1]&0xff)+(int)(buffer[2]&0xff)+(int)(buffer[3]&0xff)) 
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
			if(mOutputStream != null)
				mOutputStream.write(cmd);
			//Log.i("20_prj", "Spi "+byte2HexStr(HardwareControl.wrSPI(),5));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	void send_cmd(byte[] cmd)
	{
		try {
			int tmp=((int)(cmd[0]&0xff)+(int)(cmd[1]&0xff)+(int)(cmd[2]&0xff)+(int)(cmd[3]&0xff));
			cmd[4]=(byte) (tmp&0xff);
			cmd[5]=(byte) ((tmp>>8)&0xff);
			if(mOutputStream != null)
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
				btnXianzhen.setEnabled(false);
				btnBodong.setEnabled(false);
				int jf=1000;
				if(editJifen.getText().toString()!=null)
					jf=Integer.valueOf(editJifen.getText().toString());
				jifen_time=jf;
				synchronized (this) {
				if(xianzhen_flag)
					HardwareControl.wrSPI(cmd_switch_to_xian);
				else
					HardwareControl.wrSPI(cmd_switch_to_mian);
				try {
					Thread.sleep(jifen_time);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//HardwareControl.wrSPI();
				int bei=1;
				byte[] data = HardwareControl.wrSPI(null);
				if(!xianzhen_flag)
				{
					//Log.i("20_prj", "Spi "+byte2HexStr(data,2068*2*70));
					bei=70;
				}
				//else
				//	Log.i("20_prj", "Spi "+byte2HexStr(data,2068*2));
				
				try {		
					
					file = new File("/mnt/extsd0");
					if(file.canWrite())
					{
						file = new File("/mnt/extsd0/ad_"+date+".dat");
						outStream = new FileOutputStream(file,true);
						outStream.write(data);
						outStream.flush();
						outStream.close();				
					}
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				double tmp=0;
				int j=0;
				for(int i=0;i<Y_Max;i++)
				{
					/*for(j=i*130*bei;j<(i+1)*130*bei;j=j+2)
					{
						Log.i("LOCAL", String.valueOf((int)((data[j]&0xff)<<8|(data[j+1]&0xff))));
						tmp=tmp+(int)((data[j]&0xff)<<8|(data[j+1]&0xff));
					}
					Log.i("PJ", String.valueOf(tmp));
					fpga_data[i]=(int)(tmp/(130*bei));
					Log.i("JG", String.valueOf(fpga_data[i]));
					tmp=0;*/
					if((int)((data[i*50*bei]&0xff)<<8|(data[i*50*bei+1]&0xff))!=0)
						fpga_data[i]=(int)((data[i*50*bei]&0xff)<<8|(data[i*50*bei+1]&0xff));
					else
						fpga_data[i]=(int)((data[(i+1)*50*bei]&0xff)<<8|(data[(i+1)*50*bei+1]&0xff));
				}
				draw_cuve(fpga_data);
				}
				btnDuoci.setEnabled(true);
				btnStop.setEnabled(true);
				btnXianzhen.setEnabled(true);
				btnBodong.setEnabled(true);
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
				btnXianzhen.setEnabled(false);
				btnBodong.setEnabled(false);
				int jf=1000;
				if(editJifen.getText().toString()!=null)
					jf=Integer.valueOf(editJifen.getText().toString());
				jifen_time=jf;
				duoci_flag=true;				
			}
		});
	}
	public void setButtonStop() {
		btnStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnStop");
				duoci_flag=false;
				btnDuoci.setEnabled(true);
				btnDanci.setEnabled(true);
				btnXianzhen.setEnabled(true);
				btnBodong.setEnabled(true);
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
					int gl=100;
					if(editGonglv.getText().toString()!=null)
						gl=Integer.valueOf(editGonglv.getText().toString());
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
				}
				else
				{
					btnXianzhen.setText(g_ctx.getString(R.string.xianzhen));
					xianzhen_flag=true;
				}
				tu.setXianzhen(xianzhen_flag);
			}
		});
	}
	public void setButtonBodong() {
		btnBodong.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i("20_prj","btnBodong");
				if(bodong_flag)
				{
					btnBodong.setText(g_ctx.getString(R.string.pc));
					bodong_flag=false;
					HardwareControl.wrSPI(cmd_switch_to_usb);
					btnDanci.setEnabled(false);
					btnDuoci.setEnabled(false);
					btnXianzhen.setEnabled(false);
					btnStop.setEnabled(false);
				}
				else
				{
					btnBodong.setText(g_ctx.getString(R.string.arm));
					bodong_flag=true;
					HardwareControl.wrSPI(cmd_switch_to_spi);
					btnDanci.setEnabled(true);
					btnDuoci.setEnabled(true);
					btnXianzhen.setEnabled(true);
					btnStop.setEnabled(true);
				}
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
        for(int i=0;i<Y_Max;i++)
        xRawDatas.add(String.valueOf(i));  
        draw_cuve(fpga_data);
	}
	void create_fpga_data(int[] cuve)
	{
		Random random=new Random();
		for(int i=0;i<Y_Max;i++)
			cuve[i]=random.nextInt(73727);
	}
	void draw_cuve(int[] cuve)
	{
		//create_fpga_data(cuve);		
		yList.clear();
		for(int j=0;j<Y_Max;j++)
        {
        	yList.add((double) cuve[j]);
        	//Log.i("DATA", String.valueOf(cuve[j]));
        }
		//Log.i("20_prj", "Spi "+byte2HexStr(cuve,5));
        //tu.setData(yList, xRawDatas, 73727, 8192);
		tu.setData(yList, xRawDatas, 65536, 4096);
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
		btnBodong =(Button)findViewById(R.id.Bbodong);
		editJifen=(EditText)findViewById(R.id.Tjifentime);
		editGonglv=(EditText)findViewById(R.id.Tgonglv);
		
		setButtonDanci();
		setButtonDuoci();
		setButtonStop();
		setButtonJiguang();
		setButtonXianzhen();
		setButtonBodong();
		SimpleDateFormat sDateFormat_date = new SimpleDateFormat("yyyy-MM-dd");
		date = sDateFormat_date.format(new java.util.Date());
		
		imageviewdianchi = (ImageView) findViewById(R.id.imageviewpower);
		HardwareControl.init();
		mInputStream = HardwareControl.getInputStream();
		mOutputStream = HardwareControl.getOutputStream();
		mReadThread = new ReadThread();
		mReadThread.start();
		//HardwareControl.wrSPI(cmd_e);
		HardwareControl.wrSPI(cmd_real_data);
		HardwareControl.wrSPI(cmd_switch_to_spi);
		handler.postDelayed(task, 1000);
		//Thread th = new TestThread();
		//th.start();
		HandlerThread ht = new HandlerThread("MyThread");  
        ht.start();  
        handlerUI = new Handler(ht.getLooper(), new Handler.Callback() {  
            @Override  
            public boolean handleMessage(Message msg) {  
                // TODO Auto-generated method stub  
                int tmp = msg.arg1;  
                System.out.println(Thread.currentThread().getId() + "ccxcxc" + tmp);  
                return false;  
            }  
        });  
        handlerUI.post(mUpdateResults);
		init_draw2();
	}
}
