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
import java.lang.String;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;


public class BuzzfeedCrawler {
    // keeps track of where we started
    private final String source;
    
    // the index where the results go
    private JedisIndex index;

    public static Elements links;
    public static Elements captions;
    
    // queue of URLs to be indexed
    private List<String> queue = new ArrayList<String>();
    
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
        queue.add(source);
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
    public String crawl(boolean testing, boolean images, String url) throws IOException {
        //didn't index a page
        
        
        
        //System.out.println(url);
        //makes sure the url hasn't been indexed
         // if(testing == false && index.isIndexed(url) && !images){
         //     return null;
         // }
        
        // nextPageLinks = list of <a>
        Elements nextPageLinks;
        //if(testing){//get the contents of the url from the file
            nextPageLinks = wf.readBuzzfeed(url);
    
            if (images) {
                
                links = wf.fetchBuzzfeed(url, images);  
                //System.out.println(links);
            }
            else {
                captions = wf.fetchBuzzfeed(url, images);
                //System.out.println(captions);
            }
            
        //}
        //System.out.println(paragraph);
        //add all other internal links to the queue
        if (images) {
            queueInternalLinks(nextPageLinks);
        }

        if (images && captions != null && links != null) {
            storeGifs(url, captions, links);
        }
        
        // index the page
        //index.indexPage(url, paragraph);
        
        
        
        //return the url that was indexed
        //System.out.println("returned");
        return url;
    }
    
    /**
     * Parses paragraphs and extracts the Gifs out of the webpage
     * Gets keywords from meta data and adds the keyword and gif to Jedis
     *
     * @param url
     */
    void storeGifs(String url, Elements gifCaptions, Elements gifLinks){
        //get the top 5 keywords on the page
        //indexes the page
        TermCounter tc = new TermCounter(url);
        tc.processElements(gifCaptions);
                //
        
        ArrayList<String> urls = new ArrayList<String>();
        //store the gif with each keyword
        for(Element el : gifLinks){
            if (el.hasAttr("rel:bf_image_src")) {
                //System.out.println(el);
                String gifURL = el.attr("rel:bf_image_src");
                if (gifURL.contains(".gif")) {
                    urls.add(gifURL);
                }
                    

                    //for(int i = 0; i < keywords.size(); i++){     
                        //System.out.println(keywords.get(i) + " put to " + gifURL);
                        //index.pushMap(keywords, gifURL);
                    //}
                
            }
        }
        ArrayList<String> keywords = tc.getKeywords();
            index.pushMap(keywords, urls); 
    }

    
    /**
     * Parses paragraphs and adds internal links to the queue.
     *
     * @param paragraphs
     */
    // NOTE: absence of access level modifier means package-level
    void queueInternalLinks(Elements urlLinks) {
        System.out.println("Queuing links");
        String url;
        //System.out.println(paragraphs);
            //if it's an internal link it adds it to the queue
            for (Element link : urlLinks)
            {
                url = link.attr("href");
                //url.startsWith("/buzzfeed/") &&
                if (!url.contains("webappstatic") && url.startsWith("/")) {
                    if (!queue.contains ("https://www.buzzfeed.com" + url)) {
                        queue.add("https://www.buzzfeed.com" + url);
                    }
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


    public List<String> getGifURLs(String term) {
        //Set<String> set = index.smembers(index.urlSetKey(term));
        List<String> list = index.getGifURLs(term);
        return list;
    }



    /**
     * Looks up a term and returns a map from term to ArrayList of GIF URLs.
     * 
     * @param term
     * @return Map from URL to count.
     */
    public Map<String, ArrayList> getGIFList(String term) {
        Map<String, ArrayList> map = new HashMap<String, ArrayList>();
        List<String> urls = new ArrayList<String>();
        urls = getGifURLs(term);

        ArrayList<String> GIFlist = new ArrayList<String>();
        for (String url: urls) {
            //Adds URL to ArrayList
            GIFlist.add(url);
            //index.set(term, url);
        }
        //Puts the ArrayList of GIF URLs to the keyword/term
        map.put(term, GIFlist);
        
        return map;
    }





    /**
    * Returns URL at a random position
    */
    public String grabGifURL(String term, Jedis jedis) {
        
        Long length = jedis.llen(index.urlListKey(term));
        //System.out.println(length);
        System.out.println(term + ": " + length + " results found.");

        if(length == 0){
            String temp = "No URL found";
            return temp;
        }
        int i = (int) (long) length;
        Random randomGenerator = new Random();
        long randomPosition = randomGenerator.nextInt(i);
        return jedis.lindex(index.urlListKey(term), randomPosition);
    }



    

    public static void main(String[] args) throws IOException {
        
        // make a BuzzfeedCrawler
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);
        //index.deleteURLSets();
        //queueBuzzfeed();
        String source = "https://www.buzzfeed.com/matthewchampion/this-british-scientist-was-shocked-to-see-his-gif-appear-in?utm_term=.xmGXXdJOK#.uey22gl9o";
        BuzzfeedCrawler wc = new BuzzfeedCrawler(source, index);
        
        // for testing purposes, load up the queue
        // Elements paragraphs = wf.fetchBuzzfeed(source);
        // wc.queueInternalLinks(paragraphs);
        
        // loop until we index a new page
        // String res;
        // do {
            
        //     //System.out.println("test2");
        //     //get the next url from the queue
        //     Random randomGenerator = new Random();
        //     long randomPosition = randomGenerator.nextInt(wc.queue.size());
        //     String url = wc.queue.get((int) (long) randomPosition);
            
        //     System.out.println(url);

        //     res = wc.crawl(false, false, url);
        //     res = wc.crawl(false, true, url);
        //     //wc.storeGifs(res, wc.captions, wc.links);
            
        //     // REMOVE THIS BREAK STATEMENT WHEN crawl() IS WORKING
        //     // break;
        // } while (true);
            
            Scanner reader = new Scanner(System.in);
            System.out.println("Write a status: ");
            String input = reader.nextLine();
            System.out.println("input: " + input);
            String[] words = input.split("\\s+");

            
            for (int i = 0; i < words.length; ++i) {
               String word = words[i];
               //Map<String, ArrayList> map = new HashMap<String, ArrayList>();
               //map = wc.getGIFList(word); 
               //ArrayList<String> GIFlist = new ArrayList<String>();
               //GIFlist = map.get(word);
               String gifURL = wc.grabGifURL(word, jedis);
               if (gifURL != "No URL found") {
                 System.out.println("img.buzzfeed.com/buzzfeed-static/static/" + gifURL);
               }
               else {
                   System.out.println(gifURL);
                }

               
            }
            
    }
}
