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

package jknnl.metrics;

/**
 * Minkowski metric return distance calculated by function:
 * {sum[(x_i - y_i)^p]}^(1/p)
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class MinkowskiMetric implements MetricModel{
    
    /**
     * 
     *     Matric parameter. Default value is 1
     *     
     */
    private double p = 1;
    
    /**
     * Creates a new instance of MinkowskiMetrics.
     * Default value of the parameter <I>p</I> is <I>1</I>
     * @param p metrics parameter
     */
    public MinkowskiMetric(double p) {
        this.p = p;
    }
    
    /**
     * Set parameter <I>p</I>
     * @param paramateresList array of parameter
     */
    public void setParameteres(double[] paramateresList) {
        p = paramateresList[0];
    }

    /**
     * Return array of parameters containing <I>p</I>
     * @return array of parameter containing p
     */
    public double[] getParamateres() {
        double [] parameter = new double[1];
        parameter[0] = p;
        return parameter;
    }
    
    /**
     * Return distance beetwen input vectors. 
     * firstVector vector and secondVector must have the same size otherwise 
     * function return -1
     * d = {sum[(x_i - y_i)^p]}^(1/p)
     * @param firstVector first input vector
     * @param secondVector second input vector
     * @return distance beetwen vectors
     */
    public double getDistance(double[] firstVector, double[] secondVector) {
        double distance = 0;
        double x = 0, w = 0;
        double sum = 0;
        int weightLenght = firstVector.length;
        
        if(firstVector.length != secondVector.length)
            return -1;
        
        for(int i=0; i< weightLenght; i++){
            w = firstVector[i]; 
            x = secondVector[i];
            sum +=Math.pow(Math.abs(x - w),p);
        }
        distance = Math.pow(sum,1/p);
        return distance;
    }
}


