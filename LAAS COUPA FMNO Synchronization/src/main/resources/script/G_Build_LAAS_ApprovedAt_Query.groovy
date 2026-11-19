import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {

    String manualRun = message.getProperty("manualrun")
        ?.toString()
        ?.trim()

    String manualDate = message.getProperty("lastRunDate")
        ?.toString()
        ?.trim()

    String storedLastRun = message.getProperty("lastRunValue")
        ?.toString()
        ?.trim()

    String selectedDate = "true".equalsIgnoreCase(manualRun)
        ? manualDate
        : storedLastRun

    if (!selectedDate) {
        throw new Exception(
            "LAAS query date is missing. Check manualrun, lastRunDate and lastRunValue."
        )
    }

    selectedDate = selectedDate
        .replace("\"", "")
        .replace("'", "")
        .trim()

    // Remove milliseconds because the LAAS endpoint may reject them.
    selectedDate = selectedDate.replaceFirst(/\.000Z$/, "Z")
    selectedDate = selectedDate.replaceFirst(/\.000$/, "")

    if (!selectedDate.endsWith("Z")) {
        selectedDate += "Z"
    }

    String laasQuery =
        "\$filter=approvedAt ge ${selectedDate}" +
        "&\$orderby=approvedAt desc"

    message.setProperty("LAAS_Query", laasQuery)
    message.setProperty("LAAS_QueryStartDate", selectedDate)

    def log = messageLogFactory.getMessageLog(message)

    if (log) {
        log.addAttachmentAsString(
            "Generated_LAAS_Query",
            laasQuery,
            "text/plain"
        )
    }

    return message
}