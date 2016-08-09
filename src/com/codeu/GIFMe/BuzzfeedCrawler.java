package com.codeu.GIFMe;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;


public class BuzzfeedCrawler {
    // keeps track of where we started
    private final String source;
    
    // the index where the results go
    private JedisIndex index;
    
    // queue of URLs to be indexed
    private Queue<String> queue = new LinkedList<String>();
    
    // fetcher used to get pages from Buzzfeed
    final static BuzzfeedFetcher wf = new BuzzfeedFetcher();
    
    /**
     * Constructor.
     *
     * @param source
     * @param index
     */
    public BuzzfeedCrawler(String source, JedisIndex index) {
        this.source = source;
        this.index = index;
        queue.offer(source);
    }
    
    /**
     * Returns the number of URLs in the queue.
     *
     * @return
     */
    public int queueSize() {
        return queue.size();
    }
    
    /**
     * Gets a URL from the queue and indexes it.
     * @param b
     *
     * @return Number of pages indexed.
     * @throws IOException
     */
    public String crawl(boolean testing) throws IOException {
        //didn't index a page
        
        if(queue.isEmpty()){
            return null;
        }
        System.out.println("test2");
        //get the next url from the queue
        String url = queue.poll();
        System.out.println(url);
        //makes sure the url hasn't been indexed
        if(testing == false && index.isIndexed(url)){
            return null;
        }
        
        Elements paragraph;
        if(testing){//get the contents of the url from the file
            paragraph = wf.readBuzzfeed(url);
        }else{//get the contents from the web
            paragraph = wf.fetchBuzzfeed(url);
        }
        System.out.println(paragraph);
        //add all other internal links to the queue
        //queueInternalLinks(paragraph);

        storeGifs(url, paragraph);
        
        // index the page
        //index.indexPage(url, paragraph);
        
        
        
        //return the url that was indexed
        return url;
    }
    
    /**
     * Parses paragraphs and extracts the Gifs out of the webpage
     * Gets keywords from meta data and adds the keyword and gif to Jedis
     *
     * @param url
     */
    void storeGifs(String url, Elements images){
        //get the top 5 keywords on the page
        //indexes the page
        TermCounter tc = new TermCounter(url);
        tc.processElements(images);
        index.pushTermCounterToRedis(tc);
        
        
        //store the gif with each keyword
        for(Element el : images){
            String gifURL = el.attr("src");
            if(url.contains("gif")){
                for(int i = 0; i < 5; ++i){     
                    index.add(tc.getKeywords()[i], gifURL);
                }
            }
        }
    }

    
    /**
     * Parses paragraphs and adds internal links to the queue.
     *
     * @param paragraphs
     */
    // NOTE: absence of access level modifier means package-level
    void queueInternalLinks(Elements paragraphs) {
        String url;
        Elements urlLinks;
        for (Element paragraph : paragraphs)
        {
            urlLinks = paragraph.select("a[href]");
            
            //if it's an internal link it adds it to the queue
            for (Element link : urlLinks)
            {
                url = link.attr("href");
                if (url.startsWith("/buzzfeed/"))
                    queue.add("https://www.buzzfeed.com" + url);
            }
        }
    }


    /**
     * Adds a URL to the set associated with `term`.
     * 
     * @param term
     * @param gifURL
     */
    public void addGifURL(String term, String gifURL) {
        index.add(index.urlSetKey(term), gifURL);
    }


    public Set<String> getGifURLs(String term) {
        //Set<String> set = index.smembers(index.urlSetKey(term));
        Set<String> set = index.getGifURLs(term);
        return set;
    }



    /**
     * Looks up a term and returns a map from term to ArrayList of GIF URLs.
     * 
     * @param term
     * @return Map from URL to count.
     */
    public Map<String, ArrayList> getGIFList(String term) {
        Map<String, ArrayList> map = new HashMap<String, ArrayList>();
        Set<String> urls = new HashSet<String>();
        urls = getGifURLs(term);

        ArrayList<String> GIFlist = new ArrayList<String>();
        for (String url: urls) {
            //Adds URL to ArrayList
            GIFlist.add(url);
            //index.set(term, url);
        }
        //Puts the ArrayList of GIF URLs to the keyword/term
        map.put(term, GIFlist);
        //index.set(term, GIFlist);
        return map;
    }





    /**
    * Returns URL at a random position
    */
    public String grabGifURL(ArrayList<String> GIFlist) {
        int size = GIFlist.size();
        if(size == 0){
            String temp = "No URL found";
            return temp;
        }
        Random randomGenerator = new Random();
        int randomPosition = randomGenerator.nextInt(size);
        return GIFlist.get(randomPosition);
    }
    

    public static void main(String[] args) throws IOException {
        
        // make a BuzzfeedCrawler
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);
        String source = "https://www.buzzfeed.com/juliegerstein/heres-how-you-can-fold-basically-everything-better?utm_term=.bkNg49qOz#.cfbO30Pnv";
        BuzzfeedCrawler wc = new BuzzfeedCrawler(source, index);
        
        // for testing purposes, load up the queue
        // Elements paragraphs = wf.fetchBuzzfeed(source);
        // wc.queueInternalLinks(paragraphs);
        
        // loop until we index a new page
        String res;
        do {
            res = wc.crawl(false);
            
            // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
            // break;
        } while (res == null);
            
            Scanner reader = new Scanner(System.in);
            System.out.println("Write a status: ");
            String input = reader.nextLine();
            System.out.println("input: " + input);
            String[] words = input.split("\\s+");

            
            for (int i = 0; i < words.length; ++i) {
               String word = words[i];
               Map<String, ArrayList> map = new HashMap<String, ArrayList>();
               map = wc.getGIFList(word); 
               ArrayList<String> GIFlist = new ArrayList<String>();
               GIFlist = map.get(word);
               String gifURL = wc.grabGifURL(GIFlist);
               System.out.println(word + ": " + gifURL);
               jedis.set(word, gifURL);
            }
    }
}
