import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    String body = message.getBody(String)

    message.setProperty("LAAS_StoredPayload", body ?: "")

    return message
}