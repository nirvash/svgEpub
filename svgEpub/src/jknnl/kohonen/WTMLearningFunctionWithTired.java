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

import jknnl.learningFactorFunctional.LearningFactorFunctionalModel;
import jknnl.network.NetworkModel;
import jknnl.topology.NeighbourhoodFunctionModel;
import jknnl.metrics.MetricModel;
import jknnl.network.TiredNeuronModel;

/**
 * WTMLearningFunctionWithTired class - learnig class used to learn 
 * neuron with tiredness
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/07/11
 */

public class WTMLearningFunctionWithTired extends WTMLearningFunction{
    
    /**
     * Creates a new instance of WTMLearningFunction
     * @param networkModel reference to network Model
     * @param maxIteration max number of iteration
     * @param metrics reference to metrics
     * @param learningData reference to learning data
     * @param functionalModel reference to functional Model
     * @param neighboorhoodFunction reference to Neighboorhood Function
     */
    public WTMLearningFunctionWithTired(NetworkModel networkModel,int maxIteration,MetricModel metrics,
            LearningDataModel learningData,LearningFactorFunctionalModel functionalModel,
            NeighbourhoodFunctionModel neighboorhoodFunction) {
    super(networkModel,maxIteration,metrics,learningData,functionalModel,
            neighboorhoodFunction);
    }
    
    
     /**
     * Return number of best neuron for specified input vector. 
     * Tiredness for all neurons increas
     * @param vector input vector
     * @return NeuronModelnumber
     */
    protected int getBestNeuron(double[] vector){
        int bestNeuron = super.getBestNeuron(vector);
        TiredNeuronModel tempNeuron;
        int networkSize = networkModel.getNumbersOfNeurons();
        int tiredness;
        for(int i=0; i< networkSize; i++){
            tempNeuron = (TiredNeuronModel) networkModel.getNeuron(i);
            tiredness = tempNeuron.getTiredness();
            tempNeuron.setTiredness(++tiredness);
        }
        return bestNeuron;
    }
    
    
    /**
     * Change neuron weights for specified neuron number, iteration, input data vector and distance
     * and distance to winning neuron. All changed neurons'
     * tiredness is decrease.
     * @param distance distance to winning neuron
     * @param neuronNumber neuron number
     * @param vector input vector
     * @param iteration iteration number
     */
    protected void changeNeuronWeight(int neuronNumber, double[] vector, int iteration, int distance){
        super.changeNeuronWeight(neuronNumber,vector,iteration,distance);
        
        TiredNeuronModel tempNeuron = (TiredNeuronModel) networkModel.getNeuron(neuronNumber);
        int tiredness = tempNeuron.getTiredness();
        tempNeuron.setTiredness(tiredness - 2); 
    }
}
