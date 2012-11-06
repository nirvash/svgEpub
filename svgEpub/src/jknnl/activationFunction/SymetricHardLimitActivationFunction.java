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

package jknnl.activationFunction;

/**
 * 
 * Symetric Hard Limit function return 1 if input value is greater then
 * threshold and -1 otherwise.
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 * 
 */

public class SymetricHardLimitActivationFunction implements ActivationFunctionModel{
    
    /**
     * threshold of hard limit activation function
     */
    private double p = 0;
    
    /**
     * Creates a new instance of SymetricHardLimitActivationFunction with specified paremeter <I>p</I>
     * @param p threhsold
     */
    public SymetricHardLimitActivationFunction(double p) {
        this.p = p;
    }
    
    /**
     * Set parameters of Hard limit activation funciton
     * @param paramateresList Array of parameters
     */
    public void setParameteres(double[] paramateresList){
        p = paramateresList[0];
    }
    
    /**
     * Return array contains parameters list
     * @return Array of parameters
     */
    public double[] getParamateres(){
        double [] parameter = new double[1];
        parameter[0] = p;
        return parameter;
    }

    
    /**
     * Return<I> 1</I> if input value is greater then <I>p</I> and <I>-1</I> otherwise.
     * @param inputValue input Value
     * @return value of the function
     */
    public double getValue(double inputValue) {
        double value;
        if (inputValue > p)
            return 1;
        else
            return -1;
    }
}
