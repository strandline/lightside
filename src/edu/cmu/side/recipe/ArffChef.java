package edu.cmu.side.recipe;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.view.util.RecipeExporter;

/**
 * loads a model trained using lightSIDE uses it to label new instances.
 * 
 * @author dadamson
 */
public class ArffChef extends Chef
{
//    static 
//    { 
//        System.setProperty("java.awt.headless", "true");
//        System.out.println(java.awt.GraphicsEnvironment.isHeadless()?"Running in headless mode.":"Not actually headless");
//     }
    
	


	public static void main(String[] args) throws Exception
	{
		
		String recipePath, outPath;
		if (args.length < 2)
		{
			System.err.println("usage: arff.sh saved/template.side saved/newtable.arff data.csv...");
			return;
		}
		
		recipePath = args[0];
		outPath = args[1];

		Set<String> corpusFiles = new HashSet<String>();
		
		String dataFile = "data/MovieReviews.csv";
		if (args.length < 3) corpusFiles.add(dataFile);
		else for(int i = 2; i < args.length; i++)
		{
			corpusFiles.add(args[i]);
		}

		if(!quiet) System.out.println("Loading "+recipePath);
		Recipe recipe = loadRecipe(recipePath);
		printMemoryUsage();

		if(!quiet) System.out.println("Loading documents: "+corpusFiles);
		Recipe result = followRecipe(recipe, new DocumentList(corpusFiles), Stage.MODIFIED_TABLE, recipe.getFeatureTable().getThreshold());
		

		System.out.println("Saving finished recipe to "+outPath);
		RecipeExporter.exportToARFF(result.getTrainingTable(), new File(outPath));
	}


}
