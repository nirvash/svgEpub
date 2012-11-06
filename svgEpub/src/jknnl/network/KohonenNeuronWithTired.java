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

/**
 * Class representing <B> kohenen neuron with tiredness</B> 
 * with specyfied activation function
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/07/13
 */
public class KohonenNeuronWithTired extends KohonenNeuron implements TiredNeuronModel{

    /**
     * Tiredness default set to 10
     */
    int tiredness = 10;
    
    /**
     * Creates new kohonen neuron with specyfied numbers of weight. 
     * Value of the weight is random where max value is definited by maxWeight.
     * Activation function is definied by activationFunction parameter.
     * @param weightNumber numbers of weight
     * @param maxWeight max value of the weight
     * @param activationFunction activation function
     */
    public KohonenNeuronWithTired(int weightNumber, double[] maxWeight, ActivationFunctionModel activationFunction) {
        super(weightNumber,maxWeight,activationFunction);
    }
    
    
    /**
     * Creates new kohonen neuron with weight specyfied by array and specified activation funciton. 
     * Numbers of weights are the same as length of the array.
     * @param weightArray array of the weight
     * @param activationFunction activation function
     */
    public KohonenNeuronWithTired(double[] weightArray,ActivationFunctionModel activationFunction) {
        super(weightArray,activationFunction);
    }
    
    
    /**
     * Set tiredness
     * @param tiredness tiredness
     */
    public void setTiredness(int tiredness){
        this.tiredness = tiredness;
    }
    
    /**
     * Return value of tiredness
     * @return value of tiredness
     */
    public int getTiredness(){
        return this.tiredness;
    }
}
