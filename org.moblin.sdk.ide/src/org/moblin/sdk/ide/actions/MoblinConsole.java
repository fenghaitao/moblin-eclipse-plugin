package org.moblin.sdk.ide.actions;

import org.eclipse.linuxtools.internal.cdt.autotools.ui.Console;
import org.moblin.sdk.ide.MoblinSDKMessages;

@SuppressWarnings("restriction")
public class MoblinConsole extends Console {
	private static final String CONTEXT_MENU_ID = "MoblinConsole"; 
	private static final String CONSOLE_NAME = MoblinSDKMessages.getString("Console.SDK.Name");
	
	public MoblinConsole() {
		super(CONSOLE_NAME, CONTEXT_MENU_ID);
	}
}
