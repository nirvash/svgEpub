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
 * Hiperbolic Function describe by funtion <I>c1/(c2 + k) </I> <BR>
 * where: <BR>
 * <I>c1, c2 </I> - parameters
 * <I>k </I> - iteration number
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewodzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */
public class HiperbolicFunctionalFactor implements LearningFactorFunctionalModel{
    private double c1;
    
    private double c2;
    
    /**
     * Creates a new instance of HiperbolicFunctionalFactor with specified parameters <I>c1, c2</I>
     * @param c1 parameter
     * @param c2 parameter
     */
    public HiperbolicFunctionalFactor(double c1, double c2) {
        this.c1 = c1;
        this.c2 = c2;
    }
    
    /**
     * Get function parameters. First value at the array is <I>n0</I> second 
     * <I>maxIteration</I>
     * @return Array of parameters
     */
    public double[] getParameters(){
        double[] parameters = new double[2];
        parameters[0] = c1;
        parameters[1] = c2;
        return parameters;
    }
    
    /**
     * Set funciton parameters. First value at the array is <I>n0</I> second 
     * <I>maxIteration</I>
     * @param parameters array of parameters
     */    
    public void setParameters(double[] parameters){
        c1 = parameters[0];
        c2 = parameters[1];
    }
    
    /**
     * Return function value for specified iteratnion. Value is
     * independent of the iteration and is the same as parameters
     * @param k iternetion number
     * @return value of function factor
     */
    public double getValue(int k){
        return c1/(c2 + k);
    }
}
