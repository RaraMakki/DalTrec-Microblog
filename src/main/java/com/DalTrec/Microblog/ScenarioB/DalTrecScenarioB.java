package ScenarioB;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import cc.twittertools.corpus.data.Status;
import cc.twittertools.index.TweetAnalyzer;

public class DalTrecScenarioB 
{
	  static Analyzer analyzer;
	  static Directory dir;
	  static IndexWriter writer;
	  static IndexSearcher indexSearcher;
	  static IndexReader indexReader;
	  static File indexLocation;

	  static long timeStamp;
	  static HashMap<String,HashMap<String,Double>> weights = new HashMap<String, HashMap<String,Double>>();
	  
	  
	  static FileWriter fout;
	  static BufferedWriter bout;
	  
	  
	  public static void main(String[] args)
	  {
		  
		  	loadWeights();
		  	int curDate = 19;
			
			while (true)
			{
			    Date date = new Date();

			    String ISOFormat = "yyyy MM dd HH:mm:ss.SSS zzz";
			    
			    String dateInUTC = "";      
			    SimpleDateFormat formatter = new SimpleDateFormat(ISOFormat); 
			    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));  
			    dateInUTC = formatter.format(date);
			    
			    int day = Integer.parseInt(dateInUTC.split(" ")[2]);
			    int hour = Integer.parseInt(dateInUTC.split(" ")[3].split(":")[0]);
			    int minute = Integer.parseInt(dateInUTC.split(" ")[3].split(":")[1]);
			    
			    if (day!=curDate)
			    {
			    	initializeLuceneHere(curDate);
			    	doIndex(curDate);
			    	DoRequest(curDate);
			    	curDate++;
			    }
			    else 
			    {
			    	long waitTime = calcDifference(day,hour,minute);
			    	try
			    	{
			    		Thread.sleep(waitTime);
			    	}
			    	catch(Exception e)
			    	{
			    		e.printStackTrace();
			    	}
			    }
			}
	  }
	  
	  private static long calcDifference(int day, int hour, int minute) 
	  {
		long waitTime = (23 - hour)*3600*1000;
		waitTime = waitTime + (59-minute)*60*1000;
		
		return waitTime;
	  }
	  
	  private static void loadWeights() 
	  {
		  try
		  {
				FileInputStream fin = new FileInputStream("../../../data/ManuallyLabeledKeyterms2.txt");
				DataInputStream din = new DataInputStream(fin);
				BufferedReader bin = new BufferedReader(new InputStreamReader(din));
				
				String text = "";
				while((text=bin.readLine())!=null)
				{
					String[] parts = text.split("\t");
					HashMap<String,Double> vec = new HashMap<String, Double>();
					for (int j=1;j<parts.length;j++)
					{
						String[] parts2 = parts[j].split(":");
						vec.put(parts2[0].toLowerCase(), Double.parseDouble(parts2[1]));
					}
					weights.put(parts[0],vec);
				}
				bin.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		
	}
	private static void initializeLuceneHere(int curDate) 
	  {
		  try 
		  {
			  indexLocation = new File("../../../data/index/"+curDate);
			  analyzer = new TweetAnalyzer(Version.LUCENE_43);		
			
			  dir = FSDirectory.open(indexLocation);
			  IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
			  config.setSimilarity(new LMDirichletSimilarity(2100.0f));
			    
			  writer = new IndexWriter(dir, config);
		}
			
		catch (Exception e) 
		{
			e.printStackTrace();
		}	
	}
	  
	protected static void doIndex(int curDate)
	{
		try
		{	
			FieldType textOptions = new FieldType();
			textOptions.setIndexed(true);
		    textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		    textOptions.setStored(true);
		    textOptions.setTokenized(true);        
		    textOptions.setStoreTermVectors(true);
		    
			Document document = new Document();
			  
			Field textField = new Field("text", "", textOptions);
			LongField timeField = new LongField("time",0L,Field.Store.YES);	
			Field IdField = new Field("Id", "", textOptions);
			
			document.add(textField);		
			document.add(IdField);
			document.add(timeField);

			File path = new File("../../../data/tweets/");
			
			File[] listOfFiles = path.listFiles(new FilenameFilter() { 
	            public boolean accept(File dir, String filename)
	                 { return filename.endsWith(""); }
			} );
			
			
			for (File f:listOfFiles)
			{
			    int day = Integer.parseInt(f.getName().split("-")[2]);
			    int hour = Integer.parseInt(f.getName().split("-")[3]);
			    if (((day==curDate) && (hour<21)) || ((day==(curDate-1))&& (hour>20)))			    	
				{
					File inFile = new File("../../../data/tweets/" + f.getName());
					FileInputStream fin = new FileInputStream(inFile);	
					DataInputStream din = new DataInputStream(fin);
					BufferedReader bin = new BufferedReader(new InputStreamReader(din));
					
					String text = "";
					while((text=bin.readLine())!= null)
					{
						Status tweet = null;
						tweet = Status.fromJson(text);
						
						if ((tweet!=null) && ((tweet.getLang().equals("en")) || (tweet.getLang().equals("und"))))
						{
							
							long ret = tweet.getRetweetedStatusId();
							if ((ret==0L) || (ret == -1L))
							{
							    preProcessTweet ppt = new preProcessTweet();
							    ppt.SetStr(tweet.getText());
							    String processedTxt = ppt.process(true, true, true);					    
							    
							    IdField.setStringValue(String.valueOf(tweet.getId()));
							    textField.setStringValue(processedTxt.toLowerCase());
							    timeField.setLongValue(System.currentTimeMillis());
							    
							    BooleanQuery categoryQuery = new BooleanQuery();
								BooleanQuery query = new BooleanQuery();
								TermQuery catQuery1 = new TermQuery(new Term("Id", String.valueOf(tweet.getId())));
								categoryQuery.add(new BooleanClause(catQuery1, BooleanClause.Occur.SHOULD));
								query.add(new BooleanClause(categoryQuery, BooleanClause.Occur.MUST));
							
								writer.deleteDocuments(query);
							    writer.addDocument(document);
							}
						}
					}
					bin.close();
				}
			}
			writer.commit();
			writer.close();
	    	indexReader = DirectoryReader.open(dir);

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	protected static void DoRequest(int curDate) 
	{	
		try
		{
		    fout = new FileWriter("../../../data/results",true);
			bout = new BufferedWriter(fout);
			  
			SearcherManager mgr = new SearcherManager(dir,new SearcherFactory());
			mgr.maybeRefresh();
			IndexSearcher searcher = mgr.acquire();
			
			searcher.setSimilarity(new LMDirichletSimilarity(2100.0f));

			for (String tStr:weights.keySet())
			{
				HashSet<String> forNearDuplicateDetection = new HashSet<String>();
				BooleanQuery newBooleanQuery = new BooleanQuery();
				HashMap<String,Double> qVec = weights.get(tStr);
				
				for (String trmStr:qVec.keySet())
				{
					TermQuery tq1 = new TermQuery(new Term("text", trmStr));
					double boostVal = qVec.get(trmStr);
					float boostValF = (float)boostVal;
					tq1.setBoost(boostValF);
					newBooleanQuery.add(tq1, BooleanClause.Occur.SHOULD);				
				}
					
				TopDocs docs = searcher.search(newBooleanQuery, Math.max(1, 1000));
				int rank =1;
				for (ScoreDoc scoreDoc : docs.scoreDocs)
				{
				    Document doc = searcher.doc(scoreDoc.doc);
				    int docId = scoreDoc.doc;
				    
				    if ((isAboveThreshold(docId,weights.get(tStr))) && (!isNearDuplicate(forNearDuplicateDetection,doc.get("text"))))
				    {
					    forNearDuplicateDetection.add(doc.get("text"));
					    bout.write("201507" + curDate + " " + tStr + " Q0 " + doc.get("Id") + " " + rank + " " + scoreDoc.score + " DALTREC_B_PREP\n");
				    	rank++;
				    	if (rank>100)
				    	{
				    		break;
				    	}
				    }
				}
			}
			bout.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	private static boolean isAboveThreshold(int tId, HashMap<String, Double> scoreWeights) 
	{
		double score = 0.0;
		try
		{
			Terms terms = indexReader.getTermVector(tId, "text");
			if (terms != null && terms.size() > 0) {
			    TermsEnum termsEnum = terms.iterator(null);
			    BytesRef term = null;
			    while ((term = termsEnum.next()) != null) 
			    {
			        DocsEnum docsEnum = termsEnum.docs(null, null);
			        int docIdEnum;
			        while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) 
			        {
			        	if (docsEnum.freq()>0)
			        	{
			        		String termStr = term.utf8ToString().toLowerCase();
			        		if (scoreWeights.containsKey(termStr))
			        		{
			        			score = score + scoreWeights.get(termStr);
			        		}
			        	}
	
			        }
			    }
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if (score>=10.0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	private static boolean isNearDuplicate(HashSet<String> forNearDuplicateDetection, String str) 
	{
		preProcessTweet ppt = new preProcessTweet();
		ppt.init();
		for (String strComp:forNearDuplicateDetection)
		{
			ppt.SetStr(strComp);
			String str1 = ppt.process(false, false, false);
			
			ppt.SetStr(str);
			String str2 = ppt.process(false,false,false);
			
			if (nearDuplicateScore(str1,str2)>0.9)
			{
				return true;
			}
		}
		return false;
	}
	private static double nearDuplicateScore(String t1,String t2)
	{
		double res = 0;
		int len = 0;
		int commonTerms = 0;
		
		String[] terms1 = t1.split(" ");
		HashSet<String> vocab1 = new HashSet<String>();
		
		for (String tStr1:terms1)
		{
			vocab1.add(tStr1);
		}
		
		String[] terms2 = t2.split(" ");
		HashSet<String> vocab2 = new HashSet<String>();
		for (String tStr2:terms2)
		{
			vocab2.add(tStr2);
		}
		
		if ((vocab1.size() != 0) && (vocab2.size() != 0))
		{
			HashSet<String> vocab = vocab1;
			len = vocab1.size();
			
			if (vocab2.size() < vocab1.size())
			{
				vocab = vocab2;
				len = vocab2.size();
			}
			
			for (String tStr:vocab)
			{
				 if ((vocab1.contains(tStr)) && (vocab2.contains(tStr)))
				 {
					 commonTerms++;
				 }
			}
		}
		
		res = (commonTerms * 1.0)/len;
		return res;
	}

	
}

