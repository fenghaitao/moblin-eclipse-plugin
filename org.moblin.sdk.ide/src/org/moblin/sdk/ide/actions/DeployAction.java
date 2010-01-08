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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.Shell;
import org.moblin.sdk.ide.MoblinSDKMessages;
import org.moblin.sdk.ide.MoblinSDKPlugin;

@SuppressWarnings("restriction")
public class DeployAction extends InvokeSyncAction {
	private static final String DEPLOY_TITLE  = "Menu.Deploy.Title";
	private static final String DEPLOY_MESSAGE  = "Menu.Deploy.Message";

	private static final String DEFAULT_RM_COMMAND = "rm";
	private static final String DEFAULT_RM_ARG_1 = "-rf";

	private static final String DEFAULT_MAKE_COMMAND = "make";
	private static final String DEFAULT_MAKE_ARG_1 = "install";
	private static final String DEFAULT_MAKE_ARG_2 = "DESTDIR=";
	private static final String DEFAULT_MAKE_INSTALL_DIR = ".install";
	private static final String CONSOLE_ACTION_MESSAGE  = "Menu.SDK.Console.Deploy.Action.Message";
	private static final String CONSOLE_EXTRACT_MESSAGE  = "Menu.SDK.Console.Deploy.Extract.Message";
	private static final String CONSOLE_SUCCESS_MESSAGE  = "Menu.SDK.Console.Deploy.Success.Message";
	private static final String CONSOLE_FAIL_MESSAGE  = "Menu.SDK.Console.Deploy.Fail.Message";

	private static final String DEFAULT_TAR_COMMAND = "tar";
	private static final String DEFAULT_TAR_ARG_1 = "-zcf";

	public void run(IAction action) {
		IContainer container = getSelectedContainer();
		if (container == null)
			return;

		IProject project = container.getProject();
		String project_name = project.getName();
		Shell shell = MoblinSDKPlugin.getActiveWorkbenchShell();
		// Invoke the SystemSelectRemoteFolderAction to get RemoteDir and RemoteConnection
		SystemSelectRemoteFolderAction rseAction = new SystemSelectRemoteFolderAction(shell);
		rseAction.setDialogTitle(MoblinSDKMessages.getFormattedString(DEPLOY_TITLE, project_name));
		rseAction.setMessage(MoblinSDKMessages.getString(DEPLOY_MESSAGE));
		rseAction.setMultipleSelectionMode(false);
		rseAction.run();
		IRemoteFile remoteDir = rseAction.getSelectedFolder();
		IHost connection = rseAction.getSelectedConnection();
		IPath execDir = getExecDir(container);

		executeDeployCommand(project, connection, remoteDir, execDir);
	}	

	protected void executeDeployCommand(final IProject project, final IHost connection,	
										final IRemoteFile remoteDir, final IPath execDir) {
		// We need to use a workspace root scheduling rule because adding MakeTargets
		// may end up saving the project description which runs under a workspace root rule.
		final ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();

		Job backgroundJob = new Job("Deploy") {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor) throws CoreException {
							if (connection != null && remoteDir != null) {
								String installDir = execDir.toString() + File.separator + DEFAULT_MAKE_INSTALL_DIR;

								// Get a Moblin console for the project
								IConsole console = CCorePlugin.getDefault().getConsole("org.moblin.sdk.ide.moblinConsole");
								console.start(project);

								//Invoke make install DESTDIR=
						        File install_dir = new File(installDir);
						        if (install_dir.exists()) {
									String rmArgumentList[] = new String[2];
									rmArgumentList[0] = DEFAULT_RM_ARG_1;
									rmArgumentList[1] = installDir;
									String rm_command_name = DEFAULT_RM_COMMAND + " " + rmArgumentList[0] + " " + rmArgumentList[1];
									try {
										executeLocalConsoleCommand(console, rm_command_name, DEFAULT_RM_COMMAND, rmArgumentList, execDir);
									} catch (CoreException e) {
									} catch (IOException e) {
									}
						        }

						        String makeArgumentList[] = new String[2];
								makeArgumentList[0] = DEFAULT_MAKE_ARG_1;
								makeArgumentList[1] = DEFAULT_MAKE_ARG_2 + installDir;
								String make_command_name = DEFAULT_MAKE_COMMAND + " " + makeArgumentList[0] + " " + makeArgumentList[1];
								try {
									executeLocalConsoleCommand(console, make_command_name, DEFAULT_MAKE_COMMAND, makeArgumentList, execDir);
								} catch (CoreException e1) {
								} catch (IOException e1) {
								}

								String[] children = install_dir.list();
								if (children.length > 0) {
									IPath tarDir = new Path(execDir.toString() + File.separator + DEFAULT_MAKE_INSTALL_DIR);
									String tarballName = DEFAULT_MAKE_INSTALL_DIR + "_" + project.getName() + ".tgz";
									String tarArgumentList[] = new String[2 + children.length];
									tarArgumentList[0] = DEFAULT_TAR_ARG_1;
									tarArgumentList[1] = tarballName;
									String tar_command_name = DEFAULT_TAR_COMMAND + " " + tarArgumentList[0] + " " + tarArgumentList[1];
									StringBuffer tar_string_buf = new StringBuffer(tar_command_name);
									for (int i = 0; i < children.length; i++) {
										tarArgumentList[2 + i] = children[i];
										tar_string_buf.append(" " + tarArgumentList[2 + i]);
									}									
									tar_command_name = tar_string_buf.toString();
									
									try {
										executeLocalConsoleCommand(console, tar_command_name, DEFAULT_TAR_COMMAND, tarArgumentList, tarDir);
									} catch (CoreException e1) {
									} catch (IOException e1) {
									}

									// Deploy tarball
									boolean success = true;				
									try {
										ConsoleOutputStream consoleOutStream = console.getOutputStream();
										String projectName = project.getName();
										String dstDir = remoteDir.getHost().getName() + ":" + remoteDir.getAbsolutePath();

										File tarball = new File(installDir + File.separator + tarballName);
										Object[] deployArgs = new Object[] {tarballName, dstDir};
										String deployMessage = MoblinSDKMessages.getFormattedString(CONSOLE_ACTION_MESSAGE, deployArgs);
										consoleOutStream.write(deployMessage.getBytes());
										consoleOutStream.flush();
										success = deploy(tarball, remoteDir, new NullProgressMonitor()) != null;
										
										if (success) {
											try {
												Object[] extractArgs = new Object[] {dstDir + File.separator + tarballName, dstDir};
												String extractMessage = MoblinSDKMessages.getFormattedString(CONSOLE_EXTRACT_MESSAGE, extractArgs);
												consoleOutStream.write(extractMessage.getBytes());
												consoleOutStream.flush();

												executeRemoteConsoleCommand(connection, "cd" + " " + remoteDir.getAbsolutePath());
												executeRemoteConsoleCommand(connection, "tar" + " " + "zxf" + " " + tarballName);
												executeRemoteConsoleCommand(connection, "rm" + " " + "-f" + " " + tarballName);
											} catch (Exception e) {
												success = false;
											}
										}

										Object[] args = new Object[] {projectName, dstDir};
										String finalMessage;
										if (success) {
											finalMessage = MoblinSDKMessages.getFormattedString(CONSOLE_SUCCESS_MESSAGE, args);
										}else {
											finalMessage = MoblinSDKMessages.getFormattedString(CONSOLE_FAIL_MESSAGE, args);
										}
										consoleOutStream.write(finalMessage.getBytes());
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

	public static Object deploy(File tarball, IRemoteFile targetFolder, IProgressMonitor monitor)
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

		String name = tarball.getName();

		StringBuffer newPathBuf = new StringBuffer(targetFolder.getAbsolutePath());
		newPathBuf.append(targetFolder.getSeparatorChar());
		newPathBuf.append(name);	
		String newPath = newPathBuf.toString();

		IRemoteFile copiedFile = null;
		try
		{
			String srcFileLocation = tarball.getAbsolutePath();
			targetFS.upload(srcFileLocation, null, newPath, null, monitor);
			copiedFile = targetFS.getRemoteFileObject(targetFolder, name, monitor);
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

		return copiedFile;
	}

	public void dispose() {

	}
}