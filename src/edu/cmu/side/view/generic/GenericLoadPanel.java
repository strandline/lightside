package edu.cmu.side.view.generic;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.view.util.AbstractListPanel;

public abstract class GenericLoadPanel extends AbstractListPanel{

	protected JPanel buttons = new JPanel();

	protected JPanel describePanel = new JPanel(new BorderLayout());
	protected JLabel label;
	
	protected JFileChooser chooser = new JFileChooser(new File("saved"));
	
	protected GenericLoadPanel(){
		setLayout(new RiverLayout());
		combo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				if(combo.getSelectedItem() != null){
					Recipe r = (Recipe)combo.getSelectedItem();
					setHighlight(r);
					describeScroll = new JScrollPane(GenesisControl.getRecipeTree(getHighlight()));
					describePanel.removeAll();
					describePanel.add(BorderLayout.CENTER, describeScroll);
					describePanel.validate();
				}
				Workbench.update();
			}
		});
		delete.addActionListener(new DeleteFilesListener(combo, this));
	}

	public static class DeleteFilesListener implements ActionListener{
		private JComboBox parentComponent;
		private GenericLoadPanel loadPanel;

		public DeleteFilesListener(JComboBox parentComponent, GenericLoadPanel load){
			this.parentComponent = parentComponent;
			loadPanel = load;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			RecipeManager.deleteRecipe((Recipe)parentComponent.getSelectedItem());
			loadPanel.deleteHighlight();
		}
	}

	public GenericLoadPanel(String l){
		this();
		label = new JLabel(l);
		buttons.setLayout(new RiverLayout());
		ImageIcon iconDelete = new ImageIcon("toolkits/icons/cross.png");
		ImageIcon iconSave = new ImageIcon("toolkits/icons/disk.png");
		ImageIcon iconLoad = new ImageIcon("toolkits/icons/folder_table.png");
		delete.setText("");
		delete.setIcon(iconDelete);
		delete.setToolTipText("Delete");
		delete.setBorderPainted(true);
		save.setText("");
		save.setIcon(iconSave);
		save.setToolTipText("Save");
		save.setBorderPainted(true);
		load.setText("");
		load.setIcon(iconLoad);
		load.setToolTipText("Load");
		load.setBorderPainted(true);
		//buttons.add("left", load);
		//buttons.add("left", save);
		add("hfill", label);
		add("right", load);
		add("br hfill", combo);
		add("right", save);
		add("right", delete);
		describeScroll = new JScrollPane();
		describePanel.add(BorderLayout.CENTER, describeScroll);
		add("br hfill vfill", describePanel);
		//add("br left hfill", buttons);
		
		connectButtonListeners();
	}

	public abstract void setHighlight(Recipe r);

	public abstract Recipe getHighlight();

	public void deleteHighlight(){
		describeScroll = new JScrollPane();
		setHighlight(null);
	}

	@Override
	public abstract void refreshPanel();

	public void refreshPanel(Collection<Recipe> recipes){
		if(combo.getItemCount() != recipes.size()){
			Workbench.reloadComboBoxContent(combo, recipes, getHighlight());
		}
		if(getHighlight() == null && combo.getItemCount() > 0){
			Recipe r = (Recipe)combo.getItemAt(combo.getItemCount()-1);
			setHighlight(r);
		}
		if(getHighlight() != null && !RecipeManager.containsRecipe(getHighlight())){
			deleteHighlight();
		}
		if(getHighlight() != null){
			combo.setSelectedItem(getHighlight());
			save.setEnabled(true);
			combo.setEnabled(true);
			delete.setEnabled(true);
		}else{
			combo.setEnabled(false);
			combo.setSelectedIndex(-1);
			save.setEnabled(false);
			delete.setEnabled(false);
			describeScroll = new JScrollPane();
			describePanel.removeAll();
			describePanel.add(BorderLayout.CENTER, describeScroll);
		}
	}
	
	/** load/save/delete button listeners*/
	private void connectButtonListeners()
	{
		save.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(combo.getSelectedIndex() >= 0)
				{
					saveSelectedItem();
				}
			}
			
		});
		
//		delete.addActionListener(new ActionListener()
//		{
//
//			@Override
//			public void actionPerformed(ActionEvent arg0)
//			{
//				if(combo.getSelectedIndex() >= 0)
//				{
//					deleteSelectedItem();
//				}
//			}
//			
//		});
		

		load.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				loadNewItem();
			}
			
		});
	}
	
	public void saveSelectedItem()
	{
		Recipe recipe = (Recipe) combo.getSelectedItem();//TODO: should this be more generic?
		
		chooser.setSelectedFile(new File("saved/"+recipe.getRecipeName()));
		int response = chooser.showSaveDialog(this);
		if(response == JFileChooser.APPROVE_OPTION)
		{
			File target = chooser.getSelectedFile();
			if(target.exists())
			{
				response = JOptionPane.showConfirmDialog(this, "Do you want to overwrite "+target+"?");
				if(response != JOptionPane.YES_OPTION)
					return;
			}
			
			try
			{
				FileOutputStream fout = new FileOutputStream(target);
				ObjectOutputStream oos = new ObjectOutputStream(fout);
				oos.writeObject(recipe);
				
			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(this, "Error while saving:\n"+e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			
		}
	}
	
	public void deleteSelectedItem()
	{
		Recipe recipe = (Recipe) combo.getSelectedItem();//TODO: should this be more generic?
		RecipeManager.removeRecipe(recipe);
	};
	
	public void loadNewItem()
	{
		int response = chooser.showOpenDialog(this);
		if(response == JFileChooser.APPROVE_OPTION)
		{
			File target = chooser.getSelectedFile();
			if(!target.exists())
			{
				JOptionPane.showMessageDialog(this, "There's not a file there!", "No Such File", JOptionPane.ERROR_MESSAGE);
			}
			
			try
			{
				FileInputStream fout = new FileInputStream(target);
				ObjectInputStream in = new ObjectInputStream(fout);
				Recipe recipe = (Recipe) in.readObject(); //TODO: should this be more generic?
				RecipeManager.addRecipe(recipe);
			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(this, "Error while loading:\n"+e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			
		}
	}
}
