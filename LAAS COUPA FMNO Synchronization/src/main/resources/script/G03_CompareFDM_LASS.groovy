import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.util.XmlSlurper

def Message processData(Message message) {
    
    // Get CPI message log
    def log = messageLogFactory.getMessageLog(message)
    
    // === Step 1: Clean and store FDM payload ===
    def fdmBody = message.getProperty("FDM_StoredPayload")
    if(!fdmBody) {
        throw new Exception("FDM payload missing. Ensure G01_StoreFDMPayload ran before this step.")
    }
    fdmBody = fdmBody.replaceAll('^\uFEFF', '').trim() // remove BOM/whitespace
    def fdmJson = new JsonSlurper().parseText(fdmBody)
    
    // Extract FMNO list from FDM
    def fdmFMNOs = fdmJson.value.collect { it.fmno }
    
    // === Step 2: Clean LASS payload (current message body) ===
    def lassBody = message.getBody(String)
    if(!lassBody) {
        throw new Exception("LASS payload missing from HTTP/Request-Reply call.")
    }
    lassBody = lassBody.replaceAll('^\uFEFF', '').trim()
    
    // Parse LASS JSON
    def lassJson = new JsonSlurper().parseText(lassBody)
    def lassFMNOs = []
    if(lassJson.value) {
        lassFMNOs = lassJson.value.collect { it.fmno }
    }
    
    // === Step 3: Compare FDM vs LASS ===
    def missingInLASS = fdmFMNOs - lassFMNOs  // in FDM but not in LASS
    def extraInLASS   = lassFMNOs - fdmFMNOs  // in LASS but not in FDM
    
    // === Step 4: Log for monitoring ===
    if(log) {
        log.addAttachmentAsString("FDM_FMNOs", fdmFMNOs.toString(), "text/plain")
        log.addAttachmentAsString("LASS_FMNOs", lassFMNOs.toString(), "text/plain")
        log.addAttachmentAsString("MissingInLASS", missingInLASS.toString(), "text/plain")
        log.addAttachmentAsString("ExtraInLASS", extraInLASS.toString(), "text/plain")
    }
    
    // === Step 5: Set properties for downstream processing ===
    message.setProperty("MissingInLASS", missingInLASS)
    message.setProperty("ExtraInLASS", extraInLASS)
    message.setProperty("FDM_FMNOs", fdmFMNOs)
    message.setProperty("LASS_FMNOs", lassFMNOs)
    
    // === Optional: keep original LASS payload as message body ===
    message.setBody(lassBody)
    
    return message
}