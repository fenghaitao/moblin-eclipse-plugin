package org.moblin.sdk.ide.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.moblin.sdk.ide.MoblinSDKPlugin;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = MoblinSDKPlugin.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.TOOLCHAIN_TRIPLET, "i586-moblin-linux");
	}

}
