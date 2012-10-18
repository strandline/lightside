package edu.cmu.side.simple.feature;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


import java.util.*;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.Evaluation;

import edu.cmu.side.simple.FeaturePlugin;
import edu.cmu.side.simple.SimpleDocumentList;
import edu.cmu.side.simple.feature.Feature.Type;
import edu.cmu.side.simple.newui.features.FeaturePluginPanel;

/**
 * A many-directional mapping of Features, FeatureHits and indexes into the DocumentList.
 *
 */
public class FeatureTable implements Serializable
{
	private static final long serialVersionUID = 1048801132974685418L;

	public final int NUM_BASELINE_EVALUATIONS = 7;
	private Collection<FeaturePlugin> extractors;
	private SimpleDocumentList documents;
	private Map<Feature, Collection<FeatureHit>> hitsPerFeature;
	private Map<Feature, Boolean> activatedFeatures;
	private List<Collection<FeatureHit>> hitsPerDocument;
	private String tableName;
	private String[] constantEvaluations = {"predictor of","kappa","precision","recall","f-score","accuracy","hits"};

	/** Stores the type of the class value */
	private Feature.Type type = null;
	/** These show up as columns in the FeatureTablePanel */
	private Map<String, Map<Feature, Comparable>> evaluations;

	private String annot;
	private Integer threshold = 5;
	/** These variables are for weka. Filled when needed only. Stored 
	 * in the feature table so that it's cleaner to populate. */
	private FastVector fastVector = null;
	private double[] empty = null;	
	private Instances instances = null;
	private Map<String, Integer> attributeMap = new HashMap<String, Integer>();

	public void serialize(File f){
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));				
			writeSerializedTable(out);
			out.close();			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void writeSerializedTable(ObjectOutputStream out) throws Exception{
		out.writeObject(documents.getFilenames());
		out.writeObject(documents.getCurrentAnnotation());
		out.writeObject(documents.getTextColumn());
		out.writeObject(extractors);
		out.writeObject(hitsPerFeature);
		out.writeObject(activatedFeatures);
		out.writeObject(tableName);
		out.writeObject(type);
		out.writeObject((Integer)threshold);
		out.writeObject(annot);
	}

	public FeatureTable(ObjectInputStream in){
		try{
			documents = new SimpleDocumentList((Set<String>)in.readObject(), (String)in.readObject(), (String)in.readObject());
			extractors = (Collection<FeaturePlugin>)in.readObject();
			hitsPerFeature = (Map<Feature, Collection<FeatureHit>>)in.readObject();
			activatedFeatures = (Map<Feature, Boolean>)in.readObject();
			tableName = (String)in.readObject();
			type = (Feature.Type)in.readObject();
			threshold = (Integer)in.readObject();
			annot = (String)in.readObject();
			fillHitsPerDocument(this);
			this.evaluations = new TreeMap<String, Map<Feature, Comparable>>();
			defaultEvaluation();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void init(Collection<FeaturePlugin> extractors, SimpleDocumentList documents, int threshold){
		this.extractors = extractors;
		this.documents = documents;
		this.evaluations = new TreeMap<String, Map<Feature, Comparable>>();
		this.hitsPerFeature = new HashMap<Feature, Collection<FeatureHit>>(30000); //Rough guess at capacity requirement.
		this.activatedFeatures = new HashMap<Feature, Boolean>(30000);
		this.hitsPerDocument  = new ArrayList<Collection<FeatureHit>>();
		this.threshold = threshold;
		this.annot = documents.getCurrentAnnotation();
		extractAll();
	}

	public FeatureTable(FeaturePlugin extractor, SimpleDocumentList documents, int threshold)
	{
		Set<FeaturePlugin> extractors = new HashSet<FeaturePlugin>();
		extractors.add(extractor);
		init(extractors, documents, threshold);
	}

	public FeatureTable(SimpleDocumentList documents){
		Set<FeaturePlugin> extractors = new TreeSet<FeaturePlugin>();
		init(extractors, documents, 0);
	}

	public FeatureTable(Collection<FeatureHit> hits, SimpleDocumentList text){
		this.extractors = null;
		this.documents = text;
		this.evaluations = new TreeMap<String, Map<Feature, Comparable>>();
		this.hitsPerFeature = new HashMap<Feature, Collection<FeatureHit>>(30000); //Rough guess at capacity requirement.
		this.activatedFeatures = new HashMap<Feature, Boolean>(30000);
		this.hitsPerDocument  = new ArrayList<Collection<FeatureHit>>();
		for(int i = 0; i < documents.getSize(); i++){
			hitsPerDocument.add(new TreeSet<FeatureHit>());
		}
		for(FeatureHit hit : hits){
			if(!hitsPerFeature.containsKey(hit.getFeature())){
				hitsPerFeature.put(hit.getFeature(), new TreeSet<FeatureHit>());
				activatedFeatures.put(hit.getFeature(), Boolean.TRUE);
			}
			hitsPerFeature.get(hit.getFeature()).add(hit);
			hitsPerDocument.get(hit.getDocumentIndex()).add(hit);
		}
		this.threshold = threshold;
		this.annot = documents.getCurrentAnnotation();

	}

	private FeatureTable(){}

	public FeatureTable(Collection<FeaturePlugin> extractors, SimpleDocumentList documents, int threshold)
	{
		init(extractors, documents, threshold);
	}

	/**
	 * Builds a set of features for Weka's internal data structures.
	 * Doesn't convert the instances yet (use getInstances() for that).
	 */
	public void generateFastVector(){
		resetCurrentAnnotation();
		double time1 = System.currentTimeMillis();
		if(fastVector == null){
			FastVector attributes = new FastVector();
			int index = 0;
			Collection<Feature> featureSet = getSortedFeatures();
			empty = new double[featureSet.size()+1];
			for(Feature f : featureSet){
				Attribute att = null;
				FastVector fv = new FastVector();
				switch(f.getFeatureType()){
				case BOOLEAN:
					fv.addElement(Boolean.FALSE.toString());
					fv.addElement(Boolean.TRUE.toString());
					att = new Attribute(f.getFeatureName(), fv);
					break;
				case NOMINAL:
					for(String s : f.getNominalValues()) fv.addElement(s);
					att = new Attribute(f.getFeatureName(), fv);
					break;
				case NUMERIC:
					att = new Attribute(f.getFeatureName());
					break;
				case STRING:
					att = new Attribute(f.getFeatureName(), (FastVector)null);
					break;
				}
				if(att != null){
					attributes.addElement(att);		
					String id = f.getExtractorPrefix()+":"+f.getFeatureName();
					attributeMap.put(id, index++);
				}
			}
			switch(getClassValueType()){
			case NOMINAL:
				FastVector fv = new FastVector();
				for(String s : getDocumentList().getLabelArray()){ 
					fv.addElement(s);
				}
				attributes.addElement(new Attribute("CLASS", fv));
				break;
			case NUMERIC:
				attributes.addElement(new Attribute("CLASS"));
				break;
			}				
			fastVector = attributes;
		}
		double time2 = System.currentTimeMillis();
	}

	/**
	 * Generates the set of instances used for the final model (not for cross-validation)
	 * @return
	 */
	public Instances getInstances(){
		if(instances == null){
			if (fastVector == null) generateFastVector();
			Instances format = new Instances(getTableName(), fastVector, 0);
			double runningTotal = 0.0;
			Feature.Type t = getClassValueType();
			for(int i = 0; i < documents.getSize(); i++){
				double time1 = System.currentTimeMillis();
				format.add(fillInstance(format, t, i));
				double time2 = System.currentTimeMillis();
				runningTotal += (time2-time1);
			}
			format.setClass(format.attribute("CLASS"));
			instances = format;			
		}
		return instances;
	}
	static double getHitsTime = 0.0;
	static double setValueTime = 0.0;
	static double classValueTime = 0.0;
	static double cleanupTime = 0.0;


	/**
	 * Generates subsets of data from this feature table, used for cross validation. Makes a shallow copy of features
	 * from the overall Instances object.
	 * 
	 * @param foldMap Set of documents to use in this subset.
	 * @param fold Number of the fold to use for CV-by-fold radio button.
	 * @param train Whether this is the training or test set.
	 * @return
	 */
	public Instances getInstances(Map<Integer, Integer> foldMap, int fold, boolean train){
		resetCurrentAnnotation();
		if(instances == null){
			getInstances();
		}
		Instances format = new Instances(getTableName(), fastVector, 0);
		for(int i = 0; i < instances.numInstances(); i++){
			if((train && foldMap.get(i) != fold) || (!train && foldMap.get(i) == fold)){
				format.add((Instance)instances.instance(i).copy());
			}
			format.setClass(format.attribute("CLASS"));
		}
		return format;
	}

	/**
	 * Since we're doing cross-validation in a more intelligent way than SIDE originally did it (taking every nth instance
	 * for n folds, instead of taking the first 100/n% of the data for each fold), we need to keep a map of which keys in the 
	 * subset from getInstances() correspond to which instances in the whole data set. In this case, keys are the subset's 
	 * document index and values are the original document index.
	 * @param foldMap
	 * @param fold
	 * @return
	 */
	public Map<Integer, Integer> foldIndexToIndex(Map<Integer, Integer> foldMap, int fold){
		resetCurrentAnnotation();
		Map<Integer, Integer> foldIndexToIndex = new TreeMap<Integer, Integer>();
		int index = 0;
		for(int i = 0; i < getDocumentList().getSize(); i++){
			if(foldMap.get(i)==fold){
				foldIndexToIndex.put(index++, i);
			}
		}
		return foldIndexToIndex;
	}

	static double aTime = 0.0;
	static double bTime = 0.0;
	static double cTime = 0.0;
	static double dTime = 0.0;
	static double eTime = 0.0;

	public Integer getFastVectorIndex(Feature f){
		String id = f.getExtractorPrefix()+":"+f.getFeatureName();
		Integer att = attributeMap.get(id);
		return att;
	}
	/**
	 * Generates Instance objects (weka format) for a document in the corpus. Actually,
	 * these objects already exist, we're just filling the value.
	 * 
	 * @param format The Instances object to put this generated Instance in.
	 * @param i The document to fill. 
	 */
	private Instance fillInstance(Instances format, Feature.Type t, int i) {
		Collection<FeatureHit> hits = getHitsForDocument(i);
		double[] values = new double[format.numAttributes()];
		for(int j = 0; j < values.length; j++) values[j] = 0.0;
		try{
			for(FeatureHit hit : hits){
				Feature f = hit.getFeature();
				Integer att = getFastVectorIndex(f);
				Feature.Type type = f.getFeatureType();
				values[att] = getHitValueForFastVector(hit);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		if(getDocumentList().getAnnotationArray() != null){
			String[] possibleLabels = getDocumentList().getLabelArray();
			switch(t){
			case NOMINAL:
			case BOOLEAN:			
				for(int j = 0; j < possibleLabels.length; j++){
					if(possibleLabels[j].equals(getDocumentList().getAnnotationArray().get(i))){
						values[values.length-1] = j;
					}
				}
				break;
			case NUMERIC:
				values[values.length-1] = Double.parseDouble(getDocumentList().getAnnotationArray().get(i));
				break;
			}			
		}
		Instance inst = new SparseInstance(1,values);
		return inst;
	}

	public double getHitValueForFastVector(FeatureHit hit) {
		Type hitType = hit.getFeature().getFeatureType();
		switch(hitType){
		case NUMERIC:
			if(hit.getValue() instanceof Integer){
				return 0.0+(Integer)hit.getValue();
			}else{
				return (Double)hit.getValue();						
			}
		case STRING:
		case NOMINAL:
			int index = 0;
			for(String val : hit.getFeature().getNominalValues()){
				if(val.equals(hit.getValue())){
					return index;
				}
				index++;
			}
		case BOOLEAN:
			return 1;
		}
		return 0;
	}

	/**
	 * run the extractors on the documents and populate the feature hit tables.
	 */
	public void extractAll()
	{
		resetCurrentAnnotation();
		hitsPerDocument.clear();
		for(int i = 0; i < documents.getSize(); i++)
		{
			hitsPerDocument.add(new ArrayList<FeatureHit>());
		}
		Map<Feature, Collection<FeatureHit>> localMap = new HashMap<Feature, Collection<FeatureHit>>(100000);

		for(FeaturePlugin extractor : extractors)
		{
			if(extractor == null) continue;
			//Not really comfortable with FeatureTables knowing about FeaturePluginPanels. I should talk to David about this.
			Collection<FeatureHit> hits = extractor.extractFeatureHits(documents, FeaturePluginPanel.getProgressLabel());
			for(FeatureHit hit : hits)
			{
				if(! localMap.containsKey(hit.feature))
				{
					localMap.put(hit.feature, new ArrayList<FeatureHit>());
				}
				localMap.get(hit.feature).add(hit);
			}
		}

		Feature[] features = localMap.keySet().toArray(new Feature[0]);
		for(Feature f : localMap.keySet()){
			if (localMap.get(f).size() >= threshold){
				if(!hitsPerFeature.containsKey(f)){
					hitsPerFeature.put(f, new ArrayList<FeatureHit>());
				}
				if(!activatedFeatures.containsKey(f)){
					activatedFeatures.put(f, Boolean.TRUE);
				}
				for(FeatureHit hit : localMap.get(f	)){
					hitsPerDocument.get(hit.documentIndex).add(hit);
					hitsPerFeature.get(f).add(hit);
				}
			}
		}

		localMap.clear();
	}
	
	public void correval(){
		Map<Feature, Comparable> corr = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> predictorOf = new HashMap<Feature, Comparable>();
		
		Instances alldata = getInstances();
		FastVector nowattrs = new FastVector();
		nowattrs.addElement(new Attribute("f"));
		nowattrs.addElement(new Attribute("c"));
		
		for (Feature f: hitsPerFeature.keySet()){
			int i=getFastVectorIndex(f);
			Instances data = new Instances("getcorr", nowattrs, 0);
			double[] values = new double[2];
			for (int j=0; j<alldata.numInstances(); j++){
				values[0] = alldata.instance(j).value(i);
				values[1] = alldata.instance(j).classValue();
				data.add(new SparseInstance(1,values));
			}
			data.setClassIndex(1);
			try{
				LinearRegression LR = new LinearRegression();
				Evaluation eval = new Evaluation(data);
				LR.buildClassifier(data);
				eval.evaluateModel(LR, data);
				double nowcorr = eval.correlationCoefficient();
				predictorOf.put(f, nowcorr>0?"POS":"NEG");
				corr.put(f, nowcorr);
			} catch (Exception x){
				System.out.println(x);
			}
		}
		addEvaluation("sign", predictorOf);
		addEvaluation("correlation", corr);
	}

	/**
	 * Evaluates feature table for precision, recall, f-score, and kappa at creation time.
	 */
	public void defaultEvaluation(){
		
		Map<Feature, Comparable> precisionMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> recallMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> fScoreMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> accuracyMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> kappaMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> bestMap = new HashMap<Feature, Comparable>();
		Map<Feature, Comparable> hitsMap = new HashMap<Feature, Comparable>();
		Map<String, Map<Feature, Comparable>> hitsByLabelMap = new TreeMap<String, Map<Feature, Comparable>>();
		if(getClassValueType()==Type.NUMERIC){
			//correval();
		}else{
			double time1 = System.currentTimeMillis();
			resetCurrentAnnotation();

			ArrayList<String> trueAnnot = documents.getAnnotationArray();
			double timeA = 0.0;
			double timeA2 = 0.0;
			double timeB = 0.0;
			double timeC = 0.0;
			double timeD = 0.0;
			double lostTime = System.currentTimeMillis();
			Set<String> l = new TreeSet<String>();
			for(String s : trueAnnot) l.add(s);
			String[] labels = l.toArray(new String[0]);

			for(Feature f : hitsPerFeature.keySet()){
				if(evaluations.containsKey("hits") && evaluations.get("hits").containsKey(f)) continue;
				if(f.getFeatureType() == Type.NUMERIC) continue;
				
				double f1 = System.currentTimeMillis();
				Collection<FeatureHit> hits = hitsPerFeature.get(f);
				double maxPrec = Double.NEGATIVE_INFINITY;
				double maxRec = Double.NEGATIVE_INFINITY;
				double maxF = Double.NEGATIVE_INFINITY;
				double maxKappa = Double.NEGATIVE_INFINITY;
				double maxAcc = Double.NEGATIVE_INFINITY;
				String bestLabel = "[useless]";
				//			String[] labels = documents.getLabelArray();
				double f2 = System.currentTimeMillis();
				for(String label : labels){
					if(!hitsByLabelMap.containsKey(label)){
						hitsByLabelMap.put(label, new HashMap<Feature, Comparable>());						
					}
					double f3 = System.currentTimeMillis();
					double[][] kappaMatrix = new double[2][2];
					for(int i = 0; i < 2; i++){for(int j = 0; j < 2; j++){ kappaMatrix[i][j]=0;}}
					boolean[] hit = new boolean[documents.getSize()];
					int count = 0;
					for(FeatureHit fh : hits){
						if(checkHitMatch(f, fh.getValue())){
							hit[fh.getDocumentIndex()] = true;
							if(trueAnnot.get(fh.getDocumentIndex()).equals(label)){
								count++;
							}
						}
					}
					hitsByLabelMap.get(label).put(f, count);
					double f3a = System.currentTimeMillis();
					for(int i = 0; i < documents.getSize(); i++){
						kappaMatrix[trueAnnot.get(i).equals(label)?0:1][hit[i]?0:1]++;
					}
					double f4 = System.currentTimeMillis();
					double rightHits = kappaMatrix[0][0];
					double wrongHits = kappaMatrix[1][0];
					double all = documents.getSize();
					double featHits = kappaMatrix[0][0] + kappaMatrix[1][0];
					double actHits = kappaMatrix[0][0] + kappaMatrix[0][1];
					double accuracy = (kappaMatrix[0][0] + kappaMatrix[1][1])/all;
					double pChance = ((featHits/all)*(actHits/all))+(((all-featHits)/all)*((all-actHits)/all));

					double prec = rightHits/(rightHits+wrongHits);
					double rec = rightHits/actHits;
					double fmeasure = (2*prec*rec)/(prec+rec);
					double kappa = (accuracy - pChance)/(1 - pChance);

					if(Double.NaN == rec) rec = 0.0;
					if(Double.NaN == fmeasure) fmeasure = 0.0;
					if(kappa > maxKappa){
						maxPrec = prec;
						maxRec = rec;
						maxF = fmeasure;
						maxAcc = accuracy;
						maxKappa = kappa;
						bestLabel = label;
					}
					double f5 = System.currentTimeMillis();
					timeA2 += (f3a-f3);
					timeB += (f4-f3a);
					timeC += (f5-f4);
				}
				double f6 = System.currentTimeMillis();
				precisionMap.put(f, maxPrec);
				recallMap.put(f, maxRec);
				fScoreMap.put(f, maxF);
				accuracyMap.put(f, maxAcc);
				kappaMap.put(f, maxKappa);
				bestMap.put(f, bestLabel);
				hitsMap.put(f, hits.size());
				double f7 = System.currentTimeMillis();
				timeA += (f2-f1);
				timeD += (f7-f6);
			}
			addEvaluation("predictor of", bestMap);
			addEvaluation("kappa", kappaMap);
			addEvaluation("precision", precisionMap);
			addEvaluation("recall", recallMap);
			addEvaluation("f-score", fScoreMap);
			addEvaluation("accuracy", accuracyMap);
			addEvaluation("hits", hitsMap);
			if(labels != null){
				String[] hitLabels = new String[labels.length];
				for(int i = 0; i < hitLabels.length; i++){
					hitLabels[i] = "hits_" + labels[i];
					if(!hitsByLabelMap.containsKey(labels[i])){
						hitsByLabelMap.put(labels[i], new HashMap<Feature, Comparable>());
					}
					addEvaluation(hitLabels[i], hitsByLabelMap.get(labels[i]));						
				}
				if(constantEvaluations.length==7){
					String[] newConstants = new String[constantEvaluations.length+hitLabels.length];
					System.arraycopy(constantEvaluations, 0, newConstants, 0, constantEvaluations.length);
					System.arraycopy(hitLabels, 0, newConstants, constantEvaluations.length, hitLabels.length);
					constantEvaluations = newConstants;					
				}
			}
		}
		double time2 = System.currentTimeMillis();
	}

	/**
	 * Checks whether this feature "hit" a document, for the purpose of converting all these different
	 * feature types into a boolean check for basic evaluations.
	 */
	public boolean checkHitMatch(Feature f, Object value){
		switch(f.getFeatureType()){
		case BOOLEAN:
			return Boolean.TRUE.equals(value);
		case NOMINAL:
			return false;
		case NUMERIC:
			return ((Number)value).doubleValue()>0;
		case STRING:
			return value.toString().length()>0;
		}
		return false;
	}

	/**
	 * 
	 * @return the set of features extracted from the documents.
	 */
	public Set<Feature> getFeatureSet()
	{
		Set<Feature> set = hitsPerFeature.keySet();
		return set;
	}
	/**
	 * 
	 * @return the set of features extracted from the documents.
	 */
	public Collection<Feature> getSortedFeatures()
	{	
		return new TreeSet(hitsPerFeature.keySet());
	}


	public void addEmptyFeature(Feature f){
		hitsPerFeature.put(f, new HashSet<FeatureHit>());
	}

	/**
	 * Called by external classes (notably the FeaturePluginPanel and FeatureLabPanel) to edit 
	 * an existing feature table by adding new features. Generally followed by calling activationsChanged()
	 * on the FeatureTablePanel in the GUI.
	 * 
	 * @param hits
	 */
	public void addAllHits(Collection<FeatureHit> hits){
		Map<Feature, Collection<FeatureHit>> localMap = new HashMap<Feature, Collection<FeatureHit>>(100000);
		for(FeatureHit fh : hits){
			if(!localMap.containsKey(fh.getFeature())){
				localMap.put(fh.getFeature(), new ArrayList<FeatureHit>());
			}
			localMap.get(fh.getFeature()).add(fh);
		}
		for(Feature f : localMap.keySet()){
			if(localMap.get(f).size() >= threshold){
				for(FeatureHit fh : localMap.get(f)){
					if(!hitsPerDocument.get(fh.getDocumentIndex()).contains(fh)){
						hitsPerDocument.get(fh.getDocumentIndex()).add(fh);						
					}
					if(!hitsPerFeature.containsKey(fh.getFeature())){
						hitsPerFeature.put(fh.getFeature(), new HashSet<FeatureHit>());
						activatedFeatures.put(fh.getFeature(), true);
					}
					if(!hitsPerFeature.get(fh.getFeature()).contains(fh)){
						hitsPerFeature.get(fh.getFeature()).add(fh);			
					}
				}
			}
		}
		fastVector = null;
		instances = null;
		resetCurrentAnnotation();
		defaultEvaluation();
	}



	/**
	 * 
	 * @param feature
	 * @return all hits for the given feature.
	 */
	public Collection<FeatureHit> getHitsForFeature(Feature feature)
	{
		return hitsPerFeature.get(feature);
	}

	/**
	 * 
	 * @param index
	 * @return all hits on the given document index.
	 */
	public Collection<FeatureHit> getHitsForDocument(int index)
	{
		return hitsPerDocument.get(index);
	}

	public SimpleDocumentList getDocumentList()
	{
		resetCurrentAnnotation();
		return documents;
	}

	public Collection<FeaturePlugin> getExtractors()
	{
		return extractors;
	}

	public void setExtractors(Collection<FeaturePlugin> extractors)
	{
		this.extractors = extractors;
	}

	public String getTableName(){
		return tableName;
	}

	public void setTableName(String name){
		tableName = name;
	}

	public Map<String, Map<Feature, Comparable>> getEvaluations(){
		return evaluations;
	}

	public void addEvaluation(String evaluationName, Map<Feature, Comparable> eval){
		if (eval.keySet().size()==0) return;
		if (evaluations.containsKey(evaluationName)){
			for(Feature f : eval.keySet()){
				if(!evaluations.get(evaluationName).containsKey(f)){
					evaluations.get(evaluationName).put(f, eval.get(f));
				}
			}
		}else{
			evaluations.put(evaluationName, eval);			
		}
	}

	public String toString(){
		return getTableName();
	}

	/**
	 * Uses a sort of shoddy and roundabout catch-exception way of figuring out if the data type is nominal or numeric.
	 * @return
	 */
	public Feature.Type getClassValueType(){
		resetCurrentAnnotation();
		if(type == null){
			for(String s : documents.getLabelArray()){
				try{
					Double num = Double.parseDouble(s);
				}catch(Exception e){
					type = Feature.Type.NOMINAL;
					return type;
				}
			}
			type = Feature.Type.NUMERIC;
		}
		return type;
	}

	public void resetCurrentAnnotation() {
		documents.setCurrentAnnotation(annot);
	}


	/**
	 * Used for unannotated data when predicting new labels.
	 */
	public void setExternalClassValueType(Feature.Type type){
		this.type = type;
	}

	public String[] getConstantEvaluations(){
//		return evaluations.keySet().toArray(new String[0]);
//		return constantEvaluations;
		List<String> mergedEval = new ArrayList<String>();
		for (int i=0; i<constantEvaluations.length; i++)
			if (evaluations.containsKey(constantEvaluations[i]))
				mergedEval.add(constantEvaluations[i]);
/*		for (String evalkey : evaluations.keySet())
			if (!mergedEval.contains(evalkey)) mergedEval.add(evalkey);
*/			
		return mergedEval.toArray(new String[0]);	
		//return getClassValueType().equals(Feature.Type.NUMERIC)?new String[]{/*"sign","correlation"*/}:constantEvaluations;
	}

	public void setActivated(Feature f, boolean active){
		activatedFeatures.put(f, active);
	}

	public boolean getActivated(Feature f){
		if(activatedFeatures == null){
			return true;
		}else if(activatedFeatures.containsKey(f)){
			return activatedFeatures.get(f);			
		}else return false;
	}

	public Integer getThreshold(){
		return threshold;
	}

	/**
	 * Functionality for the "Freeze" button in the GUI. Removes deactivated features.
	 */
	public FeatureTable subsetClone(){
		resetCurrentAnnotation();
		FeatureTable ft = new FeatureTable();
		ft.setTableName(getTableName()+" (subset)");
		ft.extractors = extractors;
		ft.documents = documents;
		ft.evaluations = new TreeMap<String, Map<Feature, Comparable>>();
		for(String eval : evaluations.keySet()){
			ft.evaluations.put(eval, evaluations.get(eval));
		}
		ft.hitsPerFeature = new HashMap<Feature, Collection<FeatureHit>>(30000); //Rough guess at capacity requirement.
		ft.activatedFeatures = new HashMap<Feature, Boolean>();
		ft.threshold = threshold;
		fillHitsPerDocument(ft);
		return ft;
	}

	/**
	 * When creating feature hits, they're done on a per-feature basis. This fills the data structure
	 * that maps those hits per document instead.
	 */
	private void fillHitsPerDocument(FeatureTable ft) {
		resetCurrentAnnotation();
		ft.hitsPerDocument  = new ArrayList<Collection<FeatureHit>>();
		for(int i = 0; i < ft.documents.getSize(); i++)
		{
			ft.hitsPerDocument.add(new ArrayList<FeatureHit>());
		}
		for(Feature f : hitsPerFeature.keySet()){
			if(activatedFeatures.get(f)){
				ft.hitsPerFeature.put(f, hitsPerFeature.get(f));
				ft.activatedFeatures.put(f, Boolean.TRUE);
				for(FeatureHit fh : ft.hitsPerFeature.get(f)){
					ft.hitsPerDocument.get(fh.documentIndex).add(fh);
				}
			}
		}
	}

	/**
	 * Removes a feature and all of its hits from a feature table.
	 * @param f
	 */
	public void deleteFeature(Feature f){
		for(int i = 0; i < hitsPerDocument.size(); i++){
			FeatureHit[] docHits = hitsPerDocument.get(i).toArray(new FeatureHit[0]);
			for(FeatureHit hit : docHits){
				if(hit.getFeature().equals(f)){
					hitsPerDocument.get(i).remove(hit);
				}
			}
		}
		hitsPerFeature.remove(f);
		activatedFeatures.remove(f);
	}

	public void deleteFeatureSet(Set<Feature> f){
		for(int i = 0; i < hitsPerDocument.size(); i++){
			Collection<FeatureHit> tmphits = new ArrayList<FeatureHit>();
			for(FeatureHit hit : hitsPerDocument.get(i))
				if(!f.contains(hit.getFeature()))
					tmphits.add(hit);
			hitsPerDocument.set(i, tmphits);
		}
		for (Feature fe : f){
			hitsPerFeature.remove(fe);
			activatedFeatures.remove(fe);
		}
	}
	
	/**
	 * Given two feature tables, alter the feature space of the second table to match the feature
	 * space in the first table. Returns that second table post-alteration.
	 */
	public static FeatureTable reconcileFeatures(FeatureTable oldFeatureTable, FeatureTable newFeatureTable)
	{

		Set<Feature> oldTableFeatures = oldFeatureTable.getFeatureSet();
		Set<Feature> newTableFeatures = newFeatureTable.getFeatureSet();

		//weka does lots of things by index, instead of key... which is why the feature tables have to match exactly.
		int count = 0;
		if(oldTableFeatures.size() != newTableFeatures.size())
		{
			double time1 = System.currentTimeMillis();
			Set<Feature> remove = new HashSet<Feature>();
			for(Feature f: newTableFeatures)
			{
				boolean found = oldTableFeatures.contains(f);
				if(!found)
				{
					remove.add(f);
					count++;
				}
			}
			double time1a = System.currentTimeMillis();
			for(int i = 0; i < newFeatureTable.hitsPerDocument.size(); i++){
				FeatureHit[] docHits = newFeatureTable.hitsPerDocument.get(i).toArray(new FeatureHit[0]);
				for(FeatureHit hit : docHits){
					if(remove.contains(hit.getFeature())){
						newFeatureTable.hitsPerDocument.get(i).remove(hit);
					}
				}
			}
			for(Feature f : remove)
			{
				newFeatureTable.hitsPerFeature.remove(f);
				newFeatureTable.activatedFeatures.remove(f);
			}
			double time2 = System.currentTimeMillis();

			oldTableFeatures = oldFeatureTable.getFeatureSet();
			newTableFeatures = newFeatureTable.getFeatureSet();

			count = 0;
			for(Feature f : oldTableFeatures)
			{
				boolean found = newTableFeatures.contains(f);
				//boolean found = false;
				//				for(Feature newFeat : newTableFeatures)
				//				{
				//					if(newFeat.getExtractorPrefix().equals(f.getExtractorPrefix()) &&newFeat.getFeatureName().equals(f.getFeatureName()))
				//					{
				//						found = true;
				//						break;
				//					}
				//				}
				if(!found)
				{
					count++;
					newFeatureTable.addEmptyFeature(f);
				}	

			}
			double time3 = System.currentTimeMillis();
		}
		return newFeatureTable;
	}
}
