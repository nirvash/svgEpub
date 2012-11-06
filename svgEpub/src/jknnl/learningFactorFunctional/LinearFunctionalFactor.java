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
 * Linear function describe by: <I> y = (n0 / maxIter) * (maxIter - iter) </I>  where: <br>
 * <I>y</I> - output value <br>
 * <I>n0</I> - maximal factor <br>
 * <I>maxIter</I> - maximal number of iteration <br>
 * <I>iter</I> - iteration number
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class LinearFunctionalFactor implements LearningFactorFunctionalModel {
    private double n0;
    
    private double maxIteration;
    /**
     * Creates a new instance of LinearFunctionalFactor
     * @param n0 maximal factor
     * @param maxIteration maximal number of iteration
     */
    
    public LinearFunctionalFactor(double n0, double maxIteration) {
        this.n0 = n0;
        this.maxIteration = maxIteration;
    }
    
    /**
     * Get function parameters. First value at the array is <I>n0</I> second 
     * <I>maxIteration</I>
     * @return Array of parameters
     */
     public double[] getParameters(){
         double[] parameters= new double[2];
         parameters[0] = n0;
         parameters[1] = maxIteration;
        return parameters;    
    }
    
    /**
     * Set funciton parameters. First value at the array is <I>n0</I> second 
     * <I>maxIteration</I>
     * @param parameters array of parameters
     */
    public void setParameters(double[] parameters){
        n0 = parameters[0];
        maxIteration = parameters[1];
    }
    
    /**
     * Return funciton value for specified iteration
     * @param k iteration
     * @return funciton value for specified iteration
     */
    public double getValue(int k){
        return (n0/maxIteration)*(maxIteration-k); 
    }
}

