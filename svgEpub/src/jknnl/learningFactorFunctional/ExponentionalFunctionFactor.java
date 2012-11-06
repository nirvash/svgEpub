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

package jknnl.learningFactorFunctional;

/**
 * Expotentional Function Factor. Return value calculated by function:
 * indpenend <I>n0 * exp(-c*k)</I>  <BR>where:<BR>
 * <I>n0, c </I> - constatn <BR>
 * <I>k</I> - iteration number <BR>
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewodzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */
public class ExponentionalFunctionFactor implements LearningFactorFunctionalModel{
   
    private double n0;
    
    
    private double c;
    
    
    /**
     * Creates a new instance of ExponentionalFunctionFactor
     * @param n0 constant parameter
     * @param c constant parameter
     */
    public ExponentionalFunctionFactor(double n0, double c) {
        this.n0 = n0;
        this.c = c;
    }
    
     /**
     * Return array containing parameters. First parameter is <I>n0</I>, second
     * <I>c</I>
     * @return parameters array
     */
    
    public double[] getParameters(){
        double[] parameters = new double[2];
        parameters[0] = n0;
        parameters[1] = c;
        return parameters;
    }
    
    /**
     * Set parameters
     * @param parameters parameters array
     */
    public void setParameters(double[] parameters){
        n0 = parameters[0];
        c = parameters[1];
    }
    
    /**
     * Return value calculated by function describe by <I>n0 * exp(-c*k)</I>
     * @param k iteratnion number
     * @return value of function factor
     */
    public double getValue(int k){
        return n0*java.lang.Math.exp(-c*k);
    }
}
