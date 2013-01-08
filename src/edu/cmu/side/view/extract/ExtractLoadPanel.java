package edu.cmu.side.view.extract;


import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.generic.GenericLoadPanel;

public class ExtractLoadPanel extends GenericLoadPanel{

	public ExtractLoadPanel(String s){
		super();		
		ImageIcon iconDelete = new ImageIcon("toolkits/icons/cross.png");
		ImageIcon iconLoad = new ImageIcon("toolkits/icons/folder_table.png");
		delete.setText("");
		delete.setIcon(iconDelete);
		delete.setToolTipText("Delete");
		load.setText("");
		load.setToolTipText("Open");
		load.setIcon(iconLoad);
		load.addActionListener(new ExtractFeaturesControl.AddFilesListener(this));
		label = new JLabel(s);
		add("hfill", label);
		add("right", load);
		add("br hfill", combo);
		add("left", delete);
		describeScroll = new JScrollPane();
		describePanel.add(BorderLayout.CENTER, describeScroll);
		add("br hfill vfill", describePanel);
	}

	@Override
	public void setHighlight(Recipe r) {
		ExtractFeaturesControl.setHighlightedDocumentListRecipe(r);
	}

	@Override
	public Recipe getHighlight() {
		return ExtractFeaturesControl.getHighlightedDocumentListRecipe();
	}

	@Override
	public void refreshPanel() {		
		refreshPanel(ExtractFeaturesControl.getDocumentLists());
	}
}
