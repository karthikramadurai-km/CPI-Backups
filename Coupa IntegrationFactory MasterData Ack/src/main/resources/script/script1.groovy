import com.sap.gateway.ip.core.customdev.util.Message;
def Message processData(Message message) {
    def map = message.getProperties();
    def ex = map.get("CamelExceptionCaught");
    if (ex!=null) {
        // an http adapter throws an instance of org.apache.camel.component.ahc.AhcOperationFailedException
        if (ex.getClass().getCanonicalName().equals("org.apache.camel.component.ahc.AhcOperationFailedException")) {
            def messageLog = messageLogFactory.getMessageLog(message);
            messageLog.addAttachmentAsString("http.ResponseBody", ex.getResponseBody(), "text/plain");
            message.setProperty("http.ResponseBody",ex.getResponseBody());
            message.setBody(ex.getResponseBody());
            message.setProperty("http.StatusCode",ex.getStatusCode());
            message.setProperty("http.StatusText",ex.getStatusText());
        }
    }
    return message;
}