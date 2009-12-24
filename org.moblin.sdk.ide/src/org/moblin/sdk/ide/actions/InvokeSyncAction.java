package org.moblin.sdk.ide.actions;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
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
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.internal.cdt.autotools.core.AutotoolsNewMakeGenerator;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeAction;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.InvokeMessages;
import org.moblin.sdk.ide.MoblinSDKPlugin;

@SuppressWarnings("restriction")
public class InvokeSyncAction extends InvokeAction {
	protected void executeConsoleCommandSync(final IConsole console, final String actionName, final String command,
			final String[] argumentList, final IPath execDir) {
		// We need to use a workspace root scheduling rule because adding MakeTargets
		// may end up saving the project description which runs under a workspace root rule.
		final ISchedulingRule rule = ResourcesPlugin.getWorkspace().getRoot();

		Job backgroundJob = new Job(actionName) {
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

						public void run(IProgressMonitor monitor) throws CoreException {
							try {
								String errMsg = null;
								IProject project = getSelectedContainer().getProject();
								// Get a build console for the project
								ConsoleOutputStream consoleOutStream = console.getOutputStream();
								// FIXME: we want to remove need for ManagedBuilderManager, but how do we
								// get environment variables.
								IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
								IConfiguration cfg = info.getDefaultConfiguration();

								StringBuffer buf = new StringBuffer();
								String[] consoleHeader = new String[3];

								consoleHeader[0] = actionName;
								consoleHeader[1] = cfg.getName();
								consoleHeader[2] = project.getName();
								buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
								String invokeMsg = InvokeMessages.getFormattedString("InvokeAction.console.message", //$NON-NLS-1$
										new String[]{actionName, execDir.toString()}); //$NON-NLS-1$
								buf.append(invokeMsg);
								buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
								buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
								consoleOutStream.write(buf.toString().getBytes());
								consoleOutStream.flush();
								
								ArrayList<String> additionalEnvs = new ArrayList<String>();
								String strippedCommand = AutotoolsNewMakeGenerator.stripEnvVars(command, additionalEnvs);
								// Get a launcher for the config command
								CommandLauncher launcher = new CommandLauncher();
								// Set the environment
								IEnvironmentVariable variables[] = ManagedBuildManager
										.getEnvironmentVariableProvider().getVariables(cfg, true);
								String[] env = null;
								ArrayList<String> envList = new ArrayList<String>();
								if (variables != null) {
									for (int i = 0; i < variables.length; i++) {
										envList.add(variables[i].getName()
												+ "=" + variables[i].getValue()); //$NON-NLS-1$
									}
									if (additionalEnvs.size() > 0)
										envList.addAll(additionalEnvs); // add any additional environment variables specified ahead of script
									env = (String[]) envList.toArray(new String[envList.size()]);
								}
								OutputStream stdout = consoleOutStream;
								OutputStream stderr = consoleOutStream;

								launcher.showCommand(true);
								// Run the shell script via shell command.
								Process proc = launcher.execute(new Path(strippedCommand), argumentList, env,
										execDir, new NullProgressMonitor());
								if (proc != null) {
									try {
										// Close the input of the process since we will never write to
										// it
										proc.getOutputStream().close();
									} catch (IOException e) {
									}

									if (launcher.waitAndRead(stdout, stderr, new SubProgressMonitor(
											monitor, IProgressMonitor.UNKNOWN)) != CommandLauncher.OK) {
										errMsg = launcher.getErrorMessage();
									}
								} else {
									errMsg = launcher.getErrorMessage();
								}
								
								if (errMsg != null)
									MoblinSDKPlugin.logErrorMessage(errMsg);
								
							} catch (IOException e) {
								MoblinSDKPlugin.log(e);
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

		try {
			backgroundJob.join();
		} catch (InterruptedException e) {
		}
	}
}
