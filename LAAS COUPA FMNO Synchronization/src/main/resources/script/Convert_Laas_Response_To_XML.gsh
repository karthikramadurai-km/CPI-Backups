import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.StreamingMarkupBuilder

def Message processData(Message message) {

    def json = new JsonSlurper().parseText(message.getBody(String))

    def xml = new StreamingMarkupBuilder().bind {

        LAAS_Response {

            json.value.each { rec ->

                Record {
                    ID(rec.ID)
                    fmno(rec.fmno)
                    businessPartner(rec.businessPartner)
                    supplier(rec.supplier)
                    activationRequest(rec.activationRequest)
                    approvedAt(rec.approvedAt)
                    systemStatus(rec.systemStatus)
                }
            }
        }
    }

    message.setBody(xml.toString())
    return message
}