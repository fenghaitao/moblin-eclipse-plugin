package org.moblin.sdk.ide.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.linuxtools.internal.cdt.autotools.ui.actions.AbstractAutotoolsHandler;

@SuppressWarnings("restriction")
public class ConfigureHandler extends AbstractAutotoolsHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ConfigureAction a = new ConfigureAction();
		Object o = event.getApplicationContext();
		if (o instanceof IEvaluationContext) {
			IContainer container = getContainer((IEvaluationContext)o);
			if (container != null) {
				a.setSelectedContainer(container);
				a.run(null);
			}
		}
		return null;
	}
}
