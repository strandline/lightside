package old;

import edu.cmu.side.model.feature.Feature;

public class SequenceFeature extends Feature{

	Feature a;
	Feature b;
	Integer turns;
	String direction;

	public SequenceFeature(){
		a = SequencingCriterionPanel.a;
		b = SequencingCriterionPanel.b;
		turns = SequencingCriterionPanel.turn;
		direction = SequencingCriterionPanel.direction;
		this.featureName =  "<\"" + a.getFeatureName() + "\" within " + turns + " turns " + direction + " \"" + b.getFeatureName() + "\">";
		this.extractorPrefix = "lab";
		this.featureType = Type.BOOLEAN;
	}

}
