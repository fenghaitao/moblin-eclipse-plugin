package org.moblin.sdk.ide.actions;

import org.eclipse.cdt.ui.templateengine.uitree.InputUIElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.moblin.sdk.ide.MoblinSDKChecker;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckRequestFrom;
import org.moblin.sdk.ide.MoblinSDKChecker.SDKCheckResults;

public class SDKLocationDialog extends Dialog {
	private String title;
	private String toolchain_location_name;
	private String toolchain_location_value;
	private String toolchain_triplet_name;
	private String toolchain_triplet_value;
	private String toolchain_location_ret_value = null;
	private String toolchain_triplet_ret_value = null;
	
	private Text location_value;
	private Text triplet_value;

	private Text errorMessageText;

	public SDKLocationDialog(Shell parentShell, String dialogTitle, String location_name, String location_value, 
							 String triplet_name, String triplet_value, IInputValidator validator) {
        super(parentShell);
        this.toolchain_location_name  = location_name;
        this.toolchain_location_value = location_value;
        this.toolchain_triplet_name   = triplet_name;
        this.toolchain_triplet_value  = triplet_value;
        this.title = dialogTitle;
        setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		point.x = 640;
		return point;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		
		GridLayout layout = new GridLayout(2, false);
		composite.setLayout(layout);
		
		Label location_label = new Label(composite, SWT.LEAD);
		location_label.setText(toolchain_location_name);
		location_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		location_label.setFont(parent.getFont());
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		Composite textConatiner1 = new Composite(composite, SWT.NONE);
		textConatiner1.setLayout(new GridLayout(2, false));
		textConatiner1.setLayoutData(data);

		location_value = new Text(textConatiner1, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		location_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		location_value.setText(toolchain_location_value);
		location_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Button button = new Button(textConatiner1, SWT.PUSH | SWT.LEAD);
		button.setText(InputUIElement.BROWSELABEL);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String dirName = new DirectoryDialog(composite.getShell()).open();
				if (dirName != null) {
					location_value.setText(dirName);
				}
			}
		});

		Label triplet_label = new Label(composite, SWT.LEAD);
		triplet_label.setText(toolchain_triplet_name);
		triplet_label.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		triplet_label.setFont(parent.getFont());

		Composite textConatiner2 = new Composite(composite, SWT.NONE);
		textConatiner2.setLayout(new GridLayout(1, false));
		textConatiner2.setLayoutData(data);
		triplet_value = new Text(textConatiner2, SWT.SINGLE | SWT.LEAD | SWT.BORDER);
		triplet_value.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		triplet_value.setText(toolchain_triplet_value);
		triplet_value.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateInput();
            }
        });

		Composite textConatiner3 = new Composite(composite, SWT.NONE);
		textConatiner3.setLayout(new GridLayout(1, false));
		textConatiner3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));

		errorMessageText = new Text(textConatiner3, SWT.READ_ONLY);
		errorMessageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_RED));
        setErrorMessage(null);

		return composite;
	}

	protected void validateInput() {
        String errorMessage = null;
        String toolchain_location = location_value.getText();
        String toolchain_triplet  = triplet_value.getText();
		SDKCheckResults result = MoblinSDKChecker.checkMoblinSDK(toolchain_location, toolchain_triplet);
		if (result != SDKCheckResults.SDK_PASS) {
			errorMessage = MoblinSDKChecker.getErrorMessage(result, SDKCheckRequestFrom.Menu);
		}
        setErrorMessage(errorMessage);
    }

    public void setErrorMessage(String errorMessage) {
    	if (errorMessageText != null && !errorMessageText.isDisposed()) {
    		errorMessageText.setText(errorMessage == null ? " \n " : errorMessage);
    		boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
    		errorMessageText.setEnabled(hasError);
    		errorMessageText.setVisible(hasError);
    		errorMessageText.getParent().update();
    		Control button = getButton(IDialogConstants.OK_ID);
    		if (button != null) {
    			button.setEnabled(errorMessage == null);
    		}
    	}
    }

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			toolchain_location_ret_value = location_value.getText();
			toolchain_triplet_ret_value  = triplet_value.getText();
		}
		super.buttonPressed(buttonId);
	}
	
	public String getToolchainLocation() {
		return toolchain_location_ret_value;
	}

	public String getToolchainTriplet() {
		return toolchain_triplet_ret_value;
	}
}