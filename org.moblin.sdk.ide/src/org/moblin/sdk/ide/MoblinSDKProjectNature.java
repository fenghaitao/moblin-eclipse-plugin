package org.moblin.sdk.ide;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.cdt.autotools.core.AutotoolsNewProjectNature;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.AutotoolsConfigurationManager;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IAConfiguration;
import org.eclipse.linuxtools.internal.cdt.autotools.core.configure.IConfigureOption;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckRequestFrom;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckResults;
import org.moblin.sdk.ide.preferences.PreferenceConstants;

@SuppressWarnings("restriction")
public class MoblinSDKProjectNature implements IProjectNature {
	public static final  String MoblinSDK_NATURE_ID = MoblinSDKPlugin.getUniqueIdentifier() + ".MoblinSDKNature";
	private static final String WIZARD_WARNING_TITLE = "Wizard.SDK.Warning.Title";
	
	private IProject proj;

	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return proj;
	}

	public void setProject(IProject project) {
		this.proj = project;
	}

	public static void addMoblinSDKNature(IProject project, IProgressMonitor monitor) throws CoreException {
		AutotoolsNewProjectNature.addNature(project, MoblinSDK_NATURE_ID, monitor);		
	}
	
	public static void setEnvironmentVariables(IProject project, String toolchain_location, String toolchain_triplet){
		ICProjectDescription cpdesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription ccdesc = cpdesc.getActiveConfiguration();
		IEnvironmentVariableManager manager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment env = manager.getContributedEnvironment();
		String delimiter = manager.getDefaultDelimiter();

		// PATH
		String sys_path    = System.getenv("PATH");
		String moblin_path = "";
		if (sys_path != null) {
			moblin_path = toolchain_location + File.separator + "bin" + delimiter + sys_path;
		} else {
			moblin_path = toolchain_location + File.separator + "bin";
		}
		env.addVariable("PATH", moblin_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);
		
		// PKG_CONFIG_SYSROOT_DIR
		String moblin_pkg_sys_root = toolchain_location + File.separator + toolchain_triplet + File.separator + "sys-root";
		env.addVariable("PKG_CONFIG_SYSROOT_DIR", moblin_pkg_sys_root, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		// PKG_CONFIG_PATH
        String moblin_pkg_path1 = moblin_pkg_sys_root + File.separator + "usr" + File.separator + "lib"   + File.separator + "pkgconfig";
        String moblin_pkg_path2 = moblin_pkg_sys_root + File.separator + "usr" + File.separator + "share" + File.separator + "pkgconfig";
        String moblin_pkg_path =  moblin_pkg_path1 + delimiter + moblin_pkg_path2;
        env.addVariable("PKG_CONFIG_PATH", moblin_pkg_path, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

        // host_alias
		env.addVariable("host_alias", toolchain_triplet, IEnvironmentVariable.ENVVAR_REPLACE, delimiter, ccdesc);

		try {
			CoreModel.getDefault().setProjectDescription(project, cpdesc);
		} catch (CoreException e) {
			// do nothing
		}	
	}
	
	public static void configureAutotoolsOptions(IProject project, String toolchain_location, String toolchain_triplet) {
		String host_arg = "host_alias=" + toolchain_triplet;

		IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
		IConfiguration icfg = info.getDefaultConfiguration();
		IAConfiguration cfg = AutotoolsConfigurationManager.getInstance().getConfiguration(project, icfg.getName());
		Collection<IConfigureOption> values = cfg.getOptions().values(); 
		for (Iterator<IConfigureOption> j = values.iterator(); j.hasNext();) {
			IConfigureOption opt = j.next();
			if (opt.getName().equals("user")){
				opt.setValue(host_arg);
			} else if (opt.getName().equals("autogenOpts")){
				opt.setValue(host_arg);
			}
		}

		AutotoolsConfigurationManager.getInstance().saveConfigs(project.getName());
	}
	
	public static void configureAutotools(IProject project) {
		IPreferenceStore store = MoblinSDKPlugin.getDefault().getPreferenceStore();
		String toolchain_location  = store.getString(PreferenceConstants.TOOLCHAIN_LOCACATION);
		String toolchain_triplet  = store.getString(PreferenceConstants.TOOLCHAIN_TRIPLET);

		SDKCheckResults result = MoblinSDKChecker.checkMoblinSDK(toolchain_location, toolchain_triplet);
		if (result == SDKCheckResults.SDK_PASS){
			setEnvironmentVariables(project, toolchain_location, toolchain_triplet);
			//configureAutotoolsOptions(project, toolchain_location, toolchain_triplet);
		}else {
			String title   =  MoblinSDKMessages.getString(WIZARD_WARNING_TITLE);		
			String message =  MoblinSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Wizard);
			MessageDialog.openWarning(MoblinSDKPlugin.getActiveWorkbenchShell(), title, message);
		}
	}
}