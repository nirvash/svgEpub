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

package jknnl.network;

import java.io.FileWriter;
import java.io.PrintWriter;
import jknnl.topology.TopologyModel;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Default Network Model
 * 
 * @author Janusz Rybarski
 * e-mail: janusz.rybarski AT ae DOT krakow DOT pl
 * @author Seweryn Habdank-Wojewodzki
 * e-mail: habdank AT megapolis DOT pl
 * @version 1.0 2006/05/02
 */

public class DefaultNetwork implements NetworkModel{
    
    /**
     * Array of neurons
     */
    private NeuronModel[] neuronList;
    
    /**
     * Reference to topology
     */
    private TopologyModel topology;
    
    /**
     * Create network with specified topology, random weight from
     * definied interval and number of inputs
     * @param weightNumber number of weights (inputs)
     * @param maxWeight array with specified weight
     * @param topology Topology
     */
    public DefaultNetwork(int weightNumber, double[] maxWeight, TopologyModel topology) {
        this.topology = topology;
        int numberOfNeurons = topology.getNumbersOfNeurons();
        neuronList = new KohonenNeuron[numberOfNeurons];
        for (int i=0; i<numberOfNeurons; i++){
            neuronList[i] = new KohonenNeuron(weightNumber,maxWeight,null);
        }
    }
    
    /**
     * Create network with specified topology and parameters get from 
     * specified file
     * @param fileName File Name
     * @param topology Topology
     */
    public DefaultNetwork(String fileName, TopologyModel topology){
        File file = new File(fileName);
        int neuronNumber = topology.getNumbersOfNeurons();
        neuronList = new KohonenNeuron[neuronNumber];
        String[] tempTable;
        double[] tempList;
        int rows = 0;
        try{
            FileReader fr = new FileReader(file);
            BufferedReader input = new BufferedReader(fr);
            String line;
            System.out.println("Data from: \"" + fileName + "\" are importing...");
            while((line = input.readLine()) != null){
                tempTable = line.split("\t");
                int tableLenght = tempTable.length;
                tempList = new double[tableLenght];
                for(int i = 0; i< tableLenght; i++){
                    tempList[i] = Double.valueOf(tempTable[i]);
                }
                neuronList[rows] = new KohonenNeuron(tempList,null);
                rows ++;
             }
            fr.close();
            System.out.println(rows + " rows was imported");
        }catch(IOException e){
            System.out.println("File can not be read!. Error: " + e);
        }
        this.topology = topology;
    }
        
    /**
     * 
     * Return specified by number neuron
     * 
     * @param neuronNumber neuron number
     * @return Neuorn
     */
    public NeuronModel getNeuron(int neuronNumber) {
        return neuronList[neuronNumber];
    }
    
    /**
     * Return number of neuorns
     * @return nmber of neurons
     */
    public int getNumbersOfNeurons() {
        return neuronList.length;
    }
    
    /**
     * Get topology reference
     * @return Topology
     */
    public TopologyModel getTopology() {
        return topology;
    }
    
    /**
     * Set topology
     * @param topology Topology
     */
    public void setTopology(TopologyModel topology){
        this.topology = topology;
    }
    
    /**
     * Returns a string representation of the Coords object
     * @return Returns a string representation of the Coords object
     */
    public String toString(){
        String text = "";
        int networkSize = neuronList.length;
        for (int i=0; i< networkSize; i++ ){
            text +="Neuron number "+ (i + 1) + ": " +  neuronList[i];
            if(i < networkSize-1){
                text += "\n";
            }
        }
        return text;
    }
    
    /**
     * Save network into file
     * @param fileName File Name
     */
    public void networkToFile(String fileName){
        File outFile =  new File(fileName);
        String weightList;
        double[] weight;
        try{
        FileWriter fw = new FileWriter(outFile);
        PrintWriter pw = new PrintWriter(fw);
        int networkSize = neuronList.length;
        for (int i=0; i< networkSize; i++ ){
            weightList ="";
            weight = neuronList[i].getWeight();
            for (int j=0; j< weight.length; j++){
                weightList += weight[j];
                if (j < weight.length -1){
                    weightList += "\t";
                }
            }
            pw.println(weightList);
        }
        fw.close();
        }catch(IOException e){
            System.out.println("File can not be read!. Error: " + e);
        }
    }
}
