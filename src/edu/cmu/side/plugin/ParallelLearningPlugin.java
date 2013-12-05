package edu.cmu.side.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;

public abstract class ParallelLearningPlugin extends LearningPlugin
{
	final static ExecutorService LEARNER_THREAD_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	private static final long serialVersionUID = 1L;

	public List<PredictionResult> doCrossValidation(final FeatureTable table, final Map<Integer, Integer> foldsMap, final Set<Integer> folds, final OrderedPluginMap wrappers,
			final StatusUpdater progressIndicator) throws Exception
	{
		final int numFolds = folds.size();
		
		List<Callable<PredictionResult>> tasks = new ArrayList<Callable<PredictionResult>>();
		
//		final ArrayList<Double> times = new ArrayList<Double>(numFolds);
		List<PredictionResult> results = new ArrayList<PredictionResult>();

		final Map<String, String> learnerSettings = ParallelLearningPlugin.this.generateConfigurationSettings();
		
		for(final int fold : folds)
		{
			Callable<PredictionResult> task = new Callable<PredictionResult>()
			{

				@Override
				public PredictionResult call() throws Exception
				{
					// clone the learner with its current settings
					LearningPlugin clonedLearner;
					try
					{
						clonedLearner = ParallelLearningPlugin.this.getClass().newInstance();
						clonedLearner.configureFromSettings(learnerSettings);
					}
					catch (Exception e)
					{
						System.err.println("ParallelLearningPlugin 52:\tError cloning learner "+ParallelLearningPlugin.this+". Continuing with un-cloned learner");
						e.printStackTrace();
						clonedLearner = ParallelLearningPlugin.this;
					}
					
					//clone the *wrappers* too!
					final OrderedPluginMap clonedWrappers = new OrderedPluginMap();
					
					for(SIDEPlugin key : wrappers.keySet())
					{
						SIDEPlugin clone = key.getClass().newInstance();
						
						clonedWrappers.put(clone, wrappers.get(key));
					}

					System.out.println(new Date()+"\tParallelLearningPlugin 57:\tstarting to validate fold "+fold);
					PredictionResult result = clonedLearner.validateFold(fold, table, foldsMap, numFolds, clonedWrappers, progressIndicator);
					System.out.println(new Date()+"\tParallelLearningPlugin 59:\tdone validating fold "+fold);
					return result;
				}
				
			};
			tasks.add(task);
		}
		
		List<Future<PredictionResult>> futureResults = LEARNER_THREAD_POOL.invokeAll(tasks);
		
		for(Future<PredictionResult> future : futureResults)
		{
			results.add(future.get());
		}
		
		return results;
	}

	/*@Override
	public FeatureTable wrapTableBefore(FeatureTable newData, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator,
			OrderedPluginMap wrappers, boolean learn)
	{
		for (SIDEPlugin wrapper : wrappers.keySet())
		{
			synchronized(wrapper)
			{
				SIDEPlugin clone;
				try
				{
					clone = wrapper.getClass().newInstance();
				}
				catch (Exception e)
				{
					System.err.println("ParallelLearningPlugin 91:\tError cloning wrapper "+wrapper+". Continuing with un-cloned wrapper");
					e.printStackTrace();
					clone = wrapper;
				}
				clone.configureFromSettings(wrappers.get(wrapper));
				if (learn)
				{
					((WrapperPlugin) clone).learnFromTrainingData(newData, fold, foldsMap, progressIndicator);
					//whatever the wrapper has learned is stored in configuration settings.
					wrappers.put(wrapper, clone.generateConfigurationSettings());
				}
			
				newData = ((WrapperPlugin) clone).wrapTableBefore(newData, fold, foldsMap, progressIndicator);
			}
		}
		return newData;
	}*/
	
}
