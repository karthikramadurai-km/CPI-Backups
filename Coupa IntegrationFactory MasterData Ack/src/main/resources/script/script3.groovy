import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    //Body 
    
    
       def body = message.getBody(String) as String;
     message.setBody(body.replaceAll("upload-error","upload_error").replaceAll("data-file-source","data_file_source").replaceAll("field-error","field_error"));
       return message;
}
