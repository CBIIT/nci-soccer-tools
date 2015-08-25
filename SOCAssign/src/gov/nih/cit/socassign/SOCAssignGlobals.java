package gov.nih.cit.socassign;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import gov.nih.cit.socassign.actions.LoadDBAction;
import gov.nih.cit.util.AppProperties;
import gov.nih.cit.util.RollingList;

public class SOCAssignGlobals {
	public static final String title="SOCAssign v0.0.2";

	private static LoadDBAction loadDBAction = new LoadDBAction();

	private static JFrame applicationFrame;
	private static JTable resultsTable;
	private static CodingSystemPanel codingSystemPanel;
	private static RollingList<File> lastWorkingFileList;
	private static AppProperties appProperties;

	private static JMenu fileMenu;
	private static JFileChooser jfc;
	private static FileFilter dbFF;

	public static JFrame intializeApplicationFrame(JFrame applicationFrame) {
		SOCAssignGlobals.applicationFrame = applicationFrame;
		return applicationFrame;
	}

	public static JFrame getApplicationFrame() {
		return applicationFrame;
	}

	public static JTable intializeResultsTable(JTable resultsTable) {
		SOCAssignGlobals.resultsTable = resultsTable;
		return resultsTable;
	}

	public static JTable getResultsTable() {
		return resultsTable;
	}

	public static CodingSystemPanel intializeCodingSystemPanel(CodingSystemPanel codingSystemPanel) {
		SOCAssignGlobals.codingSystemPanel = codingSystemPanel;
		return codingSystemPanel;
	}

	public static CodingSystemPanel getCodingSystemPanel() {
		return codingSystemPanel;
	}

	public static RollingList<File> initializeLastWorkingFileList(RollingList<File> lastWorkingFileList) {
		SOCAssignGlobals.lastWorkingFileList = lastWorkingFileList;
		return lastWorkingFileList;
	}

	public static RollingList<File> getLastWorkingFileList() {
		return lastWorkingFileList;
	}

	public static AppProperties initializeAppProperties(AppProperties appProperties) {
		SOCAssignGlobals.appProperties = appProperties;
		return appProperties;
	}

	public static AppProperties getAppProperties() {
		return appProperties;
	}

	public static JFileChooser initializeJFC(JFileChooser jfc) {
		SOCAssignGlobals.jfc = jfc;
		return jfc;
	}

	public static JFileChooser getJFC() {
		return jfc;
	}

	public static FileFilter initializeDBFF(FileFilter dbFF) {
		SOCAssignGlobals.dbFF = dbFF;
		return dbFF;
	}

	public static FileFilter getDBFF() {
		return dbFF;
	}

	public static JMenu initializeFileMenu(JMenu fileMenu) {
		SOCAssignGlobals.fileMenu = fileMenu;
		return fileMenu;
	}

	public static JMenu getFileMenu() {
		return fileMenu;
	}

	public static void updateLastWorkingFileList(File f) {
		// only update the file menu if the file is not on the list...
		if (lastWorkingFileList.add(f)) {
			updateFileMenu();

			List<String> props=new ArrayList<String>();
			for (File file:lastWorkingFileList){
				props.add(file.getAbsolutePath());
			}
			appProperties.setListOfProperties("last.file", props);
		}
	}

	public static void updateFileMenu() {

		for (int i=fileMenu.getMenuComponentCount()-4;i>=3;i-- ){
			fileMenu.remove(i);
		}

		for (int i=0;i<lastWorkingFileList.size();i++){
			File file=lastWorkingFileList.get(i);
			JMenuItem menuItem=new JMenuItem(loadDBAction);menuItem.setText(file.getName());menuItem.setActionCommand(file.getAbsolutePath());
			fileMenu.insert(menuItem, 3);
		}
		fileMenu.invalidate();
	}
}