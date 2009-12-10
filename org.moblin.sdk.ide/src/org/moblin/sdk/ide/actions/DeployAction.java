package org.moblin.sdk.ide.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.moblin.sdk.ide.MoblinSDKPlugin;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeAction;

@SuppressWarnings("restriction")
public class DeployAction extends InvokeAction {

	private static final String DEFAULT_COMMAND = "make"; 

	public void run(IAction action) {

		IContainer container = getSelectedContainer();
		if (container == null)
			return;
		IPath execDir = getExecDir(container);
		
		IProject project = container.getProject();
		String project_name = project.getName();
		String title = "Delopy " + project_name + " to ...";
		
		DirectoryDialog locationDialog = new DirectoryDialog(MoblinSDKPlugin.getActiveWorkbenchShell());	
		locationDialog.setText(title); 
		String rawArg = locationDialog.open();

		if (rawArg != null) {
			String argumentList[] = new String[2];
			argumentList[0] = "install";
			argumentList[1] = "DESTDIR=" + rawArg;
			String command_name = DEFAULT_COMMAND + " " + argumentList[0] + " " + argumentList[1];  

			executeConsoleCommand(command_name, DEFAULT_COMMAND, argumentList, execDir);
		}
	}

	public void dispose() {

	}
}
