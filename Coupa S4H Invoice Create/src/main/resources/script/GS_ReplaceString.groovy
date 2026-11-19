import com.sap.it.api.mapping.*;
import groovy.xml.XmlUtil
import groovy.xml.MarkupBuilder;
import com.sap.it.api.mapping.MappingContext

def String replaceString(String InputString, MappingContext context){
    
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer);
    def id = context.getProperty("messageId_bulk");
    def dateTime = context.getProperty("createDateTime_bulk")
    
    builder.'ns1:JournalEntryBulkCreateRequest'('xmlns:ns1': "http://sap.com/xi/SAPSCORE/SFIN"){
        'MessageHeader'() {
        'ID'(id)
        'CreationDateTime'(dateTime)
        }
    }  
    
    def parent = writer.toString()

    def recordWithParent = new XmlParser().parseText(parent)
    def recordWithoutParent = new XmlParser().parseText(InputString)

    recordWithParent.children().add(recordWithoutParent)
    String output = groovy.xml.XmlUtil.serialize( recordWithParent )

    return output
}

def String splitString(int seqNumber, String InputString){
    
    List<String[]> str = InputString.split("\\|")
    def output = str[seqNumber];
    
	return output
}


def void concatAllValuesInContext(String[] noteArray, Output output, MappingContext context) {
     def concatenatedString = '';
     for(int i=0; i<noteArray.length; i++)
        concatenatedString = concatenatedString.concat(noteArray[i]).concat('|');
	output.addValue(concatenatedString);

}

