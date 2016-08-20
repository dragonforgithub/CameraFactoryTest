package com.example.cameratest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class WriteLog {

	private static String TAG="WriteLog";
	public static void appendLog(Context context,String text,String Path)
	{
		if(Path==null)
		{
			if(context!=null)
				//Toast.makeText(context,"log Path null", Toast.LENGTH_LONG).show();
			return;
		}
		Log.v(TAG, "appendLog="+Path);
		//SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		//String currentDateandTime = sdf.format(new Date());
		//text=currentDateandTime+"------------>"+text;
		File logFile = new File(Path);
		if (!logFile.exists())
		{
	      Log.v(TAG, "logFile not exist");
		   try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	         Log.e(TAG, "logFile creat fail : "+ e.getMessage());
	         Toast.makeText(context,"create file result="+e.getMessage(), Toast.LENGTH_LONG).show();
	      }
	   }

	   try
	   {
	      //BufferedWriter for performance, true to set append to file flag
	      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	      buf.append(text);
	      buf.newLine();
	      buf.close();
	   }
	   catch (IOException e)
	   {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	      Log.e(TAG, "io fail="+ e.getMessage());
	   }

		Log.v(TAG, "wlog done!");
	}
	
}
