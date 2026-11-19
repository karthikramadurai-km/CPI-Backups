import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    def body = message.getBody(java.lang.String) as String;
    //def map = message.getProperties();
	//def indicatorValue = map.get("indicator");
	//def logId = map.get("SAP_MessageProcessingLogID");
	//def entryId = logId.concat(indicatorValue);
	
	//message.setProperty("dsEntryID", entryId);
	
    def messageLog = messageLogFactory.getMessageLog(message);
    def mapH = message.getHeaders();
    def EnablePayloadLogging = mapH.get("EnablePayloadLogging");
    if(messageLog != null && EnablePayloadLogging == 'Y'){
        messageLog.addAttachmentAsString("Journal Entry Request Payload", body, "text/plain");
     }
    return message;
}