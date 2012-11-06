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
 * Euclides metric return distance calculated by function:
 * sum[sqrt(x_i - y_i)] for each element from inputs vectors, where x_i is first input vector element,
 * y_i is second vector element.
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class EuclidesMetric implements MetricModel{
    
    /** Creates a new instance of CityBlockMetric */
    public EuclidesMetric(){
    }
    
    /**
     * Return value containing the distance information. 
     * firstVector vector and secondVector must have the same size otherwise 
     * function return -1
     * @param firstVector first vector
     * @param secondVector second vector
     * @return distance information
     */
    public double getDistance(double[] firstVector, double[] secondVector) {
        double distance = 0;
        double x = 0, w = 0;
        double sum = 0;
        int weightSize = firstVector.length;
        
        if(weightSize != secondVector.length)
            return -1;
        
        for(int i=0; i< weightSize; i++){
            w = firstVector[i]; 
            x = secondVector[i];
            sum += (x - w) *( x - w);
        }
        
        distance = Math.sqrt(sum);
        return distance;
    }
}
