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
      def FileName = message.getProperty('coupaFileName');
      def Datetime = message.getProperty('DateTime')
      //Fetch the request payload which is stored inside the property.
      def initialRequestPayload = message.getProperty("initialPayload")
      //Parse the payload
      def RequestPayloadlist = new XmlParser().parseText(initialRequestPayload)
    
      def flag1 = 0
      def serializenode
      def successData
      //store the incoming payload which the response payload from S4.
      def responsePayload = message.getBody(java.lang.String)
      //Parse the response payload
      def responsePay = new XmlParser().parseText(responsePayload)
      def flag = 0
      //Writer and builder are used for creating a new xml.
      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      def payload
      //declare empty lists which will be used to append the payload specific to each transaction.
      def ErrorRequestlistitem = '''<Invoices></Invoices>'''
      //def respPayload = '''<root></root>''' (not in use)
      def successPayload = '''<Invoices></Invoices>'''
      //Parse the declared lists.
      def ErrorRequestPayloadList = new XmlParser().parseText(ErrorRequestlistitem)  
      //def responseData = new XmlParser().parseText(respPayload) (not in use)
      def requestSuccessData = new XmlParser().parseText(successPayload)
        StringBuilder F1 = new StringBuilder()
         String F1Header = "\"" + "Source Invoice Number" + "\"" + "," + "\"" + "S4 Invoice Number" + "\"" + "," + "\"" + "Status" + "\"" + "," + "\"" + "Error Reason" +"\"" + "\r\n";

        F1.append(F1Header)
        String details
        
          StringBuilder F2 = new StringBuilder()
         String F2Header = "\"" + "Source Invoice Number" + "\"" + "," + "\"" + "S4 Invoice Number" + "\"" + "," + "\"" + "Status" + "\"" + "," + "\"" + "Details" +"\"" + "\r\n";

        F2.append(F2Header)
        String successdetails
        
 def sourceinvoice = '' , successMessage = '' , errorMessageFile = ''
//build xml with root tag as CEF.
         builder.CEF {
  
                'iFlowMessageId'(HeaderValue)
                'Event'('OK2PAY')
                'SourceIdentifier'(FileName)
            //Loop through each node of response payload using Index.
            responsePay.JournalEntryCreateConfirmation.eachWithIndex
            {rp,
            index->
            def errorMessage = ''

             //Loop through each log item. This block of code is used to concatenate the error messages. As there were multiple errors in single response node.
              rp.Log.Item.each
              {zr->
                errorMessage = errorMessage + ' ' + zr.Note.text()
              }
      
                //Bad Data 400 Error, Condition to check if the AccountingDocument is blank, which means an error has occurred.
                if(rp.JournalEntryCreateConfirmation.AccountingDocument.text() == ""){
                    
                //Multiple transactions will be build under root tag 'CEF'.
                   builder.Transaction {
                       'Status'('ERROR')
                    //RequestPayloadlist contains the request payload. Loop through each node of the request payload. If the index for response paylaod and request payload will match the node for request payload will be picked and appended in the list item element which is an empty list.
                    RequestPayloadlist.Invoice.eachWithIndex{lt,
                    index1->
                    
                    //Checking the index of request and response loop, If index matches means for the respective request there is a response for same index.
                    if(index==index1)
                    {
                        sourceinvoice = lt.id.text()
                    //Appending the node to the Empty List.    
                        ErrorRequestPayloadList.append(lt)
                    //serializing the node to add it in Request Payload.
                //    ErrorRequestPayloadList = ErrorRequestPayloadList.replace(/<?xml version="1.0" encoding="UTF-8"?>/,"")
                       serializenode = XmlUtil.serialize(ErrorRequestPayloadList);
                       
                       'RequestPayload'(serializenode)
                       flag = 1
                       'Level'("Critical")
                       'Code'('400')
                       'OccurredAt'(Datetime)
                     //  responseData.append(rp). (not in use)
                      // serializeData = XmlUtil.serialize(responseData) (not in use)
                       'ResponsePayload'()
                       'Message'(errorMessage)
                       
                       
                              
                    rp.Log.Item.each
                    {qr->
                      errorMessageFile = qr.Note.text()
                      
                       details = "\""+sourceinvoice+"\""+ ","+"\""+rp.JournalEntryCreateConfirmation.AccountingDocument.text()+"\""+","+"\""+'Error'+"\""+","+"\""+errorMessageFile+"\""+"\r\n"
                    F1.append(details)
           
                        
                    }
                   
                       
                    }

                    }
             
                       
                   }
                   
                }
                //flag condition is used to check if the code block is executed. If it is executed and the value is equal to one . The list is made empty.
                if(flag == 1){
                    ErrorRequestPayloadList = '''<Invoices></Invoices>'''
                    ErrorRequestPayloadList = new XmlParser().parseText(ErrorRequestPayloadList)  
                    //respPayload = '''<Invoices></Invoices>'''
             //   responseData = new XmlParser().parseText(respPayload) (not in use)
                errorMessage = ''
              //   message.setProperty("successPayload","")
                }
                
                //code block for success records.
                if(rp.JournalEntryCreateConfirmation.AccountingDocument.text() != ""){
                   
               RequestPayloadlist.Invoice.eachWithIndex{lt,
                    index2->
                    if(index==index2){
                        requestSuccessData.append(lt)
                         successData = XmlUtil.serialize(requestSuccessData);
                         message.setProperty("successPayload",successData)
                        flag1 = 1
                        
                        successMessage = rp.Log.Item.Note.text()
                        sourceinvoice = lt.id.text()
                    }
                  
               }
                   builder.Transaction {
                     'Status'('SUCCESS')
                        'ResponsePayload'()
                        'Level'()
                         'Code'('200')
                         'OccurredAt'(Datetime) 
                       'Message'(rp.JournalEntryCreateConfirmation.AccountingDocument.text())
                       'RequestPayload'(successData)
                     
                       
                        successdetails = "\""+sourceinvoice+"\""+ ","+"\""+rp.JournalEntryCreateConfirmation.AccountingDocument.text()+"\""+","+"\""+'Success'+"\""+","+"\""+successMessage+"\""+"\r\n"
                    F2.append(successdetails)
                       
                   }
                }
                else{
                    if(flag1==1){
                        message.setProperty("successPayload",successData)
                        
                    successPayload = '''<Invoices></Invoices>'''
                    requestSuccessData = new XmlParser().parseText(successPayload)  
                    }
                    else{
                        message.setProperty("successPayload","")
                    }
                }
              
            }

}
message.setProperty("errorfile",F1.toString())
message.setProperty("successfile",F2.toString())
//writer = writer.toString()
//def str = writer.replaceAll(/<?xml version="1.0" encoding="UTF-8"?>/,"")
 message.setBody(writer.toString())
    return message
}