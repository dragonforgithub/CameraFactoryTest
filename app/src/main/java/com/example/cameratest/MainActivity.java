package com.example.cameratest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	private static final String TAG = "CameraTestApp";
	public SurfaceView surfaceView;
	public SurfaceView surfaceView1;
	public CameraPreview mPreview;
	public CameraPreview mPreview1;

	public TextView previewTV;
	public TextView previewTV_1;

	Camera mCamera = null;
	Camera mCamera1= null;

	private int mOpenCamIndex =-1;
	private int mOpenCamIndex1 =-1;

	private boolean mRear0Exist=false;
	private boolean mFrontExist=false;
	private boolean mRear1Exist=false;
	private int mCameraNumber=0;
	private int mPicOrientation=0;
	private int mDisOrientation=0;

	String mSavaPath = "";
	String mLogPath = "";
	private boolean mbTkPicture = false;
	private boolean mbTkPicture_1 = false;
	CamReceiver mCamReceiver =null;
	private int mCameraMode=-1;

	private boolean isSupportFlash=false;
	private boolean isSupportFlash_1=false;
	private boolean isSupportFocuse=false;
	private boolean isSupportFocuse_1=false;

    public Handler mHandler;
	private MyOrientationDetector mOrientationListener = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.main);

		//Read Project ID------
		/*
		Process process_PJ = null;
		try {
			process_PJ = Runtime.getRuntime().exec("getprop ro.pro.apid");
		} catch (IOException e) {
			e.printStackTrace();
		}

		InputStreamReader ir_PJ = new InputStreamReader(process_PJ.getInputStream());
		BufferedReader input_PJ = new BufferedReader(ir_PJ);

		try {
			String apid = input_PJ.readLine();
			Log.e(TAG, "apid = "+ apid);
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/

		//Read Module Name------
		File file_RearModule_1=new File("/proc/driver/RearModule");
		File file_FrontModule=new File("/proc/driver/FrontModule");
		File file_RearModule_2=new File("/proc/driver/RearModule2");

		if(file_RearModule_1.exists()) {
			mRear0Exist=true;
		}else {
			Log.e(TAG,"RearModule not Exist!");
		}

		if(file_FrontModule.exists()) {
			mFrontExist=true;
		}else {
			Log.e(TAG,"FrontModule not Exist!");
		}

		if(file_RearModule_2.exists()) {
			mRear1Exist=true;
		}else {
			Log.e(TAG,"RearModule2 not Exist!");
		}

		//get current camera number and information
		mCameraNumber = Camera.getNumberOfCameras();
		Log.e(TAG,"exist camera number : "+mCameraNumber);


		String Model=Build.MODEL;
		Log.v(TAG, "phone model is "+Model);

		Intent intent = getIntent();
		mCameraMode = intent.getIntExtra("cameramode", -1);
		mLogPath = intent.getStringExtra("logpath");

		if(mCameraMode==-1 || mLogPath==null || mCameraNumber==0){
			finish();
			return;
		}

		Log.i(TAG,"mOrientationListener start:");
		mOrientationListener = new MyOrientationDetector(this);
		mOrientationListener.enable();

		Log.v(TAG, "CameraID="+mCameraMode+";log path="+mLogPath);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "onPause");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		List<String> supportedFlashMode;
		List<String> supportedFlashMode_1;
		List<String> supportedFocuseMode;
		List<String> supportedFocuseMode_1;

		try {

			previewTV = (TextView) findViewById(R.id.previewName);
			previewTV_1 = (TextView) findViewById(R.id.previewName_1);

			Parameters parameters = null;
			Parameters parameters_1 = null;
			switch (mCameraMode){
				case 0 : //rear0 & rear1
					if(mRear0Exist && mRear1Exist){
						mOpenCamIndex = FindBackCamera0();
						mCamera = Camera.open(mOpenCamIndex);
						parameters = mCamera.getParameters();

						mOpenCamIndex1 = FindBackCamera1();
						mCamera1 = Camera.open(mOpenCamIndex1);
						parameters_1 = mCamera1.getParameters();
						Log.i(TAG, "open rear two cameraS!");
					}else {
						Log.e(TAG, "mRear0Exist:"+mRear0Exist+",mRear1Exist:"+mRear1Exist);
						finish();
					}
					break;
				case 1 : //rear0
					previewTV.setText("RearCamera0");
					previewTV_1.setVisibility(View.INVISIBLE);

					if(mRear0Exist){
						mOpenCamIndex = FindBackCamera0();
						mCamera = Camera.open(mOpenCamIndex);
						parameters = mCamera.getParameters();
						Log.i(TAG, "open rear 0 camera!");
					}else {
						Log.e(TAG, "No Rear0 Camera!");
						finish();
					}
					break;
				case 2 : //rear1
					previewTV.setText("RearCamera1");
					previewTV_1.setVisibility(View.INVISIBLE);

					if(mRear1Exist){
						mOpenCamIndex = FindBackCamera1();
						mCamera = Camera.open(mOpenCamIndex);
						parameters = mCamera.getParameters();
						Log.i(TAG, "open rear 1 camera!");
					}else {
						Log.e(TAG, "No Rear1 Camera!");
						finish();
					}
					break;
				case 3 : //front
					previewTV.setText("FrontCamera");
					previewTV_1.setVisibility(View.INVISIBLE);
					if(mFrontExist){
						mOpenCamIndex = FindFrontCamera();
						mCamera = Camera.open(mOpenCamIndex);
						parameters = mCamera.getParameters();
						Log.i(TAG, "open front camera!");
					}else {
						Log.e(TAG, "No Front Camera!");
						finish();
					}
					break;
				default:
					Log.e(TAG, "Invalid Camera ID!");
					break;
			}

			supportedFlashMode = parameters.getSupportedFlashModes();
			if(supportedFlashMode == null || supportedFlashMode.isEmpty()){
				isSupportFlash = false;
				Log.e(TAG, "supportedFlashMode : Null");
			}else {
				isSupportFlash = true;
				for(int i=0 ;i<supportedFlashMode.size(); i++){
					Log.e(TAG, "supportedFlashMode : "+supportedFlashMode.get(i).toString());
				}
			}

			if(mCameraMode == 0 && parameters_1 != null) {
				supportedFlashMode_1 = parameters_1.getSupportedFlashModes();
				if(supportedFlashMode_1 == null || supportedFlashMode_1.isEmpty()){
					isSupportFlash_1 = false;
					Log.e(TAG, "supportedFlashMode : Null");
				}else {
					isSupportFlash_1 = true;
					for(int i=0 ;i<supportedFlashMode_1.size(); i++){
						Log.e(TAG, "supportedFlashMode_1 : "+supportedFlashMode_1.get(i).toString());
					}
				}
			}

			supportedFocuseMode = parameters.getSupportedFocusModes();
			if(supportedFocuseMode == null || supportedFocuseMode.isEmpty()){
				isSupportFocuse = false;
				Log.e(TAG, "supportedFocuseMode : Null");
			}else {
				for(int i=0;i<supportedFocuseMode.size();i++){
					Log.e(TAG, "supportedFocuseMode : "+supportedFocuseMode.get(i).toString());
					if(supportedFocuseMode.get(i).toString().equals("auto")){
						isSupportFocuse = true;
					}
				}
			}

			if(mCameraMode == 0 && parameters_1 != null) {
				supportedFocuseMode_1 = parameters_1.getSupportedFocusModes();
				if(supportedFocuseMode_1 == null || supportedFocuseMode_1.isEmpty()){
					isSupportFocuse_1 = false;
					Log.e(TAG, "supportedFocuseMode_1 : Null");
				}else {
					for(int i=0;i<supportedFocuseMode_1.size();i++){
						Log.e(TAG, "supportedFocuseMode_1 : "+supportedFocuseMode_1.get(i).toString());
						if(supportedFocuseMode_1.get(i).toString().equals("auto")){
							isSupportFocuse_1 = true;
						}
					}
				}
			}

			Log.i(TAG,"set default rotation:");
			//set default rotation
			mDisOrientation=90;
			switch (mCameraMode){
				case 0 : //rear0&rear1
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(mDisOrientation);

					parameters_1.setRotation(0);
					mCamera1.setDisplayOrientation(mDisOrientation);
					break;
				case 1 : //rear0
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(mDisOrientation);
					break;
				case 2 : //rear1
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(mDisOrientation);
					break;
				case 3 : //front
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(mDisOrientation);
					break;
				default:
					Log.e(TAG,"error mCameraMode!");
					break;
			}

			//parameters.setPictureFormat(256);  //0x11:NV21 / 0x100 : JPEG
			if(mCameraMode == 0 && mCamera1 != null) {
				mCamera.setParameters(parameters);
				surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
				mPreview = new CameraPreview(this, mCamera ,surfaceView, mOpenCamIndex, mCameraMode);
				mPreview.setlogPath(mLogPath);

				mCamera1.setParameters(parameters_1);
				surfaceView1 = (SurfaceView) findViewById(R.id.camera_preview1);
				mPreview1 = new CameraPreview(this, mCamera1 ,surfaceView1, mOpenCamIndex1, mCameraMode);
				mPreview1.setlogPath(mLogPath);
			}else {
				mCamera.setParameters(parameters);
				surfaceView = (SurfaceView) findViewById(R.id.camera_preview2);
				mPreview = new CameraPreview(this, mCamera ,surfaceView, mOpenCamIndex, mCameraMode);
				mPreview.setlogPath(mLogPath);
			}

			wlog("camera open finish");
			Log.i(TAG,"onResume done.");
		} catch (Exception e) {
			// TODO: handle exception
			 wlog("camera open fail");
			 wlog(e.getMessage());
			 Toast.makeText(this, "camera open fail", Toast.LENGTH_LONG).show();
			 finish();
			 return;
		}

		if(mHandler == null){
			    Log.v(TAG, "mCamReceiver==null");

			    mHandler = new Handler() {
				public void handleMessage(Message msg)
				{
					switch (msg.what) {

						case HandleMsg.MSG_CLOSE_CAMERA:

							onDestroy();
							break;

						case HandleMsg.MSG_SET_SAVE_PATH:

							mSavaPath=(String)msg.obj;

							break;
						case HandleMsg.MSG_TAKE_PIC:

							int focusneed=-1,rawneed=-1;

							focusneed=msg.arg1;
							rawneed=msg.arg2;

							takePic(focusneed,rawneed);
							break;

						case HandleMsg.MSG_SET_PARAMETER:

							Bundle bundle=msg.getData();
							int flashmode= bundle.getInt("flashmode");
							int focusmode= bundle.getInt("focusmode");
							//int zoom= bundle.getInt("zoom");
							//mCamera.stopPreview();
							//if(mCameraMode == 0 && mCamera1 != null){
							//	mCamera1.stopPreview();
							//}
							Parameters parameters = mCamera.getParameters();
							if(flashmode!=-1)
							{
								switch(flashmode){
									case 0:
										if(isSupportFlash){
											parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
											mCamera.setParameters(parameters);
										}

										if(isSupportFlash_1 && mCameraMode == 0 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFlashMode(Parameters.FLASH_MODE_OFF);
											mCamera1.setParameters(parameters_1);
										}
										break;
									case 1:
										if(isSupportFlash){
											parameters.setFlashMode(Parameters.FLASH_MODE_ON);
											mCamera.setParameters(parameters);
										}

										if(isSupportFlash_1 && mCameraMode == 0 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFlashMode(Parameters.FLASH_MODE_ON);
											mCamera1.setParameters(parameters_1);
										}
										break;
									case 2:
										if(isSupportFlash){
											parameters.setFlashMode(Parameters.FLASH_MODE_AUTO);
											mCamera.setParameters(parameters);
										}

										if(isSupportFlash_1 && mCameraMode == 0 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFlashMode(Parameters.FLASH_MODE_AUTO);
											mCamera1.setParameters(parameters_1);
										}
										break;
								}
							}

							if(focusmode!=-1)
							{
								switch(focusmode){
									case 0:
										if(isSupportFocuse){
											parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
											mCamera.setParameters(parameters);
											setFocusArea(mCameraMode);
											Log.e(TAG, "do Focus :");
											mCamera.autoFocus(mAutoFocusCallbackParameter);
										}

										if(isSupportFocuse_1 && mCameraMode == 0 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFocusMode(Parameters.FOCUS_MODE_AUTO);
											mCamera1.setParameters(parameters_1);
											setFocusArea(mCameraMode);
											Log.e(TAG, "do Focus tele :");
											mCamera1.autoFocus(mAutoFocusCallbackPrameter_1);
										}
										break;
									case 1:
										if(isSupportFocuse){
											parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
											mCamera.setParameters(parameters);
										}

										if(isSupportFocuse_1 && mCameraMode == 0 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
											mCamera1.setParameters(parameters_1);
										}
										break;
								}
							}

							//mCamera.startPreview();
							//if(mCameraMode == 0 && mCamera1 != null){
							//	mCamera1.startPreview();
							//}
							break;

						case HandleMsg.SET_EXP:
							int exp=msg.arg1;
							int cameraIDexp=msg.arg2;
							Log.i(TAG,"exp = "+exp+",cameraIDexp = "+cameraIDexp);
							switch(mCameraMode){
								case 0:
									if(cameraIDexp == 0) {
										mPreview.setExp(exp);
									}else if(cameraIDexp == 2) {
										mPreview1.setExp(exp);
									}
									break;
								default:
									mPreview.setExp(exp);
									break;
							}
							break;

						case HandleMsg.SET_ISO:
							int iso=msg.arg1;
							int cameraIDiso=msg.arg2;
							Log.i(TAG,"iso = "+iso+",cameraIDiso = "+cameraIDiso);
							switch(mCameraMode){
								case 0:
									if(cameraIDiso == 0) {
										mPreview.setiso(iso);
									}else if(cameraIDiso == 2) {
										mPreview1.setiso(iso);
									}
									break;
								default:
									mPreview.setiso(iso);
									break;
							}
							break;

						default:
							Log.e(TAG, "mCamReceiver error message!");
							break;
					}
				}
			};
		}

		if(mCamReceiver!=null){
			mCamReceiver.setHandler(mHandler);
			Log.v(TAG, "mCamReceiver!=null");
		}
		else {
			Log.v(TAG, "mCamReceiver==null");
			mCamReceiver=new CamReceiver();
			this.registerReceiver(mCamReceiver,new IntentFilter("android.user.camera_preview"),null,mHandler);
			mCamReceiver.setHandler(mHandler);
			Log.v(TAG, "setHandler done.");
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.v(TAG,"onDestroy enter");

		if(mOrientationListener != null){
			mOrientationListener.disable();
		}

		if(mCameraMode == 0) {
			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.lock();
				mCamera.release();
				mCamera = null;
			}

			if(mCamera1 != null){
				mCamera1.stopPreview();
				mCamera1.setPreviewCallback(null);
				mCamera1.lock();
				mCamera1.release();
				mCamera1 = null;
			}
		}else {
			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.lock();
				mCamera.release();
				mCamera = null;
			}
		}

		if(mCamReceiver!=null){
			this.unregisterReceiver(mCamReceiver);
			mCamReceiver=null;
		}

		Log.e(TAG,"release camera done.");
		wlog("close camera finish");
		finish();
	}

	private int FindBackCamera0() {
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int camIdx = 0; camIdx < mCameraNumber; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				Log.e(TAG,"FindBackCamera0 index =  "+camIdx);
				return camIdx;
			}
		}
		return -1;
	}

	private int FindBackCamera1() {
		boolean findFirst;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		findFirst=false; //init
		for (int camIdx = 0; camIdx < mCameraNumber; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
				if(findFirst == true || !mRear0Exist){
					Log.e(TAG,"FindBackCamera1 index =  "+camIdx);
					return camIdx;
				}else {
					findFirst=true;
				}
			}
		}
		return -1;
	}

	private int FindFrontCamera() {
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int camIdx = 0; camIdx < mCameraNumber; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				Log.e(TAG,"FindFrontCamera index =  "+camIdx);
				return camIdx;
			}
		}
		return -1;
	}

	public class MyOrientationDetector extends OrientationEventListener {
		public MyOrientationDetector( Context context ) {
			super(context );
		}
		@Override
		public void onOrientationChanged(int orientation) {
			//Log.i("MyOrientationDetector ","onOrientationChanged:"+orientation);
			if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN || mbTkPicture || mbTkPicture_1) {
				return;  //手机平放或者正在拍照，返回
			}

			//只检测是否有四个角度的改变
			if( orientation > 315 || orientation <= 45 ) { //0度
				mPicOrientation = 0;
			}
			else if( orientation > 45 &&orientation <= 135 ) { //90度
				mPicOrientation= 90;
			}
			else if( orientation > 135 &&orientation <= 225 ) { //180度
				mPicOrientation= 180;
			}
			else if( orientation > 225 &&orientation <= 315  ) { //270度
				mPicOrientation= 270;
			}
			else {
				Log.e(TAG,"Invalid orientation!");
				return;
			}

			Log.e(TAG,"mPicOrientation:"+mPicOrientation);

			if(mPicOrientation == 0){
				mPicOrientation = 90;
			} else if(mPicOrientation == 180){
				mPicOrientation = 270;
			}

			Parameters parameters = mCamera.getParameters();
			if (mCameraMode == 0 && mCamera1 != null) {  //rear0 & rear1
				mPicOrientation = (mPicOrientation+90)%360;

				parameters.setRotation(mPicOrientation);
				mCamera.setParameters(parameters);

				Parameters parameters1 = mCamera1.getParameters();
				parameters1.setRotation(mPicOrientation);
				mCamera1.setParameters(parameters1);
			}
			else if(mCameraMode == 1){ //rear0
				mPicOrientation = (mPicOrientation+90)%360;
				parameters.setRotation(mPicOrientation);
				mCamera.setParameters(parameters);
			}
			else if(mCameraMode == 2){ //rear1
				mPicOrientation = (mPicOrientation+90)%360;
				parameters.setRotation(mPicOrientation);
				mCamera.setParameters(parameters);
			}
			else if(mCameraMode == 3){ //front
				if(mPicOrientation == 0){
					mPicOrientation = 180;
				}else if(mPicOrientation == 180){
					mPicOrientation = 0;
				}
				mPicOrientation = (mPicOrientation+90)%360;
				parameters.setRotation(mPicOrientation);
				mCamera.setParameters(parameters);
			}
			else {
				Log.e(TAG,"error camera mode!");
			}

			Log.i(TAG,"set picture Orientation:"+mPicOrientation);
			//Log.i(TAG,"set display Orientation:"+mDisOrientation);
		}
	}

	AutoFocusCallback mAutoFocusCallback=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "mAutoFocusCallback:");
			if(success==false){
				Log.e(TAG, "auto focus fail");
				wlog("auto focus fail");
			}

			if(mCameraMode == 0 && mCamera1 != null) {
				if(isSupportFocuse_1){
					Log.e(TAG, "is SupportFocuse_1 :");
					mCamera1.autoFocus(mAutoFocus_1Callback);
				}else {
					Log.e(TAG, "not SupportFocuse_1 :");
					mCamera.takePicture(null, null, mPictureCallback);
				}
			}else{
				mCamera.takePicture(null, null, mPictureCallback);
			}
		}
	};

	AutoFocusCallback mAutoFocusCallbackParameter=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "mAutoFocusCallback:parameter");
			if(success==false){
				Log.e(TAG, "parameter auto focus fail");
				wlog("parameter auto focus fail");
			}
		}
	};

	AutoFocusCallback mAutoFocusCallbackPrameter_1=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "mAutoFocusCallback_1:parameter");
			if(success==false){
				Log.e(TAG, "parameter_1 auto focus fail");
				wlog("parameter_1 auto focus fail");
			}
		}
	};

	AutoFocusCallback mAutoFocus_1Callback=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "mAutoFocus_1Callback:");
			if(success==false){
				Log.e(TAG, "auto focus_1 fail");
				wlog("auto focus_1 fail");
			}
			//mCamera1.stopPreview();
			mCamera.takePicture(null, null, mPictureCallback);
		}
	};

	PictureCallback mPictureCallback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onPictureTaken");
			String savaPath = mSavaPath;
			try {
				if (data != null && mSavaPath != null){
					if(mCameraMode == 0){
						savaPath = mSavaPath+"_1.jpg";
					}
					File rawOutput = new File(savaPath);
					FileOutputStream outStream = new FileOutputStream(rawOutput);
					outStream.write(data);
					outStream.close();
				}
			}catch(IOException e){
				mbTkPicture=false;
				wlog(e.getMessage());
			}

			Log.e(TAG, "takePicture finish to : "+savaPath);
			wlog("takePicture finish");
			mbTkPicture=false;

			if(mCameraMode == 0 && mCamera1 != null) {
				//mCamera.stopPreview();
				//mCamera1.startPreview();
				Log.i(TAG,"take picture_1 : ");
				mCamera1.takePicture(null, null, mPicture_1Callback);
			}else{
				//mCamera.startPreview();
				try {
					//android读取图片EXIF信息
					ExifInterface exifInterface=new ExifInterface(savaPath);
					String picISO=exifInterface.getAttribute(ExifInterface.TAG_ISO);
					String picEXP=exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
					Toast.makeText(MainActivity.this,"ISO:"+picISO+","+"EXP:"+picEXP, Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	PictureCallback mPicture_1Callback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "onPictureTaken_1");
			String savaPath="";
			try {
				if (data != null && mSavaPath != null){
					savaPath = mSavaPath+"_2.jpg";
					File rawOutput = new File(savaPath);
					FileOutputStream outStream = new FileOutputStream(rawOutput);
					outStream.write(data);
					outStream.close();
				}
			}catch(IOException e){
				mbTkPicture_1=false;
				wlog(e.getMessage());
			}

			Log.e(TAG, "takePicture_1 finish to : "+savaPath);
			wlog("takePicture_1 finish");
			mbTkPicture_1=false;
			//mCamera.startPreview();
			mCamera1.startPreview();
		}
	};

	PictureCallback mRawPictureCallback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "takeRawPicture");
			wlog("takeRawPicture finish");
			mbTkPicture=false;

			if(mCameraMode == 0 && mCamera1 != null) {
				//mCamera.stopPreview();
				//mCamera1.startPreview();
				mCamera1.takePicture(null, null, mRawPicture_1Callback);
			}
		}
	};

	PictureCallback mRawPicture_1Callback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.e(TAG, "takeRawPicture_1");
			wlog("takeRawPicture_1 finish");
			mbTkPicture_1=false;
			//mCamera.startPreview();
			mCamera1.startPreview();
		}
	};

		public  void wlog(String msg)
		{
			if(msg!=null){
				Log.v(TAG, msg+" to "+mLogPath);
			}
			WriteLog.appendLog(getApplicationContext(),msg, mLogPath);
		}


		private void takePic(int focus_need,int raw_need)
		{
			Log.e(TAG,"takePic enter:");
			if(mSavaPath==null||mSavaPath.length()==0){
				mbTkPicture=false;
				mbTkPicture_1=false;
				Toast.makeText(MainActivity.this, "picture path no input parameter", Toast.LENGTH_LONG).show();
				return;
			}

			if(mbTkPicture && mbTkPicture_1){
				Toast.makeText(MainActivity.this, "last take picture failed,retry...", Toast.LENGTH_LONG).show();
				//if you need protect take picture, open this:
				//return;
			}else{
				mbTkPicture = true;
				if(mCameraMode == 0 && mCamera1 != null) {
					mbTkPicture_1 = true;
				}
			}

			Parameters parameters = mCamera.getParameters();
			/*List<Integer> mSupportedPictureFormats = parameters.getSupportedPictureFormats();
			for (int i = 0; i < mSupportedPictureFormats.size(); ++i) {
				Integer format = mSupportedPictureFormats.get(i);
				Log.i(TAG,"format is "+format);
			}

			if(mCameraMode == 0 && mCamera1 != null) {
				Parameters parameters_1 = mCamera1.getParameters();
				List<Integer> mSupportedPictureFormats_1 = parameters_1.getSupportedPictureFormats();
				for (int i = 0; i < mSupportedPictureFormats_1.size(); ++i) {
					Integer format_1 = mSupportedPictureFormats_1.get(i);
					Log.i(TAG,"format_1 is "+format_1);
				}
			}
			*/
			parameters.setPictureFormat(256); //0x11:NV21 / 0x100 : JPEG
			mCamera.setParameters(parameters);

			if(mCameraMode == 0 && mCamera1 != null) {
				Parameters parameters_1 = mCamera1.getParameters();
				parameters_1.setPictureFormat(256); //0x11:NV21 / 0x100 : JPEG
				mCamera1.setParameters(parameters_1);
			}

			if(raw_need==1)
			{
				try {
					//mCamera.stopPreview();
					wlog("take raw pic");
					//if(mCameraMode == 0 && mCamera1 != null) {
					//	mCamera1.stopPreview();
					//}
					//mCamera.startPreview();
					mCamera.takePicture(null, null, mRawPictureCallback);
				} catch (Exception e) {
					// TODO: handle exception
					mbTkPicture=false;
					mbTkPicture_1=false;
					Log.v(TAG, e.getMessage());
					wlog(e.getMessage());
				}
			}
			else if(focus_need==1 && isSupportFocuse)
			{

				mCamera.cancelAutoFocus(); //reset focusState=0
				parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
				mCamera.setParameters(parameters);

				if(mCameraMode == 0 && mCamera1 != null && isSupportFocuse_1) {
					mCamera1.cancelAutoFocus(); //reset focusState=0
					Parameters parameters_1 = mCamera1.getParameters();
					parameters_1.setFocusMode(Parameters.FOCUS_MODE_AUTO);
					mCamera1.setParameters(parameters_1);
				}

				try {
						setFocusArea(mCameraMode);
						Log.e(TAG, "setFocusArea end");
						mCamera.autoFocus(mAutoFocusCallback);
						Log.e(TAG, "autoFocus end");
				} catch (Exception e) {
						// TODO: handle exception
						Log.e(TAG, "autoFocus error!");
						mbTkPicture=false;
						mbTkPicture_1=false;
						wlog(e.getMessage());
				}
			}
			else{
				try {
						Log.i(TAG,"take picture start without af");
						//if(mCameraMode == 0 && mCamera1 != null) {
						//	mCamera1.stopPreview();
						//}
						mCamera.takePicture(null, null, mPictureCallback);
				} catch (Exception e) {
						// TODO: handle exception
						mbTkPicture=false;
						mbTkPicture_1=false;
						Log.v(TAG, e.getMessage());
						wlog(e.getMessage());
				}
			}
		}

		public void setFocusArea(int CameraMode)
		 {
			if(Build.VERSION.SDK_INT>=14 )
			{
				Parameters parameters=mCamera.getParameters();
				int focusAreaNum=parameters.getMaxNumFocusAreas() ;
				Log.e(TAG,"focusAreaNum="+focusAreaNum+", CameraMode="+CameraMode);
				if(focusAreaNum>0 )
				{
					/*
					List<Area> areallist=parameters.getFocusAreas();
					Log.e(TAG,"areallist="+areallist);
					if(areallist!=null){
						Log.i(TAG, "size="+areallist.size());
						for (int i = 0; i < areallist.size(); i++)
						{
							Area area=areallist.get(i);
							Rect rect=area.rect;
							Log.i(TAG,"test rect left="+rect.left+"right"+rect.right+"top="+rect.top+"bottom="+rect.bottom);
						}
					}else{
					*/
						ArrayList<Area> focusArea = new ArrayList<Area>();
						focusArea.add(new Area(new Rect(), 1000));
						Log.e(TAG,"set focusArea start:");
						if(CameraMode == 0 && mCamera1 != null){
							focusArea.get(0).rect.set(-500, -250, 0, 250);
							parameters.setFocusAreas(focusArea);
							mCamera.setParameters(parameters);

							Parameters parameters_1=mCamera1.getParameters();
							focusArea.get(0).rect.set(0, -250, 500, 250);
							parameters_1.setFocusAreas(focusArea);
							mCamera1.setParameters(parameters_1);
						}else{
							focusArea.get(0).rect.set(-250, -250, 250, 250);
							parameters.setFocusAreas(focusArea);
							mCamera.setParameters(parameters);
						}
						Log.e(TAG,"set focusArea end.");
					//}
				}

				int meteringAreaNum = parameters.getMaxNumMeteringAreas();
				Log.e(TAG,"max metering area = " + meteringAreaNum);
				if (meteringAreaNum > 0) {
					/*
					List<Area> meteringAreaList = parameters.getMeteringAreas();
					if (meteringAreaList != null) {
						Log.v(TAG, "metering areas size =" + meteringAreaList.size());
						for (int i = 0; i < meteringAreaList.size(); i++) {
							Area area = meteringAreaList.get(i);
							Rect rect = area.rect;
							Log.v(TAG,"test rect left="+rect.left+"right"+rect.right+"top="+rect.top+"bottom="+rect.bottom);
						}
					} else {
					*/
						ArrayList<Area> meteringArea = new ArrayList<Area>();
						meteringArea.add(new Area(new Rect(), 1000));

						if(mCameraMode == 0 && mCamera1 != null) {
							meteringArea.get(0).rect.set(-500, -250, 0, 250);
							parameters.setMeteringAreas(meteringArea);
							mCamera.setParameters(parameters);

							Parameters parameters_1=mCamera1.getParameters();
							meteringArea.get(0).rect.set(0, -250, 500, 250);
							parameters_1.setMeteringAreas(meteringArea);
							mCamera1.setParameters(parameters_1);
						}else{
							meteringArea.get(0).rect.set(-250, -250, 250, 250);
							parameters.setMeteringAreas(meteringArea);
							mCamera.setParameters(parameters);
						}
					//}
				}
			}
		}
}
