package io.seldon.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import opennlp.tools.util.Span;

public class AddEntities {

	NLPUtils nlp;
	Set<String> stopWords;
	boolean findNames = false;
	boolean findOrganizations = false;
	boolean findLocations = false;
	boolean useStopWords = false;
	boolean extractNouns = false;
	
	public AddEntities(String stopWords)
	{
		loadStopWords(stopWords);
	}
	
	public AddEntities(String sentModel,
			String tokenModel,
			String nameModel,
			String organizationModel,
			String locationModel,
			String stopWords,
			String posModel) throws FileNotFoundException {
		nlp = new NLPUtils();
		nlp.createSentenceDetector(sentModel);
		nlp.createTokenizer(tokenModel);
		if (nameModel != null)
		{
			nlp.createNameFinder(nameModel);
			findNames = true;
		}
		if (organizationModel != null)
		{
			nlp.createOrganizationFinder(organizationModel);
			this.findOrganizations = true;
		}
		if (locationModel != null)
		{
			nlp.createLocationFinder(locationModel);
			this.findLocations = true;
		}
		if (stopWords != null)
			loadStopWords(stopWords);
		else
			this.stopWords = null;
		
		if (posModel != null)
		{
			nlp.createTagger(posModel);
			this.extractNouns = true;
		}

	}
	
	private void loadStopWords(String path)
	{
		System.out.println("Loading stop words at "+path);
		stopWords = new HashSet<String>();
        File dictFile = new File(path);
        BufferedReader input = null;
    	try {
    	  //use buffering
    	  //this implementation reads one line at a time
    	  input = new BufferedReader( new FileReader(dictFile) );
    	  String line = null; //not declared within while loop
    	  while (( line = input.readLine()) != null)
    	  {
    		stopWords.add(line.toLowerCase().trim());
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
	
	public String removeStopWords(String text)
	{
		StringBuffer p = new StringBuffer();
		text = text.replaceAll("[\\?\\!,;\\.\\:]","").toLowerCase();
		text = text.replaceAll("\\&\\#\\d\\d\\d", ""); // remove html codes
		String[] parts = text.split("\\s+");
		for(int i=0;i<parts.length;i++)
			if (!stopWords.contains(parts[i]))
				p.append(parts[i]).append(" ");
		return p.toString();
	}
	
	public String process(String text)
	{
		if (nlp == null && stopWords != null)
		{
			System.out.println("INFO - removing stopwords... with simple tokenization");
			return removeStopWords(text);
		}
		else if (nlp != null)
		{
			System.out.println("Processing text using OpenNLP");
			StringBuffer p = new StringBuffer();
			String[] sentences = nlp.findSentences(text);
			for(String s : sentences)
			{
				String[] tokens = nlp.findTokens(s);
				
				if (stopWords != null)
				{
					for(int i=0;i<tokens.length;i++)
					{
						if (!stopWords.contains(tokens[i].toLowerCase()))
							p.append(" ").append(tokens[i]);
					}
				}
				
				if (this.extractNouns)
				{
					String[] pos = nlp.runTagger(tokens);
					for(int i=0;i<pos.length;i++)
					{
						if ("NN".equals(pos[i]))
							p.append(" ").append(tokens[i]);
					}
				}
				
				Span[] spans;
				String[] spanNames;
				
				if (this.findNames)
				{
					spans = nlp.findNames(tokens);
					spanNames = Span.spansToStrings(spans, tokens);
					for(String name : spanNames)
						p.append(" ").append(name.replaceAll("\\s", "_"));
				}
				
				if (this.findOrganizations)
				{
					spans = nlp.findOrganizations(tokens);
					spanNames = Span.spansToStrings(spans, tokens);
					for(String name : spanNames)
						p.append(" ").append(name.replaceAll("\\s", "_"));
				}
				
				if (this.findLocations)
				{
					spans = nlp.findLocations(tokens);
					spanNames = Span.spansToStrings(spans, tokens);
					for(String name : spanNames)
						p.append(" ").append(name.replaceAll("\\s", "_"));
				}
		}
		return p.toString().toLowerCase();
		}
		else
		{
			System.out.println("WARN - returning text with no modification");
			return text;
		}
	}

	public static void main(String[] args)
	{
		String r = "hell&#123&#127 there".replaceAll("\\&\\#\\d\\d\\d", "");
		System.out.println(r);
	}
	
}
