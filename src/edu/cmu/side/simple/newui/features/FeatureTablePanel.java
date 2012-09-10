package edu.cmu.side.simple.newui.features;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import edu.cmu.side.SimpleWorkbench;
import edu.cmu.side.simple.feature.Feature;
import edu.cmu.side.simple.feature.FeatureTable;
import edu.cmu.side.simple.feature.Feature.Type;
import edu.cmu.side.simple.newui.AbstractListPanel;
import edu.cmu.side.simple.newui.FeatureActivationCell;
import edu.cmu.side.simple.newui.FeatureTableModel;
import edu.cmu.side.simple.newui.SIDETable;

import se.datadosen.component.RiverLayout;

/**
 * Feature table details appear in this panel, as a giant JTable full of evaluation columns.
 * @author emayfiel
 *
 */
public class FeatureTablePanel extends AbstractListPanel{
	private static final long serialVersionUID = 8864372850829867678L;

	private FeatureActivationCell activationCellRenderer = new FeatureActivationCell();
	private JLabel selectedTableName = new JLabel();
	private JLabel selectedTableSize = new JLabel();
	private JTextField filterField = new JTextField();
	private JButton filterButton = new JButton("Filter");
	private SIDETable featureTable = new SIDETable();
	private static FeatureTableModel tableModel;
	private FeatureTable currentFeatureTable = null;
	private String currentFilter = "";
	private JButton activateButton = new JButton("(De)activate");
	private JButton labButton = new JButton("Move to Lab");
	private JButton freezeButton = new JButton("Freeze");
	private static boolean activationsChanged = false;


	private JButton saveButton = new JButton("Save");
	private JButton loadButton = new JButton("Load");
	private JButton exportButton = new JButton("Export");
	public FeatureTablePanel(){
		setLayout(new BorderLayout());
		tableModel = new FeatureTableModel();
		JPanel topPanel = new JPanel(new RiverLayout());
		topPanel.add("left", new JLabel("Feature Table: "));
		topPanel.add("left", selectedTableName);
		topPanel.add("left", selectedTableSize);
		filterField.setBorder(BorderFactory.createLineBorder(Color.gray));
		topPanel.add("br hfill", filterField);
		filterField.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if(e.getKeyCode()==KeyEvent.VK_ENTER){
					refreshPanel();
				}
			}
		});
		loadButton.addActionListener(new SimpleWorkbench.FeatureTableLoadListener());
		saveButton.addActionListener(new SimpleWorkbench.FeatureTableSaveListener());
		exportButton.addActionListener(new SimpleWorkbench.FeatureTableExportListener());

		activateButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				featureTable.activateSelected();
				activationsChanged = true;
				refreshPanel();
			}
		});
		labButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				for(int i : featureTable.getSelectedRows()){
					Object feat = featureTable.getSortedValue(i, 1);
					if(feat instanceof Feature){
						FeatureLabPanel.addFeatureToLab((Feature)feat);							
					}
				}
				fireActionEvent();
			}
		});
		topPanel.add("right", filterButton);
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshPanel();
			}
		});
		freezeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				SimpleWorkbench.addFeatureTable(FeatureTableListPanel.getSelectedFeatureTable().subsetClone());
				fireActionEvent();
			}
		});
		featureTable.setModel(tableModel);
		featureTable.setBorder(BorderFactory.createLineBorder(Color.gray));
		featureTable.setShowHorizontalLines(true);
		featureTable.setShowVerticalLines(true);
		JScrollPane tableScroll = new JScrollPane(featureTable);
		tableScroll.setPreferredSize(new Dimension(500, 200));
		
		JPanel bottomPanel = new JPanel(new RiverLayout());
		bottomPanel.add("br left", activateButton);
		bottomPanel.add("left", labButton);
		bottomPanel.add("left", freezeButton);
		bottomPanel.add("left", saveButton);
		bottomPanel.add("left", exportButton);
		bottomPanel.add("left", loadButton);

		add(BorderLayout.NORTH, topPanel);
		add(BorderLayout.CENTER, tableScroll);
		add(BorderLayout.SOUTH, bottomPanel);
		featureTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	}

	public Set<Feature> filterFeatureSet(FeatureTable table){
		Set<Feature> features = new HashSet<Feature>();
		String[] filter = filterField.getText().split("\\s+");
		Map<String, Double> numericFilters = new TreeMap<String, Double>();
		Map<String, String> nominalFilters = new TreeMap<String, String>();
		for(String filt : filter){
			Pattern pattern = Pattern.compile(">|<|=");
			Matcher match = pattern.matcher(filt);
			if(!filt.startsWith("<") && !filt.endsWith(">") && match.find()){
				numericFilters.put(filt.substring(0, match.start()+1), Double.parseDouble(filt.substring(match.start()+1)));	
			}else if(filt.contains(":")){
				nominalFilters.put(filt.substring(0, filt.indexOf(':')), filt.substring(filt.indexOf(':')+1));
			}else if(filt.trim().length()>0){
				nominalFilters.put("feature name", filt);
			}

		}
		for(Feature f : table.getFeatureSet()){
			boolean add = true;
			for(String numFilt : numericFilters.keySet()){
				if(add){
					String filt = numFilt.substring(0, numFilt.length()-1);
					String sign = numFilt.substring(numFilt.length()-1);
					Double featVal = (Double)table.getEvaluations().get(filt).get(f);
					if(sign.equals("<") && featVal > numericFilters.get(numFilt) ||
							sign.equals(">") && featVal < numericFilters.get(numFilt) ||
							sign.equals("=") && featVal != numericFilters.get(numFilt)){
						add = false;
					}					
				}
			}
			for(String nomFilt : nominalFilters.keySet()){
				if(add){
					if(nomFilt.equals("feature name")){
						if(!f.getFeatureName().contains(nominalFilters.get(nomFilt))){
							add = false;
						}
					}else{
						if(table.getEvaluations().get(nomFilt).equals(nominalFilters.get(nomFilt))){
							add = false;
						}
					}
				}
			}
			if(add){
				features.add(f);
			}
		}
		return features;
	}

	public void refreshPanel(){
		FeatureTable table = FeatureTableListPanel.getSelectedFeatureTable();
		if(table == null){
			tableModel = new FeatureTableModel();
			selectedTableName.setText("");
			selectedTableSize.setText("");
			featureTable.setModel(tableModel);
			currentFeatureTable = table;
		}
		if(table != null && (table != currentFeatureTable || activationsChanged || !currentFilter.equals(filterField.getText().trim()) || table.getEvaluations().keySet().size() != tableModel.getColumnCount()-3)){
			currentFilter = filterField.getText();
			selectedTableName.setText(table.getTableName());
			selectedTableSize.setText("("+table.getFeatureSet().size() + " features)");
			tableModel = new FeatureTableModel();
			tableModel.addColumn("from");
			tableModel.addColumn("feature name");
			tableModel.addColumn("type");

			for(String eval : table.getConstantEvaluations()){
				tableModel.addColumn(eval);
			}
			List<String> otherEvals = new ArrayList<String>();
			for(String eval : table.getEvaluations().keySet()){
				boolean found = false;
				for(String s : table.getConstantEvaluations()){
					if(eval.equals(s)){ found = true; break; }
				}
				if(!found){
					otherEvals.add(eval); 
					tableModel.addColumn(eval);
				}
			}
			for(Feature f : filterFeatureSet(table)){
				Object[] row = getFeatureDisplayRow(FeatureTableListPanel.getSelectedFeatureTable(), otherEvals, tableModel, f);
				tableModel.addRow(row);
			}
			currentFeatureTable = table;
			featureTable.setModel(tableModel);
			featureTable.setRowSorter(new TableRowSorter<TableModel>(tableModel));
			TableColumnModel columnModel = featureTable.getColumnModel();
			columnModel.getColumn(0).setPreferredWidth(30);
			columnModel.getColumn(1).setPreferredWidth(120);
			featureTable.setColumnModel(columnModel);
			TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel);
			featureTable.setRowSorter(sorter);
			for(int i = 0; i < tableModel.getColumnCount(); i++){
				if(tableModel.getColumnName(i).equals("kappa") || tableModel.getColumnName(i).equals("correlation")){
					ArrayList<RowSorter.SortKey> sortKey = new ArrayList<RowSorter.SortKey>();
					sortKey.add(new RowSorter.SortKey(i, SortOrder.DESCENDING));
					sorter.setSortKeys(sortKey);
					sorter.sort();								
				}
			}
			if(tableModel.getColumnCount()>2){
				for(int i = 0; i < featureTable.getColumnCount(); i++){					
					featureTable.getColumnModel().getColumn(i).setCellRenderer(activationCellRenderer);
				}
			}
		}
		activationsChanged = false;
		repaint();
	}

	public static TableModel getTableModel(){
		return tableModel;
	}

	public static Object[] getFeatureDisplayRow(FeatureTable table, List<String> otherEvals, TableModel model, Feature f) {
		Map<String, Map<Feature, Comparable>> evals = table.getEvaluations();
		Object[] row = new Object[model.getColumnCount()];
		row[0] = f.getExtractorPrefix();
		row[1] = f;
		row[2] = f.getFeatureType();
		int i = 3;
		for(String eval : table.getConstantEvaluations()){
			if(evals.containsKey(eval) && evals.get(eval).containsKey(f) && i < row.length){
				row[i++] = evals.get(eval).get(f);				
			}
		}	
		if(otherEvals != null){
			for(String eval : otherEvals){
				row[i++] = evals.get(eval).get(f);
			}			
		}
		return row;
	}

	public static void activationsChanged(){
		activationsChanged = true;
	}
}
