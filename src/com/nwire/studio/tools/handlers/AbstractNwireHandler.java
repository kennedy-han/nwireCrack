package com.nwire.studio.tools.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.IHandler;

public abstract class AbstractNwireHandler extends AbstractHandler
  implements IHandler
{
  protected final transient Log logger = LogFactory.getLog(super.getClass());

  private boolean enabled = true;

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
  
  
  public Object execute(ExecutionEvent event) throws ExecutionException {
	  getCaller() ;
    try {
      doExecute();
    } catch (Throwable t) {
      this.logger.warn("Handler execution failed", t);
    }
    return null;
  }

  protected abstract void doExecute();

  public boolean isEnabled()
  {
    return this.enabled;
  }

  protected void setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      fireHandlerChanged(new HandlerEvent(this, true, false));
    }
  }
}