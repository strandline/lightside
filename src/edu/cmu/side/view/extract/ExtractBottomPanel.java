package edu.cmu.side.view.extract;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericTableMetricPanel;

public class ExtractBottomPanel extends JPanel{

	GenericLoadPanel control = new GenericLoadPanel("Feature Table:") {	
		
		@Override
		public void setHighlight(Recipe r) {
			ExtractFeaturesControl.setHighlightedFeatureTableRecipe(r);
		}

		@Override
		public void refreshPanel() {
			refreshPanel(ExtractFeaturesControl.getFeatureTables());
		}

		@Override
		public Recipe getHighlight() {
			return ExtractFeaturesControl.getHighlightedFeatureTableRecipe();
		}
	};

	ExtractTableChecklistPanel checklist = new ExtractTableChecklistPanel();
	GenericTableMetricPanel display = new GenericTableMetricPanel();

	public ExtractBottomPanel(){
		setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane();
		split.setLeftComponent(control);
		split.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane displaySplit = new JSplitPane();
		displaySplit.setLeftComponent(checklist);
		displaySplit.setRightComponent(display);
		displaySplit.setBorder(BorderFactory.createEmptyBorder());
		displaySplit.setPreferredSize(new Dimension(650,200));
		checklist.setPreferredSize(new Dimension(275,200));
		display.setPreferredSize(new Dimension(350, 200));
		split.setRightComponent(displaySplit);
		control.setPreferredSize(new Dimension(275,200));
		add(BorderLayout.CENTER, split);
	}

	public void refreshPanel(){
		control.refreshPanel();
		checklist.refreshPanel();
		if(ExtractFeaturesControl.hasHighlightedFeatureTable()){                	
			display.refreshPanel(ExtractFeaturesControl.getHighlightedFeatureTableRecipe().getFeatureTable(), ExtractFeaturesControl.getTableEvaluationPlugins());
		}else{
			display.refreshPanel(null, ExtractFeaturesControl.getTableEvaluationPlugins());
		}
	}
}