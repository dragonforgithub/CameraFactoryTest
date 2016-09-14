#!/system/bin/sh
#echo "$0 $*"> /proc/fac_printklog

#Author:Sheldon_Li@asus.com
ATD_Interface_VERSION="V1.2"
#Camera solution: XXXXXX XXXXXXX

VERSION="V0.2"


#script log file
SCRIPT_LOG_FILE="/sdcard/CameraTest_Script.log"

#apk log file
APK_LOG_FILE="/sdcard/CameraTest_Apk.log"

#locat log file
LOGCAT_LOG_FILE="/data/logcat_log/logcat.txt"

#Debug message switch
DEBUG=0

mCameraID=`getprop persist.camera.mCameraID`


function disableTouch(){ 
#    echo "Disable touch!!"
    if [ -f "/data/data/touch_function" ]; then
   	/data/data/touch_function 0 > /dev/null
    fi
}

function enableTouch(){ 
#    echo "Enable touch!!"
    if [ -f "/data/data/touch_function" ]; then
   	/data/data/touch_function 1 > /dev/null
    fi
}



if [ ! -d "/data/logcat_log/" ]; then
  mkdir /data/logcat_log/
fi

if [ ! -d "/data/factory/" ]; then
  mkdir /data/factory/
fi


logi () {
  echo "`date` I $@" >> $SCRIPT_LOG_FILE
  if [ $DEBUG -eq "1" ]
  then
    echo "`date +%H:%M:%S` I $@"
  fi
}

logd () {
  echo "`date` D $@" >> $SCRIPT_LOG_FILE
  if [ $DEBUG -eq "1" ]
  then
    echo "`date +%H:%M:%S` D $@"
  fi
}

loge () {
  echo "`date` E $@" >> $SCRIPT_LOG_FILE
  if [ $DEBUG -eq "1" ]
  then
    echo "`date +%H:%M:%S` E $@"
  fi
}


setrawprop() {
  if [ $1 -eq 1 ]; then 
      # echo "=========CameraTest setrawprop==========" >> $LOGCAT_LOG_FILE
		#setprop  persist.camera.HAL3.enabled 0 #disable HAL3
		setprop  persist.camera.dumpimg 16
		setprop  persist.camera.zsl_raw 1
		setprop  persist.camera.raw_yuv 1
		setprop  persist.camera.raw.dump 1     #HAL3	
		setprop  persist.camera.snapshot_raw 1
  else
      # echo "=========CameraTest setrawprop==========" >> $LOGCAT_LOG_FILE
		#setprop  persist.camera.HAL3.enabled 0 #disable HAL3
		setprop  persist.camera.dumpimg 0
		setprop  persist.camera.zsl_raw 0
		setprop  persist.camera.raw_yuv 0
		setprop  persist.camera.raw.dump 0     #HAL3
		setprop  persist.camera.snapshot_raw 0
  fi
}


#log success/fail message and show processing result on the consol
#$1:success/fail  $2:return string
my_exit() { 
  if [ "$1" == "success" ]
  then
    echo "================== CameraTest command executed success =================" >> $LOGCAT_LOG_FILE
    logi "<== return success(1)"
    ATD_ret="1"
    ret=0
  else
    echo "================== CameraTest command executed failed =================" >> $LOGCAT_LOG_FILE
    loge "<### return fail(0)"
    ATD_ret="0"
    ret=1
  fi

  echo $ATD_ret
  exit $ret
}

help() {
    logd "help()"
    echo "
Test Tool Version: $VERSION

API: ATD TestCommand Interface $ATD_Interface_VERSION

FORMAT: adb shell /data/data/DualCamera para1 para2 para3 ...

RETURN VALUE: 0/1   (0 = fail , 1 = pass)

USAGE:
  <Enable/Disable dual camera preview.>
    (para1) 0
    (para2) 0:off, 1:on
    (para3) Timedout value (second) 

  <Enable/Disable VGA camera preview.>
    (para1) 1
    (para2) 0:off, 1:on
    (para3) Timedout value (second) 

  <Take picture with af command.>
    (para1) 2
    (para2) Saved picture file name
    (para3) Timedout value 

  <Enable or disable infinity mode.>
    (para1) 3
    (para2) 0:off, 1:on
    (para3) Timedout value 

  <Take picture >
    (para1) 4
    (para2) Saved picture file name
    (para3) Timedout value

  <Take raw picture.>
    (para1) 5
    (para2) Saved picture file name
    (para3) Timedout value

  <Calibration.>
    (para1) 6
    (para2) 0: DUAL CAMERA

  <Get golden data.>
    (para1) 7
    (para2) 0: TBD
	
  <Take picture with af with flash>
    (para1) 9
    (para2) Saved picture file name
    (para3) Timedout value	

  <Take picture with flash>
    (para1) 10
    (para2) Saved picture file name
    (para3) Timedout value
 
  <DIT AF Calibration>
    (para1) 11
    (para2) TBD
    (para3) 0:Generate dit_af_cali Result
	    1:Inf
	    2:Macro
	    3:Middle(30cm)
	    4:Middle(50cm)
    (para4) 0: record Vertical calibration
	    1: OTP search
	    2: Horizon calibration and record
     
"
exit 0
}

set_timeout() {
    logd "> set_timeout($1)"
    #$1 timeout vale (decimal seconds)
	#$2 ... break condition
	let "TimeOutValue=$1*10"
	echo time out value is $TimeOutValue >> $SCRIPT_LOG_FILE
	while [ $TimeOutValue -ne "0" ]; do
		cat $APK_LOG_FILE 2>&1 | grep "$2" > /dev/null
		if [ $? = 0 ]; then
			rm -rf $APK_LOG_FILE > /dev/null
			setrawprop 0
			my_exit success
			break
		fi
		logd "<-time"
		usleep 100000
		let TimeOutValue--
		logd "time out value now is $TimeOutValue"		
		done
	
	rm -rf $APK_LOG_FILE > /dev/null
	setrawprop 0
	my_exit fail
}

set_timeout_takepic() {
    logd "> set_timeout($1)"
    #$1 timeout vale (decimal seconds)
    #$2 ... break condition
	let "TimeOutValue=$1*10"
	echo time out value is $TimeOutValue  >> $SCRIPT_LOG_FILE
	while [ $TimeOutValue -ne "0" ]; do
		#cat $APK_LOG_FILE  2>&1 | grep "takePicture finish" > /dev/null
		#if [ $? = 0 ]; then
			if [ $mCameraID == 0 ]; then
				if [ -f "$3_1.jpg" ] && [ -f "$3_2.jpg" ]; then
					rm -rf $APK_LOG_FILE > /dev/null
					setrawprop 0	
					my_exit success
				fi
			elif [ $mCameraID == 1 ]; then
  				if [ -f "$3_1.jpg" ]; then
					rm -rf $APK_LOG_FILE > /dev/null
					setrawprop 0	
					my_exit success
				fi
			fi
		#fi
		logd "<-time"
		usleep 100000
		let TimeOutValue--
		logd "time out value now is $TimeOutValue"
	done
		
	rm -rf $APK_LOG_FILE > /dev/null
	setrawprop 0
	my_exit fail
}


set_timeout_takepic_raw() {
    logd "> set_timeout($1)"
    #$1 timeout vale (decimal seconds)
	#$2 ... break condition
	let "TimeOutValue=$1*10"
	echo time out value is $TimeOutValue  >> $SCRIPT_LOG_FILE
	while [ $TimeOutValue -ne "0" ]; do

		if [ $mCameraID == 0 ]; then

			cat $APK_LOG_FILE  2>&1 | grep "takeRawPicture finish" > /dev/null
			if [ $? = 0  ]; then
				if [ -f /data/misc/camera/*0.raw ]; then
					mv  /data/misc/camera/*0.raw  $3_1.raw
				fi	
			fi

			cat $APK_LOG_FILE  2>&1 | grep "takeRawPicture_1 finish" > /dev/null
			if [ $? = 0 ]; then
				if [ -f /data/misc/camera/*1.raw ]; then
					mv  /data/misc/camera/*1.raw  $3_2.raw
				fi
				
				if [ -f /data/misc/camera/*2.raw ]; then
					mv  /data/misc/camera/*2.raw  $3_2.raw
				fi
				
			fi

			if [ -f "$3_1.raw" ] && [ -f "$3_2.raw" ]; then
				setrawprop 0
				my_exit success	
			fi

		elif [ $mCameraID == 1 ]; then
			mv  /data/misc/camera/*2.raw  "$3_3.raw" 
  			if [ -f "$3_3.raw" ]; then
				setrawprop 0	
				my_exit success
			fi
		fi
					
		let TimeOutValue--

		usleep 100000

		logd "<-time"
		
		let TimeOutValue--
		logd "time out value now is $TimeOutValue"
	done
		
	rm -rf $APK_LOG_FILE > /dev/null
	setrawprop 0
	my_exit fail
}

#cameramode : 0=rear0&rear1 , 1=rear0 , 2=rear1 , 3=front
maincamera_preview_switch(){
	#0: off 
	if [ "$1" == 0 ]; then
		echo "================== CameraTest disable main camera preview start=================" >> $LOGCAT_LOG_FILE
		setprop persist.camera.mCameraID -1
		setrawprop 0
		am force-stop com.example.cameratest
		enableTouch;
		my_exit success
	fi
	#1: on
	if [ "$1" == 1 ]; then
		echo "================== CameraTest enable main camera preview start=================" >> $LOGCAT_LOG_FILE
		setprop persist.camera.mCameraID 0
		disableTouch;
		#setprop befor preview
		setrawprop 1
		pid1=`ps | /system/bin/grep com.example.cameratest | busybox awk '{print $1}'`
		if [ "$pid1" != "" ]; then
			am force-stop com.example.cameratest
			sleep 3
			echo "================== am force-stop com.example.cameratest=================" >> $LOGCAT_LOG_FILE
		fi
		am start -n com.example.cameratest/.MainActivity --ei cameramode 0 --es logpath "$APK_LOG_FILE" > /dev/null
	fi
}

VGAcamera_preview_switch(){
	#0: off 
	if [ "$1" == 0 ]; then
		echo "================== CameraTest disable sub camera preview start=================" >> $LOGCAT_LOG_FILE
		setprop persist.camera.mCameraID -1
		setrawprop 0
		am force-stop com.example.cameratest
		enableTouch;
		my_exit success
	fi
	#1: on
	if [ "$1" == 1 ]; then
		echo "================== CameraTest enable sub camera preview start=================" >> $LOGCAT_LOG_FILE
		setprop persist.camera.mCameraID 1
		disableTouch;
		#setprop befor preview
		setrawprop 1
		pid1=`ps | /system/bin/grep com.example.cameratest | busybox awk '{print $1}'`
		if [ "$pid1" != "" ]; then
			am force-stop com.example.cameratest
			sleep 3
			echo "================== am force-stop com.example.cameratest=================" >> $LOGCAT_LOG_FILE
		fi
		am start -n com.example.cameratest/.MainActivity --ei cameramode 3 --es logpath "$APK_LOG_FILE" > /dev/null
	fi
}

take_picture_with_af(){
	#$1 picture file name
	if [ -f $1.jpg -o -f $1_1.jpg -o -f $1_2.jpg ]; then
		rm $1*.jpg
	fi

	echo "================== CameraTest Take picture with af command start=================" >> $LOGCAT_LOG_FILE
	am broadcast -a asus.camera.setsavepath  --es savepath "$1" -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.setparameter --ei flashmode 0 -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.takepicture --ei focusneed 1 --ei rawneed 0 -f 0x10000000 > /dev/null

}




take_picture_with_flash(){
	#$1 picture file name
	if [ -f $1.jpg -o -f $1_1.jpg -o -f $1_2.jpg ]; then
		rm $1*.jpg
	fi

	echo "================== CameraTest Take picture with flash command start=================" >> $LOGCAT_LOG_FILE
	am broadcast -a asus.camera.setsavepath  --es savepath "$1" -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.setparameter --ei flashmode 1 -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.takepicture --ei focusneed 0 --ei rawneed 0 -f 0x10000000 > /dev/null

}

dit_af_calibration(){
	sh /system/bin/dit_af_cali $1 $2 $3
	exit 0
}

infinity_mode_switch(){
	#0: off 
	if [ "$1" == 0 ]; then
		echo "================== CameraTest disable infinity mode start=================" >> $LOGCAT_LOG_FILE
		am broadcast -a asus.camera.setparameter --ei focusmode 0 -f 0x10000000 > /dev/null
	fi
	#1: on
	if [ "$1" == 1 ]; then
		echo "================== CameraTest enable infinity mode start=================" >> $LOGCAT_LOG_FILE
		am broadcast -a asus.camera.setparameter --ei focusmode 1 -f 0x10000000 > /dev/null
	fi
}

take_picture(){
	#$1 picture file name
	if [ -f $1.jpg -o -f $1_1.jpg -o -f $1_2.jpg ]; then
		rm $1*.jpg
	fi

	echo "================== CameraTest Take picture command start=================" >> $LOGCAT_LOG_FILE
	am broadcast -a asus.camera.setsavepath  --es savepath "$1" -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.setparameter --ei flashmode 0 -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.takepicture --ei focusneed 0 --ei rawneed 0 -f 0x10000000 > /dev/null

}

take_picture_with_af_with_flash(){
	#$1 picture file name
	if [ -f $1.jpg -o -f $1_1.jpg -o -f $1_2.jpg ]; then
		rm $1*.jpg
	fi

	echo "================== CameraTest Take picture with af with flash command start=================" >> $LOGCAT_LOG_FILE
	am broadcast -a asus.camera.setsavepath  --es savepath "$1" -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.setparameter --ei flashmode 1 -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.takepicture --ei focusneed 1 --ei rawneed 0 -f 0x10000000 > /dev/null
}

take_raw_picture(){
	setrawprop 1
	echo "================== CameraTest Take raw picture command start=================" >> $LOGCAT_LOG_FILE
	am broadcast -a asus.camera.setsavepath  --es savepath "$1" -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.setparameter --ei flashmode 0 -f 0x10000000 > /dev/null
	am broadcast -a asus.camera.takepicture --ei focusneed 0 --ei rawneed 1 -f 0x10000000 > /dev/null
	
}




calibration(){
	echo "================== CameraTest calibration command start=================" >> $LOGCAT_LOG_FILE

	#for dit calibration
	setprop  persist.camera.zsl_raw 1
	setprop  persist.camera.raw_yuv 1
	setprop  persist.camera.snapshot_raw 1

	sh /system/bin/dit_cali $1
        
	if [ -f "/data/data/cal_rear_result.txt" ]; then
		camera_status=`cat /data/data/cal_rear_result.txt`		
	else
		echo "no cal_rear_result.txt"
		exit 0
	fi

	if [ -f "/data/data/cal_rear2_result.txt" ]; then
		camera_status_1=`cat /data/data/cal_rear2_result.txt`		
	else
		echo "no cal_rear2_result.txt"
		exit 0
	fi
	
	
	if [ "$camera_status" == 1 -a "$camera_status_1" == 1 ]; then
    		echo "1"
	else
    		echo "0"
	fi		     

	#for dit calibration
	setprop  persist.camera.zsl_raw 0
	setprop  persist.camera.raw_yuv 0
	setprop  persist.camera.snapshot_raw 0
	
	exit 0
	
}

get_golden_data(){
	echo "================== CameraTest get golden data command start=================" >> $LOGCAT_LOG_FILE
	
	#for dit calibration
	setprop  persist.camera.zsl_raw 1
	setprop  persist.camera.raw_yuv 1
	setprop  persist.camera.snapshot_raw 1

	sh /system/bin/dit_cali_golden $1
        if [ "$1" == 0 ]; then
		if [ -f "/data/data/cal_golden_rear_result.txt" ]; then
			camera_status=`cat /data/data/cal_golden_rear_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi
		elif [ -f "/data/cal_golden_rear_result.txt" ]; then
			camera_status=`cat /data/cal_golden_rear_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi		     
		else
			echo "no cal_golden_rear_result.txt"
		fi
	elif [ "$1" == 1 ]; then
		if [ -f "/data/data/cal_golden_front_result.txt" ]; then
			camera_status=`cat /data/data/cal_golden_front_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi
		elif [ -f "/data/cal_golden_front_result.txt" ]; then
			camera_status=`cat /data/cal_golden_front_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi
		else
			echo "no cal_golden_front_result.txt"
		fi
	elif [ "$1" == 2 ]; then
		if [ -f "/data/data/cal_golden_rear2_result.txt" ]; then
			camera_status=`cat /data/data/cal_golden_rear2_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi
		elif [ -f "/data/cal_golden_rear2_result.txt" ]; then
			camera_status=`cat /data/cal_golden_rear2_result.txt`
			if [ "$camera_status" == 1 ]; then
		    	echo "1"
			else
		    	echo "0"
			fi
		else
			echo "no cal_golden_rear2_result.txt"
		fi
	fi

	#for dit calibration
	setprop  persist.camera.zsl_raw 0
	setprop  persist.camera.raw_yuv 0
	setprop	 persist.camera.snapshot_raw 0
	
	exit 0
}



#===============================
#main 
#===============================

#wait for system ready
countB=1;
while [ $countB -lt 300 ] 
do 
       getprop|grep bootcomplete > /dev/null
       if [ $? == "0" ]; then
               break
       fi
       sleep 1
let countB+=1
done

rm -rf $APK_LOG_FILE > /dev/null

brightness=`cat /sys/class/leds/lcd-backlight/brightness`

if [ $brightness = 0 ]; then
	input keyevent 26
	sleep 1
fi


setprop  persist.camera.zsl.mode 1     #enable ZSL 

case $1 in
#Enable/Disable camera preview
"0")
    maincamera_preview_switch $2
	set_timeout $3 'call startPreview ok'

;;
#Enable/Disable VGA camera prebiew
"1")
    VGAcamera_preview_switch $2
	set_timeout $3 'call startPreview ok'
;;
#Take picture with af
"2")
    take_picture_with_af /sdcard/$2
	set_timeout_takepic $3 'takePicture finish' /sdcard/$2
;;
#Enable or Disable infinity mode
"3")
	infinity_mode_switch $2
	#set_timeout $3 'SET_FOUCS_MODE pass'
;;
#Take picture 
"4")
    take_picture /sdcard/$2
	set_timeout_takepic $3 'takePicture finish' /sdcard/$2

;;
#Take raw picture 
"5")
    take_raw_picture  /sdcard/$2
		set_timeout_takepic_raw $3 'null' /sdcard/$2
;;
#calibration 
"6")
	calibration DUAL_$2
 
;;
#Get golden data 
"7")
	get_golden_data $2

;;
## not in ATD spec
"8")
    echo "NOT SUPPORT!"

;;

## Take picture command with af with flash
"9")
   take_picture_with_af_with_flash /sdcard/$2
	set_timeout_takepic $3 'takePicture finish' /sdcard/$2
;;
## Take picture command  with flash
"10")
    take_picture_with_flash /sdcard/$2
	set_timeout_takepic $3 'takePicture finish' /sdcard/$2
;;
## DIT af calibration
"11")

    dit_af_calibration $2 $3 $4
		
;;
*)
    help
;;
esac

my_exit success












