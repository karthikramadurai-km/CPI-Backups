import com.sap.it.api.mapping.*;

def String customFunc(String arg1){
	return arg1 
}

def String getBusinessPlace(String InputString){
    def output = '';
    if(InputString.contains('|')){
      List<String[]> str = InputString.split("\\|")
      output = str[1];  
    }else
    output = InputString;
    
	return output;
}