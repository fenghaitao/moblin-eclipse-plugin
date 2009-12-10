package org.moblin.sdk.ide.actions;

import org.eclipse.cdt.ui.IBuildConsoleManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.Console;
import org.moblin.sdk.ide.MoblinSDKMessages;

@SuppressWarnings("restriction")
public class ConfigureConsole extends Console {
	IProject project;
	IBuildConsoleManager fConsoleManager;
	
	private static final String CONTEXT_MENU_ID = "ConfigureConsole"; 
	private static final String CONSOLE_NAME = MoblinSDKMessages.getString("Console.SDK.Name");
	
	public ConfigureConsole() {
		super(CONSOLE_NAME, CONTEXT_MENU_ID);
	}
}
