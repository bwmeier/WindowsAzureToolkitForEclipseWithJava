/**
* Copyright 2014 Microsoft Open Technologies, Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*	 http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package com.persistent.ui.projwizard;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;

import waeclipseplugin.Activator;

import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.persistent.util.AppCmpntParam;
import com.persistent.util.JdkSrvConfig;
import com.persistent.util.JdkSrvConfigListener;
import com.persistent.util.WAEclipseHelper;
/**
 * Class creates wizard page which has
 * JDK, Server and Application tabs.
 * Also has listeners for UI components.
 */
public class WATabPage extends WizardPage {

	private static TabFolder folder;
	private TabItem srvTab;
	private static TabItem jdkTab;
	private TabItem appTab;
	private ArrayList<AppCmpntParam> appList =
			new ArrayList<AppCmpntParam>();
	private Object pageObj;
	private boolean isWizard;
	private WindowsAzureRole waRole;
	private boolean inHandlePgComplete = false;
	private boolean inHndlPgCmpltBackBtn = false;
	private boolean dueToBackBtn = false;
	private File cmpntFile = new File(WAEclipseHelper.
			getTemplateFile(Messages.cmpntFile));
	private int prevTabIndex;
	private static boolean accepted = false;
	private String jdkPrevName;

	/**
	 * Constructor.
	 * @param pageName
	 * @param role
	 * @param pageObj
	 * @param isWizard
	 */
	protected WATabPage(String pageName,
			WindowsAzureRole role, Object pageObj,
			boolean isWizard) {
		super(pageName);
		this.waRole = role;
		this.pageObj = pageObj;
		this.isWizard = isWizard;
		setTitle(Messages.wizPageTitle);
		setDescription(Messages.dplPageJdkMsg);
		setPageComplete(true);
		if (!Activator.getDefault().isContextMenu()) {
			try {
				AppCmpntParam acp = new AppCmpntParam();
				acp.setImpAs(waRole.getComponents().
						get(0).getDeployName());
				appList.add(acp);
			} catch (WindowsAzureInvalidProjectOperationException e) {
				Activator.getDefault().log(e.getMessage());
			}
		}
	}

	@Override
	public void createControl(Composite parent) {
		// display help contents
		PlatformUI
		.getWorkbench()
		.getHelpSystem()
		.setHelp(parent.getShell(),
				"com.persistent.winazure.eclipseplugin."
						+ "windows_azure_project");
		// Tab controls
		folder = new TabFolder(parent, SWT.NONE);

		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
		folder.setLayoutData(gridData);

		// Tab for JDK
		jdkTab = new TabItem(folder, SWT.NONE);
		jdkTab.setText(Messages.dplPageJDKGrp);
		jdkTab.setControl(createJDK(folder));

		// Tab for Server
		srvTab = new TabItem(folder, SWT.NONE);
		srvTab.setText(Messages.dplPageSerTxt);
		srvTab.setControl(createServer(folder));

		// Tab for Application
		appTab = new TabItem(folder, SWT.NONE);
		appTab.setText(Messages.dplPageAppLbl);
		appTab.setControl(createAppTblCmpnt(folder));

		/*
		 * Set the page description
		 * according to the tab selected by user.
		 */
		folder.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (folder.getSelectionIndex() == 0) {
					changeToJdkTab();
				} else if (folder.getSelectionIndex() == 1) {
					changeToSrvTab();
				} else if (folder.getSelectionIndex() == 2) {
					changeToAppTab();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		/*
		 * If wizard activated through right click on
		 * Dynamic web project then
		 * enable JDK and Server components.
		 */
		if (Activator.getDefault().isContextMenu()) {
			JdkSrvConfigListener.jdkChkBoxChecked(waRole);
			JdkSrvConfigListener.srvChkBoxChecked(waRole,
					Messages.dlNtLblDirSrv);
			JdkSrvConfig.getSerCheckBtn().setSelection(true);
			handlePageComplete();
		}

		setControl(folder);
		// Set by default tab selection to JDK
		folder.setSelection(jdkTab);
		prevTabIndex = 0;
	}

	private void changeToSrvTab() {
		if (displayLicenseAgreement()) {
			getWizard().getPage(Messages.tbPg).
			setDescription(Messages.dplPageSrvMsg);
			folder.setSelection(srvTab);
			prevTabIndex = 1;
		} else {
			folder.setSelection(jdkTab);
			prevTabIndex = 0;
		}
	}

	private void changeToAppTab() {
		if (displayLicenseAgreement()) {
			getWizard().getPage(Messages.tbPg).
			setDescription(Messages.dplPageAppMsg);
			folder.setSelection(appTab);
			prevTabIndex = 2;
		} else {
			folder.setSelection(jdkTab);
			prevTabIndex = 0;
		}
	}

	private void changeToJdkTab() {
		folder.setSelection(jdkTab);
		getWizard().getPage(Messages.tbPg).
		setDescription(Messages.dplPageJdkMsg);
		prevTabIndex = 0;
	}

	/**
	 * Method checks which page to display next.
	 * Depending on which tab is active,
	 * and the state of the check boxes,
	 * it either activates the next tab
	 * or jumps to the next screen
	 * (Key feature page) in the wizard.
	 */
	@Override
	public IWizardPage getNextPage() {
		int tabIndex = folder.getSelectionIndex();
		IWizardPage page = getWizard().getPage(Messages.tbPg);
		/*
		 * If getNextPage() is called due to
		 * setPageComplete() method
		 * then don't check anything.
		 * Be on same page.
		 */
		if (inHandlePgComplete) {
			folder.setSelection(tabIndex);
		} else if (!dueToBackBtn) {
			/*
			 * Next button has been clicked.
			 */
			if (tabIndex == 0
					&& isJdkChecked()
					&& !getJdkLoc().isEmpty()) {
				changeToSrvTab();
			} else if (tabIndex == 1
					&& isSrvChecked()
					&& !getServerName().isEmpty()
					&& !getServerLoc().isEmpty()
					&& isJdkChecked()) {
				changeToAppTab();
			} else {
				page = getWizard().getPage(Messages.keyPg);
				if (isSrvChecked()) {
					prevTabIndex = 2;
				} else if (isJdkChecked()) {
					prevTabIndex = 1;
				} else {
					prevTabIndex = 0;
				}
			}
		}
		dueToBackBtn = false;
		inHandlePgComplete = false;
		return page;
	}

	@Override
	public IWizardPage getPreviousPage() {
		int tabIndex = folder.getSelectionIndex();
		IWizardPage page = getWizard().getPage(Messages.tbPg);
		/*
		 * If getPreviousPage() is called due to
		 * setPageComplete() method
		 * then don't check anything.
		 * Be on same page.
		 */
		if (inHndlPgCmpltBackBtn) {
			folder.setSelection(tabIndex);
		} else {
			/*
			 * Back button has been clicked.
			 */
			dueToBackBtn = true;
			if (tabIndex == 1) {
				changeToJdkTab();
			} else if (tabIndex == 2) {
				folder.setSelection(srvTab);
				page.setDescription(Messages.dplPageSrvMsg);
				prevTabIndex = 1;
			} else {
				page = super.getPreviousPage();
				prevTabIndex = 0;
			}
		}
		inHndlPgCmpltBackBtn = false;
		return page;
	}

	/**
	 * Handles the page complete event of deploy page.
	 * Validates all the fields.
	 */
	public void handlePageComplete() {
		boolean isJdkValid = false;
		inHandlePgComplete = true;
		inHndlPgCmpltBackBtn = true;
		// JDK
		if (JdkSrvConfig.getJdkCheckBtn().getSelection()) {
			if (JdkSrvConfig.getTxtJdk().getText().isEmpty()) {
				setErrorMessage(Messages.jdkPathErrMsg);
				setPageComplete(false);
			} else {
				File file = new File(
						JdkSrvConfig.getTxtJdk().getText());
				if (!file.exists()) {
					setErrorMessage(Messages.dplWrngJdkMsg);
					setPageComplete(false);
				} else {
					// JDK download group
					// cloud radio button selected
					if (JdkSrvConfig.getDlRdCldBtn().getSelection()) {
						// Validate JDK URL
						String url = getJdkUrl();
						if (url.isEmpty()) {
							setErrorMessage(Messages.dlgDlUrlErrMsg);
							setPageComplete(false);
						} else {
							try {
								new URL(url);
								if (WAEclipseHelper.isBlobStorageUrl(url)) {
									String javaHome = JdkSrvConfig.getTxtJavaHome().
											getText().trim();
									if (javaHome.isEmpty()) {
										setPageComplete(false);
										setErrorMessage(Messages.jvHomeErMsg);
									} else {
										setErrorMessage(null);
										setPageComplete(true);
										isJdkValid = true;
									}
								} else {
									setErrorMessage(Messages.dlgDlUrlErrMsg);
									setPageComplete(false);
								}
							} catch (MalformedURLException e) {
								setErrorMessage(Messages.dlgDlUrlErrMsg);
								setPageComplete(false);
							}
						}
					}
					// No Validation needed if auto upload or
					// third party JDK is selected
					// local radio button selected
					else {
						setErrorMessage(null);
						setPageComplete(true);
						isJdkValid = true;
					}
				}
			}
			// Server
			if (isJdkValid && JdkSrvConfig.getSerCheckBtn().getSelection()) {
				inHandlePgComplete = true;
				inHndlPgCmpltBackBtn = true;
				if (JdkSrvConfig.getComboServer().getText().isEmpty()) {
					setErrorMessage(Messages.dplEmtSerMsg);
					setPageComplete(false);
				} else if (JdkSrvConfig.getTxtDir().getText().isEmpty()) {
					setErrorMessage(Messages.dplEmtSerPtMsg);
					setPageComplete(false);
				} else if (!(new File(JdkSrvConfig.getTxtDir().
						getText()).exists())) {
					setErrorMessage(Messages.dplWrngSerMsg);
					setPageComplete(false);
				} else {
					// Server download group
					if (JdkSrvConfig.getDlRdCldBtnSrv().getSelection()) {
						String srvUrl = getSrvUrl();
						if (srvUrl.isEmpty()) {
							setErrorMessage(Messages.dlgDlUrlErrMsg);
							setPageComplete(false);
						} else {
							try {
								// Validate Server URL
								new URL(srvUrl);
								if (WAEclipseHelper.isBlobStorageUrl(srvUrl)) {
									String srvHome = JdkSrvConfig.getTxtHomeDir().
											getText().trim();
									if (srvHome.isEmpty()) {
										setPageComplete(false);
										setErrorMessage(Messages.srvHomeErMsg);
									} else {
										setErrorMessage(null);
										setPageComplete(true);
									}
								} else {
									setErrorMessage(Messages.dlgDlUrlErrMsg);
									setPageComplete(false);
								}
							} catch (MalformedURLException e) {
								setErrorMessage(Messages.dlgDlUrlErrMsg);
								setPageComplete(false);
							}
						}
					}
					// No validations if auto upload Server is selected
					// local radio button selected
					else {
						setErrorMessage(null);
						setPageComplete(true);
					}
				}
			}
		} else {
			setErrorMessage(null);
			setPageComplete(true);
		}
	}

	/**
	 * Creates the JDK component.
	 * @param parent : parent container
	 * @return Control
	 */
	Control createJDK(Composite parent) {
		Control control = JdkSrvConfig.createJDKGrp(parent);
		// listener for JDK check button.
		JdkSrvConfig.getJdkCheckBtn().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getJdkCheckBtn().
						getSelection()) {
					JdkSrvConfigListener.jdkChkBoxChecked(waRole);
				} else {
					JdkSrvConfigListener.jdkChkBoxUnChecked();
					accepted = false;
				}
				handlePageComplete();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		JdkSrvConfig.getTxtJdk().setEnabled(false);
		// Modify listener for JDK location text box.
		JdkSrvConfig.getTxtJdk().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				JdkSrvConfigListener.modifyJdkText(waRole,
						Messages.dlNtLblDir);
				handlePageComplete();
			}
		});

		// Focus listener for JDK location text box.
		JdkSrvConfig.getTxtJdk().
		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				String jdkPath = JdkSrvConfig.getTxtJdk().getText();
				// Update note below URL text box
				JdkSrvConfigListener.focusLostJdkText(jdkPath);
			}
		});

		JdkSrvConfig.getBtnJdkLoc().setEnabled(false);

		// listener for JDK browse button.
		JdkSrvConfig.getBtnJdkLoc().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfig.utilJdkBrowseBtnListener(
						Messages.dlNtLblDir);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// JDK download group
		// listener for JDK deploy radio button.
		JdkSrvConfig.getDlRdCldBtn().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getDlRdCldBtn().getSelection()) {
					JdkSrvConfig.getTxtUrl().setText(
							JdkSrvConfig.getUrl(
									JdkSrvConfig.getCmbStrgAccJdk()));
					JdkSrvConfigListener.jdkDeployBtnSelected(waRole);
				}
				handlePageComplete();
				accepted = false;
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for JDK auto upload radio button.
		JdkSrvConfig.getAutoDlRdCldBtn().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getAutoDlRdCldBtn().getSelection()) {
					// auto upload radio button selected
					JdkSrvConfigListener.
					configureAutoUploadJDKSettings(
							waRole, Messages.dlNtLblDir);
				}
				handlePageComplete();
				accepted = false;
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for third party JDK radio button.
		JdkSrvConfig.getThrdPrtJdkBtn().
		addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				/*
				 * Check if third party radio button
				 * is already selected
				 * and user is selecting same radio button again
				 * then do not do any thing.
				 */
				if (!JdkSrvConfig.getThrdPrtJdkCmb().isEnabled()) {
					JdkSrvConfigListener.thirdPartyJdkBtnSelected(waRole,
							Messages.dlNtLblDir);
					jdkPrevName = JdkSrvConfig.
							getThrdPrtJdkCmb().getText();
				}
			}
		});

		// listener for JDK URL text.
		JdkSrvConfig.getTxtUrl().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				if (isJdkAutoUploadChecked()) {
					handlePageComplete();
					// no need to do any checks if auto upload is selected
					return;
				}
				JdkSrvConfigListener.modifyJdkUrlText();
				handlePageComplete();
			}
		});

		// listener for JAVA_HOME text box.
		JdkSrvConfig.getTxtJavaHome().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				handlePageComplete();
			}
		});

		// listener for Accounts link on JDK tab.
		JdkSrvConfig.getAccLinkJdk().
		addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				JdkSrvConfigListener.jdkAccLinkClicked();
				handlePageComplete();
			}
		});

		// listener for storage account combo box on JDK tab.
		JdkSrvConfig.getCmbStrgAccJdk().
		addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfig.updateJDKDlURL();
				handlePageComplete();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for JDK customize link.
		JdkSrvConfig.getThrdPrtJdkLink()
		.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfig.custLinkListener(
						Messages.dplSerBtnTtl,
						Messages.dplSerBtnMsg,
						isWizard,
						getShell(),
						pageObj, cmpntFile);
			}
		});

		// listener for third party JDK combo box.
		JdkSrvConfig.getThrdPrtJdkCmb().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfigListener.thirdPartyComboListener();
				/*
				 * If JDK name is changed by user then license
				 * has to be accepted again.
				 */
				String currentName = JdkSrvConfig.
						getThrdPrtJdkCmb().getText();
				if (!currentName.equalsIgnoreCase(jdkPrevName)) {
					accepted = false;
					jdkPrevName = currentName;
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		return control;
	}

	/**
	 * Creates the server components.
	 * @param parent : parent container
	 * @return Control
	 */
	Control createServer(Composite parent) {
		Control control = JdkSrvConfig.createServerGrp(parent);
		// listener for Server check button.
		JdkSrvConfig.getSerCheckBtn()
		.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getSerCheckBtn().getSelection()) {
					JdkSrvConfigListener.srvChkBoxChecked(waRole,
							Messages.dlNtLblDirSrv);
				} else {
					JdkSrvConfigListener.srvChkBoxUnChecked();
				}
				handlePageComplete();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
		// Modify listener for Server location text box.
		JdkSrvConfig.getTxtDir().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				JdkSrvConfigListener.modifySrvText(waRole,
						Messages.dlNtLblDirSrv);
				handlePageComplete();
			}
		});

		// Focus listener for Server location text box.
		JdkSrvConfig.getTxtDir().
		addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent arg0) {
				// Update note below URL text box
				String path = JdkSrvConfig.
						getTxtDir().getText().trim();
				JdkSrvConfigListener.
				focusLostSrvText(path,
						Messages.dlNtLblDirSrv,
						Messages.dlNtLblUrlSrv);
			}

			@Override
			public void focusGained(FocusEvent arg0) {
			}
		});

		// listener for Server browse button.
		JdkSrvConfig.getBtnSrvLoc().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				serBrowseBtnListener();
				JdkSrvConfigListener.modifySrvText(waRole,
						Messages.dlNtLblDirSrv);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for Server type combo box.
		JdkSrvConfig.getComboServer().
		addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.isSrvDownloadChecked()
						|| JdkSrvConfig.isSrvAutoUploadChecked()) {
					JdkSrvConfig.updateServerHome(waRole);
				}
				handlePageComplete();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for Server customize link.
		JdkSrvConfig.getCustLink()
		.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfig.custLinkListener(
						Messages.dplSerBtnTtl,
						Messages.dplSerBtnMsg,
						isWizard,
						getShell(),
						pageObj, cmpntFile);
			}
		});

		// Server download group
		// listener for Server deploy radio button.
		JdkSrvConfig.getDlRdCldBtnSrv().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getDlRdCldBtnSrv()
						.getSelection()) {
					JdkSrvConfigListener.srvDeployBtnSelected(waRole,
							Messages.dlNtLblDirSrv);
				}
				handlePageComplete();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		// listener for Server auto radio button.
		JdkSrvConfig.getAutoDlRdCldBtnSrv().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (JdkSrvConfig.getAutoDlRdCldBtnSrv()
						.getSelection()) {
					// server auto upload radio button selected
					JdkSrvConfigListener.
					configureAutoUploadServerSettings(waRole,
							Messages.dlNtLblDirSrv);
				} else {
					/*
					 * server auto upload radio button unselected
					 * and deploy button selected.
					 */
					if (JdkSrvConfig.getDlRdCldBtnSrv().getSelection()) {
						JdkSrvConfig.getTxtUrlSrv().setText(
								JdkSrvConfig.getUrl(
										JdkSrvConfig.getCmbStrgAccSrv()));
						return;
					}
				}
				handlePageComplete();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});


		// listener for Server URL text box.
		JdkSrvConfig.getTxtUrlSrv().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				// If auto upload is selected, no need to handle this case
				if (JdkSrvConfig.isSrvAutoUploadChecked()) { 
					handlePageComplete();
					return;
				}
				JdkSrvConfigListener.modifySrvUrlText();
				handlePageComplete();
			}
		});

		// listener for server home directory text box.
		JdkSrvConfig.getTxtHomeDir().
		addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				handlePageComplete();
			}
		});

		// listener for Accounts link on server tab.
		JdkSrvConfig.getAccLinkSrv().
		addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				JdkSrvConfigListener.srvAccLinkClicked();
				handlePageComplete();
			}
		});

		// listener for storage account combo box on server tab.
		JdkSrvConfig.getCmbStrgAccSrv().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				JdkSrvConfig.updateServerDlURL();
				JdkSrvConfig.updateServerHome(waRole);
				handlePageComplete();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		return control;
	}

	/**
	 * Listener for server browse button it is used in file system button.
	 * It will open the file system location.
	 */
	protected void serBrowseBtnListener() {
		JdkSrvConfig.utilSerBrowseBtnListener(Messages.dlNtLblDirSrv);
		handlePageComplete();
	}

	/**
	 * Creates the application table component.
	 * @param parent : container
	 * @return
	 */
	Control createAppTblCmpnt(Composite parent) {
		Control control = JdkSrvConfig.createAppTbl(parent);
		JdkSrvConfig.getTableViewer()
		.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer arg0,
					Object arg1, Object arg2) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public Object[] getElements(Object arg0) {
				return getAppsAsNames().toArray();
			}
		});

		JdkSrvConfig.getTableViewer()
		.setLabelProvider(new ITableLabelProvider() {
			@Override
			public void removeListener(ILabelProviderListener arg0) {
			}

			@Override
			public boolean isLabelProperty(Object arg0, String arg1) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener arg0) {
			}

			@Override
			public String getColumnText(Object element, int colIndex) {
				String result = "";
				if (colIndex == 0) {
					result = element.toString();
				}
				return result;
			}
			@Override
			public Image getColumnImage(Object arg0, int arg1) {
				return null;
			}
		});

		JdkSrvConfig.getTableViewer().
		setInput(getAppsAsNames());

		// Add selection listener for Add Button
		JdkSrvConfig.getBtnAdd()
		.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				addButtonListener();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

		JdkSrvConfig.getTblApp().
		addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				JdkSrvConfig.getBtnRemove().
				setEnabled(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {

			}
		});
		// Add selection listener for Remove Button
		JdkSrvConfig.getBtnRemove().
		addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				removeButtonListener();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		// By default disable server component
		// as JDK not selected.
		if (isJdkChecked()) {
			JdkSrvConfig.setEnableServer(true);
		} else {
			JdkSrvConfig.setEnableServer(false);
		}
		return control;
	}

	/**
	 * Add Application button listener.
	 */
	private void addButtonListener() {
		WAApplicationDialog dialog = new WAApplicationDialog(getShell(),
				this, waRole, null);
		dialog.open();
		JdkSrvConfig.getTableViewer().refresh();
	}

	/**
	 * Remove application button listener.
	 */
	private void removeButtonListener() {
		int selIndex = JdkSrvConfig.getTableViewer()
				.getTable().getSelectionIndex();
		if (selIndex > -1) {
			try {
				appList.remove(selIndex);
				JdkSrvConfig.getTableViewer().
				refresh();
			} catch (Exception e) {
				Activator.getDefault().log(e.getMessage(), e);
			}
		}
	}

	/**
	 * Gives server installation location specified by user.
	 * @return server home location
	 */
	public String getServerLoc() {
		return JdkSrvConfig.getTxtDir().getText().trim();
	}

	/**
	 * Gives server name selected by user.
	 * @return serverName
	 */
	public String getServerName() {
		return JdkSrvConfig.getComboServer().getText();
	}

	/**
	 * Return whether Server check box is checked or not.
	 * @return boolean
	 */
	public static boolean isSrvChecked() {
		return JdkSrvConfig.getSerCheckBtn().getSelection();
	}


	/**
	 * Adds the application to the application list.
	 * @param src : import source location
	 * @param name : import as name
	 * @param method : import method
	 */
	public void addToAppList(String src, String name, String method) {
		AppCmpntParam param = new AppCmpntParam();
		param.setImpSrc(src);
		param.setImpAs(name);
		param.setImpMethod(method);
		appList.add(param);
	}

	/**
	 * @return added application Asnames which is to be set in table.
	 */
	public ArrayList<String> getAppsAsNames() {
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < appList.size(); i++) {
			list.add(appList.get(i).getImpAs());
		}
		return list;
	}

	/**
	 * @return applist
	 */
	public ArrayList<AppCmpntParam> getAppsList() {
		return appList;
	}

	/**
	 * Gives JDK location specified by user.
	 * @return JDK home location
	 */
	public String getJdkLoc() {
		return JdkSrvConfig.getTxtJdk().getText().trim();
	}

	/**
	 * Return JDK URL specified by user.
	 * @return JDK URL
	 */
	public String getJdkUrl() {
		return JdkSrvConfig.getTxtUrl().getText().trim();
	}

	/**
	 * Return JDK access key specified by user.
	 * @return JDK access key
	 */
	public String getJdkKey() {
		return JdkSrvConfig.getAccessKey(
				JdkSrvConfig.getCmbStrgAccJdk());
	}

	/**
	 * Return whether JDK check box is checked or not.
	 * @return boolean
	 */
	public static boolean isJdkChecked() {
		return JdkSrvConfig.getJdkCheckBtn().getSelection();
	}

	/**
	 * Return whether JDK download group
	 * check box is checked or not.
	 * @return
	 */
	public static boolean isJdkDownloadChecked() {
		return JdkSrvConfig.getDlRdCldBtn().getSelection();
	}

	/**
	 * Return whether JDK auto upload group
	 * check box is checked or not.
	 * @return
	 */
	public static boolean isJdkAutoUploadChecked() {
		return JdkSrvConfig.getAutoDlRdCldBtn().getSelection();
	}

	/**
	 * Returns whether third party radio button
	 * is selected or not.
	 * @return
	 */
	public static boolean isThirdPartyJdkChecked() {
		return JdkSrvConfig.getThrdPrtJdkBtn().getSelection();
	}

	/**
	 * Returns name of third party JDK.
	 * @return
	 */
	public static String getJdkName() {
		return JdkSrvConfig.getThrdPrtJdkCmb().getText();
	}

	/**
	 * Return Server URL specified by user.
	 * @return Server URL
	 */
	public String getSrvUrl() {
		return JdkSrvConfig.getTxtUrlSrv().getText().trim();
	}

	/**
	 * Return Java Home specified by user.
	 * @return
	 */
	public String getJavaHome() {
		return JdkSrvConfig.getTxtJavaHome().getText().trim();
	}

	/**
	 * Return Server Home specified by user.
	 * @return
	 */
	public String getSrvHomeDir() {
		return JdkSrvConfig.getTxtHomeDir().getText().trim();
	}

	/**
	 * Return Server access key specified by user.
	 * @return server access key
	 */
	public String getSrvKey() {
		return JdkSrvConfig.getAccessKey(
				JdkSrvConfig.getCmbStrgAccSrv());
	}

	/**
	 * Method returns Tabfolder.
	 * @return
	 */
	public static TabFolder getFolder() {
		return folder;
	}

	/**
	 * Method returns JDK tab.
	 * @return
	 */
	public static TabItem getJdkTab() {
		return jdkTab;
	}

	/**
	 * Method returns if license is accepted
	 * or not by user.
	 * User have to accept license only once.
	 * @return
	 */
	public static boolean isAccepted() {
		return accepted;
	}

	/**
	 * If user is trying to move from JDK tab
	 * and third party JDK is selected
	 * but license is not accepted till now
	 * then show license agreement dialog.
	 * @return boolean
	 * true : license accepted
	 * false : license not accepted
	 */
	private boolean displayLicenseAgreement() {
		boolean temp = true;
		if (prevTabIndex == 0
				&& isThirdPartyJdkChecked()
				&& !accepted) {
			temp = JdkSrvConfig.createAccLicenseAggDlg();
			accepted =  temp;
		}
		return temp;
	}
}
