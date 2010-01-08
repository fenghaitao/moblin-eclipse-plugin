package org.moblin.sdk.ide.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.files.ui.actions.SystemSelectRemoteFolderAction;
import org.eclipse.rse.internal.files.ui.Activator;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.rse.internal.files.ui.ISystemFileConstants;
import org.eclipse.rse.services.clientserver.messages.SimpleSystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.services.clientserver.messages.SystemMessageException;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
import org.moblin.sdk.ide.MoblinSDKMessages;
import org.moblin.sdk.ide.MoblinSDKPlugin;

@SuppressWarnings("restriction")
public class UndeployAction extends InvokeSyncAction {
	private static final String UNDEPLOY_TITLE  = "Menu.Undeploy.Title";
	private static final String UNDEPLOY_MESSAGE  = "Menu.Undeploy.Message";

	private static final String DEFAULT_RM_COMMAND = "rm"; 
	private static final String DEFAULT_RM_ARG_1 = "-rf"; 

	private static final String DEFAULT_MAKE_COMMAND = "make"; 
	private static final String DEFAULT_MAKE_ARG_1 = "install"; 
	private static final String DEFAULT_MAKE_ARG_2 = "DESTDIR="; 
	private static final String DEFAULT_MAKE_UNINSTALL_DIR = ".uninstall"; 
	private static final String CONSOLE_SUCCESS_MESSAGE  = "Menu.SDK.Console.Undeploy.Success.Message";
	private static final String CONSOLE_FAIL_MESSAGE  = "Menu.SDK.Console.Undeploy.Fail.Message";

	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;

		IProject project = container.getProject();
		String project_name = project.getName();
		
		// Invoke the SystemSelectRemoteFolderAction to get RemoteDir and RemoteConnection
		SystemSelectRemoteFolderAction rseAction = new SystemSelectRemoteFolderAction(MoblinSDKPlugin.getActiveWorkbenchShell());
		rseAction.setDialogTitle(MoblinSDKMessages.getFormattedString(UNDEPLOY_TITLE, project_name));
		rseAction.setMessage(MoblinSDKMessages.getString(UNDEPLOY_MESSAGE));
		rseAction.setMultipleSelectionMode(false);
		rseAction.run();
		IRemoteFile remoteDir = rseAction.getSelectedFolder();
		IHost connection = rseAction.getSelectedConnection();
		IPath execDir = getExecDir(container);		

		executeUndeployCommand(project, connection,remoteDir, execDir);
	}

	protected void executeUndeployCommand(final IProject project, final IHost connection,	
			final IRemoteFile remoteDir, final IPath execDir) {
		// We need to use a workspace root scheduling rule because adding MakeTargets
		// may end up saving the project description which runs under a workspace root rule.
		final ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();

		Job backgroundJob = new Job("Undeploy") {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor) throws CoreException {
							if (connection != null && remoteDir != null) {		
								// Get a Moblin console for the project
								IConsole console = CCorePlugin.getDefault().getConsole("org.moblin.sdk.ide.moblinConsole");
								console.start(project);

								String uninstallDir = execDir.toString() + File.separator + DEFAULT_MAKE_UNINSTALL_DIR;

						        File uninstall_dir = new File(uninstallDir);
						        if (uninstall_dir.exists()) {
									String rmArgumentList[] = new String[2];
									rmArgumentList[0] = DEFAULT_RM_ARG_1;
									rmArgumentList[1] = uninstallDir;
									String rm_command_name = DEFAULT_RM_COMMAND + " " + rmArgumentList[0] + " " + rmArgumentList[1]; 
									try {
										executeLocalConsoleCommand(console, rm_command_name, DEFAULT_RM_COMMAND, rmArgumentList, execDir);
									} catch (CoreException e) {
									} catch (IOException e) {
									}
						        }

						        String makeArgumentList[] = new String[2];
								makeArgumentList[0] = DEFAULT_MAKE_ARG_1;
								makeArgumentList[1] = DEFAULT_MAKE_ARG_2 + uninstallDir;
								String make_command_name = DEFAULT_MAKE_COMMAND + " " + makeArgumentList[0] + " " + makeArgumentList[1]; 
								try {
									executeLocalConsoleCommand(console, make_command_name, DEFAULT_MAKE_COMMAND, makeArgumentList, execDir);
								} catch (CoreException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (IOException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}

								if (uninstall_dir.exists()) {
									// Deploy installDir
									File[] children = uninstall_dir.listFiles();
									boolean success = true;
									for (int i = 0; i < children.length; i++) {
								        if (undeploy(children[i], remoteDir, monitor) == null) {
								        	success = false;
								        	break;
								        }
									}
						
									try {
										ConsoleOutputStream consoleOutStream = console.getOutputStream();
										String projectName = project.getName();
										String dstDir = remoteDir.getHost().getName() + ":" + remoteDir.getAbsolutePath();
										Object[] args = new Object[] {projectName, dstDir };
										String message;
										if (success) {
											message = MoblinSDKMessages.getFormattedString(CONSOLE_SUCCESS_MESSAGE, args);
										}else {
											message = MoblinSDKMessages.getFormattedString(CONSOLE_FAIL_MESSAGE, args);
										}
										consoleOutStream.write(message.getBytes());
										consoleOutStream.flush();
										consoleOutStream.close();
									} catch (CoreException e) {
									} catch (IOException e) {
									}
								}
							}
						}
					}, rule, IWorkspace.AVOID_UPDATE, monitor);
				} catch (CoreException e) {
					return e.getStatus();
				}
				IStatus returnStatus = Status.OK_STATUS;
				return returnStatus;
			}
		};

		backgroundJob.setRule(rule);
		backgroundJob.schedule();
	}
	
	public Object undeploy(File srcFileOrFolder, IRemoteFile targetFolder, IProgressMonitor monitor)
	{
		IRemoteFileSubSystem targetFS = targetFolder.getParentRemoteFileSubSystem();

		if (targetFolder.isStale())
		{
			try
			{
				targetFolder = targetFS.getRemoteFileObject(targetFolder.getAbsolutePath(), monitor);
			}
			catch (Exception e)
			{
				return null;
			}
		}

		if (!targetFolder.exists())
		{
			String msgTxt = NLS.bind(FileResources.FILEMSG_FOLDER_NOTFOUND, 
							targetFolder.getHost().getName() + ":" + targetFolder.getAbsolutePath());
			SystemMessage errorMsg = new SimpleSystemMessage(Activator.PLUGIN_ID,
						ISystemFileConstants.FILEMSG_FOLDER_NOTFOUND,
						IStatus.ERROR, msgTxt);
			SystemMessageDialog.displayMessage(MoblinSDKPlugin.getActiveWorkbenchShell(), errorMsg.toString());
			return null;
		}

		if (!targetFolder.canWrite())
		{
			String msgTxt = FileResources.FILEMSG_SECURITY_ERROR;
			String msgDetails = NLS.bind(FileResources.FILEMSG_SECURITY_ERROR_DETAILS, targetFS.getHostAliasName());
			SystemMessage errorMsg = new SimpleSystemMessage(Activator.PLUGIN_ID,
					ISystemFileConstants.FILEMSG_SECURITY_ERROR,
					IStatus.ERROR, msgTxt, msgDetails);
			SystemMessageDialog.displayMessage(MoblinSDKPlugin.getActiveWorkbenchShell(), errorMsg.toString());
			return null;
		}

		if (!targetFS.isConnected())
		{
			return null;
		}

		String name = srcFileOrFolder.getName();

		if (srcFileOrFolder.isFile())
		{
			StringBuffer newPathBuf = new StringBuffer(targetFolder.getAbsolutePath());
			newPathBuf.append(targetFolder.getSeparatorChar());
			newPathBuf.append(name);	
			String newPath = newPathBuf.toString();
			try
			{
				IRemoteFile targetFile = targetFS.getRemoteFileObject(newPath, monitor);				
				try {
					executeRemoteConsoleCommand(targetFile.getHost(), "rm" + " " + "-f" + " " + newPath);
				} catch (Exception e) {
				}
				//targetFS.delete(targetFile, monitor);
				return targetFile;
			}
			catch (RemoteFileIOException e)
			{
				SystemMessageDialog.displayMessage(e);
				return null;
			}
			catch (SystemMessageException e)
			{
				SystemMessageDialog.displayMessage(e);
				return null;
			}
		}
		else if (srcFileOrFolder.isDirectory())
		{
			StringBuffer newPathBuf = new StringBuffer(targetFolder.getAbsolutePath());
			newPathBuf.append(targetFolder.getSeparatorChar());
			newPathBuf.append(name);
			String newPath = newPathBuf.toString();

			// this is a directory
			// recursively remove
			try
			{
				IRemoteFile newTargetFolder = targetFS.getRemoteFileObject(newPath, monitor);
				File[] children = srcFileOrFolder.listFiles();
				for (int i = 0; i < children.length; i++)
				{
					if (monitor.isCanceled())
					{
						return null;
					}
					else
					{
						File child = children[i];
						if (undeploy(child, newTargetFolder, monitor) == null)
						{
							return null;
						}
					}
				}
				return newTargetFolder;
			}
			catch (SystemMessageException e)
			{
				SystemMessageDialog.displayMessage(e);
			}
			catch (Exception e)
			{
			}
			return null;
		}
		
		return null;
	}

	public void dispose() {

	}
}