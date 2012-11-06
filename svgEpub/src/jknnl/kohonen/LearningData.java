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

package jknnl.kohonen;

import java.io.File;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.IOException;

/**
 * Obiect containing learning data. Data are stored as array 
 * with double values.
 * @author Janusz Rybarski
 */
public class LearningData implements LearningDataModel{
    
    /**
     * ArrayList contains learning data
     */
    ArrayList <double[]> dataList = new ArrayList<double[]>();
    
    /**
     * Creates a new instance of LearningData. Import data from file.
     * @param fileName path to the file with data
     */
    public LearningData(String fileName){
        File file = new File(fileName);
        String[] tempTable;
        double[] tempList;
        int rows = 0;
        try{
            FileReader fr = new FileReader(file);
            BufferedReader input = new BufferedReader(fr);
            String line;
            System.out.println("Data from: \"" + fileName + "\" are importing...");
            while((line = input.readLine()) != null){
                rows ++;
                tempTable = line.split("\t");
                int tableLenght = tempTable.length;
                tempList = new double[tableLenght];
                for(int i = 0; i< tableLenght; i++){
                    tempList[i] = Double.valueOf(tempTable[i]);
                }
                dataList.add(tempList);     
             }
            fr.close();
            System.out.println(rows + " rows was imported");
        }catch(IOException e){
            System.out.println("File can not be read!. Error: " + e);
        }
    }
    
    /**
     * Return ArrayList as vector of data
     * @param index index of data
     * @return data vector
     */
    public double[] getData(int index){
        return dataList.get(index);
    }
    
    /**
     * Return a string representation of the data. [<I>value, value, ... </I>]
     * @return string representation of the data
     */
    public String toString(){
        String text="";
        int dataSize = dataList.size();
        double [] vector;
        int vectorSize;
        for (int i=0; i<dataSize; i++){
            text += "[";
            vector = dataList.get(i);
            vectorSize = vector.length;
            for(int j=0; j<vectorSize; j++){
                text += vector[j];
                if( j < vectorSize-1){
                    text += ", ";
                }
            }
            text += "]" + "\n";
        }
        return text;
    }
   
    /**
     * Return numbers of data
     * @return numbers of data
     */
    public int getDataSize(){
        return dataList.size();
    }
    
    /**
     * Return length of the vector
     * @return Return length of the vector
     */
    public int getVectorSize(){
        return dataList.get(0).length;
    }
    
}
