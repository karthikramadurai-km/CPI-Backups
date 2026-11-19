import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    def body = message.getBody(String)
    def json = new JsonSlurper().parseText(body)

    boolean isEmptyLaasResponse =
            json?.'@odata.context' == '$metadata#BPExceptionRequestForS4' &&
            json?.value instanceof List &&
            json.value.isEmpty() &&
            json.size() == 2

    message.setProperty("empty_laas_response", isEmptyLaasResponse.toString())

    return message
}