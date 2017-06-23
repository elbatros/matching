package src;

public class Product{
  final String prod_name;
  final String manuf    ;
  final String model    ;
  final String family   ;
  final String ann_date ;
  
  // The next two variable will be used when necessary in "filterProds()" of "MainFile.java"
  public int modelInd =-1;
  public int familyInd=-1;
  
  // constructor
  public Product(String prod_name, String  manuf, String model, String family, String  ann_date){
    this.prod_name=prod_name;
    this.manuf=manuf        ;
    this.model=model        ;
    this.family=family      ; 
    this.ann_date=ann_date  ;
  }
  
  public String toString() {
    if ( family!=null ){
      return "[product_name:\""+prod_name+"\" , manufacturer:\""  +manuf   +"\" , model:\""+model
              +"\" , family:\""+family   +"\" , announced-date:\""+ann_date+"\"]";
    }else{
      return "[product_name:\""+prod_name+"\" , manufacturer:\""  +manuf   +"\" , model:\""+model
                                         +"\" , announced-date:\""+ann_date+"\"]";
    }
  }
  
}