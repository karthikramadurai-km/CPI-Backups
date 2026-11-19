import com.sap.it.api.mapping.*;


def String GetHeader(String headerName, MappingContext context){
	String Parameter = context.getHeader(headerName);

	 return Parameter;
}
def String GetProperty(String propertyName, MappingContext context){
	String Parameter = context.getProperty(propertyName);
	
	 return Parameter;
}