package src;
import org.json.simple.JSONObject;

public class Listing{
  final String title;
  final String manuf;
  final String curr ;
  final String price; 
  
  //constructor
  public Listing(String title,String  manuf,String  curr,String  price){
    this.title=title;
    this.manuf=manuf;
    this.curr =curr ;
    this.price=price;
  }
  
  //creates a JSONObject from this class
  public JSONObject createJSONObj(){
    JSONObject jo = new JSONObject();
    jo.put("title"       , title);
    jo.put("manufacturer", manuf);
    jo.put("currency"    , curr );
    jo.put("price"       , price);
    return jo;
  }
  
  public String toString() {
    return "[title:\""+title+"\" , manuf:\""+manuf+"\" , curr:\""+curr+"\" , price:\""+price+"\"]";
  }
  
}