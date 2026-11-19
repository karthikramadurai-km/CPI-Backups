import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    //Body 
    
    
       def body = message.getBody(String) as String;
      message.setBody(body.replaceAll("&lt;", "<").replaceAll("&gt;", ">"));
       return message;
}
