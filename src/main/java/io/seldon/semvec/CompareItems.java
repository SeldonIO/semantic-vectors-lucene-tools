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
package io.seldon.semvec;

import io.seldon.db.DocumentStore;
import io.seldon.db.SeldonMySQLDocumentStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import pitt.search.semanticvectors.FlagConfig;
import pitt.search.semanticvectors.SearchResult;
import pitt.search.semanticvectors.VectorSearcher;
import pitt.search.semanticvectors.VectorStoreRAM;
import pitt.search.semanticvectors.vectors.ZeroVectorException;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

public class CompareItems {

	@Argument(alias = "j", description = "Seldon JDBC", required = true)
	private String jdbc;
	
	@Argument(alias = "doc-vectors-file", description = "docvectors file", required = true)
	private String docVectorsFile;
	
	@Argument(alias = "input-file", description = "input file containing ids to compare", required = false)
	private String inputFile;
	
	@Argument(alias = "results-file", description = "results file", required = true)
	private String resultsFile;
	
	@Argument(alias = "it", description = "limit to items of this type", required = false)
	private Integer itemType = 1;
	
	@Argument(alias = "item-limit",description = "limit items from db, -1 no limit (default)", required = false)
	Integer itemLimit = -1;
	
	@Argument(alias = "max-results",description = "max results for each query", required = false)
	Integer maxResults = 10;
	
	@Argument(alias = "filter-attr-enum", description = "filter by this attr_id:value_id from table item_map_enum", required = false)
	String filterAttrEnumId;
	
	DocumentStore docStore = null;
	
	private void compareItem(Long id,VectorStoreRAM resultsVecReader,FlagConfig config,BufferedWriter fileWriter) throws IOException
	{
		try
		{
			VectorSearcher  vecSearcher = new VectorSearcher.VectorSearcherCosine( 
					resultsVecReader, resultsVecReader, null, config, new String[] {""+id}); 
			LinkedList<SearchResult> results = vecSearcher.getNearestNeighbors(maxResults);

			for (SearchResult result: results) 
			{
				if (!id.toString().equals(result.getObjectVector().getObject().toString()))
				{
					fileWriter.write(""+id);
					fileWriter.write(" ");
					fileWriter.write(""+result.getObjectVector().getObject().toString());
					fileWriter.write(" ");
					fileWriter.write(""+result.getScore());
					fileWriter.write("\n");
				}
			}
		} catch (ZeroVectorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void compare() throws IOException
	{
		docStore = new SeldonMySQLDocumentStore(jdbc);
		
		FlagConfig config = FlagConfig.getFlagConfig(new String[] {"-docvectorsfile",docVectorsFile});
		//CloseableVectorStore resultsVecReader = VectorStoreReader.openVectorStore(config.docvectorsfile(), config);
		VectorStoreRAM termRamReader = new VectorStoreRAM(config);
		termRamReader.initFromFile(docVectorsFile);
		BufferedWriter fileWriter = new BufferedWriter(new FileWriter(resultsFile));
		
		ArrayList<Long> ids = null;
		
		if (inputFile != null)
		{
			BufferedReader filereader = new BufferedReader(new FileReader(inputFile));
			ids = new ArrayList<Long>();
			String line = null;
			while((line = filereader.readLine()) != null)
			{
				ids.add(Long.parseLong(line));
			}
			filereader.close();
		}
		else
			ids = docStore.getLatestItems(itemType,null,itemLimit,null,false,filterAttrEnumId);
		
		if (ids != null)
		{
			System.out.println("Found " + ids.size() + " items");
			int count = 0;
			for(Long id : ids)
			{
				count++;
				if (count % 1 == 0)
					System.out.println("Processing " + (count) + "/" + ids.size());
				compareItem(id, termRamReader, config,fileWriter);
			}
		}
		
		fileWriter.close();
	}
	
	public static void main(String[] args) throws IOException {
		CompareItems ci = new CompareItems();
		try
		{
			Args.parse(ci, args);
			ci.compare();
		}
		catch (IllegalArgumentException e) 
		{
			e.printStackTrace();
			Args.usage(ci);
		}	

	}

}
