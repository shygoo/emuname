package net.shygoo.plugin.debugger;

import net.shygoo.emuname.*;
import net.shygoo.misc.KeyFilter;
import net.shygoo.misc.ResourceLoader;

import java.awt.Font;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public class MemoryScanner extends ENFrame {
	
	private final static int
	COL_OFFSET = 0,
	COL_VALUE = 1;

	private NES nes;
	private Emuname emuname;
	
	private Map<Integer, Integer> results;
	private Integer[]         resultOffsets;
	private int               resultCount;
	
	private boolean           newScan = true;
	private int               rangeMin = 0x000;
	private int               rangeMax = 0x7FF;
	
	private JPanel            mainPanel;          // Container for all components
	private JTextField        textFieldValue;     // "Value" textbox
	private JButton           buttonNewScan;      // "Reset" button
	private JButton           buttonNextScan;     // "Scan" button

	private String            scanSelection = "Exact value";
	private JComboBox<String> cbNewScanSelection; // Combo box for first scan options
	private String[]          newScanOptions = {  // First scan options
		"Exact value",
		"Unknown value"
	};
	private JComboBox<String> cbNextScanSelection; // Combo box for next scan options
	private String[]          nextScanOptions = {  // Next scan options
		"Exact value",
		"Changed value",
		"Unchanged value",
		"Increased value",
		"Decreased value"
	};
	
	private JLabel            resultsLabel;        // Results count label
	private JTableHeader      resultsTableHeader;  // Column names
	private JPanel            resultsTableWrapper; // Wrapper for cells and scrollbar
	private JTable            resultsTable;        // Cells
	private JScrollBar        resultsScrollBar;    // Results scrollbar

	private int[]    tableColors = new int[20];         // Saturation values for the rows (max 15)
	private String[] currentTableVals = new String[20]; // Store current values here for color updates
	private int      scrollOffset = 0;                  // Set from the scrollbar, determines what results are shown
	
	private Timer refresher; // Refreshes the results table
	
	// For switching between hex and dec modes
	private String    numberFormat = "%02X";
	private int       numberBase = 16;
	private KeyFilter inputFilter = new KeyFilter(KeyFilter.HEX);
	
	private Font monofont = new Font("Consolas", Font.PLAIN, 11);
	
	//////////////////////
	
	public MemoryScanner(Emuname emuname){
		super("Memory Scanner");
		this.emuname = emuname;
		this.nes = emuname.nes;
		results = new HashMap<Integer, Integer>();
		
		cbNewScanSelection  = new JComboBox<String>(newScanOptions);
		cbNextScanSelection = new JComboBox<String>(nextScanOptions);
		
		cbNewScanSelection.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				scanSelection = (String) cbNewScanSelection.getSelectedItem();
				textFieldValue.setEnabled(scanSelection == "Exact value");
			}
		});
		
		cbNextScanSelection.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				scanSelection = (String) cbNextScanSelection.getSelectedItem();
				textFieldValue.setEnabled(scanSelection == "Exact value");
			}
		});
		
		this.setSize(400,400);
		mainPanel = new JPanel(new BorderLayout());
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
		
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
		
		mainPanel.add(rightPanel, BorderLayout.CENTER);
		mainPanel.add(leftPanel, BorderLayout.LINE_START);
		
		textFieldValue = new JTextField();
		buttonNewScan = new JButton("Reset");
		buttonNextScan = new JButton("Scan");
		
		textFieldValue.addKeyListener(inputFilter);
		
		buttonNewScan.setEnabled(false);
		buttonNewScan.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				resetResults();
			}
		});
		
		buttonNextScan.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				initiateScan();
			}
		});
		
		JCheckBox checkBoxHex = new JCheckBox("Hex", true);
		checkBoxHex.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				setNumberFormatHex(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		
		this.add(mainPanel);
		buttonPanel.add(buttonNewScan);
		buttonPanel.add(buttonNextScan);
		valuePanel.add(new JLabel("Value:"));
		valuePanel.add(textFieldValue);
		valuePanel.add(checkBoxHex);
		topPanel.add(buttonPanel);
		topPanel.add(valuePanel);
		topPanel.add(cbNewScanSelection);
		topPanel.add(cbNextScanSelection);
		rightPanel.add(topPanel);
		
		resultsTable = new JTable(new DefaultTableModel(new String[]{"Offset", "Value"}, 0){
			//table model override
			public Class<?> getColumnClass(int columnIndex){
				return String.class;
			}
		}){
			//table override
			public boolean isCellEditable(int row, int column){
				if(column == COL_OFFSET) return false; // disallow editing offsets
				if(getValueAt(row, COL_OFFSET) == null) return false; // disallow editing value if offset is blank
				return true;
			}
			public void editingStopped(ChangeEvent e){
				try {
					JTextField editor = (JTextField)getEditorComponent();
					editor.setBorder(new EmptyBorder(0, 0, 0, 0));
					editor.setFont(monofont);
					String enteredText = editor.getText();
					editor.addKeyListener(inputFilter);
					int newValue = Integer.parseInt(enteredText, numberBase); // fetch new value
					if(newValue > 255) return; // cancel entry
					int offset = resultOffsets[getEditingRow() + scrollOffset]; // TODO ACCOUNT FOR SCROLLBOX OFFSET
					nes.mem.write(offset, newValue, false);
					super.editingStopped(e);
					refreshTable(); // fix formatting
				} catch(Exception ex){
					// entry is cancelled from bad integer parse
					//System.out.println("invalid value");
				}
			}
		};
		resultsTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){ // extend the cell component renderer with background colors
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				int color = 0x111120 | ((int)(16 * tableColors[row]) << 16);
				c.setBackground(new Color(color));
				return c;
			}
		});
		
		for(int i = 0; i < 20; i++){ // 20 ROWS, 2 COLUMNS
			((DefaultTableModel)resultsTable.getModel()).addRow(new Object[]{"",""});
		}
		
		//resultsTable.setBorder(new EmptyBorder(0,0,0,0));
		resultsTable.setRowMargin(0);
		resultsTable.setFont(monofont);
		resultsTable.setForeground(Color.WHITE);
		resultsTable.setShowGrid(false);
		resultsTable.setIntercellSpacing(new Dimension(0, 0));
		resultsTable.setFillsViewportHeight(true);
		
		resultsLabel = new JLabel("0 result(s)");
		resultsTableHeader = resultsTable.getTableHeader();
		resultsTableHeader.setReorderingAllowed(false);
		
		resultsTableWrapper = new JPanel();
		resultsTableWrapper.setLayout(new BoxLayout(resultsTableWrapper, BoxLayout.LINE_AXIS));
		//resultsTableWrapper.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		resultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultsTableHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		resultsTableWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		leftPanel.setBorder(new EmptyBorder(5, 5, 0, 0));
		
		resultsScrollBar = new JScrollBar(JScrollBar.VERTICAL);
		resultsScrollBar.setBorder(new EmptyBorder(5, 0, 5, 0));
		resultsScrollBar.setMinimum(0);
		resultsScrollBar.setMaximum(1);
		resultsScrollBar.setUnitIncrement(1);
		resultsScrollBar.setBlockIncrement(1);
		
		resultsScrollBar.addAdjustmentListener(new AdjustmentListener(){
			public void adjustmentValueChanged(AdjustmentEvent e){
				//System.out.println(resultsScrollBar.getValue());
				scrollOffset = resultsScrollBar.getValue();
				refreshTable();
				clearColors();
			}
		});
		
		resultsTableWrapper.add(resultsTable);
		resultsTableWrapper.add(resultsScrollBar);
		
		leftPanel.add(resultsLabel);
		leftPanel.add(resultsTableHeader);
		leftPanel.add(resultsTableWrapper);
		
		cbNextScanSelection.setVisible(false);
		
		textFieldValue.setColumns(4);
		
		pack();
		setResizable(false);
		
		refresher = new Timer(50, new ActionListener(){ // refresh results
			public void actionPerformed(ActionEvent e){
				if(resultOffsets != null) refreshTable();
			}
		});
		
		refresher.start();
	}
	
	private void clearColors(){
		for(int i = 0; i < 20; i++){
			tableColors[i] = 0;
		}
	}
	
	private void setNumberFormatHex(boolean setting){
		int prevNumberBase = numberBase;
		if(setting){
			numberFormat = "%02X";
			numberBase = 16;
			inputFilter.setRegex(KeyFilter.HEX);
		} else {
			numberFormat = "%d";
			numberBase = 10;
			inputFilter.setRegex(KeyFilter.NUM);
		}
		try {
			int value = Integer.parseInt(textFieldValue.getText(), prevNumberBase);
			textFieldValue.setText(String.format(numberFormat, value));
		} catch(Exception e){
			textFieldValue.setText("");
		}
		
		refreshTable();
		clearColors();
	}
	
	private void refreshTable(){
		for(int i = 0; i < 20; i++){
			int resultIndex = scrollOffset + i;
			if(resultOffsets == null || i >= resultOffsets.length){
				resultsTable.setValueAt("", i, COL_OFFSET);
				resultsTable.setValueAt("", i, COL_VALUE);
				tableColors[i] = 0;
				continue;
			}
			int offset = resultOffsets[resultIndex];
			int value  = nes.mem.read(offset, false);
			String stringValue = String.format(numberFormat, value);
			resultsTable.setValueAt(String.format("%04X", offset), i, COL_OFFSET);
			if(tableColors[i] != 0){
				tableColors[i]--;
				//System.out.println(tableColors[i]);
			}
			if(currentTableVals[i] != null && !currentTableVals[i].equals(stringValue)){
				//System.out.printf("%s %s\n", currentTableVals[i], stringValue);
				//System.out.println((String)resultsTable.getValueAt(i, COL_VALUE));
				tableColors[i] = 15;
			}
			resultsTable.setValueAt(stringValue, i, COL_VALUE);
			currentTableVals[i] = stringValue;
		}
	}
	
	private void fillResultOffsets(){
		Set s = results.keySet();
		resultOffsets = new Integer[s.size()];
		resultOffsets = results.keySet().toArray(resultOffsets);
	}
	
	private void resetResults(){
		resultsLabel.setText("0 result(s)");
		results.clear();
		resultOffsets = null;
		for(int i = 0; i < 20; i++){
			resultsTable.setValueAt("", i, COL_OFFSET);
			resultsTable.setValueAt("", i, COL_VALUE);
			//currentTableVals[i] = "";
		}
		tableColors = new int[20];
		//resultOffsets = new Integer[20];
		currentTableVals = new String[20];
		cbNewScanSelection.setSelectedIndex(0);
		cbNextScanSelection.setSelectedIndex(0);
		scanSelection = "Exact value";
		cbNewScanSelection.setVisible(true);
		cbNextScanSelection.setVisible(false);
		buttonNewScan.setEnabled(false);
		newScan = true;
	}
	
	private void printResults(){
		for(int i = 0; i < resultOffsets.length; i++){
			int offset = resultOffsets[i];
			//System.out.printf("%04X: %02X\n", offset, results.get(offset));
		}
	}
	
	private void initiateScan(){
		//System.out.println("starting scan");
		switch(scanSelection){
			case "Exact value":
				int value = Integer.parseInt(textFieldValue.getText(), numberBase);
				scanExact(value);
				break;
			case "Unknown value":
				scanUnknown();
				break;
			case "Changed value":
				scanChanged();
				break;
			case "Unchanged value":
				scanUnchanged();
				break;
			case "Increased value":
				scanIncreased();
				break;
			case "Decreased value":
				scanDecreased();
				break;
		}
		if(newScan){
			scanSelection = "Exact value";
			textFieldValue.setEnabled(true);
			newScan = false;
		}
		buttonNewScan.setEnabled(true);
		cbNewScanSelection.setVisible(false);
		cbNextScanSelection.setVisible(true);
		fillResultOffsets();
		resultsScrollBar.setValue(0);
		scrollOffset = 0;
		int scrollMax = results.size() - 19;
		if(scrollMax < 1) scrollMax = 1;
		resultsScrollBar.setMaximum(scrollMax);
		resultsLabel.setText(results.size() + " result(s)");
		refreshTable();
	}
	
	private void scanExact(int value){
		if(newScan){
			//System.out.println("new exact scan");
			for(int offset = rangeMin; offset < rangeMax + 1; offset++){
				int v = nes.mem.read(offset, false);
				if(v == value){
					results.put(offset, v);
				}
			}
		} else {
			//System.out.println("next exact scan");
			for(int i = 0; i < resultOffsets.length; i++){
				int offset = resultOffsets[i];
				int v = nes.mem.read(offset, false);
				if(v == value){
					// value of offset in results = supplied value
					results.put(offset, value);
					continue;
				}
				// not equal, remove result
				results.remove(offset);
			}
		}
	}
	
	private void scanUnknown(){ // scan everything in range, first scan only
		for(int offset = rangeMin; offset < rangeMax + 1; offset++){
			int v = nes.mem.read(offset, false);
			results.put(offset, v);
		}
	}
	
// the following methods are inaccessible by the first scan
	
	private void scanChanged(){
		for(int i = 0; i < resultOffsets.length; i++){
			int offset = resultOffsets[i];
			int heldValue = results.get(offset);
			int currentValue = nes.mem.read(offset);
			if(heldValue == currentValue){
				results.remove(offset); // remove unchanged
				continue;
			}
			results.put(offset, currentValue); // update result with changed val
		}
	}
	
	private void scanUnchanged(){
		for(int i = 0; i < resultOffsets.length; i++){
			int offset = resultOffsets[i];
			int heldValue = results.get(offset);
			int currentValue = nes.mem.read(offset);
			if(heldValue != currentValue){
				results.remove(offset); // remove changed
				continue;
			}
		}
	}
	
	private void scanIncreased(){
		for(int i = 0; i < resultOffsets.length; i++){
			int offset = resultOffsets[i];
			int heldValue = results.get(offset);
			int currentValue = nes.mem.read(offset);
			if(heldValue >= currentValue){
				results.remove(offset); // remove not greater than
				continue;
			}
			results.put(offset, currentValue); // update result with increased val
		}
	}
	
	private void scanDecreased(){
		for(int i = 0; i < resultOffsets.length; i++){
			int offset = resultOffsets[i];
			int heldValue = results.get(offset);
			int currentValue = nes.mem.read(offset);
			if(heldValue <= currentValue){
				results.remove(offset); // remove not less than
				continue;
			}
			results.put(offset, currentValue); // update result with decreased val
		}
	}
}