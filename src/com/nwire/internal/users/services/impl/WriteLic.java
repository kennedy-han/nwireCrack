package com.nwire.internal.users.services.impl;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import javax.swing.JFileChooser;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import com.nwire.studio.core.utils.Base64;

public class WriteLic {

	public static void main(String[] args) throws Exception {
		//Windows LookAndFeel
		UIManager.LookAndFeelInfo[] installedLafs = UIManager
				.getInstalledLookAndFeels();
		for (UIManager.LookAndFeelInfo lafInfo : installedLafs) {
			try {
				Class lnfClass = Class.forName(lafInfo.getClassName());
				LookAndFeel laf = (LookAndFeel) (lnfClass.newInstance());
				if (laf.isSupportedLookAndFeel()) {
					String name = lafInfo.getName();
					if (name.equals("Windows"))
						UIManager.setLookAndFeel(laf);
				}
			} catch (Exception e) {
				continue;
			}
		}

		//Save File
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogType(1);
		chooser.setFileSelectionMode(1);
		int retval = chooser.showDialog(null, null);
	
		 if (retval == 1) {
			 System.exit(0);
		 }
		File f = chooser.getSelectedFile();
		String fileName=f.getAbsolutePath()+File.separator+"nwire.lic";
		 
		//Gen Lic
	 	ByteArrayOutputStream bao = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bao);
 		oos = new ObjectOutputStream(
				new GZIPOutputStream(new Base64.OutputStream(new BufferedOutputStream(
						new FileOutputStream(fileName)))));
		//step 1,write String
		String str = "qq124315391";
		oos.writeObject(str);

		//step 2,write IUserInfo impl classfile
		List<JavaClass> list=new ArrayList<JavaClass>();
		JavaClass jc=Repository.lookupClass(NodeLockedUserInfo.class);
		list.add(jc);
		oos.writeObject(list); 

		//step 3,write lic class instance
		NodeLockedUserInfo u = new NodeLockedUserInfo();
		u.setFullName("dd");
		u.setOrganization("dd");
		u.setEmail("ddatsh@gmail.com");
		oos.writeObject(u);

		oos.close();

	}
}
