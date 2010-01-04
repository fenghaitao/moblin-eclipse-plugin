package org.moblin.sdk.ide.actions;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeAction;
import org.eclipse.swt.widgets.Shell;
import org.moblin.sdk.ide.MoblinSDKPlugin;
import org.moblin.sdk.ide.preferences.PreferenceConstants;

@SuppressWarnings("restriction")
public class PackageAction extends InvokeAction {

	
	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;
		
		IPreferenceStore store = MoblinSDKPlugin.getDefault().getPreferenceStore();
		String toolchain_location  = store.getString(PreferenceConstants.TOOLCHAIN_LOCACATION);
		
		if (toolchain_location != null) {
			IProject project = container.getProject();
		    try{
			    Runtime.getRuntime().exec("moblin-package-creator --sdkpath=" + toolchain_location + 
			    	File.separator + ".." + File.separator + " " + project.getLocation());  
		    }catch(Exception e)		    {
		    	 MessageDialog.openError(new Shell(),"Error",
		    		e.getMessage() + ":Please make sure moblin-package-creator has been installed correctly on your system");
		    }
		   
		}
		
	}

	public void dispose() {

	}
}
