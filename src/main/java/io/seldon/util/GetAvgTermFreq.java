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
package io.seldon.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import pitt.search.semanticvectors.FlagConfig;
import pitt.search.semanticvectors.LuceneUtils;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

public class GetAvgTermFreq {
	@Argument(alias = "lucene-index", description = "Lucene index", required = true)
	private String luceneIndex;
	
	
	@Argument(alias = "results-file", description = "results file", required = true)
	private String resultsFile;
	
	public static class TermSum
	{
		int sum;
		int terms;
	}
	
	public void getDocAvgTermFreqs() throws IOException
	{
		BufferedWriter fileWriter = new BufferedWriter(new FileWriter(resultsFile));
		//Map<Integer,String> idMap = new HashMap<Integer,String>();
		FlagConfig config = FlagConfig.getFlagConfig(new String[] {"-luceneindexpath",luceneIndex});
		LuceneUtils lu = new LuceneUtils(config);
		TermsEnum termEnum = null;
		TermsEnum terms = lu.getTermsForField("contents").iterator(termEnum);
		BytesRef bytes;
		int tc = 0;
		Map<String,TermSum> docStats = new HashMap<String, TermSum>();
		while ((bytes = terms.next()) != null) 
		{
			if (( tc % 10000 == 0 ) || ( tc < 10000 && tc % 1000 == 0 )) {
		          System.out.println("Processed " + tc + " terms ... ");
		        }
			tc++;

			Term term = new Term("contents", bytes);
			//fileWriter.write(term.text()+"\n");
			//String token = term.text();
			int termfreq = lu.getGlobalTermFreq(term);
			DocsEnum docsEnum = lu.getDocsForTerm(term);
	        while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) 
	        {
	        	String docName = lu.getDoc(docsEnum.docID()).getField(config.docidfield()).stringValue();
	        	TermSum stats = docStats.get(docName);
	        	if (stats == null)
	        		stats = new TermSum();
	        	stats.sum += termfreq;
	        	stats.terms += 1;
	        	docStats.put(docName, stats);
	        }
        }
		
		System.out.println("Output results to file");
		for(Map.Entry<String, TermSum> e : docStats.entrySet())
		{
			float avg = e.getValue().sum/(float)e.getValue().terms;
			fileWriter.write(""+e.getKey()+","+avg+"\n");
		}
		fileWriter.close();
	}

	
	
	
	public static void main(String[] args) throws IOException {
		GetAvgTermFreq g = new GetAvgTermFreq();
		try
		{
			Args.parse(g, args);
			g.getDocAvgTermFreqs();
		}
		catch (IllegalArgumentException e) 
		{
			e.printStackTrace();
			Args.usage(g);
		}	

	}

}
