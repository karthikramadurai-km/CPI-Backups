import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;

def Message processData(Message message) {
	
	def body= message.getBody(String.class);
	def header_data = body.substring(0, body.indexOf('\n'));
 
	if (header_data != '')
	{
	
	 header_data = header_data.replace('\r','');
	 header_data = header_data.replace('\n','');
	 
 	 message.setProperty("P_FHeader", header_data);
 	 body = body.substring(body.indexOf('\n')+1);
    }

	message.setBody(body);
	return message;
}