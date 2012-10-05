Lang app
========

Building
--------

To build the app you additionally need the files:

  - local.properties
  - lang.keystore

the first contains the path to the SDK, e.g.

	sdk.dir=/home/name/myapps/android-sdk-linux_x86


Installing
----------

	cd app/

	ant clean release

	adb install -r bin/Lang-release.apk


Uninstalling
------------

	adb uninstall be.lukin.android.lang


Other
-----

	android create project -n Lang -t 6 -p . -k be.lukin.android.lang -a DefaultActivity

	keytool -genkey -v -keystore lang.keystore -alias langalias -keyalg RSA -keysize 2048 -validity 10000
