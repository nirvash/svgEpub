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
 * Linear Activation Function return value calculated by function describe as <I>y = a * x + b</I>. where: <br>
 * <I>a, b </I> - parameters of linear function
 * <I> x</I> - input value
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class LinearActivationFunction implements ActivationFunctionModel{
    /**
     * 
     *     value of a
     *     
     */
    private double a = 1;
    
    /**
     * 
     *     value of b
     *     
     */
    private double b = 0;
    
    /**
     * Creates a new instance of LinearActivationFunction with default parameters set <I> a=1, b=0</I>
     */
    public LinearActivationFunction() {
    }
    
    /**
     * Set the parameters of function. First parameter is <I>a</I>,
     * second is <I>b</I>.
     * @param paramateresList array of parameters
     */
    public void setParameteres(double[] paramateresList){
        a = paramateresList[0];
        b = paramateresList[1];
    }
    
    /**
     * Return parameters of the Linear Activation function.
     * First value is <I>a</I>, second is <I>b</I>.
     * @return double array of the parameters
     */
    public double[] getParamateres(){
        double [] parameter = new double[2];
        parameter[0] = a;
        parameter[1] = b;
        return parameter;
    }

    /**
     * Return value <I>y</I> of Linear Activation function for specified input value <I>x</I>.
     * <I>y = a * x + b</I>
     * @param inputValue input value
     * @return value of linear function
     */
    public double getValue(double inputValue) {
        return a * inputValue + b;
    }
}
