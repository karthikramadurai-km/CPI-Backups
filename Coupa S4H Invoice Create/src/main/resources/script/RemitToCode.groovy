import com.sap.it.api.mapping.*;

def String remitCodeForField(int field, String remitToCode){
    
    List<String[]> str = remitToCode.split("\\|")
    def output = str[field];
    
	return output
}