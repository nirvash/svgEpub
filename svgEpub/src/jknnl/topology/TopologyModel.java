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

import java.util.TreeMap;
import java.util.ArrayList;

/**
 * Topology model interface
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewdzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public interface TopologyModel {
    
    /**
     * Return ArrayList of neurons connected to neuron with <I>neuron Number</I>
     * @param neuronNumber neuron number
     * @return list of connected neurons
     * @see ArrayList
     */
    public ArrayList getConnectedNeurons(int neuronNumber);
   
    /**
     * Return number of columns
     * @return number of columns
     */
    public int getColNumber();
    
    /**
     * Return TreeMap containing information about neuron and distance to neuron for which neighbourhood is 
     * calculated
     * @param neuronNumber neuron number
     * @return Tree map containn neuron number and distance
     * @see TreeMap
     */
    public TreeMap getNeighbourhood(int neuronNumber);
    
    /**
     * Return Coord object containing intormation about neuron co-ordinate
     * @param neuronNumber neuron number
     * @return coords object
     */
    public Coords getNeuronCoordinate(int neuronNumber);
    
    /**
     * Return number of neuron.
     * @return number of neurons
     */
    public int getNumbersOfNeurons();
    
    /**
     * Return neuron number of specyfied co-ordiante
     * @param coords neuron coordinate
     * @return neuron number
     */
    public int getNeuronNumber(Coords coords);
    
    /**
     * Return radius for calculate neighbourhood
     * @return radius
     */
    public int getRadius();
    
    /**
     * Return number of rows
     * @return numbers of rows
     */
    public int getRowNumber();
    
    /**
     * Set number of columns
     * @param colNumber numbers of columns
     */
    public void setColNumber(int colNumber);
    
    /**
     * Set radius
     * @param radius Radius
     */
    public void setRadius(int radius);
    
    /**
     * Set number of rows
     * @param rowNumber numbers of rows
     */
    public void setRowNumber(int rowNumber);
    
    /**
     * Return a string representation of the topology.
     * @return string representation of the topology.
     */
    public String toString();
}
