import com.sap.gateway.ip.core.customdev.util.Message;

import java.util.HashMap;

def Message processData(Message message) {



        def map = message.getHeaders();

        def  httpStatusCode = map.get("CamelHttpResponseCode") as Integer;

        def _loopIndex=message.getProperty("CamelLoopIndex") as Integer;

        def _propRetryLimit = message.getProperty("propRetryLimit") as Integer;

        def _propRetryInterval=message.getProperty("propRetryInterval") as Integer;

        def _retryFlag=false;

        

        switch(httpStatusCode==404)

        {

            case true:

            switch(_loopIndex+1<=_propRetryLimit) {

                case true:

                    _retryFlag=true;

                    sleep(_propRetryInterval);

                    break;

                case false:

                    _retryFlag=false;

                    //def ex = properties.get("CamelExceptionCaught");

                    throw new Exception("No results match your search criteria:"+map.get("CamelHttpResponseCode")+" response body:"+ message.getBody(java.lang.String));

                    break;

            }

            break;

            case false:

            _retryFlag=false;

            break;

        }

        

        message.setProperty("propIsRetry",_retryFlag)

       

       return message;

}