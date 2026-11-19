import com.sap.it.api.mapping.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*
import java.lang.*;
import java.util.*;

def String calculateDays(String P1,String P2) {
         String inpStartDate =P1.toString();
         String inpEndDate = P2.toString();
         
         SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
         Date EmpStartDate = sdf.parse(inpStartDate);
         Date EmpEndDate = sdf.parse(inpEndDate); 


         //Use dateDiffInDays to get difference in days
         //def daysDifference = EmpStartDate.minus(EmpEndDate)
         def daysDifference=EmpStartDate - EmpEndDate
       
         return daysDifference.toString();
}
