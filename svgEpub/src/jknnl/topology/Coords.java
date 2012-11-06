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
 * Class used to get or set neuron coordinate in topology.
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class Coords {
    
    /**
     * x coordinate
     */
    public int x;
    /**
     * y coordinate
     */
    public int y;
    
    /**
     * Creates a new instance of Coords with default coordinates (0,0).
     */
    public Coords() {
        this(0,0);
    }
    
    /**
     * Creates a new instance of Coords with specified <I>x</I> and <I>y</I> coordinate.
     * @param x value of x
     * @param y value of y
     */
    public Coords(int x, int y){
        this.x = x;
        this.y = y;
    }
    
    /**
     * Return <I>x </I>coordinate
     * @return coordinate x
     */
    public int getX(){
        return x;
    }
    
    /**
     * Return<I> y</I> coordinate
     * @return coordinate y
     */
    public int getY(){
        return y;
    }
    
    /**
     * Indicates whether some other object is "equal to" Coords class..
     * @param obj obj - the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    public boolean equals(Object obj){
        if(obj instanceof Coords){
            Coords coords = (Coords) obj; 
            return (x == coords.x) && (y == coords.y);
        }
        return false;
    }
    
    /**
     * Returns a string representation of the Coords object 
     * <I> [ x = ,y = ]</I>
     * @return Returns a string representation of the Coords object
     */
    public String toString(){
        return "[ x= " + x + ",y= " + y + " ]";
    }
}


