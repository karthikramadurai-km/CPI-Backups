import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

def Message buildQuery(Message message) {
    def currentDate = message.getProperty("dateTime")
    def LRD = message.getProperty("lastRunDate")
    def lastrundate = message.getProperty("lastRunValue")
   // def manualrun = message.getProperty("manualrun")
    def logStart
    
    if(LRD == ''){
      
     logStart =  lastrundate
      }
      else{
      logStart = LRD
      }
    message.setProperty("logStart",logStart)
    
    return message;
}
