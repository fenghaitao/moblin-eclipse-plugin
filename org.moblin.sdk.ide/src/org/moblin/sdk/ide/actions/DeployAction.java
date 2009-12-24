package org.moblin.sdk.ide.actions;

import java.io.File;
import java.io.IOException;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.rse.services.files.IFilePermissionsService;
import org.eclipse.rse.services.files.IHostFilePermissions;
import org.eclipse.rse.services.files.RemoteFileIOException;
import org.eclipse.rse.subsystems.files.core.model.RemoteFileUtility;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.ui.messages.SystemMessageDialog;
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
	private static final String CONSOLE_SUCCESS_MESSAGE  = "Menu.SDK.Console.Deploy.Success.Message";
	private static final String CONSOLE_FAIL_MESSAGE  = "Menu.SDK.Console.Deploy.Fail.Message";

	public void run(IAction action) {

		IContainer container = getSelectedContainer();
		if (container == null)
			return;

		IProject project = container.getProject();
		String project_name = project.getName();
		
		// Invoke the SystemSelectRemoteFolderAction to get RemoteDir and RemoteConnection
		SystemSelectRemoteFolderAction rseAction = new SystemSelectRemoteFolderAction(MoblinSDKPlugin.getActiveWorkbenchShell());
		rseAction.setDialogTitle(MoblinSDKMessages.getFormattedString(DEPLOY_TITLE, project_name));
		rseAction.setMessage(MoblinSDKMessages.getString(DEPLOY_MESSAGE));
		rseAction.setMultipleSelectionMode(false);
		rseAction.run();
		IRemoteFile remoteDir = rseAction.getSelectedFolder();
		IHost connection = rseAction.getSelectedConnection();

		if (connection != null && remoteDir != null) {
			IPath execDir = getExecDir(container);
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
			executeConsoleCommandSync(console, rm_command_name, DEFAULT_RM_COMMAND, rmArgumentList, execDir);
	        }

	        String makeArgumentList[] = new String[2];
			makeArgumentList[0] = DEFAULT_MAKE_ARG_1;
			makeArgumentList[1] = DEFAULT_MAKE_ARG_2 + installDir;
			String make_command_name = DEFAULT_MAKE_COMMAND + " " + makeArgumentList[0] + " " + makeArgumentList[1];
			executeConsoleCommandSync(console, make_command_name, DEFAULT_MAKE_COMMAND, makeArgumentList, execDir);

			if (install_dir.exists()) {
				// Deploy installDir
				IProgressMonitor monitor = new NullProgressMonitor();
				boolean success = true;
				File[] children = install_dir.listFiles();
				for (int i = 0; i < children.length; i++) {
			        if (deploy(children[i], remoteDir, monitor) == null) {
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

	public static IFilePermissionsService getPermissionsService(IRemoteFile remoteFile){

		if (remoteFile instanceof IAdaptable){
			return (IFilePermissionsService)((IAdaptable)remoteFile).getAdapter(IFilePermissionsService.class);
		}

		return null;
	}
	
	public static Object deploy(File srcFileOrFolder, IRemoteFile targetFolder, IProgressMonitor monitor)
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

		String name = srcFileOrFolder.getName();

		if (srcFileOrFolder.isFile())
		{
			StringBuffer newPathBuf = new StringBuffer(targetFolder.getAbsolutePath());
			newPathBuf.append(targetFolder.getSeparatorChar());
			newPathBuf.append(name);	
			String newPath = newPathBuf.toString();

			try
			{
				IRemoteFile copiedFile = null;

				// just copy using local location
				String srcFileLocation = srcFileOrFolder.toString();
				String srcCharSet = null;

				boolean isText = RemoteFileUtility.getSystemFileTransferModeRegistry().isText(srcFileLocation);
				if (isText)
				{
					srcCharSet = RemoteFileUtility.getSourceEncoding((IFile)srcFileOrFolder);
					// for bug 236723, getting remote encoding for target instead of default for target fs
					String remoteEncoding = targetFolder.getEncoding();
					targetFS.upload(srcFileLocation, srcCharSet, newPath, remoteEncoding, monitor);
				}else {
					targetFS.upload(srcFileLocation, null, newPath, null, monitor);
				}

				copiedFile = targetFS.getRemoteFileObject(targetFolder, name, monitor);

				// Set executable permission if necessary 
				if (srcFileOrFolder.canExecute()){
					IFilePermissionsService service = getPermissionsService(copiedFile);

					int capabilities = service.getCapabilities(copiedFile.getHostFile());
					if ((capabilities & IFilePermissionsService.FS_CAN_SET_PERMISSIONS) != 0){
						IHostFilePermissions permission = service.getFilePermissions(copiedFile.getHostFile(), new NullProgressMonitor());
						permission.setPermissionBits(permission.getPermissionBits() | IHostFilePermissions.PERM_ANY_EXECUTE);
						service.setFilePermissions(copiedFile.getHostFile(), permission, new NullProgressMonitor());
					}
				}

				return copiedFile;
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
			// recursively copy
			try
			{
				IRemoteFile newTargetFolder = targetFS.getRemoteFileObject(newPath, monitor);
				if (!newTargetFolder.exists())
				{
					targetFS.createFolder(newTargetFolder, monitor);
					newTargetFolder.markStale(true);
					newTargetFolder = targetFS.getRemoteFileObject(newPath, monitor);
				}

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
						if (deploy(child, newTargetFolder, monitor) == null)
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
