import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import groovy.xml.*

def Message processData(Message message) {
    //Body
    def body = message.getBody();
//    message.setBody(body + " Body is modified");
    //Headers
    def headers = message.getHeaders();

    //Properties
    def properties = message.getProperties();
    def initialRequestPayload = message.getProperty("initialPayload")
     def RequestPayloadlist = new XmlParser().parseText(initialRequestPayload)
     def exceptionmessage = message.getProperty("exceptionMessage")
     
     StringBuilder F1 = new StringBuilder()
         String F1Header = "\"" + "Source Invoice Number" + "\"" + "," + "\"" + "S4 Invoice Number" + "\"" + "," + "\"" + "Status" + "\"" + "," + "\"" + "Error Reason" +"\"" + "\r\n";

        F1.append(F1Header)
        String details
        
         RequestPayloadlist.Invoice.each
         {lt->
         
             sourceinvoice = lt.id.text()
                details = "\""+sourceinvoice+"\""+ ","+"\""+''+"\""+","+"\""+'Error'+"\""+","+"\""+exceptionmessage+"\""+"\r\n"
                    F1.append(details)
         }
   
        
  //  message.setProperty("oldProperty", value + " modified");
    message.setProperty("ExceptionErrorFile", F1.toString())
 //   message.setBody(F1.toString())
    return message;
}