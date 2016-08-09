package com.codeu.GIFMe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BuzzfeedFetcher {
	private long lastRequestTime = -1;
	private long minInterval = 1000;

	/**
	 * Fetches and parses a URL string, returning a list of paragraph elements.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements fetchBuzzfeed(String url, boolean images) throws IOException {
		System.out.println("in BF");
		sleepIfNeeded();

		// download and parse the document
		
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		// select the content text and pull out the paragraphs.
		
		if (images) {
			Element content = doc.getElementById("buzz_sub_buzz");

			// TODO: avoid selecting paragraphs from sidebars and boxouts
			if (content != null) {
				Elements gifLinks = content.select("img");
			//System.out.println(captions);
			
				return gifLinks;
			}
			return null;
		}
		else {

			Element content = doc.getElementById("buzz_sub_buzz");

			// TODO: avoid selecting paragraphs from sidebars and boxouts
			if (content != null) {

				Elements captions = content.select("h2");
				return captions;
			}
			//System.out.println(captions);
		return null;
			
		}
	}

	/**
	 * Reads the contents of a Buzzfeed page from src/resources.
	 *
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public Elements readBuzzfeed(String url) throws IOException {
		System.out.println("in RF");
		sleepIfNeeded();

		// download and parse the document
		
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		// select the content text and pull out the paragraphs.
		
		return doc.getElementsByTag("a");
		
	}


	/**
	 * Rate limits by waiting at least the minimum interval between requests.
	 */
	private void sleepIfNeeded() {
		if (lastRequestTime != -1) {
			long currentTime = System.currentTimeMillis();
			long nextRequestTime = lastRequestTime + minInterval;
			if (currentTime < nextRequestTime) {
				try {
					//System.out.println("Sleeping until " + nextRequestTime);
					Thread.sleep(nextRequestTime - currentTime);
				} catch (InterruptedException e) {
					System.err.println("Warning: sleep interrupted in fetchBuzzfeed.");
				}
			}
		}
		lastRequestTime = System.currentTimeMillis();
	}
}
