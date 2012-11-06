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

package jknnl.topology;

/**
 * Gauss neighbourhood Function
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewodzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class GaussNeighbourhoodFunction implements NeighbourhoodFunctionModel{
    
    private double r;   // radius
    
    /**
     * Creates a new instance of GaussNeighbourhoodFunction with
     * specified radius
     * @param radius Radius
     */
    public GaussNeighbourhoodFunction(int radius) {
        this.r = radius;
    }
    
    /**
     * Return array containing parameter <I>r</I> - radius
     * @return constant parameter
     */
    public double[] getParameters(){
        double[] paremateres = new double[1];
        paremateres[0] = r;
        return paremateres;    
    }
    
    /**
     * Set parameters. <I>r</I> - radius
     * @param parameters constat value
     */
    public void setParameters(double[] parameters){
        r = parameters[0];
    }
    
   /**
     * Return value calculated by function describe as 
     * <I>exp(-(k^2))/(2*r^2))</I>
     * @return value of function factor
     * @param distance distance value
     */
    public double getValue(int distance){
       return java.lang.Math.exp(-(java.lang.Math.pow(distance,2))/ (2 * r * r ));
    }   
}
