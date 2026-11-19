import com.sap.gateway.ip.core.customdev.util.Message
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

def Message processData(Message message) {

    LocalDate targetDate = LocalDate.now(ZoneOffset.UTC).minusDays(365)

    def startDateTime = targetDate.atStartOfDay().atOffset(ZoneOffset.UTC)
    def endDateTime = targetDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

    def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    def query = "\$filter=terminationDate ge ${startDateTime.format(formatter)} and terminationDate lt ${endDateTime.format(formatter)} and workerStatus eq 'WITHDRAWN'"

    message.setProperty("query", query)

    return message
}