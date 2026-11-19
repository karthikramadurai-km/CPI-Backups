/*
Purpose : Construct filter query for FDM based on Manual run, Scheduled Run using lastrun date and calculated dates of 365 days earlier
Author : Senthil Kumar, Jothiraman (Senthil_Kumar_Jothiraman@external.mckinsey.com)
Date : 31-07-2026
*/

import com.sap.gateway.ip.core.customdev.util.Message
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

def Message processData(Message message) {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    LocalDate today = LocalDate.now(ZoneOffset.UTC)
    LocalDate yesterday = today.minusDays(1)

    String query = ""

    //==========================================================================
    // Manual Run
    //==========================================================================

    boolean manualRun = "true".equalsIgnoreCase(message.getProperty("manualRun")?.toString())

    if (manualRun) {

        String manualRunDate = message.getProperty("manualRunDate") == null ? "" : message.getProperty("manualRunDate").toString().trim()
        String manualRunDateRange = message.getProperty("manualRunDateRange") == null ? "" : message.getProperty("manualRunDateRange").toString().trim()

        if (manualRunDate.length() > 0) {

            query = "\$filter=terminationDate eq '${manualRunDate}' and (workerStatus eq 'WITHDRAWN' or workerStatus eq 'RETIRE')"

        } else if (manualRunDateRange.length() > 0) {

            def dates = manualRunDateRange.split(",")

            if (dates.length != 2) {
                throw new Exception("manualRunDateRange should be in format yyyyMMdd,yyyyMMdd")
            }

            String fromDate = dates[0].trim()
            String toDate = dates[1].trim()

            if (fromDate.length() == 0 || toDate.length() == 0) {
                throw new Exception("manualRunDateRange should contain two valid dates")
            }

            query = "\$filter=terminationDate ge '${fromDate}' and terminationDate le '${toDate}' and (workerStatus eq 'WITHDRAWN' or workerStatus eq 'RETIRE')"

        } else {

            throw new Exception("Either manualRunDate or manualRunDateRange must contain a value when manualRun=true")

        }

    }

    //==========================================================================
    // Scheduled Run
    //==========================================================================

    else {

        String lastRunDateStr = message.getProperty("lastRunDate") == null ? "" : message.getProperty("lastRunDate").toString().trim()

        if (lastRunDateStr.length() == 0) {
            throw new Exception("Property lastRunDate is missing")
        }

        LocalDate lastRunDate = LocalDate.parse(lastRunDateStr, formatter)

        if (lastRunDate.isEqual(yesterday)) {

            // Normal daily run
            LocalDate targetDate = today.minusDays(365)

            query = "\$filter=terminationDate eq '${targetDate.format(formatter)}' and (workerStatus eq 'WITHDRAWN' or workerStatus eq 'RETIRE')"

        } else {

            // Catch-up run
            LocalDate fromDate = lastRunDate.minusDays(365)
            LocalDate toDate = today.minusDays(365)

            query = "\$filter=terminationDate ge '${fromDate.format(formatter)}' and terminationDate le '${toDate.format(formatter)}' and (workerStatus eq 'WITHDRAWN' or workerStatus eq 'RETIRE')"

        }

    }

    message.setProperty("query", query)

    return message
}