/**
 * Copyright (c) 2006, Seweryn Habdank-Wojewodzki
 * Copyright (c) 2006, Janusz Rybarski
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms,
 * with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions
 * and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS
 * AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jknnl.kohonen;
import java.util.Iterator;
import jknnl.learningFactorFunctional.LearningFactorFunctionalModel;
import jknnl.network.NetworkModel;
import jknnl.network.NeuronModel;
import jknnl.topology.NeighbourhoodFunctionModel;
import jknnl.metrics.MetricModel;
import jknnl.topology.TopologyModel;
import java.util.TreeMap;

/**
 * <I>Winner Takes Most</I> - algorytm where winnig neuron and neurons in neighboorhood
 * weights are changed according to the formula w(k+1) = w(k) + n *N(i,x)* (x-w) where <br>
 * <I>w(k+1)</I> - neuron weight in <I>k +1</I> interation <br>
 * <I>w(k)</I> - neruon weight for <I>k</I> iteration <br>
 * <I>n</I> - value of learning function factor for <I> k </I> iteriation<br>
 * <i>N(i,x)</i> - value of neighboorhood function for <i>i </i> - specified neuron
 * <I>x</I> - learning vector od data
 * <I>w</I> - neuron weight
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 * @see WTMLearningFunction
 */
public class WTMLearningFunction {
    
     /**
     * reference to metrics
     */
    protected MetricModel metrics;
    /**
     * reference to network model
     */
    protected NetworkModel networkModel; 
     /**
     * max number of iteration
     */
    protected int maxIteration;
       /**
     * reference to learning data
     */
    protected LearningDataModel learningData;
     /**
     * reference to function model
     */
    protected LearningFactorFunctionalModel functionalModel;
    /**
     * reference to topology model
     */
    protected TopologyModel topology;
    /**
     * reference to neighboorhood function model
     */
    protected NeighbourhoodFunctionModel neighboorhoodFunction;
    /**
     * <I>True</I> if comments during learning must be shown,<I> false</I> otherwise.
     */
    private boolean showComments = false;
    
    /**
     * Creates a new instance of WTMLearningFunction
     * @param networkModel reference to network Model
     * @param maxIteration max number of iteration
     * @param metrics reference to metrics
     * @param learningData reference to learning data
     * @param functionalModel reference to functional Model
     * @param neighboorhoodFunction reference to Neighboorhood Function
     */
    public WTMLearningFunction(NetworkModel networkModel,int maxIteration,MetricModel metrics,
            LearningDataModel learningData,LearningFactorFunctionalModel functionalModel,
            NeighbourhoodFunctionModel neighboorhoodFunction) {
        this.maxIteration = maxIteration;
        this.networkModel = networkModel;
        this.metrics = metrics;
        this.learningData = learningData;
        this.functionalModel = functionalModel;
        this.topology = networkModel.getTopology();
        this.neighboorhoodFunction = neighboorhoodFunction;
    }

      /**
     * Return information if learning process dispalys information
     * about learning process.
     * @return true if learning process display information
     */
    public boolean isShowComments() {
        return showComments;
    }

      /**
     * Set if comments durring learning process must be shown.
     * @param showComments <B>true</B> if comments must be shown, <B>false</B> otherwise
     */
    public void setShowComments(boolean showComments) {
        this.showComments = showComments;
    }
    
    /**
     * Set reference to neighboorhood function
     * @param neighboorhoodFunction reference to neighboorhood function
     */
    public void setNeighboorhoodFunction(NeighbourhoodFunctionModel neighboorhoodFunction) {
        this.neighboorhoodFunction = neighboorhoodFunction;
    }

    /**
     * Return reference to neighboorhood function
     * @return reference to neighboorhood function
     */
    public NeighbourhoodFunctionModel getNeighboorhoodFunction() {
        return neighboorhoodFunction;
    }
    
     /**
     * Return metrics
     * @return metrics
     * @see MetricModel
     */
    public MetricModel getMetrics() {
        return metrics;
    }

    /**
     * Set metrics
     * @param metrics metrics
     */
    public void setMetrics(MetricModel metrics) {
        this.metrics = metrics;
    }

     /**
     * Set network model
     * @param networkModel network model
     */
    public void setNetworkModel(NetworkModel networkModel) {
        this.networkModel = networkModel;
    }

      /**
     * Return network model
     * @return network model
     */
    public NetworkModel getNetworkModel() {
        return networkModel;
    }

     /**
     * Set max interation
     * @param maxIteration max interation
     */
    public void setMaxIteration(int maxIteration) {
        this.maxIteration = maxIteration;
    }

    /**
     * Return maximal number of iteration
     * @return maximal number of iteration
     */
    public int getMaxIteration() {
        return maxIteration;
    }

      /**
     * Set reference to learning data
     * @param learningData reference to learning data
     */
    public void setLearningData(LearningDataModel learningData) {
        this.learningData = learningData;
    }

    /**
     * Return reference to learning data
     * @return reference to learning data
     */
    public LearningDataModel getLearningData() {
        return learningData;
    }

     /**
     * Set functional learning factor model
     * @param functionalModel functional learning factor model
     */
    public void setFunctionalModel(LearningFactorFunctionalModel functionalModel) {
        this.functionalModel = functionalModel;
    }

     /**
     * Return function model
     * @return function model
     */
    public LearningFactorFunctionalModel getFunctionalModel() {
        return functionalModel;
    }
    
    /**
     * Rerturn number of best neuron for specified input vector
     * 
     * @param vector input vector
     * @return NeuronModelnumber
     */
    protected int getBestNeuron(double[] vector){
        NeuronModel tempNeuron;
        double distance, bestDistance = -1;
        int networkSize = networkModel.getNumbersOfNeurons();
        int bestNeuron = 0;
        for(int i=0; i< networkSize; i++){
            tempNeuron = networkModel.getNeuron(i);
            if(tempNeuron != null){
                distance = metrics.getDistance(tempNeuron.getWeight(), vector);
                if((distance < bestDistance) || (bestDistance == -1)){
                    bestDistance = distance;
                    bestNeuron = i;
                }
            }
        }
        return bestNeuron;
    }
    
    /**
     * Change neuron weights for specified neuron number, iteration, input data vector and distance
     * and distance to winning neuron
     * @param distance distance to winning neuron
     * @param neuronNumber neuron number
     * @param vector input vector
     * @param iteration iteration number
     */
    protected void changeNeuronWeight(int neuronNumber, double[] vector,
        int iteration, int distance){
        double[] weightList = networkModel.getNeuron(neuronNumber - 1).getWeight();
        int weightNumber = weightList.length;
        double weight;
        if(showComments){
            String vectorText="[";
            for(int i=0; i<vector.length; i++){
                vectorText += vector[i];
                if(i < vector.length -1 ){
                    vectorText += ", ";
                }
            }
            vectorText += "]";
            System.out.println("Vector: " + vectorText);
            String weightText="[";
            for(int i=0; i<weightList.length; i++){
                weightText += weightList[i];
                 if(i < weightList.length -1 ){
                    weightText += ", ";
                }
            }
            weightText += "]";
            System.out.println("Neuron "+ (neuronNumber +1 ) + " weight before change: " + weightText);    
        }
        for (int i=0; i<weightNumber; i++){
            weight = weightList[i];
            weightList[i] += functionalModel.getValue(iteration) * neighboorhoodFunction.getValue(distance) * (vector[i] - weight);
        }
        networkModel.getNeuron(neuronNumber).setWeight(weightList);
        
        if(showComments){
            String weightText="[";
            for(int i=0; i<weightList.length; i++){
                weightText += weightList[i];
                if(i < weightList.length -1 ){
                    weightText += ", ";
                }
            }
            weightText += "]";
            System.out.println("Neuron "+ (neuronNumber +1 ) + " weight after change: " + weightText);
        }
    }
    
    /**
     * Change specified neuron weight
     * @param neuronNumber neuron Number
     * @param vector input vector
     * @param iteration iteration number
     */
    public void changeWeight(int neuronNumber,double[] vector, int iteration){
        TreeMap neighboorhood = topology.getNeighbourhood(neuronNumber);
        Iterator it = neighboorhood.keySet().iterator();
        int neuronNr;
        while(it.hasNext()){
            neuronNr = (Integer)it.next();
            changeNeuronWeight(neuronNr,vector,iteration,(Integer)neighboorhood.get(neuronNr));
        }
    }
    
     /**
     * Start learning process
     */
    public void learn(){
        int bestNeuron = 0;
        double[] vector;
        
        int dataSize = learningData.getDataSize();
        for (int i=0; i< maxIteration; i++){
            if(showComments){
                System.out.println("Iteration number: " + (i + 1));
            }
            for(int j= 0; j<dataSize; j++){
                vector = learningData.getData(j);
                bestNeuron = getBestNeuron(vector); 
                if(showComments){
                    System.out.println("Best neuron number: " +  (bestNeuron + 1));
                }
                changeWeight(bestNeuron, vector, i);
            }
        }
    }
}
