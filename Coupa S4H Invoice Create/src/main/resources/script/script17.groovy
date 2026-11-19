import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    def body = message.getBody(java.lang.String) as String;
    def map = message.getHeaders();
    def EnablePayloadLogging = map.get("EnablePayloadLogging");
    def messageLog = messageLogFactory.getMessageLog(message);
    if(messageLog != null && EnablePayloadLogging == 'Y'){
        messageLog.addAttachmentAsString("Exception Message", body, "text/plain");
     }
    return message;
}