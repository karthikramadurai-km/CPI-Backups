import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def body = message.getBody(String)
    
    // Remove BOM if present
    body = body.replaceAll(/^\uFEFF/, "")
    
    // Remove any leading/trailing whitespace or newlines
    body = body.trim()
    
    // Remove any non-printable characters at the start
    while(body.size() > 0 && !body[0].matches("\\<|\\s*")) {
        body = body.substring(1)
    }
    
    message.setBody(body)
    return message
}