--------
Commands
--------

None

------------
Requirements
------------

App is build on android studio.

"iot.apk" is provided to install the app on your device.

Compatible with Android 9+

Used chaquopy to integrate python with android.

Used can and bitmap to plot path on the app in real-time. 

-----------
Calibration
-----------

Move your phone like drawing infinity symbol in air, if you have an offset in theta.
Hold the phone tightly and walk slowly. Move/turn slowly for accurate results.

---------------------------
Error/weaknesses of program
---------------------------

Slight movement could be taken as a step.

-----------
Assumptions
-----------

Initial positions x and y are kept zero.

----------------
Path plot on App
----------------

green line is the expected path.
 
where, 1 unit on x-axis are y-axis is 1 centimeter.

and, 1cm = 0.8 pixels along x-axis. 

-----------
Observation
-----------

It is observed that PDR without RTT is more accurate, but PDR with RTT is more stable.

---------------------
build.gradle(project)
---------------------
>>add maven and classpath lines under builscript.

buildscript {
    repositories {
        maven { url "https://chaquo.com/maven" }
    }
}

buildscript {
    dependencies {
        classpath "com.chaquo.python:gradle:9.1.0"
    }
}

---------------------
build.gradle(module)
---------------------
>>add chaquo plugin

plugins {
    id 'com.chaquo.python'
}

>>add ndk

android {
    defaultConfig {
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        }
    }
}

>>add dependencies

dependencies {
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
