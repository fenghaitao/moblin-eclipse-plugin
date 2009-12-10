package org.moblin.sdk.ide.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedMap;

import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyManager;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.BuildListComparator;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.ui.properties.Messages;
import org.eclipse.cdt.managedbuilder.ui.wizards.AbstractCWizard;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.cdt.ui.wizards.EntryDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.wizards.AutotoolsBuildWizardHandler;

@SuppressWarnings("restriction")
public class MoblinBuildWizard extends AbstractCWizard {
	public static final String OTHERS_LABEL = Messages.getString("CNewWizard.0");
	public static final String MOBLIN_PROJECTTYPE_ID = "org.moblin.sdk.ide.projectType";
	
	/**
	 * @since 5.1
	 */
	public static final String EMPTY_PROJECT = Messages.getString("AbstractCWizard.0");

	@Override
	public EntryDescriptor[] createItems(boolean supportedOnly, IWizard wizard) {
		IBuildPropertyManager bpm = ManagedBuildManager.getBuildPropertyManager();
		IBuildPropertyType bpt = bpm.getPropertyType(MBSWizardHandler.ARTIFACT);
		IBuildPropertyValue[] vs = bpt.getSupportedValues();
		Arrays.sort(vs, BuildListComparator.getInstance());
		ArrayList<EntryDescriptor> items = new ArrayList<EntryDescriptor>();
		
		// look for Autotools project type
		EntryDescriptor oldsRoot = null;
		SortedMap<String, IProjectType> sm = ManagedBuildManager.getExtensionProjectTypeMap();
		for (String s : sm.keySet()) {
			IProjectType pt = (IProjectType)sm.get(s);
			if (pt.getId().equals(MOBLIN_PROJECTTYPE_ID)) {
				AutotoolsBuildWizardHandler h = new AutotoolsBuildWizardHandler(pt, parent, wizard);
				IToolChain[] tcs = ManagedBuildManager.getExtensionToolChains(pt);
				for(int i = 0; i < tcs.length; i++){
					IToolChain t = tcs[i];
					if(t.isSystemObject()) 
						continue;
					if (!isValid(t, supportedOnly, wizard))
						continue;

					h.addTc(t);
				}

				String pId = null;
				if (CDTPrefUtil.getBool(CDTPrefUtil.KEY_OTHERS)) {
					if (oldsRoot == null) {
						oldsRoot = new EntryDescriptor(OTHERS_LABEL, null, OTHERS_LABEL, true, null, null);
						items.add(oldsRoot);
					}
					pId = oldsRoot.getId();
				} else { // do not group to <Others>
					pId = null;
				}
				items.add(new EntryDescriptor(pt.getId(), pId, pt.getName(), true, h, null));
			}
		}
		return (EntryDescriptor[])items.toArray(new EntryDescriptor[items.size()]);
	}
}