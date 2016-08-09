package com.codeu.GIFMe;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.lang.Math;

import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * Represents a Redis-backed web search index.
 * 
 */
public class JedisIndex {

	private Jedis jedis;

	/**
	 * Constructor.
	 * 
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		//System.out.println("isIndexed");
		String redisKey = termCounterKey(url);
		//System.out.println(jedis.exists(redisKey));
		return jedis.exists(redisKey);
	}
	
	/**
	 * Returns the Redis key for a given search term.
	 * 
	 * @return Redis key.
	 */
	public String urlSetKey(String term) {
		return "URLSet:" + term;
	}

	public String urlListKey(String term) {
		return "URLList:" + term;
	}
	
	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * 
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		//System.out.println("termCounterKey");
		return "TermCounter:" + url;
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}


	public void pushMap (ArrayList<String> keywords, ArrayList<String> urls) {
		//System.out.println("Adding " + url + " to " + urlListKey(term));
		Transaction trans = jedis.multi();
		System.out.println("# URLS: " + urls.size());
		System.out.println("# keywords: " + keywords.size());
		for (String url : urls) {
			for (String keyword : keywords) {
				if (keyword.length() < 4) { continue; }
				url = url.replace("https://img.buzzfeed.com/buzzfeed-static/static/", ""); 
				// System.out.println("keyword: " + keyword + " | url: " + url);
				trans.lpush(urlListKey(keyword), url);
			}
		}
		trans.exec();
		
		//jedis.sadd(term, url);
	}
	
	
	/**
	 * Adds a URL to the set associated with `term`.
	 * 
	 * @param term
	 * @param tc
	 */
	public void add(String term, String url) {
		jedis.sadd(urlSetKey(term), url);
	}

	/**
	 * Looks up a search term and returns a set of URLs to GIFs.
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public List<String> getGifURLs(String term) {
		List<String> list = jedis.mget(urlListKey(term));
		return list;
	}

	// /**
	//  * Looks up a search term and returns one gif for a term
	//  * 
	//  * @param term
	//  * @return one Gif URL
	//  */
	// public String getGif(String term) {
	// 	Set<String> set = getGifURLs(term);
	// 	String url = "";

	// 	//get a random integer from the Set
	// 	Random rand = new Random();
	// 	int index = rand.nextInt(set.size());

	// 	//go through the set and find the random gif
	// 	Iterator<Strings> it = set.iterator();
	// 	for(int i = 0; i < index; ++i){
	// 		url = it.next();
	// 	}

	// 	return url;
	// }



	public List<Object> pushTermCounterToRedis(TermCounter tc) {
		Transaction t = jedis.multi();
		
		String url = tc.getLabel();
		String hashname = termCounterKey(url);
		
		// if this page has already been indexed; delete the old hash
		t.del(hashname);

		// for each term, add an entry in the termcounter and a new
		// member of the index
		for (String term: tc.keySet()) {
			Integer count = tc.get(term);
			t.hset(hashname, term, count.toString());
			t.sadd(urlSetKey(term), url);
		}
		List<Object> res = t.exec();
		return res;
	}

	/**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Integer getCount(String url, String term) {
		String redisKey = termCounterKey(url);
		String count = jedis.hget(redisKey, term);
		return new Integer(count);
	}

	/**
	 * Prints the contents of the index.
	 * 
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term: termSet()) {
			System.out.println(term);
			
			// for each term, print the pages where it appears
			List<String> urls = getGifURLs(term);
			for (String url: urls) {
				Integer count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key: keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return jedis.keys("*");
	}

	

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			System.out.println("deleting " + key);
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Stores two pages in the index for testing purposes.
	 * 
	 * 
	 * @return
	 * @throws IOException
	 */
	private static void loadIndex(JedisIndex index) throws IOException {
		BuzzfeedFetcher bf = new BuzzfeedFetcher();

		String source = "https://www.buzzfeed.com";
		//Elements paragraphs = bf.fetchBuzzfeed(source);
		//index.indexPage(source, paragraphs);
		
		
	}
}