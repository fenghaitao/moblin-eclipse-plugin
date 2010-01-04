package org.moblin.sdk.ide.actions;

import java.io.IOException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeAction;
import org.eclipse.swt.widgets.Shell;
import org.moblin.sdk.ide.MoblinSDKMessages;
import org.moblin.sdk.ide.MoblinSDKPlugin;
import org.moblin.sdk.ide.MoblinSDKProjectNature;
import org.moblin.sdk.ide.preferences.PreferenceConstants;

@SuppressWarnings("restriction")
public class ConfigureAction extends InvokeAction {
	private static final String SDK_LOCATION = "Preferences.SDK.Location.Name";
	private static final String SDK_TRIPLET  = "Preferences.SDK.Triplet.Name";
	private static final String DIALOG_TITLE  = "Menu.SDK.Dialog.Title";
	private static final String CONSOLE_MESSAGE  = "Menu.SDK.Console.Configure.Message";
	
	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;

		IPreferenceStore store = MoblinSDKPlugin.getDefault().getPreferenceStore();
		String toolchain_location  = store.getString(PreferenceConstants.TOOLCHAIN_LOCACATION);
		String toolchain_triplet  = store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);

		SDKLocationDialog optionDialog = new SDKLocationDialog(
				new Shell(),
				MoblinSDKMessages.getString(DIALOG_TITLE),
				MoblinSDKMessages.getString(SDK_LOCATION),
				toolchain_location,
				MoblinSDKMessages.getString(SDK_TRIPLET),
				toolchain_triplet,
				null);

		optionDialog.open();
		
		String location = optionDialog.getToolchainLocation();
		String triplet  = optionDialog.getToolchainTriplet();  
		if (location != null) {
			IProject project = container.getProject();
			MoblinSDKProjectNature.setEnvironmentVariables(project, location, triplet);
			//MoblinSDKProjectNature.configureAutotoolsOptions(project, location, triplet);
			
			IConsole console = CCorePlugin.getDefault().getConsole("org.moblin.sdk.ide.moblinConsole");
			console.start(project);
			ConsoleOutputStream consoleOutStream;
			try {
				consoleOutStream = console.getOutputStream();
				String messages = MoblinSDKMessages.getString(CONSOLE_MESSAGE);
				consoleOutStream.write(messages.getBytes());
				consoleOutStream.flush();
				consoleOutStream.close();
			} catch (CoreException e1) {
			} catch (IOException e2) {
			}
		}
	}

	public void dispose() {

	}
}
