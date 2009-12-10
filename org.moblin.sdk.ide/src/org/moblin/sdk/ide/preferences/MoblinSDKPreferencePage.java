package org.moblin.sdk.ide.preferences;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.moblin.sdk.ide.MoblinSDKChecker;
import org.moblin.sdk.ide.MoblinSDKMessages;
import org.moblin.sdk.ide.MoblinSDKPlugin;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckRequestFrom;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckResults;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class MoblinSDKPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	private static final String PREFERENCES_SDK_DESCRIPTION  = "Preferences.SDK.Description.Name";
	private static final String PREFERENCES_INFORM_TITLE   = "Preferences.SDK.Informing.Title";
	private static final String PREFERENCES_INFORM_MESSAGE = "Preferences.SDK.Informing.Message";
	private static final String PREFERENCES_SDK_LOCATION = "Preferences.SDK.Location.Name";
	private static final String PREFERENCES_SDK_TRIPLET  = "Preferences.SDK.Triplet.Name";
	
	private DirectoryFieldEditor dirEditor;
	private StringFieldEditor strEditor;

	public MoblinSDKPreferencePage() {
		super(GRID);
		setPreferenceStore(MoblinSDKPlugin.getDefault().getPreferenceStore());
		setDescription(MoblinSDKMessages.getString(PREFERENCES_SDK_DESCRIPTION));
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		dirEditor = new DirectoryFieldEditor(PreferenceConstants.TOOLCHAIN_LOCACATION, 
				MoblinSDKMessages.getString(PREFERENCES_SDK_LOCATION), getFieldEditorParent()); 
		dirEditor.setEmptyStringAllowed(false);
		addField(dirEditor);
		
		strEditor = new StringFieldEditor(PreferenceConstants.TOOLCHAIN_TRIPLET, 
				MoblinSDKMessages.getString(PREFERENCES_SDK_TRIPLET), getFieldEditorParent());
		strEditor.setEmptyStringAllowed(false);
		addField(strEditor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	public boolean performOk() {
		IPreferenceStore store = MoblinSDKPlugin.getDefault().getPreferenceStore();
		String orig_toolchain_location  = store.getString(PreferenceConstants.TOOLCHAIN_LOCACATION);
		String orig_toolchain_triplet   = store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);

		String toolchain_location = dirEditor.getStringValue();
		String toolchain_triplet  = strEditor.getStringValue();
		SDKCheckResults result = MoblinSDKChecker.checkMoblinSDK(toolchain_location, toolchain_triplet);
		if (result == SDKCheckResults.SDK_PASS) {
			if (!(toolchain_location.equals(orig_toolchain_location) && toolchain_triplet.equals(orig_toolchain_triplet))) {
				String title   = MoblinSDKMessages.getString(PREFERENCES_INFORM_TITLE);
				String message = MoblinSDKMessages.getString(PREFERENCES_INFORM_MESSAGE);
				MessageDialog.openInformation(MoblinSDKPlugin.getActiveWorkbenchShell(), title, message);
			}
			return super.performOk();
		} else {
			String errorMessage = MoblinSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Preferences);
    		setErrorMessage(errorMessage);
    		return false;
		}
	}
}