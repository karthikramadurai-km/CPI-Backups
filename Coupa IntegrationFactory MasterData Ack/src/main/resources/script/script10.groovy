import com.sap.gateway.ip.core.customdev.util.Message;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.AccessTokenAndUser;
import com.sap.it.api.securestore.UserCredential;
import groovy.xml.*;

def Message setProperty_CoupaLookup(Message message) {
    
    def service = ITApiFactory.getApi(SecureStoreService.class, null);

    def propertyMap = message.getProperties();
    
    def CoupaHost = service.getUserCredential("Coupa_Host");              
  
    String Coupa_Host = new String(CoupaHost.getPassword());
   

    message.setProperty("CoupaHost",Coupa_Host);             
 
    
    if (Coupa_Host == null) {
    throw new IllegalStateException("No Secure Parameter configured for Coupa_Host name");
    }
return message;

}