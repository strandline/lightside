#!/bin/bash
java -Xmx5g -classpath bin:lib/jfreechart-1.0.11.jar:lib/lingpipe-2.3.0.jar:lib/riverlayout.jar:lib/stanford-postagger-2010-05-26.jar:lib/trove.jar:lib/quiet-weka.jar:lib/XMLBoss.jar:lib/xmlparserv2.jar:lib/yeritools.jar:lib/java-cup.jar:lib/JFlex.jar:lib/junit.jar:lib/packageManager.jar:lib/quiet-weka.jar:lib/commons-math3-3.1.jar:lib/simple-5.0.4.jar:wekafiles/packages/chiSquaredAttributeEval/chiSquaredAttributeEval.jar:wekafiles/packages/bayesianLogisticRegression/bayesianLogisticRegression.jar:wekafiles/packages/LibLINEAR/lib/liblinear-1.8.jar:wekafiles/packages/LibLINEAR/LibLINEAR.jar:wekafiles/packages/LibSVM/lib/libsvm.jar:wekafiles/packages/LibSVM/LibSVM.jar edu.cmu.side.recipe.ModelTrainingChef $1

