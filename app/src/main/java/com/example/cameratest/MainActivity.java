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
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
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

	private int mCameraNumber=0;
	private int mPicOrientation=0;

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


		//get current camera number and information
		mCameraNumber = Camera.getNumberOfCameras();
		Log.i(TAG,"exist camera number : "+mCameraNumber);

		String Model=Build.MODEL;
		Log.v(TAG, "phone model is "+Model);

		Intent intent = getIntent();
		//先清除堆顶的Activity，然后再以singleTop的启动模式打开，这样生命周期就从onCreate()开始调用。
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		mCameraMode = intent.getIntExtra("cameramode", -1);
		mLogPath = intent.getStringExtra("logpath");
		Log.i(TAG, "CameraID="+mCameraMode+";log path="+mLogPath);

		if(mCameraMode==-1 || mLogPath==null || mCameraNumber==0){
			System.exit(0);
		}

		Log.i(TAG,"mOrientationListener start:");
		mOrientationListener = new MyOrientationDetector(this);
		mOrientationListener.enable();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "onPause : release camera");
		if(mOrientationListener != null){
			mOrientationListener.disable();
			mOrientationListener=null;
		}

		if(mCamera1 != null) {
			if(mCamera != null && mCamera1 != null){
				mCamera1.setPreviewCallback(null);
				mCamera.setPreviewCallback(null);
				mCamera1.release();
				mCamera.release();
				mCamera1 = null;
				mCamera = null;
			}
		}else {
			if(mCamera != null){
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}

		Log.e(TAG,"onDestroy&System.exit(0):");
		super.onDestroy();
		System.exit(0);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		Log.v(TAG,"onDestroy enter : release camera");
		if(mOrientationListener != null){
			mOrientationListener.disable();
			mOrientationListener=null;
		}

		if(mCamReceiver!=null){
			this.unregisterReceiver(mCamReceiver);
			mCamReceiver=null;
		}


		if(mCamera1 != null) {
			if(mCamera != null && mCamera1 != null){
				mCamera1.stopPreview();
				mCamera.stopPreview();
				mCamera1.setPreviewCallback(null);
				mCamera.setPreviewCallback(null);
				mCamera1.release();
				mCamera.release();
				mCamera1 = null;
				mCamera = null;
			}
		}else {
			if(mCamera != null){
				mCamera.stopPreview();
				mCamera.setPreviewCallback(null);
				mCamera.release();
				mCamera = null;
			}
		}

		Log.e(TAG,"onDestroy&System.exit(0):");
		super.onDestroy();

		//execute the task
		wlog("close camera finish");
		System.exit(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "onResume mode : "+mCameraMode);
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
					previewTV.setText("RearCamera0");
					previewTV.setVisibility(View.VISIBLE);
					mOpenCamIndex = 0;//FindBackCamera0();
					mCamera = Camera.open(mOpenCamIndex);
					//Thread.sleep(300);
					parameters = mCamera.getParameters();
					Log.i(TAG, "open main camera!");

					previewTV_1.setText("RearCamera1");
					previewTV_1.setVisibility(View.VISIBLE);
					mOpenCamIndex1 = 2;//FindBackCamera1();
					mCamera1 = Camera.open(mOpenCamIndex1);
					//Thread.sleep(300);
					parameters_1 = mCamera1.getParameters();
					Log.i(TAG, "open sub camera!");
					break;
				case 1 : //rear0
					previewTV.setText("RearCamera");
					previewTV_1.setVisibility(View.INVISIBLE);

					mOpenCamIndex = 0;//FindBackCamera0();
					mCamera = Camera.open(mOpenCamIndex);
					//Thread.sleep(300);
					parameters = mCamera.getParameters();
					Log.i(TAG, "open rear 0 camera!");
					break;
				case 2 : //rear1 || front1
					previewTV.setText("SubCamera");
					previewTV_1.setVisibility(View.INVISIBLE);

					mOpenCamIndex = 2;//FindBackCamera1();
					mCamera = Camera.open(mOpenCamIndex);
					//Thread.sleep(300);
					parameters = mCamera.getParameters();
					Log.i(TAG, "open rear 1 camera!");
					break;
				case 3 : //front0
					previewTV.setText("FrontCamera");
					previewTV_1.setVisibility(View.INVISIBLE);

					mOpenCamIndex = 1;//FindFrontCamera();
					mCamera = Camera.open(mOpenCamIndex);
					//Thread.sleep(300);
					parameters = mCamera.getParameters();
					Log.i(TAG, "open front camera!");
					break;
				case 4 : //front0 & front1
 					previewTV.setText("FrontCamera0");
					previewTV.setVisibility(View.VISIBLE);
					mOpenCamIndex = 1;
					mCamera = Camera.open(mOpenCamIndex);
					parameters = mCamera.getParameters();
					Log.i(TAG, "open main camera!");

					//Thread.sleep(500);
					previewTV_1.setText("FrontCamera1");
					previewTV_1.setVisibility(View.VISIBLE);
					mOpenCamIndex1 = 2;
					mCamera1 = Camera.open(mOpenCamIndex1);
					parameters_1 = mCamera1.getParameters();
					Log.i(TAG, "open sub camera!");
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
				//for(int i=0 ;i<supportedFlashMode.size(); i++){
				//	Log.i(TAG, "supportedFlashMode : "+supportedFlashMode.get(i).toString());
				//}
			}

			if(mCameraMode == 0  || mCameraMode == 4) {
				supportedFlashMode_1 = parameters_1.getSupportedFlashModes();
				if(supportedFlashMode_1 == null || supportedFlashMode_1.isEmpty()){
					isSupportFlash_1 = false;
					Log.e(TAG, "supportedFlashMode : Null");
				}else {
					isSupportFlash_1 = true;
					//for(int i=0 ;i<supportedFlashMode_1.size(); i++){
					//	Log.i(TAG, "supportedFlashMode_1 : "+supportedFlashMode_1.get(i).toString());
					//}
				}
			}

			supportedFocuseMode = parameters.getSupportedFocusModes();
			if(supportedFocuseMode == null || supportedFocuseMode.isEmpty()){
					isSupportFocuse = false;
					Log.e(TAG, "supportedFocuseMode : Null");
			}else {
				for(int i=0;i<supportedFocuseMode.size();i++){
					//Log.i(TAG, "supportedFocuseMode : "+supportedFocuseMode.get(i).toString());
					if(supportedFocuseMode.get(i).toString().equals("auto")){
						isSupportFocuse = true;
					}
				}
			}

			if(mCameraMode == 0  || mCameraMode == 4) {
				supportedFocuseMode_1 = parameters_1.getSupportedFocusModes();
				if(supportedFocuseMode_1 == null || supportedFocuseMode_1.isEmpty()){
					isSupportFocuse_1 = false;
					Log.e(TAG, "supportedFocuseMode_1 : Null");
				}else {
					for(int i=0;i<supportedFocuseMode_1.size();i++){
						//Log.i(TAG, "supportedFocuseMode_1 : "+supportedFocuseMode_1.get(i).toString());
						if(supportedFocuseMode_1.get(i).toString().equals("auto")){
							isSupportFocuse_1 = true;
						}
					}
				}
			}

			Log.i(TAG,"set default rotation:");
			//set default rotation
			switch (mCameraMode){
				case 0 : //rear0&rear1
					parameters.setRotation(0); //picture rotation
					mCamera.setDisplayOrientation(90); //display rotation

					parameters_1.setRotation(0);
					mCamera1.setDisplayOrientation(90);
					break;
				case 1 : //rear0
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(90);
					break;
				case 2 : //rear1
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(270);
					break;
				case 3 : //front
					parameters.setRotation(180);
					mCamera.setDisplayOrientation(90);
					break;
				case 4 : //front0 & front1
					parameters.setRotation(0);
					mCamera.setDisplayOrientation(90);

					parameters_1.setRotation(0);
					mCamera1.setDisplayOrientation(270);
					break;
				default:
					Log.e(TAG,"error mCameraMode!");
					break;
			}

			parameters.setPictureFormat(256);  //0x11:NV21 / 0x100 : JPEG
			if(mCamera1 != null) {
				mCamera.setParameters(parameters);
				surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
				mPreview = new CameraPreview(this, mCamera ,surfaceView, mOpenCamIndex, mCameraMode);
				mPreview.setlogPath(mLogPath);

				parameters_1.setPictureFormat(256);  //0x11:NV21 / 0x100 : JPEG
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

							Parameters parameters = mCamera.getParameters();
							if(flashmode!=-1)
							{
								switch(flashmode){
									case 0:
										if(isSupportFlash){
											parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
											mCamera.setParameters(parameters);
										}

										if(isSupportFlash_1 && mCamera1 != null){
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

										if(isSupportFlash_1 && mCamera1 != null){
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

										if(isSupportFlash_1 && mCamera1 != null){
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
										}

										if(isSupportFocuse_1 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFocusMode(Parameters.FOCUS_MODE_AUTO);
											mCamera1.setParameters(parameters_1);
										}
										wlog("focus success");
										break;
									case 1:
										if(isSupportFocuse){
											parameters.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
											mCamera.setParameters(parameters);
										}

										if(isSupportFocuse_1 && mCamera1 != null){
											Parameters parameters_1 = mCamera1.getParameters();
											parameters_1.setFocusMode(Parameters.FOCUS_MODE_INFINITY);
											mCamera1.setParameters(parameters_1);
										}
										wlog("focus success");
										break;
									case 2:
										if(isSupportFocuse){
											parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
											mCamera.setParameters(parameters);

											if(isSupportFocuse_1 && mCamera1 != null){
												Parameters parameters_1 = mCamera1.getParameters();
												parameters_1.setFocusMode(Parameters.FOCUS_MODE_AUTO);
												mCamera1.setParameters(parameters_1);
											}
											setFocusArea(mCameraMode);
											Log.i(TAG, "do Focus wide :");
											mCamera.autoFocus(mAutoFocusCallbackWide);
										} else {
											wlog("focus success");
										}
										break;
								}
							}
							break;

						case HandleMsg.SET_EXP:
							int exp=msg.arg1;
							int cameraIDexp=msg.arg2;
							Log.i(TAG,"exp = "+exp+",cameraIDexp = "+cameraIDexp);
							if(mCamera1 != null){
								if(cameraIDexp == 0) {
									mPreview.setExp(exp);
								}else if(cameraIDexp == 2) {
									mPreview1.setExp(exp);
								}
							}else{
								mPreview.setExp(exp);
							}
							break;

						case HandleMsg.SET_ISO:
							int iso=msg.arg1;
							int cameraIDiso=msg.arg2;
							Log.i(TAG,"iso = "+iso+",cameraIDiso = "+cameraIDiso);

							if(mCamera1 != null){
								if(cameraIDiso == 0) {
									mPreview.setiso(iso);
								}else if(cameraIDiso == 2) {
									mPreview1.setiso(iso);
								}
							}else{
								mPreview.setiso(iso);
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

	/*
        private int FindBackCamera0() {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int camIdx = 0; camIdx < mCameraNumber; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    Log.i(TAG,"FindBackCamera0 index =  "+camIdx);
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
                    if(findFirst == true){
                        Log.i(TAG,"FindBackCamera1 index =  "+camIdx);
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
                    Log.i(TAG,"FindFrontCamera index =  "+camIdx);
                    return camIdx;
                }
            }
            return -1;
        }
    */

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

			//Log.e(TAG,"mPicOrientation:"+mPicOrientation);

			if(mPicOrientation == 0){
				mPicOrientation = 90;
			} else if(mPicOrientation == 180){
				mPicOrientation = 270;
			}

			//keep all picture orientation same as 270 for DIT
			if(mPicOrientation != 270){
				mPicOrientation += 180;
			}

			Parameters parameters = mCamera.getParameters();
			if (mCamera1 != null) {  //rear0 & rear1
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
				mPicOrientation = (mPicOrientation+270)%360;
				parameters.setRotation(mPicOrientation);
				mCamera.setParameters(parameters);
			}
			else {
				Log.e(TAG,"error camera mode!");
			}
			//Log.i(TAG,"set picture Orientation:"+mPicOrientation);
		}
	}

	AutoFocusCallback mAutoFocusCallback=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.d(TAG, "mAutoFocusCallback:");
			if(success==false){
				Log.e(TAG, "auto focus fail");
				wlog("auto focus fail");
			}

			if(mCameraMode == 0  || mCameraMode == 4) {
				if(isSupportFocuse_1){
					new Handler().postDelayed(new Runnable(){
						public void run() {
							Log.i(TAG, "is SupportFocuse_1 :");
							mCamera1.autoFocus(mAutoFocus_1Callback);
						}
					}, 300);
				}else {
					Log.i(TAG, "not SupportFocuse_1 :");
					mCamera.takePicture(null, null, mPictureCallback);
				}
			}else{
				mCamera.takePicture(null, null, mPictureCallback);
			}
		}
	};

	AutoFocusCallback mAutoFocus_1Callback=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.d(TAG, "mAutoFocus_1Callback:");
			if(success==false){
				Log.e(TAG, "auto focus_1 fail");
				wlog("auto focus_1 fail");
			}

			new Handler().postDelayed(new Runnable(){
				public void run() {
					//take picture:
					mCamera.takePicture(null, null, mPictureCallback);
				}
			}, 600);
		}
	};

	AutoFocusCallback mAutoFocusCallbackWide=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.d(TAG, "mAutoFocusCallbackWide:parameter");
			if(success==false){
				Log.e(TAG, "wide auto focus fail");
			}else {
				if(isSupportFocuse_1 && mCamera1 != null){
					Log.i(TAG, "do Focus tele :");
					new Handler().postDelayed(new Runnable(){
						public void run() {
							Log.i(TAG, "is SupportFocuse_1 :");
							mCamera1.autoFocus(mAutoFocusCallbackTele);
						}
					}, 300);
				}else{
					wlog("focus success");
				}
			}
		}
	};

	AutoFocusCallback mAutoFocusCallbackTele=new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			// TODO Auto-generated method stub
			Log.d(TAG, "mAutoFocusCallbackTele:parameter");
			if(success==false){
				Log.e(TAG, "tele auto focus fail");
			}else {
				wlog("focus success");
			}
		}
	};



	PictureCallback mPictureCallback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onPictureTaken");
			String savaPath = mSavaPath;
			try {
				if (data != null && mSavaPath != null){
					if(mCamera1 != null){
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

			mbTkPicture=false;
			wlog("takePicture finish");

			if(mCamera1 != null) {
				new Handler().postDelayed(new Runnable(){
					public void run() {
						Log.i(TAG,"take picture_1 : ");
						mCamera1.takePicture(null, null, mPicture_1Callback);
					}
				}, 300);
			}else{
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
			Log.d(TAG, "onPictureTaken_1");
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
			mbTkPicture_1=false;
			wlog("takePicture_1 finish");
		}
	};

	PictureCallback mRawPictureCallback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			mbTkPicture=false;
			wlog("takeRawPicture finish");
			if(mCamera1 != null) {
				new Handler().postDelayed(new Runnable(){
					public void run() {
						mCamera1.takePicture(null, null, mRawPicture_1Callback);
					}
				}, 300);
			}
		}
	};

	PictureCallback mRawPicture_1Callback=new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			mbTkPicture_1=false;
			//mCamera1.startPreview();
			wlog("takeRawPicture_1 finish");
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
		Log.i(TAG,"takePic enter:");
		if(mSavaPath==null||mSavaPath.length()==0){
			mbTkPicture=false;
			mbTkPicture_1=false;
			Toast.makeText(MainActivity.this, "picture path no input parameter", Toast.LENGTH_LONG).show();
			return;
		}else {
			mbTkPicture=true;
			if(mCamera1 != null){
				mbTkPicture_1=true;
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
			/*
			parameters.setPictureFormat(256); //0x11:NV21 / 0x100 : JPEG
			mCamera.setParameters(parameters);

			if(mCameraMode == 0 && mCamera1 != null) {
				Parameters parameters_1 = mCamera1.getParameters();
				parameters_1.setPictureFormat(256); //0x11:NV21 / 0x100 : JPEG
				mCamera1.setParameters(parameters_1);
			}
			*/

		if(raw_need==1)
		{
			try {
				wlog("take raw pic");
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
			parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
			mCamera.setParameters(parameters);
			mCamera.cancelAutoFocus(); //reset focusState=0

			if(mCamera1 != null && isSupportFocuse_1) {
				Parameters parameters_1 = mCamera1.getParameters();
				parameters_1.setFocusMode(Parameters.FOCUS_MODE_AUTO);
				mCamera1.setParameters(parameters_1);
				mCamera1.cancelAutoFocus(); //reset focusState=0
			}

			try {
				//set ROI:
				setFocusArea(mCameraMode);
				Log.i(TAG, "setFocusArea end");
				mCamera.autoFocus(mAutoFocusCallback);
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
			Log.i(TAG,"focusAreaNum="+focusAreaNum+", CameraMode="+CameraMode);

			if(focusAreaNum > 0)
			{
				ArrayList<Area> focusArea = new ArrayList<Area>();
				focusArea.add(new Area(new Rect(), 1000));
				focusArea.get(0).rect.set(-250, -250, 250, 250);

				if(CameraMode == 0 || CameraMode == 4){
					parameters.setFocusAreas(focusArea);
					mCamera.setParameters(parameters);

					Parameters parameters_1=mCamera1.getParameters();
					parameters_1.setFocusAreas(focusArea);
					mCamera1.setParameters(parameters_1);
				}else{
					parameters.setFocusAreas(focusArea);
					mCamera.setParameters(parameters);
				}
			}

			int meteringAreaNum = parameters.getMaxNumMeteringAreas();
			Log.i(TAG,"max metering area = " + meteringAreaNum);
			if (meteringAreaNum > 0) {

				ArrayList<Area> meteringArea = new ArrayList<Area>();
				meteringArea.add(new Area(new Rect(), 1000));
				meteringArea.get(0).rect.set(-250, -250, 250, 250);

				if(CameraMode == 0 || CameraMode == 4) {
					parameters.setMeteringAreas(meteringArea);
					mCamera.setParameters(parameters);

					Parameters parameters_1=mCamera1.getParameters();
					parameters_1.setMeteringAreas(meteringArea);
					mCamera1.setParameters(parameters_1);
				}else{
					parameters.setMeteringAreas(meteringArea);
					mCamera.setParameters(parameters);
				}
			}
		}
	}
}
