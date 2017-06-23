package src;

import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject; 
import org.json.simple.parser.JSONParser;


public class MainFile {
  // allProds maps the manufacturer to a list of its products
  HashMap<String, LinkedList<Product> > allProds;
  LinkedList<Listing> ads;
  //every time there is a match between a listing and a product, output get updated
  HashMap<Product, LinkedList<Listing> > output;
  
  // constructor: takes the name of the two input files, creates two file readers, and initiliazes the class variable.
  public MainFile(String file_prods, String file_ads){
      allProds = new HashMap<String, LinkedList<Product> >();
      ads = new LinkedList<Listing>();
      output = new HashMap<Product, LinkedList<Listing>>() ;
    
    try{
      BufferedReader prodsReader=new BufferedReader( new InputStreamReader( new FileInputStream(file_prods), "UTF8") );
      BufferedReader adsReader  =new BufferedReader( new InputStreamReader( new FileInputStream(file_ads  ), "UTF8") );
      readJSONProds(prodsReader);
      readJSONAds(adsReader);
    } catch (Exception e) {e.printStackTrace();}
  }
  
  // read the JSON products file, fill allProds and initialize the hashmap "output"
  public void readJSONProds(BufferedReader prodsReader ){
    try{
      String line;
      
      while( ((line = prodsReader.readLine()) != null) ){
        JSONObject JSONprod = (JSONObject) (new JSONParser()).parse(line);
        
        String key = (String) JSONprod.get("manufacturer");
        if ( !allProds.containsKey( key ) ){
          allProds.put(key, new LinkedList<Product>());
        }
        
        String a=(String) JSONprod.get("product_name"  ), 
               b=(String) JSONprod.get("manufacturer"  ),
               c=(String) JSONprod.get("model"         ), 
               d=(String) JSONprod.get("family"        ),
               e=(String) JSONprod.get("announced-date");
        Product tempProd = new Product( a,b,c,d,e );
        
        allProds.get(key).add( tempProd );
        output.put(tempProd, new LinkedList<Listing>()); 
      }
    } catch (Exception e) {e.printStackTrace();}
  }
  
  // read the JSON listings file and fill the list "ads"
  public void readJSONAds(BufferedReader adsReader ){
    try{  
      String line;
      
      while( ((line = adsReader.readLine()) != null) ){
        JSONObject JSONad = (JSONObject) (new JSONParser()).parse(line);
        String a = (String) JSONad.get("title"       ), 
               b = (String) JSONad.get("manufacturer"),
               c = (String) JSONad.get("currency"    ),
               d = (String) JSONad.get("price"       );
        ads.add(new Listing( a,b,c,d ));
      }
    } catch(Exception e){e.printStackTrace();}
  }
  
  // write output in JSON format to Result.txt
  public void writeJSON(){
    try{
      PrintWriter writer = new PrintWriter("data/Result.txt", "UTF-8");
      
      for (Map.Entry<Product ,LinkedList<Listing>> e : output.entrySet()){
        JSONObject jo = new JSONObject();
        jo.put("product_name", e.getKey().prod_name);
      
        JSONArray ja = buildJSONArrayFromLL(e.getValue());// build the JSON array
        jo.put("listings", ja);
        
        writer.println(jo);
      }
      writer.close();
    } catch(Exception e){e.printStackTrace();}
  }
  
  // This method converts a linked list to a JSONArray
  public JSONArray buildJSONArrayFromLL( LinkedList<Listing> ll ){
    JSONArray ja = new JSONArray();
    for (Listing l : ll){
      ja.add( l.createJSONObj() );
    }
    return ja;
  }
  
  // For each manufacturer, this method gets the list of ads and products associated with it. It gets
  //   the first by removing listings  from "ads"  and saving them in a separate list, and gets the 
  //   second using "allProds". It then calls matchLL() on those lists which tries to find matching 
  //   between the two given lists.
  public void matching(){
    for(String company : allProds.keySet()){
      LinkedList<Listing> compa_ads = new LinkedList<Listing>(); // collects listing related to company

      ListIterator<Listing> iter = ads.listIterator();
      while( iter.hasNext() ){
        Listing ad = iter.next();
        if(ad.manuf.toLowerCase().contains(company.toLowerCase())){
          iter.remove(); // remove it from ads
          compa_ads.add( ad );// add it to compa_ads
        }
      }

      matchLL( allProds.get(company), compa_ads );
    }
    
    writeJSON(); 
  }
  
  // Finds matchings between the product list "pl" and the ads list "al". Every match gets added to "output".
  public void matchLL( LinkedList<Product> pl, LinkedList<Listing> al ){ 
    for (Listing ad : al){ // loop ads list "al"
      String adTitleL=ad.title.toLowerCase();
      adTitleL=titleModify(adTitleL);
      
      // ad_pl: holds the possible Product candidates for "ad"
      LinkedList<Product> ad_pl = new LinkedList<Product>();
      for (Product prod : pl){ // loop products list "pl"
        String prodModL = prod.model.toLowerCase();
        String regexStr = get_regexStr(prodModL);
        
        if ( adTitleL.matches(regexStr) ){
          ad_pl.add(prod);
        }
      }
      
      if (ad_pl.size()==0){        // no possible product candidate
        ads.add(ad);              // add it to ads which will have all unmatched ads at the end
      }else if (ad_pl.size()==1){ // there is a match
        output.get( ad_pl.getFirst() ).add(ad);
      }else {
        filterProds(ad_pl, ad);
      }
    }
  }

  // This method inserts string t between letters and numbers of string s. Exp: "df20k"->"df 20 k" for t=" "
  public String strModify(String s, String t){
    return s.replaceAll( "(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])", t );
  }
  
  // This method is to be used when more than one product candidate for the listing "ad".
  //    It tries to break the tie by checking the following:
  //      - Which product model appeared first in the title of "ad". 
  //      - If two or more product models appeared at the same index then we pick the product with longest model name.
  //      - If we still have more than one candidate then we check the product which family name (if it has one) 
  //        appeared first in "ad"'s title.
  // Noticed that there were cases where two product candidates were identical in everything but the name 
  //    (e.g., Samsung_SL202 & Samsung-SL202). In that case, this function just picks the first one.
  // The packages "Pattern" and "Matcher" seem to be slower than "matches(String regex)" that we use in "matchLL()".
  //    We use them here to get the index of the matched regex. This should not be bottleneck as this function will 
  //    only get called if there is more than one possible candidate for a certain ad (we do not expect this to happen
  //    a lot).
  public void filterProds(LinkedList<Product> ad_pl, Listing ad){
    String adTitleL=ad.title.toLowerCase();
    adTitleL=titleModify(adTitleL);
    
    for (Product prod : ad_pl){ // loop product candidates for Listing "ad"
      String prodModL=prod.model.toLowerCase();
      String regexStr = get_regexMiniStr(prodModL);
      
      Pattern pattern = Pattern.compile(regexStr);
      Matcher matcher = pattern.matcher(adTitleL);
      matcher.find(); // find the matched regexStr in adTitleL
      prod.modelInd=matcher.start()+1; // see the regexStr to figure out why +1
    }
    
    min_modelIndLL(ad_pl);    // keep product(s) which model appeared first
    if (ad_pl.size()==0){ System.exit(0); } // This should not happen
    if (ad_pl.size()==1){ output.get( ad_pl.getFirst() ).add(ad); return; }
    
    // else: we still have more than one product candidate
    max_modelLengthLL(ad_pl); // keep product(s) with longest model string 
    if (ad_pl.size()==0){ System.exit(0); } // This should not happen
    if (ad_pl.size()==1){ output.get( ad_pl.getFirst() ).add(ad); return; }
    
    // else: we still have more than one product candidate
    for (Product prod : ad_pl){
      if (prod.family == null) {continue;}
      String prodFamL = prod.family.toLowerCase();
      String regexStr = get_regexMiniStr(prodFamL);
      
      Pattern pattern = Pattern.compile(regexStr);
      Matcher matcher = pattern.matcher(adTitleL);
      if (matcher.find()){ // unlike the model, the family might not appear in "ad"'s title string
        prod.familyInd = matcher.start()+1; // see the regexStr to figure out why +1
      }
    }
    
    min_familyIndLL(ad_pl);  // keep product(s) which family appeared first
    if (ad_pl.size()==0){ System.exit(0); } // This should not happen
    if (ad_pl.size()==1){ output.get( ad_pl.getFirst() ).add(ad); return; }
    
    //else: we still have more than one product candidate
    output.get( ad_pl.getFirst() ).add(ad);  
  }
  
  // The next two methods return back the regex we will use to try to match the products' model to the different listings.
  //   It tries to accomodate for the different ways the model could have been modified in the listing's title.
  public String get_regexStr(String prodModL){
    return "(^|.*[^a-z0-9\\-_])("+prodModL         // try to match the product model as it is
        +"|"+prodModL.replaceAll("[^a-z0-9]", "" ) // delete non-alphanumeric charachters 
        +"|"+prodModL.replaceAll("[^a-z0-9]", " ") // replace non-alphanumeric charachters by space
        +"|"+prodModL.replaceAll("[^a-z0-9]", "-") // replace non-alphanumeric charachters by "-"
        +"|"+strModify(prodModL, " ")              // insert " " between alphabets and numbers
        +"|"+strModify(prodModL, "-")              // insert "-" between alphabets and numbers
        +")([^a-z0-9\\-_].*|\\.$|$)"; 
  }
  public String get_regexMiniStr(String prodModL){
    return "(^|[^a-z0-9\\-_])("+prodModL           // try to match the product model as it is
        +"|"+prodModL.replaceAll("[^a-z0-9]", "" ) // delete non-alphanumeric charachters 
        +"|"+prodModL.replaceAll("[^a-z0-9]", " ") // replace non-alphanumeric charachters by space
        +"|"+prodModL.replaceAll("[^a-z0-9]", "-") // replace non-alphanumeric charachters by "-"
        +"|"+strModify(prodModL, " ")              // insert " " between alphabets and numbers
        +"|"+strModify(prodModL, "-")              // insert "-" between alphabets and numbers
        +")([^a-z0-9\\-_]|\\.$|$)"; 
  }
  
  // Takes a list of products, figures out the maximal model length value, then removes the elements of the list 
  //   with model length value strictly smaller than the maximal value.
  public void max_modelLengthLL(LinkedList<Product> ll) {
    int maxValue = ll.getFirst().model.length();
    
    for (Product p : ll){
      if (p.model.length()>maxValue){maxValue=p.model.length();}
    }
    
    ListIterator<Product> iter = ll.listIterator();
    while( iter.hasNext() ){
      Product p = iter.next();
      if (p.model.length()>maxValue){ System.exit(0); } // This should not happen
      if (p.model.length()<maxValue){ iter.remove() ; }
    }
  }
  
  // Takes a list of products, figures out the minimal modelInd value, then removes the elements of the list 
  //   with modelInd value strictly bigger than the minimal value.
  public void min_modelIndLL(LinkedList<Product> ll) {
    int minValue = ll.getFirst().modelInd;
    
    for (Product p : ll){
      if (p.modelInd<minValue){minValue=p.modelInd;}
    }
    
    ListIterator<Product> iter = ll.listIterator();
    while( iter.hasNext() ){
      Product p = iter.next();
      if (p.modelInd<minValue){  System.exit(0); } // This should not happen
      if (p.modelInd!=minValue){ iter.remove();  }
    }
  }
  
  // Takes a list of products, figures out the minimal familyInd value, then removes the elements of the list 
  //   with familyInd value strictly bigger than the minimal value.
  public void min_familyIndLL(LinkedList<Product> ll) {
    ListIterator<Product> iter1 = ll.listIterator();
    int minValue = ll.getFirst().familyInd;
    
    while (minValue==-1 && iter1.hasNext()){ 
      minValue = iter1.next().familyInd;
    }
    
    if (minValue == -1){return;} // none of the products has family that appeared in the listing title
    for (Product p : ll){
      if (p.familyInd<minValue && p.familyInd!=-1){minValue=p.familyInd;}
    }
    
    ListIterator<Product> iter2 = ll.listIterator();
    while( iter2.hasNext() ){
      Product p = iter2.next();
      if (p.familyInd!=minValue){iter2.remove();}
    }
  }
  
  // This method takes a string as an input and gets rid of everything that come after one of the words: "for",
  //   "f\u00fcr" (the german translation of "for" with "u umlat" encoded to "\u00fc"),"pour" (the french 
  //   translation of "for"), "with", and "w/".
  public String titleModify(String adTitleL){
    if ( adTitleL.indexOf("for")     !=-1 ){ return adTitleL.substring(0,adTitleL.indexOf("for")     ); }
    if ( adTitleL.indexOf("with")    !=-1 ){ return adTitleL.substring(0,adTitleL.indexOf("with")    ); }
    if ( adTitleL.indexOf("f\u00fcr")!=-1 ){ return adTitleL.substring(0,adTitleL.indexOf("f\u00fcr")); }
    if ( adTitleL.indexOf("pour")    !=-1 ){ return adTitleL.substring(0,adTitleL.indexOf("pour")    ); }
    if ( adTitleL.indexOf("w/")      !=-1 ){ return adTitleL.substring(0,adTitleL.indexOf("w/")      ); }
    return adTitleL;
  }

  // print products
  public void print_allProds(){
    for (Map.Entry<String, LinkedList<Product>> entry : allProds.entrySet()) {
      System.out.print("\n=====================================================================\n");
      System.out.print(entry.getKey() + ": ----> ");
      LinkedList<Product> entryLL=entry.getValue();
      
      ListIterator iter = entryLL.listIterator();
      while( iter.hasNext() ){
        System.out.println( iter.next() );
      }
    }
  }
  
  // print listings
  public void print_ads(){
    ListIterator iter = ads.listIterator();
    System.out.print("\n\n *************************************************************************");
    System.out.print(  "\n *************************************************************************");
    while( iter.hasNext() ){
      System.out.println( iter.next() );
    }
  }

  
  /****************************************************************************************
  ********************************     MAIN     *******************************************
  ****************************************************************************************/
  public static void main(String[] args) {
    MainFile program=new MainFile("data/products.txt","data/listings.txt");
    
    program.matching();
    
  }
}
