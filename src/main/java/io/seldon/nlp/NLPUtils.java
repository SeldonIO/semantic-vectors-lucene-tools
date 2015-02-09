package io.seldon.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class NLPUtils {

	SentenceDetectorME sentenceDetector;
	Tokenizer tokenizer;
	NameFinderME nameFinder;
	NameFinderME organizationFinder;
	NameFinderME locationFinder;
	POSTaggerME tagger;
	
	public NLPUtils()
	{
		
	}
	
	public NLPUtils(String sentenceModelPath,String tokenModelPath,String nameModelPath) throws FileNotFoundException
	{
		this.createSentenceDetector(sentenceModelPath);
		this.createTokenizer(tokenModelPath);
		this.createNameFinder(nameModelPath);
	}
	
	public void createTagger(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  POSModel model = new POSModel(modelIn);
		  tagger = new POSTaggerME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	public void createTokenizer(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  TokenizerModel model = new TokenizerModel(modelIn);
		  tokenizer = new TokenizerME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
				
	}
	
	public void createSentenceDetector(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  SentenceModel model = new SentenceModel(modelIn);
		  sentenceDetector = new SentenceDetectorME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	public void createNameFinder(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
		  nameFinder = new NameFinderME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	public void createOrganizationFinder(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
		  organizationFinder = new NameFinderME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	public void createLocationFinder(String path) throws FileNotFoundException
	{
		InputStream modelIn = new FileInputStream(path);

		try {
		  TokenNameFinderModel model = new TokenNameFinderModel(modelIn);
		  locationFinder = new NameFinderME(model);
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
		finally {
		  if (modelIn != null) {
		    try {
		      modelIn.close();
		    }
		    catch (IOException e) {
		    }
		  }
		}
	}
	
	public String[] findSentences(String text)
	{
		return sentenceDetector.sentDetect(text);
	}
	
	public String[] findTokens(String sentence)
	{
		return tokenizer.tokenize(sentence);
	}
	
	public Span[] findNames(String[] tokens)
	{
		return nameFinder.find(tokens);
	}
	
	public Span[] findOrganizations(String[] tokens)
	{
		return organizationFinder.find(tokens);
	}
	
	public Span[] findLocations(String[] tokens)
	{
		return locationFinder.find(tokens);
	}
	
	public String[] runTagger(String[] tokens)
	{
		return tagger.tag(tokens);
	}
	
}
