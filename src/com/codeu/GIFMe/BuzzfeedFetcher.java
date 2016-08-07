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
	public Elements fetchBuzzfeed(String url) throws IOException {
		sleepIfNeeded();

		// download and parse the document
		Connection conn = Jsoup.connect(url);
		Document doc = conn.get();
		// select the content text and pull out the paragraphs.
		if (url.contains("search")) {
			Element content = doc.getElementById();

			// TODO: avoid selecting paragraphs from sidebars and boxouts
			Elements links = content.select();
			return links;

		}
		else {
			Element content = doc.getElementById("buzz_sub_buzz");

			// TODO: avoid selecting paragraphs from sidebars and boxouts
			Elements captions = content.select("h2");
			return captions;
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
		URL realURL = new URL(url);

		// assemble the file name
		String slash = File.separator;
		String filename = "resources" + slash + realURL.getHost() + realURL.getPath();

		// read the file
		InputStream stream = BuzzfeedFetcher.class.getClassLoader().getResourceAsStream(filename);
		Document doc = Jsoup.parse(stream, "UTF-8", filename);
		
		//select all of the images on the page
		Elements images = doc.select("img");

		//Element content = doc.getElementById("mw-content-text");
		//Elements paras = content.select("img[src~=(?i)\\.(gif)]");//ALSO include "video" tag
		return images;
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
