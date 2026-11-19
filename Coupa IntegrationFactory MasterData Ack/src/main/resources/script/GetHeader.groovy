import com.sap.it.api.mapping.*;


def String GetHeader(String arg1, MappingContext context){
	String Parameter = context.getHeader(arg1);

	 return Parameter;
}
def String GetProperty(String arg1, MappingContext context){
	String Parameter = context.getProperty(arg1);

	 return Parameter;
}