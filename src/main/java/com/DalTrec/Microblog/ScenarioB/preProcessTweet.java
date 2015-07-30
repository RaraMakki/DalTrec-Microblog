package ScenarioB;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class preProcessTweet 
{
	
	HashSet<String> stopWords = new HashSet<String>();
	String input ="";
	
	public void init()
	{
		try
		{
			FileInputStream fin = new FileInputStream("../../../data/stop.txt");
			DataInputStream din = new DataInputStream(fin);
			BufferedReader bin = new BufferedReader(new InputStreamReader(din));
			
			String text = "";
			
			while ((text=bin.readLine())!=null)
			{
				stopWords.add(text.toLowerCase());
			}
			
			bin.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void SetStr(String inStr)
	{
		this.input = inStr.toLowerCase();
	}
	public String process(boolean keepUserMention, boolean keepUrl, boolean keepStopWord)
	{
		String filteredText = "";
		String []prts = input.split("\\s+");
		
		for (String ptr:prts)
		{	
			if ((keepUserMention) && (ptr.startsWith("@")))
			{
				filteredText = filteredText + ptr + " ";
			}
			
			if ((keepUrl) && (ptr.contains("http")))
			{
				filteredText = filteredText + ptr + " ";			
			}
			
			if ((!ptr.startsWith("@")) && (!ptr.contains("http")))
			{
				filteredText = filteredText + ptr + " ";
			}
		}
		
		filteredText = filteredText.toLowerCase();

		filteredText = filteredText.replaceAll("[!,:\";.#/?@(><)]+", " ");
		filteredText = filteredText.replaceAll("\\*", " ");
		filteredText = filteredText.replaceAll("'s?\\W", " ");
		filteredText = filteredText.replaceAll("[\\s]+", " ");
		filteredText = filteredText.trim();
		
		//return input;
		String []parts = filteredText.split(" ");
		String res = "";
		for (String ptr:parts)
		{
			Pattern pattern = Pattern.compile("\\w+");
			Matcher matcher = pattern.matcher(ptr);
			
			if (matcher.find())
			{
				if (keepStopWord)
				{
					res = res + ptr + " ";
				}
				else if (!stopWords.contains(ptr.toLowerCase()))
				{
					res = res + ptr + " "; 
				}
			}
		}
		return res.trim();
	}
}
