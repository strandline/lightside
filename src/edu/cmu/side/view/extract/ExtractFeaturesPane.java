package edu.cmu.side.view.extract;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.view.generic.GenericPluginChecklistPanel;
import edu.cmu.side.view.generic.GenericPluginConfigPanel;
import edu.cmu.side.view.generic.GenericTripleFrame;

public class ExtractFeaturesPane extends JPanel{

	private static GenericTripleFrame top;
	private static ExtractActionPanel action = new ExtractActionPanel(ExtractFeaturesControl.getUpdater());
	private static ExtractBottomPanel bottom = new ExtractBottomPanel(action);
	
	public ExtractFeaturesPane(){
		setLayout(new BorderLayout());

		GenericPluginChecklistPanel<FeaturePlugin> pluginChecklist = new GenericPluginChecklistPanel<FeaturePlugin>("Feature Extractor Plugins:"){
			@Override
			public Map<FeaturePlugin, Boolean> getPlugins() {
				return ExtractFeaturesControl.getFeaturePlugins();
			}
		};
		
		GenericPluginConfigPanel<FeaturePlugin> pluginConfig = new GenericPluginConfigPanel<FeaturePlugin>(){
			@Override
			public void refreshPanel() {
				refreshPanel(ExtractFeaturesControl.getFeaturePlugins());
				revalidate();
				repaint();
			}
		};
		
		pluginConfig.refreshPanel();
		
		ExtractCombinedLoadPanel load = new ExtractCombinedLoadPanel("CSV Files:");
		top = new GenericTripleFrame(load, pluginChecklist, pluginConfig);
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, top);
		panel.add(BorderLayout.SOUTH, action);
		pane.setTopComponent(panel);
		pane.setBottomComponent(bottom);
		top.setPreferredSize(new Dimension(950,400));
		bottom.setPreferredSize(new Dimension(950,200));
		add(BorderLayout.CENTER, pane);

		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, pluginChecklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, pluginConfig);
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, bottom);
		GenesisControl.addListenerToMap(pluginChecklist, pluginConfig);
		
	}

}
