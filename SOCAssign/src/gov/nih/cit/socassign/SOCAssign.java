package gov.nih.cit.socassign;

import gov.nih.cit.socassign.Assignments.FlagType;
import gov.nih.cit.socassign.codingsystem.OccupationCode;
import gov.nih.cit.util.AppProperties;
import gov.nih.cit.util.RollingList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * SOCAssign is the main class.  
 * 
 * @author Daniel Russ
 *
 */
public class SOCAssign{

	public static Logger logger=Logger.getLogger(SOCAssign.class.getName());

	/** the main application frame.  Held to changed the Title */
	private static JFrame applicationFrame;
	/** holds most of the data required by the gui components */
	private static SOCAssignModel testModel=SOCAssignModel.getInstance();
	/** displays the results of SOCcer */
	private static JTable resultsTable=new JTable(testModel.getTableModel());
	/** display the codes assigned by the coder. */
	private static JList<OccupationCode> assigmentList=new JList<OccupationCode>(testModel.getAssignmentListModel());
	/** A text field where coders can type in a code */
	private static JTextField assignmentTF=new JTextField(8);
	/** A table that displays the results of SOCcer for a single job description.  This is filled
	 * when the user selects a row in the resultsTable*/
	private static JTable singleJobDescriptionTable=new JTable(testModel.getTop10Model());
	/** A JPanel that display all the codes for a coding system */
	private static CodingSystemPanel codingSystemPanel=new CodingSystemPanel();
	/** Displays the selected Job Description from the resultsTable */
	private static JList<String> jobDescriptionInfoList=new JList<String>(testModel.getSingleJobDescriptionListModel());
	/** A list that holds the last 3 files used */
	private static RollingList<File> lastWorkingFileList=new RollingList<File>(3);
	/** Stores information (the last files used) in a properties file so it will be remembered next time the program starts*/
	private static AppProperties appProperties;
	/** used in the JFrame title */
	private static final String title="SOCAssign v0.0.2";


	public static Font fontAwesome;
	/**
	 * "Main" method of the application should be run on the Event Dispatch Thread.  
	 */
	public static void createAndShowGUI() {
		// create the application frame ...
		applicationFrame=new JFrame(title);
		applicationFrame.addWindowListener(windowListener);
		applicationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		// format the results table ...
		resultsTable.setAutoCreateRowSorter(true);
		resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsTable.getSelectionModel().addListSelectionListener(resultsTableSelectionListener);
		resultsTable.setDefaultRenderer(String.class, resultsRenderer);
		resultsTable.setDefaultRenderer(Double.class, resultsRenderer);
		resultsTable.setDefaultRenderer(Integer.class, resultsRenderer);
		resultsTable.setDefaultRenderer(Boolean.class, flagRenderer);

		// and the selected soccer result table...
		singleJobDescriptionTable.setAutoCreateRowSorter(true);
		singleJobDescriptionTable.addMouseListener(selectAnotherSoccerResultListener);
		singleJobDescriptionTable.setDefaultRenderer(Integer.class, selectedResultRenderer);
		singleJobDescriptionTable.setDefaultRenderer(Double.class, selectedResultRenderer);

		// and the assignmentList
		assigmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		assigmentList.addListSelectionListener(assignmentListSelectionListener);

		// if you hit the up/down arrow in the textfield, it switches the selected soccerResult
		assignmentTF.setEditable(true);
		assignmentTF.setAction(addSelectedAssignment);
		assignmentTF.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "DOWN");
		assignmentTF.getActionMap().put("DOWN", nextJobDescription);
		assignmentTF.getInputMap().put(KeyStroke.getKeyStroke("UP"), "UP");
		assignmentTF.getActionMap().put("UP", previousJobDescription);
		// assignmentTF.addKeyListener(myKeyListener);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(new JScrollPane(resultsTable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS),BorderLayout.WEST);

		SpringLayout layout = new SpringLayout();
		JPanel centerPanel = new JPanel(layout);
		centerPanel.setPreferredSize(new Dimension(458,761));
		// add the code assignment box ...
		centerPanel.add(assignmentTF);
		layout.putConstraint(SpringLayout.WEST, assignmentTF, 0, SpringLayout.WEST, centerPanel);
		layout.putConstraint(SpringLayout.EAST, assignmentTF, 0, SpringLayout.EAST, centerPanel);
		layout.putConstraint(SpringLayout.NORTH, assignmentTF, 0, SpringLayout.NORTH, centerPanel);

		// and the button Panel
		//JPanel buttonPanel=new JPanel(new GridLayout(2, 2));
		JPanel buttonPanel=new JPanel(new GridLayout(1, 4));

		// add assignment button
		JButton addSOCAssignment=new JButton(addSelectedAssignment);
		buttonPanel.add(addSOCAssignment);
		// move selection up
		JButton moveAssignmentUp=new JButton(increaseSelection);
		buttonPanel.add(moveAssignmentUp);
		// move selection down
		JButton moveAssignmentDown=new JButton(decreaseSelection);
		buttonPanel.add(moveAssignmentDown);
		// remove assignment button
		JButton removeSOCAssignment=new JButton(removeSelectedAssignment);
		buttonPanel.add(removeSOCAssignment);

		// load the icons on the button.  FontAwesome is an open-source font distributed with SOCassign.
		// if there is a problem, use the icons that I drew.  They are not as pretty.
		try {
			try {
				fontAwesome = Font.createFont(Font.TRUETYPE_FONT, SOCAssign.class.getResourceAsStream("fonts/fontawesome-webfont.ttf"));				
				fontAwesome = fontAwesome.deriveFont(20f);
				addSOCAssignment.setFont(fontAwesome); addSOCAssignment.setForeground(Color.BLUE);
				removeSOCAssignment.setFont(fontAwesome); removeSOCAssignment.setForeground(Color.BLUE);
				moveAssignmentUp.setFont(fontAwesome); moveAssignmentUp.setForeground(Color.BLUE);
				moveAssignmentDown.setFont(fontAwesome); moveAssignmentDown.setForeground(Color.BLUE);
				addSOCAssignment.setText("\uf055");
				removeSOCAssignment.setText("\uf056");
				moveAssignmentUp.setText("\uf0aa");
				moveAssignmentDown.setText("\uf0ab");
			} catch (Exception e) {
				addSOCAssignment.setIcon(new ImageIcon(ImageIO.read(SOCAssign.class.getResourceAsStream("images/add-blue.png"))));
				removeSOCAssignment.setIcon(new ImageIcon(ImageIO.read(SOCAssign.class.getResourceAsStream("images/remove-blue.png"))));
				moveAssignmentUp.setIcon(new ImageIcon(ImageIO.read(SOCAssign.class.getResourceAsStream("images/up-blue.png"))));
				moveAssignmentDown.setIcon(new ImageIcon(ImageIO.read(SOCAssign.class.getResourceAsStream("images/down-blue.png"))));

			}

		} catch (IOException e) {
			e.printStackTrace();
		}			

		centerPanel.add(buttonPanel);
		alignSpring(layout,buttonPanel,centerPanel,assignmentTF);
		JScrollPane assignmentScroll = new JScrollPane(assigmentList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		centerPanel.add(assignmentScroll);
		alignSpring(layout,assignmentScroll,centerPanel,buttonPanel);

		// and the single Job Description Panel.
		JScrollPane tableScroll = new JScrollPane(singleJobDescriptionTable,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		centerPanel.add(tableScroll);
		alignSpring(layout,tableScroll,centerPanel,assignmentScroll);
		JScrollPane infoScroll = new JScrollPane(jobDescriptionInfoList,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		centerPanel.add(infoScroll);
		alignSpring(layout,infoScroll,centerPanel,tableScroll);
		layout.putConstraint(SpringLayout.SOUTH, infoScroll, 0, SpringLayout.SOUTH, centerPanel);
		mainPanel.add(centerPanel,BorderLayout.CENTER);

		// add the Coding System panel on the right
		//codingSystemPanel.addTreeSelectionListener(codingSystemTreeListener);
		codingSystemPanel.addMouseListenerToJTree(codingSystemMouseAdapter);
		mainPanel.add(codingSystemPanel, BorderLayout.EAST);

		createMenus();

		//mainPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,KeyEvent.SHIFT_DOWN_MASK), "FirstJobDescription");

		// if you are not in the text box,  The "<" key selected the previous row job description ,SHIFT-"<" the first.
		// The ">" key selects the next job description and shift-">" the last.
		mainPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,0), "PreviousJobDescription");
		mainPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD,0), "NextJobDescription");		
		mainPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,KeyEvent.SHIFT_DOWN_MASK), "FirstJobDescription");
		mainPanel.getInputMap(JPanel.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD,KeyEvent.SHIFT_DOWN_MASK), "LastJobDescription");
		mainPanel.getActionMap().put("LastJobDescription", lastJobDescription);
		mainPanel.getActionMap().put("NextJobDescription", nextJobDescription);
		mainPanel.getActionMap().put("PreviousJ/gettetobDescription", previousJobDescription);
		mainPanel.getActionMap().put("FirstJobDescription", firstJobDescription);

		resultsTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F,0), "ToggleFlag");
		resultsTable.getActionMap().put("ToggleFlag", toggleFlagAction);
		
		applicationFrame.setContentPane(mainPanel);
		applicationFrame.pack();
		applicationFrame.setVisible(true);

//		addAutoCompleteBox(layout,centerPanel);
	}

	private static void alignSpring(SpringLayout layout, JComponent child, JComponent parent, JComponent predecessor) {
		layout.putConstraint(SpringLayout.WEST, child, 0, SpringLayout.WEST, parent);
		layout.putConstraint(SpringLayout.EAST, child, 0, SpringLayout.EAST, parent);
		layout.putConstraint(SpringLayout.NORTH, child, 0, SpringLayout.SOUTH, predecessor);
	}

	private static void addAutoCompleteBox(SpringLayout layout, JComponent centerPanel) {
		JButton test = new JButton("CLICK HERE");
		centerPanel.add(test,0);
		alignSpring(layout,test,centerPanel,assignmentTF);
	}

	private static JMenu fileMenu=new JMenu("File");
	private static void createMenus(){
		JMenuBar menuBar=new JMenuBar();

		// fileMenu is a field because it needs to be updated when a user selects a database.
		// create File > load
		JMenuItem loadMI=new JMenuItem(loadAction);
		fileMenu.add(loadMI);

		// create File > load previous coding
		JMenuItem loadDBMI=new JMenuItem(loadDBAction);loadDBMI.setActionCommand("");
		fileMenu.add(loadDBMI);

		fileMenu.add(new JSeparator());
		// create File > LAST 3 Working Files...
		if (lastWorkingFileList.size()>0){
			for (File file:lastWorkingFileList.asRollingStack()){
				JMenuItem menuItem=new JMenuItem(loadDBAction);menuItem.setText(file.getName());menuItem.setActionCommand(file.getAbsolutePath());
				fileMenu.add(menuItem);
			}
		}
		fileMenu.add(new JSeparator());

		fileMenu.add(exportAction);

		// create File > Quit
		JMenuItem quitMI=new JMenuItem(quitAction);quitMI.setText("Quit");
		fileMenu.add(quitMI);
		menuBar.add(fileMenu);

		// create System
		JMenu systemMenu=new JMenu("CodingSystem");
		//create System > SOC2010 ...
		ButtonGroup codingSystemButtonGroup=new ButtonGroup();
		for (AssignmentCodingSystem system:AssignmentCodingSystem.values()){
			JRadioButtonMenuItem item=new JRadioButtonMenuItem(selectCodingSystemAction);
			codingSystemButtonGroup.add(item);
			if (system==AssignmentCodingSystem.SOC2010) {
				item.setSelected(true);
			}
			item.setText(system.toString());
			systemMenu.add(item);
		}
		menuBar.add(systemMenu);		
		selectCodingSystemAction.actionPerformed(new ActionEvent(systemMenu, 0, "SOC2010"));

		applicationFrame.setJMenuBar(menuBar);
	}


	private static void setAppProperties(AppProperties appProperties) {
		SOCAssign.appProperties = appProperties;
	}

	private static void fillLastWorkingFileList(){
		lastWorkingFileList.clear();
		for (int i=0;i<lastWorkingFileList.capacity();i++){
			String fileName=appProperties.getProperty("last.file."+i, "");
			File file=new File(fileName);
			if (file.exists()){
				lastWorkingFileList.add(file);
			} else {
				appProperties.remove(fileName);
			}
		}
	}

	private static void updateLastWorkingFileList(File f){		
		// only update the file menu if the file is not on the list...
		if ( lastWorkingFileList.add(f) ) {
			updateFileMenu();

			List<String> props=new ArrayList<String>();
			for (File file:lastWorkingFileList){
				props.add(file.getAbsolutePath());
			}
			appProperties.setListOfProperties("last.file", props);
		}
	}

	private static void updateFileMenu(){

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

	private static boolean validResultSelected(){
		return resultsTable.getSelectedRow()>=0;
	}

	private static JFileChooser jfc=new JFileChooser(System.getProperty("user.home"));	
	private static AbstractAction loadAction=new AbstractAction("Load SOCcer Results") {
		private static final long serialVersionUID = 8203307202836747344L;

		@Override
		public void actionPerformed(ActionEvent event) {
			jfc.setCurrentDirectory(new File(appProperties.getProperty("last.directory", System.getProperty("user.home"))));
			jfc.setFileFilter(csvFF);
			int res=jfc.showOpenDialog(applicationFrame);
			if (res==JFileChooser.APPROVE_OPTION){
				System.out.println("Selected file: "+jfc.getSelectedFile().getAbsolutePath());
				try {
					testModel.resetModel();
					SOCcerResults results=SOCcerResults.readSOCcerResultsFile(jfc.getSelectedFile());
					testModel.setResults(results);
					resultsTable.invalidate();

					boolean systemSpecified=testModel.isCodingSystemSpecifiedInResults();
					if (systemSpecified){
						selectCodingSystemAction.setEnabled(false);
						codingSystemPanel.updateCodingSystem(testModel.getCodingSystem());
					}else{
						selectCodingSystemAction.setEnabled(true);
					}

					String fileName=jfc.getSelectedFile().getAbsolutePath();
					int indx=fileName.lastIndexOf('.');
					if (indx<0 || fileName.substring(indx)==".db"){
						fileName=fileName+".db";
					}else{
						fileName=fileName.substring(0,indx)+".db";
					}					
					testModel.setNewDB(fileName);
					updateLastWorkingFileList(new File(fileName));

					applicationFrame.setTitle(title+" ("+fileName+")");
				} catch (IOException ioe) {
					JOptionPane.showMessageDialog(applicationFrame, "Error trying to Open File "+jfc.getSelectedFile().getAbsolutePath(), "SOCassign Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	};

	private static FileFilter dbFF=new FileNameExtensionFilter("Working Files (.db)","db");
	private static FileFilter csvFF=new FileNameExtensionFilter("SOCcer Results Files (.csv)","csv");
	private static FileFilter annFF=new FileNameExtensionFilter("Annotation Results Files (.csv)","csv");

	private static AbstractAction loadDBAction=new AbstractAction("Load Previous Work") {
		private static final long serialVersionUID = -7230933573954875342L;

		public File getFile(){
			jfc.setCurrentDirectory(new File(appProperties.getProperty("last.directory", System.getProperty("user.home"))));
			int res=jfc.showOpenDialog(applicationFrame);
			if (res==JFileChooser.APPROVE_OPTION){
				appProperties.setProperty("last.directory", jfc.getCurrentDirectory().getAbsolutePath());
				return jfc.getSelectedFile();
			}
			return null;
		}

		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			jfc.setFileFilter(dbFF);
			// if the user selected a file from the menu ... load the file
			// else get the file from a JFileChooser...
			File dbFile= (actionEvent.getActionCommand().length() == 0) ? getFile() : new File(actionEvent.getActionCommand());
			if (dbFile == null ) return;

			// if somehow the file does not exist delete it from the lastWorkingFileList...
			if (!dbFile.exists()) {
				lastWorkingFileList.remove(dbFile);
				appProperties.remove(dbFile.getAbsolutePath());
				updateFileMenu();
				return;
			}

			// load the db...
			try {
				testModel.loadPreviousWork(dbFile);
				updateLastWorkingFileList(dbFile);
				resultsTable.invalidate();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(applicationFrame, "Error trying to Open database: (Did you select results instead of a working file?) "+dbFile.getAbsolutePath(), "SOCassign Error", JOptionPane.ERROR_MESSAGE);
			}
			applicationFrame.setTitle(title+" ("+dbFile.getAbsolutePath()+")");
			boolean systemSpecified=testModel.isCodingSystemSpecifiedInResults();
			if (systemSpecified){
				selectCodingSystemAction.setEnabled(false);
				codingSystemPanel.updateCodingSystem(testModel.getCodingSystem());
			}else{
				selectCodingSystemAction.setEnabled(true);
			}

		}
	};

	private static AbstractAction exportAction=new AbstractAction("Export Annotation to CSV") {
		private static final long serialVersionUID = -3145117572994231404L;

		@Override
		public void actionPerformed(ActionEvent event) {
			if (resultsTable.getRowCount()==0) return;
			jfc.setCurrentDirectory(new File(appProperties.getProperty("last.directory", System.getProperty("user.home"))));
			jfc.setFileFilter(annFF);
			int res=jfc.showSaveDialog(applicationFrame);
			if (res==JFileChooser.APPROVE_OPTION){
				try {
					testModel.exportAssignments(jfc.getSelectedFile());
				} catch (IOException e) {
					JOptionPane.showMessageDialog(applicationFrame, "Warning could not write out the annotation: "+e.getMessage());
					e.printStackTrace();
				}
				appProperties.setProperty("last.directory", jfc.getCurrentDirectory().getAbsolutePath());
			}

		}
	};
	private static AbstractAction quitAction=new AbstractAction() {
		private static final long serialVersionUID = 1999213561478931972L;

		@Override
		public void actionPerformed(ActionEvent e) {
			testModel.onExit();
			System.exit(0);
		}
	};

	private static AbstractAction selectCodingSystemAction =new AbstractAction() {
		private static final long serialVersionUID = 1875881058998874597L;

		@Override
		public void actionPerformed(ActionEvent event) {
			AssignmentCodingSystem codingSystem=AssignmentCodingSystem.valueOf(event.getActionCommand());
			if (testModel.getCodingSystem()!=codingSystem){
				testModel.setCodingSystem(codingSystem);
				codingSystemPanel.updateCodingSystem(codingSystem);
			}
		}
	};

	private static AbstractAction firstJobDescription=new AbstractAction() {
		private static final long serialVersionUID = -8885241378300233154L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!validResultSelected()) return;			
			resultsTable.setRowSelectionInterval(0, 0);			
			resultsTable.scrollRectToVisible( resultsTable.getCellRect(0, 0, true) );
		}
	};

	private static AbstractAction nextJobDescription=new AbstractAction() {
		private static final long serialVersionUID = -8834879565030941627L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			logger.finer("NJD called!!!");
			if (!validResultSelected()) return;

			int nextRow=(resultsTable.getSelectedRow()+1)%resultsTable.getRowCount();
			resultsTable.setRowSelectionInterval(nextRow, nextRow);

			resultsTable.scrollRectToVisible( resultsTable.getCellRect(nextRow, 0, true) );
		}
	};

	private static AbstractAction previousJobDescription=new AbstractAction() {
		private static final long serialVersionUID = 6155782637763420434L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!validResultSelected()) return;

			int nextRow=(resultsTable.getSelectedRow()+resultsTable.getRowCount()-1)%resultsTable.getRowCount();
			resultsTable.setRowSelectionInterval(nextRow, nextRow);

			resultsTable.scrollRectToVisible( resultsTable.getCellRect(nextRow, 0, true) );
		}
	};

	private static AbstractAction lastJobDescription=new AbstractAction() {
		private static final long serialVersionUID = 1645600806658008798L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!validResultSelected()) return;
			int row=resultsTable.getRowCount()-1;
			resultsTable.setRowSelectionInterval(row, row);			
			resultsTable.scrollRectToVisible( resultsTable.getCellRect(row, 0, true) );
		}
	};

	private static AbstractAction addSelectedAssignment=new AbstractAction() {
		private static final long serialVersionUID = -2503045351214365029L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (!validResultSelected()) return;

			String txt=(String)assignmentTF.getText();
			if (testModel.getCodingSystem().matches(txt)){
				testModel.addSelection(txt);
			}else{
				JOptionPane.showMessageDialog(applicationFrame, "Assignment is not formatted appropriately "+txt, "SOCassign Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	};

	private static AbstractAction removeSelectedAssignment = new AbstractAction() {
		private static final long serialVersionUID = -9156982327895641153L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!validResultSelected() || assigmentList.getSelectedIndex()<0) return;
			testModel.removeElementAt(assigmentList.getSelectedIndex());
		}
	};

	private static AbstractAction increaseSelection = new AbstractAction() {
		private static final long serialVersionUID = -1834161874835015764L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!validResultSelected()) return;

			int selectedIndex=assigmentList.getSelectedIndex();
			if (selectedIndex>0){
				testModel.increaseSelection(selectedIndex);
				assigmentList.setSelectedIndex(selectedIndex-1);
			}
		}
	};

	private static AbstractAction decreaseSelection = new AbstractAction() {
		private static final long serialVersionUID = 3549967732525161523L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!validResultSelected()) return;

			int selectedIndex=assigmentList.getSelectedIndex();
			if (selectedIndex<0) return;

			testModel.decreaseSelection(selectedIndex);
			if (selectedIndex<assigmentList.getModel().getSize()-1){
				assigmentList.setSelectedIndex(selectedIndex+1);
			}
		}
	};

	private static AbstractAction toggleFlagAction=new AbstractAction() {
		private static final long serialVersionUID = 2025234944233086908L;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			int row= resultsTable.getSelectedRow();
			if (row>=0){
				int selectedRow=resultsTable.convertRowIndexToModel( row );
				boolean flagValue=(Boolean)resultsTable.getValueAt(selectedRow, 0);
				int rowID=(Integer)resultsTable.getValueAt(selectedRow, 1);
				System.out.println("FLAG TOGGLER: (row) "+selectedRow+" (rowID) "+rowID+" (current value) "+flagValue);
				testModel.updateFlag(selectedRow, flagValue?FlagType.NOT_FLAGGED:FlagType.FLAGGED);
				testModel.getTableModel().fireTableRowsUpdated(selectedRow, selectedRow);
			}else{
				System.out.println("bad row selected..."+row);
			}
		}
	};

	private static ListSelectionListener resultsTableSelectionListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent event) {
			if (!validResultSelected()) return;

			assignmentTF.setText("");
			assigmentList.clearSelection();
			codingSystemPanel.clearSelection();

			int selectRow=resultsTable.getSelectedRow();
			selectRow=resultsTable.convertRowIndexToModel(selectRow);
			testModel.setSelectedResult(selectRow);
		}
	};

	/**
	 * Use a MouseListener instead of a TreeSelectionListener to handle double clicks..
	 */
	private static MouseAdapter codingSystemMouseAdapter = new MouseAdapter() {
		public void mousePressed(MouseEvent e) {

			if (!validResultSelected() || codingSystemPanel.getSelectedPathCount()<=1) return;

			OccupationCode code=codingSystemPanel.getLastSelectedPathComponent();
			if (code.isLeaf() && e.getClickCount()>1){
				logger.finer("Setting the Assigned SOC...");
				testModel.addSelection(code.getName());
			}
			assignmentTF.setText(code.getName());
		};
	};
	static MouseAdapter selectAnotherSoccerResultListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent event) {

			int row=singleJobDescriptionTable.rowAtPoint(event.getPoint());
			row=singleJobDescriptionTable.convertRowIndexToModel(row);
			OccupationCode code= testModel.getOccupationCodeForTop10Row(row);
			codingSystemPanel.selectOccupation(code);			
			if (event.getClickCount()>=2){
				logger.finer("selected row: "+code.getName());
				testModel.addSelection(code.getName());
			}
		}
	};
	private static ListSelectionListener assignmentListSelectionListener = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent event) {
			int row=event.getFirstIndex();
			if (row<0 || row>= assigmentList.getModel().getSize()){
				assigmentList.clearSelection();
				codingSystemPanel.clearSelection();
				return;
			}
			OccupationCode code=(OccupationCode)assigmentList.getModel().getElementAt(row);
			codingSystemPanel.selectOccupation(code);
		}
	};

	public SOCAssign() {}

	public static void main(String[] args) {

		setAppProperties(AppProperties.getDefaultAppProperties("SOCassign"));
		fillLastWorkingFileList();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 *  Closes the database connection when the window is closed.
	 */
	public static WindowListener windowListener=new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			testModel.onExit();
		}
	};

	private static Color PALE_GREEN=new Color(152, 251, 152);
	private static DecimalFormat fmt1=new DecimalFormat("0.0000");
	private static DecimalFormat fmt2=new DecimalFormat("0.000E0");

	private static TableCellRenderer selectedResultRenderer = new DefaultTableCellRenderer(){
		/**
		 * 
		 */
		private static final long serialVersionUID = -1231757356685263171L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (column<3){
				setHorizontalAlignment(JLabel.CENTER);

				if (column==2){
					double val=Double.parseDouble(getText());
					if (val<1e-4){
						setText(fmt2.format(val));
					}else{
						setText(fmt1.format(val));
					}
				}

			}
			return this;
		};

	};

	private static TableCellRenderer flagRenderer = new DefaultTableCellRenderer(){
		/**
		 * 
		 */
		private static final long serialVersionUID = 6723769622571872667L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (fontAwesome!=null){
				setFont(fontAwesome);
				setForeground(Color.RED);
				setText(((Boolean)value)?"\uf024":"");
			}
			if (!isSelected){
				if ( testModel.isRowAssigned(resultsTable.convertRowIndexToModel(row))) {
					setBackground(PALE_GREEN);
				} else {
					setBackground(Color.WHITE);
				}
			}
			return this;
		}
	};

	private static TableCellRenderer resultsRenderer = new DefaultTableCellRenderer(){

		/**
		 * 
		 */
		private static final long serialVersionUID = -6681281477992789310L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (column==2||column==4){
				setToolTipText(value.toString());
			}else if (column==5){
				AssignmentCodingSystem codingSystem=testModel.getCodingSystem();
				OccupationCode code=codingSystem.getOccupationalCode(value.toString());
				if (code!=null){
					setToolTipText(code.getTitle()+" - "+code.getDescription());
				}else{
					setToolTipText(null);
				}
			} else{
				setToolTipText(null);
			}

			if (!isSelected){
				if ( testModel.isRowAssigned(resultsTable.convertRowIndexToModel(row))) {
					setBackground(PALE_GREEN);
				} else {
					setBackground(Color.WHITE);
				}
			}
			return this;
		};

	};
}


