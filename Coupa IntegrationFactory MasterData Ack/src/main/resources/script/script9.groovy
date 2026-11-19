import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import groovy.xml.*

def Message processData(Message message) {
    
      def HeaderValue = message.getHeader('SAP_MessageProcessingLogID', String); 
      HeaderValue= HeaderValue.toString();
      //property for storing MPL ID.
      message.setProperty("MPL", HeaderValue);
      def Datetime = message.getProperty('DateTime')
      def successdata = message.getProperty('initialPayload')
      //Fetch the request payload which is stored inside the property.
      def initialRequestPayload = message.getProperty("initialPayload")
      //Parse the payload
      def RequestPayloadlist = new XmlParser().parseText(initialRequestPayload)
    
      def flag1 = 0
      def serializenode
      def successData
      def responsePayload = message.getBody(java.lang.String)
      //Parse the response payload
      def responsePay = new XmlParser().parseText(responsePayload)
      def flag = 0
      //Writer and builder are used for creating a new xml.
      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      def payload
      def successPayload = '''<root></root>'''
      def requestSuccessData = new XmlParser().parseText(successPayload)

def rownumber
int rownum
//build xml with root tag as CEF.
         builder.CEF {
 
          RequestPayloadlist.Record.eachWithIndex
                  {rp,
                    index->
                 builder.Transaction{
                  'Status' ('ERROR')
                   'ResponsePayload'('NULL')
                         'Level'('NULL')
                          'Code'('404')
                        'OccurredAt'(Datetime)
                      'Message'('All Records Processed Successfully')
                      requestSuccessData.append(rp)
                       serializenode = XmlUtil.serialize(requestSuccessData);
                      
                          'RequestPayload'(serializenode)
                          flag = 1
                
                  }
                  if(flag == 1){
                    requestSuccessData = '''<root></root>'''
                    requestSuccessData = new XmlParser().parseText(requestSuccessData) 
                     respPayload = '''<Record></Record>'''
             
                
                   }
          }
      
         
         }
 message.setBody(writer.toString())
    return message

}