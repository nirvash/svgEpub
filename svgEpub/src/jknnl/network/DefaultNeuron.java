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

package jknnl.network;


import jknnl.activationFunction.ActivationFunctionModel;
import java.util.Random;

/**
 * Class representing default <B>neuron</B> with specyfied 
 * activation function
 *
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/07/09
 * @see ActivationFunctionModel
 */

public class DefaultNeuron implements NeuronModel {
    
    /**
     * array of the weight
     */
    private double[] weight;
    
    /**
    * activation function
    */
    private ActivationFunctionModel activationFunction;
    
   
    /**
     * Creates a new instance of DefaultNeuronWithBias with random weights value
     * @param weightNumber numbers of weigths
     * @param maxWeight maximal value of neuron weight
     * @param activationFunction reference to activation function
     */
    public DefaultNeuron(int weightNumber, double[] maxWeight, ActivationFunctionModel activationFunction) {
        if(weightNumber == maxWeight.length){
            Random rand = new Random();
            weight = new double[weightNumber];
            for(int i=0; i< weightNumber; i++){
                weight[i] = rand.nextDouble() * maxWeight[i];
            }
        }
        this.activationFunction = activationFunction;
    }
    
    /**
     * Creates a new instance of DefaultNeuronWithBias with specified weights
     * defined in array
     * @param weightArray array of weights value
     * @param activationFunction reference to activation function
     */
    public DefaultNeuron(double[] weightArray,ActivationFunctionModel activationFunction){
        int weightSize = weightArray.length;
        weight = new double[weightSize];
        for(int i=0; i< weightSize; i++){
            weight[i] = weightArray[i];
        }
        this.activationFunction = activationFunction;
    }

    /**
     * 
     * Returns array contains valu of the weights
     * @return array of the weights
     * 
     */
    public double[] getWeight() {
        return weight.clone();
    }

    /**
     * 
     * Set weigths from array as parameter
     * @param weight array of the weights
     * 
     */
    public void setWeight(double[] weight) {
        for (int i=0; i < weight.length; i++ ){
            this.weight[i] = weight[i]; 
        }
    }

    /**
     * 
     * Return value of the neuron after activation.
     * @param inputVector input vector for neuron
     * @return return
     * 
     */
    public double getValue(double[] inputVector) {
        double value = 0;
        int inputSize = inputVector.length;
        
        for(int i=0; i< inputSize; i++){
            value = value + inputVector[i] * weight[i];
        }
        
        if( activationFunction != null)
            return activationFunction.getValue(value);
        else
            return value;
    }

    /**
     * Return reference to activation function
     * @return reference to activation function
     */
    public ActivationFunctionModel getActivationFunction() {
        return activationFunction;
    }

    
    /**
     * Set activation function
     * @param activationFunction reference to activation function
     */
    public void setActivationFunction(ActivationFunctionModel activationFunction) {
        this.activationFunction = activationFunction;
    }
    
}
