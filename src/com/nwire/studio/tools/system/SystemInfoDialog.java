package com.nwire.studio.tools.system;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.nwire.studio.core.system.EncodedLicenseRequest;
import com.nwire.studio.core.system.INwireContext;
import com.nwire.studio.core.system.IUserInfo;
import com.nwire.studio.core.system.UserInfoDecodingException;
import com.nwire.studio.tools.control.AbstractBrowserDialog;
import com.nwire.studio.tools.webservices.ServiceException;

public class SystemInfoDialog extends AbstractBrowserDialog
{
  private static final String PACK_LINK_PREFIX = "http://pack/";
  private static final int ACTIVATE_TRIAL_ID = 1025;
  private static final int ACTIVATE_FULL_ID = 1026;
  private static final int BUY_NOW_ID = 1027;
  private static final int MANUAL_LOAD_ID = 1028;
  private final Log logger = LogFactory.getLog(super.getClass());

  private INwireContext context = null;
  private IToolsPackagesManager packagesManager = null;
  private boolean hadValidPackage = false;
  private boolean showPackageManagerMessages = true;

  private Button activateTrialButton = null;
  private Button activateFullButton = null;
  private Button buyNowButton = null;
  private Button manualLoadButton = null;
  private Button closeButton = null;

  private SystemInfoDialog(Shell parentShell, INwireContext context, IToolsPackagesManager packagesManager)
  {
    super(parentShell);
    this.context = context;
    this.packagesManager = packagesManager;
  }

  public static int openDefault() {
    int result = 0;
    com.nwire.studio.tools.Activator activator = com.nwire.studio.tools.Activator.getDefault();
    INwireContext nWireContext = activator.getContext();
    IToolsPackagesManager packagesManager = activator
      .getPackagesManager();
    if (packagesManager != null) {
      SystemInfoDialog dialog = new SystemInfoDialog(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
        nWireContext, packagesManager);
      result = dialog.open();
    } else {
      activator
        .showErrorMessage("nWire failed to load properly. Please contact the nWire support team.");
    }
    return result;
  }

  public int open()
  {
    int result = 0;
    this.hadValidPackage = this.packagesManager.hasValidPackages();
    result = super.open();
    if (this.packagesManager.hasPendingChanges()) {
      if (this.hadValidPackage)
      {
        showMessage(SystemInfoStrings.SystemInfo_RestartRequiredDialog);
      } else if (this.packagesManager.hasValidPackages()) {
        com.nwire.studio.tools.Activator.getDefault().restart();
      }
    }

    return result;
  }

  private boolean checkRestartRequired()
  {
    return ((this.packagesManager.hasPendingChanges()) && 
      (this.packagesManager.hasValidPackages()) && (this.hadValidPackage));
  }

  protected Control createContents(Composite parent)
  {
    Control result = super.createContents(parent);

    getBrowser().addProgressListener(new ProgressListener() {
      public void completed(ProgressEvent event) {
        if (SystemInfoDialog.this.showPackageManagerMessages) {
          SystemInfoDialog.this.showPackageManagerMessages = false;
          SystemInfoDialog.this.packagesManager.postShowSysInfo(SystemInfoDialog.this);
        }
      }

      public void changed(ProgressEvent event)
      {
      }
    });
    return result;
  }

  protected String fillHtmlTemplate(String htmlBefore)
  {
    String result = super.fillHtmlTemplate(htmlBefore);
    String namsString = null;
    String orgString = null;
    String licenseString = null;
    String messageString = null;

    List<PackagesManager.PackageInfo> packages = ((PackagesManager)this.packagesManager)
      .getPackages();

    if ((packages == null) || (packages.isEmpty())) {
      namsString = SystemInfoStrings.SystemInfo_UnknownInfo;
      licenseString = "";
      StringBuilder sb = new StringBuilder(
        SystemInfoStrings.SystemInfo_InstallationError);
      sb.append("&nbsp;&nbsp;&nbsp;<a style=\"font-size: 60%;\" href=\"")
        .append("http://pack/").append("/\">More Details...</a>");
      sb.append("</li>");
      messageString = sb.toString();
    } else {
      StringBuilder sb = new StringBuilder();
      for (PackagesManager.PackageInfo packageInfo : packages) {
        buildPackageLicenseInfo(sb, packageInfo);
        IUserInfo currUserInfo = packageInfo.getUserInfo();
        if (currUserInfo != null) {
          String currFullName = currUserInfo.getFullName();
          if (((namsString != null) && (!(namsString.equals("Unknown")))) || 
            (currFullName == null)) continue;
          namsString = currFullName;
          orgString = currUserInfo.getOrganization();
        }

      }

      licenseString = sb.toString();
      if (namsString == null)
        namsString = SystemInfoStrings.SystemInfo_UnknownInfo;
      else if (orgString != null) {
        namsString = namsString + "&nbsp;/&nbsp;" + orgString;
      }
      if (messageString == null) {
        if (checkRestartRequired())
          messageString = SystemInfoStrings.SystemInfo_RestartRequiredMessage;
        else {
          messageString = "";
        }
      }
    }

    result = result.replace("$version$", com.nwire.studio.tools.Activator.getDefault()
      .getNwireVersion());
    result = result.replace("$license$", licenseString);
    result = result.replace("$user$", namsString);
    result = result.replace("$message$", messageString);
    return result;
  }

  private void buildPackageLicenseInfo(StringBuilder sb, PackagesManager.PackageInfo packageInfo)
  {
    sb.append("<li><span style=\"font-weight: bold;\">").append(
      packageInfo.getName()).append("</span>&nbsp;&nbsp;&nbsp;");
    if (!(packageInfo.isRunnable())) {
      sb
        .append("<span  style=\"font-weight: bold;\">Not installed correctly.</span>");
    } else {
      IUserInfo currUserInfo = packageInfo.getUserInfo();
      if (currUserInfo == null) {
        sb
          .append("<span  style=\"font-weight: bold;\">No license, please activate.</span>");
      } else {
        sb.append(currUserInfo.getSystemStatus().getDisplayName());
        String systemStatusMessage = currUserInfo
          .getSystemStatusMessage();
        if ((systemStatusMessage != null) && 
          (systemStatusMessage.length() > 0)) {
          sb.append(", ").append(systemStatusMessage);
        }
      }
    }
    sb.append("&nbsp;&nbsp;&nbsp;<a style=\"font-size: 60%;\" href=\"")
      .append("http://pack/").append(packageInfo.getId()).append(
      "/\">More Details...</a>");
    sb.append("</li>");
  }

  protected void createButtonsForButtonBar(Composite parent)
  {
    parent.setLayoutData(new GridData(768));

    this.activateTrialButton = createButton(parent, 1025, 
      SystemInfoStrings.SystemInfo_ButtonActivateTrial, false);

    this.activateTrialButton.setEnabled(true);

    this.activateFullButton = createButton(parent, 1026, 
      SystemInfoStrings.SystemInfo_ButtonActivateFull, false);
    this.activateFullButton.setEnabled(true);

    this.buyNowButton = createButton(parent, 1027, 
      SystemInfoStrings.SystemInfo_ButtonBuyNow, false);
    this.buyNowButton.setEnabled(true);

    this.manualLoadButton = createButton(parent, 1028, 
      SystemInfoStrings.SystemInfo_ButtonManualLoad, false);
    this.manualLoadButton.setEnabled(true);

    this.closeButton = createButton(parent, 0, 
      IDialogConstants.CLOSE_LABEL, true);
    this.closeButton.setEnabled(true);
  }

  public static void getCaller() {
		try {
			List<String> skipList=new ArrayList();
			String fileStr = "z:/dd.txt";
			File f = new File(fileStr);
			BufferedReader br = new BufferedReader(new FileReader(f));
			String skipPkgStr = br.readLine();
			if (skipPkgStr != null) {
				skipList=Arrays.asList(skipPkgStr.split(","));
			}

			StringBuffer sb = new StringBuffer();
			int i;
			StackTraceElement stack[] = (new Throwable().getStackTrace());

			for (i = 0; i < stack.length; i++) {
				boolean logFlag=true;
				StackTraceElement ste = stack[i];
				for (String  skipStr : skipList) {
					if (ste.getClassName().startsWith(skipStr)) {
						logFlag=false;
					}
				}
				if(logFlag) {
					sb.append(ste.getClassName());
					sb.append("." + ste.getMethodName());
					sb.append("() line ");
					sb.append(ste.getLineNumber());
					sb.append("\n");
				}
			}
			// System.out.println(sb);
			//javax.swing.JOptionPane.showMessageDialog(null, sb.toString());

			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
			bw.write(sb.toString());
			bw.write("\n");
			bw.flush();
			bw.close();
			f = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
  
  protected void buttonPressed(int buttonId)
  {
	  
	  getCaller() ;
	   
	   
    switch (buttonId)
    {
    case 1025:
      handleActivatePressed(false);
      break;
    case 1026:
      handleActivatePressed(true);
      break;
    case 1027:
      handleBuyNowPressed();
      break;
    case 1028:
      handleManualLoadPressed();
      break;
    default:
      super.buttonPressed(buttonId);
    }
  }

  private void handleBuyNowPressed()
  {
    try {
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser()
        .openURL(new URL("http://www.nwiresoftware.com/buy/"));
    } catch (Throwable t) {
      this.logger.debug("Failed to open external URL", t);
    }
  }

  private void handleActivatePressed(boolean enterLicense) {
    if (!(checkPackagesBeforeActivation(enterLicense))) return;
    try {
      ActivationDialog activationDialog = new ActivationDialog(
        getParentShell(), enterLicense);
      if (activationDialog.open() == 0) {
        EncodedLicenseRequest elr = activationDialog
          .getEncodedLicenseRequest();
        if (elr != null) {
          String results = invokeRegistrationService(elr
            .toEncodedString());
          if (results != null)
            handleInfoEntry(results);
        }
      }
    }
    catch (Throwable t) {
      if (this.logger.isTraceEnabled()) {
        this.logger.trace("Activation failed", t);
      }
      showMessage(t.getMessage());
    }
  }

  private boolean checkPackagesBeforeActivation(boolean enterLicense)
  {
    boolean result = true;
    if (!(this.packagesManager.hasRunnablePackage())) {
      showMessage("Please verify that nWire is installed correctly before activating nWire.");
      result = false;
    } else if (!(enterLicense)) {
      List packageIds = this.packagesManager.getPackageIds(Boolean.valueOf(true), 
        Boolean.valueOf(false));
      if ((packageIds == null) || (packageIds.isEmpty())) {
        showMessage("nWire license is fully operational. Trial activation is available when a license is required.");
        result = false;
      }
    } else {
      List packageIds = this.packagesManager.getPackageIds(null, 
        null);
      if ((packageIds == null) || (packageIds.isEmpty())) {
        showMessage("Please verify that nWire is installed correctly before activating nWire.");
        result = false;
      }
    }
    return result;
  }

  private String invokeRegistrationService(String encodedContents) throws ServiceException
  {
    String result = null;

    RegistrationServiceRunner runner = new RegistrationServiceRunner(
      encodedContents);
    try {
      new ProgressMonitorDialog(getShell()).run(true, true, runner);
    } catch (InvocationTargetException localInvocationTargetException) {
      throw new ServiceException("Internal error has occured");
    } catch (InterruptedException localInterruptedException) {
      throw new ServiceException("Operation cancelled by user");
    }

    result = runner.result;
    if (runner.error != null)
      throw runner.error;
    if (result == null)
      throw new ServiceException("No response from server");
    if (result.startsWith("http")) {
      new ManualServiceInvocationDialog(getParentShell(), result)
        .open();
      result = null;
    }
    return result;
  }

  private void handleInfoEntry(String encodedUserInfo)
    throws UserInfoDecodingException, IOException, ServiceException
  {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("Uploading encoded info (length: " + 
        encodedUserInfo.length() + ")");
    }
    if (this.context != null) {
      String strippedEncodedUserInfo = stripContents(encodedUserInfo);
      List<IUserInfo> uploadUserInfos = this.context
        .enterEncodedUserInfo(strippedEncodedUserInfo);
      if (uploadUserInfos != null) {
        boolean operational = false;
        for (IUserInfo currentUserInfo : uploadUserInfos) {
          this.packagesManager.updatePackagesLicense(currentUserInfo);
          if (currentUserInfo.getSystemStatus().isOperational()) {
            operational = true;
          }
        }
        if (!(operational)) {
          this.logger.info("All entered licenses are not operational");
        }
        this.showPackageManagerMessages = true;
        refreshBrowserContents();
      }
    } else {
      this.logger.warn("No context, failed to upload file");
    }
  }

  private void handleManualLoadPressed() {
    FileDialog fileDialog = new FileDialog(getShell(), 4096);
    fileDialog.setText(SystemInfoStrings.EnterInfo_Message);

    String path = fileDialog.open();
    if (path == null) return;
    try {
      handleInfoUpload(path);
    } catch (UserInfoDecodingException localUserInfoDecodingException) {
      showMessage(SystemInfoStrings.EnterInfo_ErrorInvalidContextException);
    } catch (IOException localIOException) {
      showMessage(SystemInfoStrings.EnterInfo_ErrorIOException);
    } catch (Throwable t) {
      if (this.logger.isTraceEnabled()) {
        this.logger.trace("Unknown error during upload", t);
      }
      showMessage(SystemInfoStrings.EnterInfo_ErrorUnknown);
    }
  }

  private void handleInfoUpload(String filename)
    throws UserInfoDecodingException, IOException, ServiceException
  {
    if (this.logger.isTraceEnabled()) {
      this.logger.trace("Uploading file: " + filename);
    }
    String contents = readFile(filename);
    handleInfoEntry(contents);
  }

  private String stripContents(String contents) throws ServiceException {
    String result = contents;
    try {
      int startPos = contents
        .indexOf("--start-nwire-info--");
      int endPos = contents
        .indexOf("--end-nwire-info--");

      if ((startPos >= 0) && (endPos >= 0))
        result = contents.substring(startPos + 
          "--start-nwire-info--".length(), 
          endPos);
    }
    catch (Throwable e) {
      this.logger.info("stripContents failed", e);
    }
    if (result.length() < 500) {
      throw new ServiceException(result);
    }
    return result;
  }

  private String readFile(String filename) throws IOException {
    String result = null;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(filename)));
      StringBuilder sb = new StringBuilder();
      while (reader.ready()) {
        sb.append(reader.readLine()).append('\n');
      }
      result = sb.toString();
      return result;
    } finally {
      if (reader != null)
        try {
          reader.close();
        }
        catch (IOException e) {
          if (this.logger.isTraceEnabled())
            this.logger.trace("Failed to close file", e);
        }
    }
  }

  protected String getDialogTitle()
  {
    return SystemInfoStrings.SystemInfo_Title;
  }

  protected String getHtmlFilename()
  {
    return "system-info.html";
  }

  protected Point getBrowserSize()
  {
    return new Point(624, 380);
  }

  protected boolean handleLink(String url)
  {
    boolean result = false;
    if (url.startsWith("http://pack/")) {
      String packId = url.substring("http://pack/".length());
      packId = packId.replace("/", "");
      if (packId.length() == 0) {
        InstallationIssuesDialog.openDefault(null, this);
        result = true;
      } else {
        List<PackagesManager.PackageInfo> packages = ((PackagesManager)this.packagesManager)
          .getPackages();
        if (packages != null) {
          PackagesManager.PackageInfo foundPack = null;
          for (PackagesManager.PackageInfo packageInfo : packages) {
            if (packageInfo.getId().equals(packId)) {
              foundPack = packageInfo;
              break;
            }
          }
          if (foundPack != null) {
            if (foundPack.isRunnable())
              LicenseIssuesDialog.openDefault(foundPack, this);
            else {
              InstallationIssuesDialog.openDefault(foundPack, 
                this);
            }
            result = true;
          }
        }
      }
    } else {
      result = super.handleLink(url);
    }
    return result;
  }

  private class RegistrationServiceRunner
    implements IRunnableWithProgress
  {
    String result = null;
    ServiceException error = null;
    private final String encodedContents;

    public RegistrationServiceRunner(String paramString)
    {
      this.encodedContents = paramString;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
    {
      try {
        this.result = 
          com.nwire.studio.tools.webservices.Activator.getDefault().getRegistrationServices()
          .getUserInfoForLicenseRequest(SystemInfoDialog.this.getMainComposite(), 
          this.encodedContents, monitor);
      } catch (ServiceException e) {
        this.error = e;
      }
    }
  }
}