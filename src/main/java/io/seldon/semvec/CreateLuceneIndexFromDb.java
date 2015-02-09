package io.seldon.semvec;


import io.seldon.db.DocumentStore;
import io.seldon.db.SeldonMySQLDocumentStore;
import io.seldon.nlp.AddEntities;
import io.seldon.nlp.TransliteratorPeer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import net.htmlparser.jericho.Source;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
/**
 * Create a lucene index in a form usable by Semantic Vectors.
 * At present utilizes the Seldon DB structure for JDBC databases as only option.
 * @author clive
 *
 */
public class CreateLuceneIndexFromDb {

	@Argument(alias = "l", description = "Lucene index directory", required = true)
	private String luceneDir;

	@Argument(alias = "j", description = "Seldon JDBC", required = true)
	private String jdbc;

	@Argument(alias = "it", description = "limit to items of this type", required = false)
	private Integer itemType = 1;
	
	@Argument(alias = "use-item-attrs", description = "use item attributes", required = false)
	private Boolean useItemAttrs;
	
	@Argument(alias = "use-comments", description = "use comments", required = false)
	private Boolean useComments;

	@Argument(alias = "use-users", description = "use users", required = false)
	private Boolean useUsers;

	@Argument(alias = "use-user-actions", description = "use users actions", required = false)
	private Boolean useUserActions;

	@Argument(alias = "use-dim", description = "use dimension", required = false)
	private Boolean useDim;
	
	@Argument(alias = "raw-ids", description = "whether to use the raw ids from db as the ids to put into lucene", required = false)
	Boolean rawIds;
	
	@Argument(alias = "attr-names", description = "the attribute names from the db", required = false)
	String[] attrNames;

	@Argument(description = "whether to recreate the lucene index", required = false)
	Boolean recreate = false;

	@Argument(description = "print extra debug", required = false)
	boolean debug = false;

	@Argument(alias = "item-limit",description = "limit items from db, -1 no limit (default)", required = false)
	Integer itemLimit = -1;

	@Argument(alias = "delta-mins",description = "delta mins in past to get items", required = false)
	Integer deltaMins = 0;
	
	@Argument(alias = "delta-days",description = "delta days in past to get items", required = false)
	Integer deltaDays = 0;

	@Argument(alias = "use-item-map-datetime", description = "use date field in item_map_datetime db table", required = false)
	boolean useItemMapDatetime = false;

	@Argument(alias = "append-only", description = "append new items to the lucene index and don't add existing entries", required = false)
	boolean appendOnly = false;

	@Argument(alias = "use-item-ids", description = "use item ids or names", required = false)
	boolean useItemIds = false;

	@Argument(alias = "remove-html", description = "remove html from text", required = false)
	boolean removeHtml = false;

	@Argument(alias = "positional-index", description = "create a positional index", required = false)
	boolean positionalIndex = false;

	@Argument(alias = "text-attr-ids", description = "attribute ids for data in item_map_text (deprecated)", required = false)
	Integer[] textAttrIds;

	@Argument(alias = "attr-ids", description = "attribute ids for data in item_map_varchar (deprecated)", required = false)
	Integer[] attrIds;
	
	@Argument(alias = "nlp-attr-ids", description = "attribute ids for data in item_map_varchar (deprecated)", required = false)
	Integer[] nlpAttrIds;
	
	@Argument(alias = "extract-persons", description = "exteract people entities from text", required = false)
	boolean extractPersons = false;
	
	@Argument(alias = "extract-organisations", description = "extract organisations from text", required = false)
	boolean extractOrganisations = false;
	
	@Argument(alias = "extract-places", description = "extract places from text", required = false)
	boolean extractPlaces = false;
	
	@Argument(alias = "extract-nouns", description = "extract nouns from text", required = false)
	boolean extractNouns = false;
	
	@Argument(alias = "use-stop-words", description = "use stop words", required = false)
	boolean useStopwords = false;
	
	@Argument(alias = "stop-words-file", description = "stop words file", required = false)
	String stopWordsFile = null;
	
	@Argument(alias = "open-nlp-location", description = "location of open nlp files", required = false)
	String nlpLocation = null;
	
	@Argument(description = "transliterate text to remove accents and punctuation", required = false)
	boolean transLiterate = false;
	
	@Argument(alias = "sequential-ids", description = "ensure sequential ids in lucene", required = false)
	boolean sequentialIds = false;
	
	@Argument(alias = "store-term-vectors", description = "store term vectors", required = false)
	boolean storeTermVectors = false;
	
	@Argument(alias = "yahoo-lda-file", description = "output for yahoo LDA", required = false)
	String yahooLDAfile = null;
	
	@Argument(alias = "client-item-pattern", description = "item pattern to limit items", required = false)
	String clientItemPattern = null;
	
	private EXTRACTION_TYPE extractionMethod;
	public static  enum EXTRACTION_TYPE {COMMENTS, ITEM_ATTR, USERS, USER_DIM, USER_ACTIONS; };
	static final String FIELD_PATH = "path";
	DocumentStore docStore = null;
	AddEntities addEntities = null;
	
	long seqId = 0;
	
	public void createIndex() throws CorruptIndexException, LockObtainFailedException, IOException
	{
		docStore = new SeldonMySQLDocumentStore(jdbc);
		File luceneFile = new File(luceneDir);
		//IndexWriter writer = new IndexWriter(FSDirectory.open(luceneFile), new StandardAnalyzer(Version.LUCENE_CURRENT), recreate, IndexWriter.MaxFieldLength.LIMITED);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_CURRENT, new WhitespaceAnalyzer(Version.LUCENE_CURRENT));

		IndexWriter writer = new IndexWriter(FSDirectory.open(luceneFile), config);
		IndexSearcher reader = null;
		DirectoryReader ireader = null;
		if (!recreate)
		{
			ireader = DirectoryReader.open(FSDirectory.open(luceneFile));
			reader = new IndexSearcher(ireader);
		}
	    
		BufferedWriter yahooWriter = null;
		if (yahooLDAfile != null)
			yahooWriter = new BufferedWriter(new FileWriter(yahooLDAfile));
		updateComments(reader,writer,itemType,recreate,yahooWriter);
		
		
		if (yahooWriter != null)
			yahooWriter.close();
		if (ireader != null)
			ireader.close();
		writer.close();
	}
	
	private Document createDoc(String docPath,String val)
	{
		Document doc = new Document();
		//FieldType ft = new FieldType(StringField.TYPE_STORED);
		//ft.setOmitNorms(false);
		//new Field("field", "value", ft);
	    doc.add(new Field(FIELD_PATH, docPath, Field.Store.YES, Field.Index.NOT_ANALYZED));
	    if (positionalIndex)
	    	doc.add(new Field("contents", val,Field.Store.NO,Field.Index.ANALYZED,Field.TermVector.WITH_POSITIONS));
	    else
	    	doc.add(new Field("contents", val,Field.Store.NO,Field.Index.ANALYZED,storeTermVectors ? Field.TermVector.YES : Field.TermVector.NO));
	    return doc;
	}
	
	private String toSV(Long id) {
		String idStr = "" + id;
		if (idStr.length() < 5)
			return "docs/0000/" + id;
		else
		{
			String[] struc = new String[2];
			struc[0] = idStr.substring(0, 4);
			struc[1] = idStr.substring(4);
			String docName = "docs/" + struc[0] + "/" +struc[1];
			return docName;
		}
	}
	
	private void saveDocument(long id,IndexSearcher reader,IndexWriter writer,BufferedWriter yahooWriter) throws CorruptIndexException, IOException
	{
		String path;
		if (rawIds)
			path = ""+id;
		else
			path = sequentialIds ? toSV(this.seqId++) : toSV(id);
		if (reader != null && appendOnly)
		{
			Term docPathTerm = new Term(FIELD_PATH,path);
			TermQuery tq = new TermQuery(docPathTerm);
			int hits = reader.search(tq, 1).totalHits;
			if (hits > 0)
			{
				if (debug)
					System.out.println("Skipping existing doc with id "+id);
				return; // document exists so don't do anything
			}
		}
		String comments = null;
		String nlpComments = null;
		switch(this.extractionMethod)
		{
		case COMMENTS:
			comments = docStore.getComments(id);
			break;
		case ITEM_ATTR:
			if (attrIds != null && attrIds.length>0)
				comments = docStore.getItemTextualById(id, new HashSet<Integer>(Arrays.asList(attrIds)));
			else if (attrNames != null && attrNames.length > 0)
				comments = docStore.getItemTextualByName(id, new HashSet<String>(Arrays.asList(attrNames)));
			else
				comments = docStore.getItemTextual(id);
			if (nlpAttrIds != null && nlpAttrIds.length>0)
				nlpComments = docStore.getItemTextualById(id, new HashSet<Integer>(Arrays.asList(nlpAttrIds)));
			break;
		case USERS:
			comments = docStore.getUserItems(id,useItemIds);
			break;
		case USER_ACTIONS:
			comments = docStore.getUserActionAttrs(id, new HashSet<Integer>(Arrays.asList(attrIds)));
			break;
		case USER_DIM:
			comments = docStore.getDimTextual(id, new HashSet<Integer>(Arrays.asList(textAttrIds)),itemLimit);
			break;
		}
		
		if (comments != null)
		{
			if (this.removeHtml)
			{
				System.out.println("removing html");
				Source source=new Source(comments);
				comments = source.getTextExtractor().toString();
				
				if (nlpComments != null)
				{
					source=new Source(comments);
					nlpComments = source.getTextExtractor().toString();
				}
			}
			
			if (addEntities != null && nlpComments == null)
				comments = addEntities.process(comments);
			else if (addEntities != null && nlpComments != null)
			{
				nlpComments = addEntities.process(nlpComments);
				//System.out.println("NLP Comments:["+nlpComments+"]");
				//System.out.println("Existing comments:"+comments);
				comments = comments + " " + nlpComments;
			}
			else
			{
				comments = comments.replaceAll("\\,|\\.|\\!|\\;|\\/", " ");
				if (transLiterate)
				{
					comments = TransliteratorPeer.getPunctuationTransLiterator().transliterate(comments);
				}
			}
			comments = comments.replaceAll("\\|", "");
			comments = comments.trim();
			if (!"".equals(comments))
			{
				if (debug)
					System.out.println("adding document for id "+id+" with text:["+comments+"]");
				if (reader != null)
				{
					Term docPathTerm = new Term(FIELD_PATH,path);
					TermQuery tq = new TermQuery(docPathTerm);
					int hits = reader.search(tq, 1).totalHits;
					if (hits > 0) // doc exists in index (assumes a unique match...)
						writer.updateDocument(docPathTerm, createDoc(path,comments));
					else
						writer.addDocument(createDoc(path,comments));
				}
				else
					writer.addDocument(createDoc(path,comments));
			
				if (yahooWriter != null)
				{
					yahooWriter.write(""+id);
					yahooWriter.write(" ");
					yahooWriter.write(path);
					yahooWriter.write(" ");
					yahooWriter.write(comments);
					yahooWriter.write("\n");
				}
			}
		}
	}
	
	private void updateComments(IndexSearcher reader,IndexWriter writer,int itemType,boolean recreate,BufferedWriter yahooWriter) throws CorruptIndexException, IOException
	{
		Calendar cal = Calendar.getInstance();
		if (deltaMins > 0)
			cal.add(Calendar.MINUTE, deltaMins*-1);
		ArrayList<Long> ids = null;
		switch(this.extractionMethod)
		{
		case COMMENTS:
			ids = docStore.getLatestComments(itemType,deltaMins > 0 ? cal.getTime() : null);
			break;
		case ITEM_ATTR:
			ids = docStore.getLatestItems(itemType,deltaMins > 0 ?  cal.getTime() : null,itemLimit,clientItemPattern,useItemMapDatetime);
			break;
		case USERS:
		case USER_ACTIONS:
			ids = docStore.getLatestUsers(deltaMins > 0 ? cal.getTime() : null);
			break;
		case USER_DIM:
			ids = docStore.getUserDim(new HashSet<Integer>(Arrays.asList(attrIds)));
			break;
		}
		
		if (ids != null)
		{
			System.out.println("Found " + ids.size() + " new items with comments");
			int count = 0;
			for(Long id : ids)
			{
				System.out.println("Processing " + (++count) + "/" + ids.size());
				saveDocument(id,reader,writer,yahooWriter);
			}
		}
	}
	
	
	
	public boolean config(String[] args) throws IOException
	{
		if (luceneDir == null && docStore != null)
		{
			System.out.println("bad args");
			return false; 
		}
		
		if (useItemAttrs)
			this.extractionMethod = EXTRACTION_TYPE.ITEM_ATTR;
		else if (useComments)
			this.extractionMethod = EXTRACTION_TYPE.COMMENTS;
		else if (useUsers)
			this.extractionMethod = EXTRACTION_TYPE.USERS;
		else if (useUserActions)
			this.extractionMethod = EXTRACTION_TYPE.USER_ACTIONS;
		else if (useDim)
			this.extractionMethod = EXTRACTION_TYPE.USER_DIM;
		
		if (deltaDays > 0)
			deltaMins = deltaMins + (deltaDays * 24 * 60);
		
		if (extractionMethod == null)
		{
			System.out.println("must supply either -use-item-attrs or -use-comments or -use-users");
			return false;
		}
		
		if (nlpLocation != null)
		{
			addEntities = new AddEntities(nlpLocation+"/en-sent.bin",nlpLocation+"/en-token.bin",
					extractPersons ? nlpLocation+"/en-ner-person.bin" : null,
					extractOrganisations ? nlpLocation+"/en-ner-organization.bin" : null,
					extractPlaces ? nlpLocation+"/en-ner-location.bin" : null,
					useStopwords ? (stopWordsFile == null ? nlpLocation+"/stopwords.txt" : stopWordsFile) : null,
					extractNouns ? nlpLocation+"/en-pos-maxent.bin" : null);
		}
		else if (useStopwords)
		{
			addEntities = new AddEntities(stopWordsFile == null ? nlpLocation+"/stopwords.txt" : stopWordsFile);
		}

		return true;
	}
	
	
	
	public static void main(String[] args) throws IOException
	{
			CreateLuceneIndexFromDb cr = new CreateLuceneIndexFromDb();
			try
			{
			Args.parse(cr, args);
			if (cr.config(args))
			{
				cr.createIndex();
			}
			}
			catch (IllegalArgumentException e) 
			{
				e.printStackTrace();
				Args.usage(cr);
			}	
	}
	
	
}
