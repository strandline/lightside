package edu.cmu.side.view.generic;

import java.io.File;

import javax.swing.ImageIcon;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;

public abstract class GenericLoadCSVPanel extends GenericLoadPanel
{
	public GenericLoadCSVPanel(String title)
	{
		super(title);
		this.remove(save);
		chooser.setCurrentDirectory(new File("data"));
		ImageIcon iconLoad = new ImageIcon("toolkits/icons/folder_page.png");
		load.setIcon(iconLoad);
	}
	
	@Override
	public void refreshPanel()
	{
		refreshPanel(Workbench.getRecipesByPane(Stage.DOCUMENT_LIST));
	}

	@Override
	public void loadNewItem()
	{
		loadNewDocumentsFromCSV();
	}

}
