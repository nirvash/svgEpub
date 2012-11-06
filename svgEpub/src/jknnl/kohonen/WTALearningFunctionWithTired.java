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
import jknnl.metrics.MetricModel;
import jknnl.network.NetworkModel;
import jknnl.network.TiredNeuronModel;

/**
 * WTALearningFunctionWithTired class - learnig class used to learn 
 * neurons with tiredness
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/07/11
 */

public class WTALearningFunctionWithTired extends WTALearningFunction{
    
     /**
     * Creates a new instance of WTALearningFunction.
     * @param networkModel network model
     * @param maxIteration iteration number
     * @param metrics metrics
     * @param learningData learnig data
     * @param functionalModel functional model
     * @see MetricModel
     * @see LearningData
     * @see NetworkModel
     * @see LearningFactorFunctionalModel
     */
    public WTALearningFunctionWithTired(NetworkModel networkModel,int maxIteration,MetricModel metrics,
            LearningDataModel learningData,LearningFactorFunctionalModel functionalModel) {
        super(networkModel,maxIteration,metrics,learningData,functionalModel);
    }
    
    
     /**
     * Return number of the best neuron for specified input vector.
     * All neuron's tiredness is increasing
     * @param vector input vector
     * @return Neuron number
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
     * Change neuron weights for specified neuron number,iteration and input data vector.
     * Changed neruon's tiredness is decreasing.
     * @param neuronNumber neuron number
     * @param vector input vector
     * @param iteration iteration number
     */
    
    protected void changeNeuronWeight(int neuronNumber, double[] vector, int iteration){
        super.changeNeuronWeight(neuronNumber,vector,iteration);
        
        TiredNeuronModel tempNeuron = (TiredNeuronModel) networkModel.getNeuron(neuronNumber);
        int tiredness = tempNeuron.getTiredness();
        tempNeuron.setTiredness(tiredness - 2); 
    }
}
