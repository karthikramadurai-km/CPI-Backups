import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    def body = message.getBody(java.lang.String) as String;
    def messageLog = messageLogFactory.getMessageLog(message);
    def map = message.getHeaders();
    def EnablePayloadLogging = map.get("EnablePayloadLogging");
    if(messageLog != null && EnablePayloadLogging == 'Y'){
        messageLog.addAttachmentAsString("CEF MultiMapping Input", body, "text/plain");
     }
    return message;
}