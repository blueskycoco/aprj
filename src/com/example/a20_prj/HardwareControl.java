package com.example.a20_prj;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.util.Log;

public class HardwareControl {
	private static FileDescriptor mFd;
	private static FileInputStream mFileInputStream;
	private static FileOutputStream mFileOutputStream;
	private static final String TAG = "SerialPort";
	public native static byte[] wrSPI();
	public native static FileDescriptor openSerial();
	public native static void closeSerial();	
	public native static  int getBattery();
	static {
		System.loadLibrary("a20");
	}
	
	public static boolean init()
	{
		mFd = openSerial();
		if (mFd == null) {
			Log.e(TAG, "native open returns null");
			return false;
		}
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
		return true;
	}
	public static InputStream getInputStream() {
		return mFileInputStream;
	}

	public static OutputStream getOutputStream() {
		return mFileOutputStream;
	}
}