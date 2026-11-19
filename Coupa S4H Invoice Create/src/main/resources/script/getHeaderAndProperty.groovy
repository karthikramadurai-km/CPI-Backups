import com.sap.it.api.mapping.*;

def String GetProperty(String propertyName, MappingContext context){
	String Parameter = context.getProperty(propertyName);
	
	 return Parameter;
}