package edu.cmu.side.genesis.view.extract;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.cmu.side.genesis.view.generic.GenericTripleFrame;

public class ExtractFeaturesPane extends JPanel{

	private static GenericTripleFrame top;
	private static ExtractActionPanel action = new ExtractActionPanel();
	private static ExtractBottomPanel bottom = new ExtractBottomPanel();
	
	public ExtractFeaturesPane(){
		setLayout(new BorderLayout());
		top = new GenericTripleFrame(new ExtractLoadPanel(), new ExtractPluginChecklistPanel(), new ExtractPluginConfigPanel());
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, top);
		panel.add(BorderLayout.SOUTH, action);
		pane.setTopComponent(panel);
		pane.setBottomComponent(bottom);
//		this.setPreferredSize(new Dimension(950,675));
		top.setPreferredSize(new Dimension(950,400));
		bottom.setPreferredSize(new Dimension(950,200));
		add(BorderLayout.CENTER, pane);
	}

	public void refreshPanel() {
		top.refreshPanel();
		action.refreshPanel();
		bottom.refreshPanel();
	}
}
