package org.moblin.sdk.ide;

import java.io.File;

public class MoblinSDKChecker {
	public static enum SDKCheckResults {
		SDK_PASS,
		SDK_LOCATION_EMPTY,
		SDK_TRIPLET_EMPTY,
		SDK_BIN_NON_EXIST,
		SDK_SYSROOT_NON_EXIST,
		SDK_PKGCONFIG_NON_EXIST
	};

	public static enum SDKCheckRequestFrom {
		Wizard,
		Menu,
		Preferences
	};

	private static final String WIZARD_SDK_LOCATION_EMPTY     = "Wizard.SDK.Location.Empty";
	private static final String WIZARD_SDK_TRIPLET_EMPTY      = "Wizard.SDK.Triplet.Empty";
	private static final String WIZARD_SDK_BIN_NONEXIST       = "Wizard.SDK.Bin.Nonexist";
	private static final String WIZARD_SDK_SYSROOT_NONEXIST   = "Wizard.SDK.Sysroot.Nonexist";
	private static final String WIZARD_SDK_PKGCONFIG_NONEXIST = "Wizard.SDK.Pkgconfig.Nonexist";

	private static final String MENU_SDK_LOCATION_EMPTY     = "Menu.SDK.Location.Empty";
	private static final String MENU_SDK_TRIPLET_EMPTY      = "Menu.SDK.Triplet.Empty";
	private static final String MENU_SDK_BIN_NONEXIST       = "Menu.SDK.Bin.Nonexist";
	private static final String MENU_SDK_SYSROOT_NONEXIST   = "Menu.SDK.Sysroot.Nonexist";
	private static final String MENU_SDK_PKGCONFIG_NONEXIST = "Menu.SDK.Pkgconfig.Nonexist";

	private static final String PREFERENCES_SDK_BIN_NONEXIST       = "Preferences.SDK.Bin.Nonexist";
	private static final String PREFERENCES_SDK_SYSROOT_NONEXIST   = "Preferences.SDK.Sysroot.Nonexist";
	private static final String PREFERENCES_SDK_PKGCONFIG_NONEXIST = "Preferences.SDK.Pkgconfig.Nonexist";

	public static SDKCheckResults checkMoblinSDK(String toolchain_location, String toolchain_triplet) {
		if (toolchain_location.isEmpty()) {
			return SDKCheckResults.SDK_LOCATION_EMPTY;			
		} else if (toolchain_triplet.isEmpty()) {
			return SDKCheckResults.SDK_TRIPLET_EMPTY;
		} else {
			String moblin_sdk_path = toolchain_location + File.separator + "bin";
	        File sdk_bin_dir = new File(moblin_sdk_path);
	        if (! sdk_bin_dir.exists())
	        	return SDKCheckResults.SDK_BIN_NON_EXIST;

	        String moblin_pkg_sys_root = toolchain_location + File.separator + toolchain_triplet 
	                                                        + File.separator + "sys-root";
			File sys_root_dir = new File(moblin_pkg_sys_root);
			if (!sys_root_dir.exists())
				return SDKCheckResults.SDK_SYSROOT_NON_EXIST;
			
			String moblin_pkg_path1 = moblin_pkg_sys_root + File.separator + "usr" + File.separator + "lib"
														  + File.separator + "pkgconfig";
			String moblin_pkg_path2 = moblin_pkg_sys_root + File.separator + "usr" + File.separator + "share" 
														  + File.separator + "pkgconfig";
			File pkg_path_dir1 = new File(moblin_pkg_path1);
			File pkg_path_dir2 = new File(moblin_pkg_path2);
			if (!(pkg_path_dir1.exists() || pkg_path_dir2.exists()))
				return SDKCheckResults.SDK_PKGCONFIG_NON_EXIST;
		}
		
		return SDKCheckResults.SDK_PASS;
	}

	private static String getWizardErrorMessage(SDKCheckResults result) {
		switch (result) {
		case SDK_LOCATION_EMPTY:
			return  MoblinSDKMessages.getString(WIZARD_SDK_LOCATION_EMPTY);
		case SDK_TRIPLET_EMPTY:
			return  MoblinSDKMessages.getString(WIZARD_SDK_TRIPLET_EMPTY);
		case SDK_BIN_NON_EXIST:
			return  MoblinSDKMessages.getString(WIZARD_SDK_BIN_NONEXIST);
		case SDK_SYSROOT_NON_EXIST:
			return  MoblinSDKMessages.getString(WIZARD_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  MoblinSDKMessages.getString(WIZARD_SDK_PKGCONFIG_NONEXIST);
		default:
			return null;
		}
	}

	private static String getMenuErrorMessage(SDKCheckResults result) {
		switch (result) {
		case SDK_LOCATION_EMPTY:
			return  MoblinSDKMessages.getString(MENU_SDK_LOCATION_EMPTY);
		case SDK_TRIPLET_EMPTY:
			return  MoblinSDKMessages.getString(MENU_SDK_TRIPLET_EMPTY);
		case SDK_BIN_NON_EXIST:
			return  MoblinSDKMessages.getString(MENU_SDK_BIN_NONEXIST);
		case SDK_SYSROOT_NON_EXIST:
			return  MoblinSDKMessages.getString(MENU_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  MoblinSDKMessages.getString(MENU_SDK_PKGCONFIG_NONEXIST);
		default:
			return null;
		}
	}

	private static String getPreferencesErrorMessage(SDKCheckResults result) {
		switch (result) {
		case SDK_BIN_NON_EXIST:
			return  MoblinSDKMessages.getString(PREFERENCES_SDK_BIN_NONEXIST); 
		case SDK_SYSROOT_NON_EXIST:
			return  MoblinSDKMessages.getString(PREFERENCES_SDK_SYSROOT_NONEXIST);
		case SDK_PKGCONFIG_NON_EXIST:
			return  MoblinSDKMessages.getString(PREFERENCES_SDK_PKGCONFIG_NONEXIST);		
		default:
			return null;
		}
	}

	public static String getErrorMessage(SDKCheckResults result, SDKCheckRequestFrom from) {
		switch (from) {
			case Wizard:
				return getWizardErrorMessage(result);
			case Menu:
				return getMenuErrorMessage(result);
			case Preferences:
				return getPreferencesErrorMessage(result);
			default:
				return null;
		}
	}	
}
