import com.sap.it.api.mapping.*;

/*Add MappingContext parameter to read or set headers and properties
def String customFunc1(String P1,String P2,MappingContext context) {
         String value1 = context.getHeader(P1);
         String value2 = context.getProperty(P2);
         return value1+value2;
}

Add Output parameter to assign the output value.
def void custFunc2(String[] is,String[] ps, Output output, MappingContext context) {
        String value1 = context.getHeader(is[0]);
        String value2 = context.getProperty(ps[0]);
        output.addValue(value1);
        output.addValue(value2);
}*/

def void concatAllValuesInContext(String[] noteArray, Output output, MappingContext context) {
     def concatenatedString = '';
     if(noteArray.length < 2) // if there is only one note node, read the error message from it and dont skip
     concatenatedString = noteArray[0]
     else{
        for(int i=1; i<noteArray.length; i++) //Skip error message in note at index 1, so starting from i=1
        concatenatedString = concatenatedString.concat(noteArray[i]).concat('|'); 
     }
     
	output.addValue(concatenatedString);

}