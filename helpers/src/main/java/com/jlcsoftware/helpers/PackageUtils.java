package com.jlcsoftware.helpers;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class PackageUtils {

	public static PackageInfo packageInfo;

	public static PackageInfo getPackageInfo(Context context) {
		try {
			packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			versionNumber = packageInfo.versionCode;
			versionName = packageInfo.versionName;
			packageName = packageInfo.packageName;

		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return packageInfo;
	}

	/**
	 * getConfigurationClass
	 * 
	 * - AndroidManifest.xml MUST implement <Application <meta-data android:name="ca.jlcreative.config_class" android:value="ca.jlcreatice.some_package.some_class" /> />
	 * 
	 * @return
	 * @throws RuntimeException
	 */

	public static String getConfigurationClass(Context context) throws RuntimeException {
		String config_class = null;
		ApplicationInfo ai;
		try {
			// Required to get the full application info ( populates the metaData )
			ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

			if (ai.metaData != null) {
				Object value = ai.metaData.get("ca.jlcreative.config_class");
				config_class = (String) value;
			}
		} catch (NameNotFoundException e) {
		}
		if (null == config_class) {
			throw new RuntimeException(
					"AndroidManifest.xml MUST implement <Application <meta-data android:name=\"ca.jlcreative.config_class\" android:value=\"ca.jlcreatice.some_package.some_class\" /> />");
		}
		return config_class;
	}

	private static String packageName; // Name of the Package, different for every product <manifest android:versionCode="2" android:versionName="1.2"
	private static int versionNumber; // An integer value that represents the version of the application code, relative to other versions.
	private static String versionName; // A string value that represents the release version of the application code, as it should be shown to users.

	public static String getPackageName(Context context) {
		if (null == packageInfo) {
			getPackageInfo(context);
		}
		return packageName;
	}

	public static String getVersionName(Context context) {
		if (null == packageInfo) {
			getPackageInfo(context);
		}
		return versionName;
	}

	public static int getVersionNumber(Context context) {
		if (null == packageInfo) {
			getPackageInfo(context);
		}
		return versionNumber;
	}
}
