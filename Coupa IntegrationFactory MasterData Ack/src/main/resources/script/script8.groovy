import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
    
    
    def Message removeXMLDeclaration(Message message) {
    
        def body = message.getBody(String);
        body=body.replaceAll("<\\?xml version=\"1.0\" encoding=\"UTF-8\"\\?>","").trim()
        message.setBody(body)
        return message;
        }