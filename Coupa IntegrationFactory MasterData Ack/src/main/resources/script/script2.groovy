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
      def errorField = message.getProperty('errorField')
      
      def successdata = message.getProperty('initialPayload') 
      //Fetch the request payload which is stored inside the property.
      def initialRequestPayload = message.getProperty("initialPayload")
      //Parse the payload
      def RequestPayloadlist = new XmlParser().parseText(initialRequestPayload)
    
      def flag1 = 0
      def flag2=0
      def serializenode
      def successData
      //store the incoming payload with the response payload from S4.
      def responsePayload = message.getBody(java.lang.String)
      //Parse the response payload
      def responsePay = new XmlParser().parseText(responsePayload)
      def flag = 0
      //Writer and builder are used for creating a new xml.
      def writer = new StringWriter()
      def builder = new MarkupBuilder(writer)
      def payload
      //declare empty lists which will be used to append the payload specific to each transaction.
      def ErrorRequestlistitem = '''<root></root>'''
      //def respPayload = '''<root></root>''' (not in use)
      def successPayload = '''<root></root>'''
      //Parse the declared lists.
      def ErrorRequestPayloadList = new XmlParser().parseText(ErrorRequestlistitem)  
      //def responseData = new XmlParser().parseText(respPayload) (not in use)
      def requestSuccessData = new XmlParser().parseText(successPayload)

def rownumber
int rownum

      def ErrMessage = '''<root></root>'''
      def ErrMessageData = new XmlParser().parseText(ErrMessage)
      def flag5 = 0

def indexList =[]
def successList =[]
def errorList=[]

//build xml with root tag as CEF.
builder.CEF {
        if(responsePay.data_file_source.upload_errors.upload_error.id.text()!= ''){
          //     row = 2, 3
            //   index = 0 , 1
            def i
            def totalrecords = responsePay.data_file_source.progress.text()
            
            totalrecords = totalrecords.toInteger()
            message.setProperty("records",totalrecords)
            for(def j=2; j<=totalrecords+1; j++){
                indexList.add(j);
            }
            
            RequestPayloadlist.Record.eachWithIndex
                   {rp,
                    index->
                    
                def errorMessage = responsePay.data_file_source.upload_errors.upload_error.field_errors.field_error.text()
                   
            //          message.setProperty("errorMessage",errorMessage)
            //          rp.Log.Item.each
            //   {zr->
            //     errorMessage = errorMessage + ' ' + zr.Note.text()
              
            //   }
                    responsePay.data_file_source.upload_errors.upload_error.each{
                        er->
                   rownumber = er.row.text()
                   rownum = rownumber.toInteger()
                   rownum = rownum-2
                   message.setProperty("rownum",rownum)
           
                 if(rownum == index){
                     message.setProperty("true","true")
                     errorList.add(rownum+2);
                builder.Transaction{
                        'Status' ('ERROR')
                        ErrorRequestPayloadList.append(rp)
                        serializenode = XmlUtil.serialize(ErrorRequestPayloadList);
                        'RequestPayload'(serializenode)
                        flag = 1
                        'Code'('400')
                        er.field_errors.field_error.each{
                        err->
                        def e_msg= err.value().text()
                            errorMessage = errorMessage + ' ' + err.Note.text()
                            //ErrMessageData.append(err.text())
                            'Message'(e_msg)
                            serializenode5 = XmlUtil.serialize(ErrMessageData);
                            //'Message'(serializenode5)
                            flag5 = 1
              }
                        flag1=1
                  }
            //         message.setProperty("errorMessage",errorMessage)
                     
            //          responsePay.data_file_source.upload_errors.upload_error.field_errors.field_error.each{
            //             err->
            //                 errorMessage = errorMessage + ' ' + err.Note.text()
            //                 ErrMessageData.append(err)
            //                 serializenode5 = XmlUtil.serialize(ErrMessageData);
            //                 'Message'(serializenode5)
            //                 flag5 = 1
            //   }
                     
                   } 
                   
                   if(flag5 == 1){
                    //ErrMessageData = '''<root></root>'''
                    ErrMessageData = (ErrMessageData) 
                    // respPayload = '''<Record></Record>'''

                   }
                 }
                    message.setProperty("errorList",errorList)
                  if(flag == 1){
                    ErrorRequestPayloadList = '''<root></root>'''
                    ErrorRequestPayloadList = new XmlParser().parseText(ErrorRequestPayloadList) 
                    //resp    Payload = '''<Record></Record>'''
                    
                errorMessage = ''
                   }
                
                  }
                  for(def x=0; x<errorList.size; x++){
                    for(def y=0; y<indexList.size; y++){
                        if(!errorList.contains(indexList[y])){
                            successList.add(indexList[y])
                        }
                    }
                }
                def newList = successList.unique()
                message.setProperty("successList",newList)
                
                // to build a SUCCESS Transaction
                
                RequestPayloadlist.Record.eachWithIndex
                {res,
                    index->
                        for(item in successList){
                            def newItem = item -2;
                            if(newItem == index){
                                builder.Transaction{
                                'Status' ('SUCCESS')
                                requestSuccessData.append(res)
                                serializenode = XmlUtil.serialize(requestSuccessData);
                                'RequestPayload'(serializenode)
                                flag2 = 1
                                'Code'('200')  
                                 'Message'('Record processed successfully')
                                }
                            } 
                        }
                        if(flag2 == 1){
                            requestSuccessData = '''<root></root>'''
                            requestSuccessData = new XmlParser().parseText(requestSuccessData) 
                            resPayload = '''<Record></Record>'''
                        }
                    }
      }
        
}

 message.setBody(writer.toString())
    return message

}