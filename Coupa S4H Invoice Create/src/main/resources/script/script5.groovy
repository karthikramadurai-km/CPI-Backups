import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
    
    def Message processData(Message message) {
        println "You can print and see the result in the console!"
        //Body 
        def body = message.getBody(String);
        body=body.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>","")
        message.setBody(body)
        return message;
        }
    