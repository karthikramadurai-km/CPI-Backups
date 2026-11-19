import com.sap.gateway.ip.core.customdev.util.Message;
import com.sap.it.api.ITApiFactory;
import com.sap.it.api.securestore.SecureStoreService;
import com.sap.it.api.securestore.AccessTokenAndUser;
import com.sap.it.api.securestore.UserCredential;
import java.util.HashMap;

def Message processData(Message message) {
    def service = ITApiFactory.getApi(SecureStoreService.class, null);
    
    def S4HANAHost = service.getUserCredential("S4_Host");   
    def S4HANAClient = service.getUserCredential("S4_Client"); 
      
    String S4_HANA_Host = new String(S4HANAHost.getPassword());
    String S4_HANA_Client = new String(S4HANAClient.getPassword());
    
    if (S4_HANA_Host != null)
     message.setProperty("S4_Host",S4_HANA_Host);
     else
    throw new IllegalStateException("No Secure Parameter configured for S4_Host name");
    
    if (S4_HANA_Client != null)
     message.setProperty("S4_Client",S4_HANA_Client);
     else
    throw new IllegalStateException("No Secure Parameter configured for S4_Client name");

    return message;   
}
