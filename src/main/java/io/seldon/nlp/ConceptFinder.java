/*
 * Seldon -- open source prediction engine
 * =======================================
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 **********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at       
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************************** 
*/
package io.seldon.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Joiner;

public class ConceptFinder {

	int maxConceptLen = 4;
	int minConceptLen = 2;
	Set<String> concepts;
	
	public ConceptFinder(String conceptFilename)
	{
		loadConcepts(conceptFilename);
	}
	
	
	private void loadConcepts(String path)
	{
		System.out.println("Loading concepts from "+path);
		concepts = new HashSet<String>();
        File dictFile = new File(path);
        BufferedReader input = null;
    	try {
    	  //use buffering
    	  //this implementation reads one line at a time
    	  input = new BufferedReader( new FileReader(dictFile) );
    	  String line = null; //not declared within while loop
    	  while (( line = input.readLine()) != null)
    	  {
    		concepts.add(line.toLowerCase().trim());
    	  }
    	}
    	catch (FileNotFoundException ex) {
    	  System.out.println("File not found " + dictFile.toString());
    	  return;
    	}
    	catch (IOException ex){
    		System.out.println("IO Exception on reading file " + dictFile.toString());
    		return;
    	}
    	finally
    	{
    		if (input != null)
    		{
				try 
				{
					input.close();
				} catch (IOException e) 
				{
		    		System.out.println("IO Exception on reading file " + dictFile.toString());
				}
    		}
    	}
	}
	
	public String[] find_concepts(String[] tokens)
	{
		StringBuffer res = new StringBuffer();
		for(int i=0;i<tokens.length;)
		{
			boolean found = false;
			for(int j=maxConceptLen;j>=minConceptLen & !found;j--)
			{
				if (i+j-1 < tokens.length)
				{
					String concept = Joiner.on(" ").join(Arrays.copyOfRange(tokens, i, i+j));
					if (concepts.contains(concept))
					{
						res.append(concept.replaceAll(" ", "_")+" ");
						i = i + j;
						found = true;
					}
				}
			}
			if (!found)
			{
				i++;
			}
		}
		return res.toString().trim().split(" ");
	}
	
	public static void main(String[] args)
	{
		ConceptFinder c = new ConceptFinder("/home/clive/work/seldon/tractionHub/wiki/concepts_uniq.txt");
		String s = "Open source recommendation engine, predictive analytics & machine learning Turbocharge engagement & conversion with content discovery & personalization";
		String[] res = c.find_concepts(s.split(" "));
		String r = Joiner.on(" ").join(res);
		System.out.println(r);
	}
}
