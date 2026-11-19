import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {

    String laasPayload =
        message.getProperty("LAAS_StoredPayload")?.toString()

    if (!laasPayload?.trim()) {
        throw new Exception(
            "Property LAAS_StoredPayload is missing or empty. " +
            "Check the previous Store LAAS Response step."
        )
    }

    laasPayload = laasPayload
        .replace("\uFEFF", "")
        .trim()

    if (!laasPayload.startsWith("{") &&
        !laasPayload.startsWith("[")) {
        throw new Exception(
            "Expected LAAS JSON payload, but payload starts with: " +
            laasPayload.take(150)
        )
    }

    def parsed = new JsonSlurper().parseText(laasPayload)

    def records = parsed instanceof Map
        ? (parsed.value ?: [])
        : parsed

    List<String> fmnoList = records
        .collect { record ->
            record?.fmno?.toString()?.trim()
        }
        .findAll { fmno ->
            fmno
        }
        .unique()

    if (fmnoList.isEmpty()) {
        throw new Exception(
            "No FMNO values found in LAAS_StoredPayload."
        )
    }

    String client =
        message.getProperty("client")?.toString()?.trim()

    if (!client) {
        throw new Exception(
            "Exchange property client is missing."
        )
    }

    String fmnoConditions = fmnoList.collect { fmno ->
        String safeFmno = fmno.replace("'", "''")
        "fmno eq '${safeFmno}'"
    }.join(" or ")

    String fdmQuery =
        "\$filter=(${fmnoConditions})" +
        " and latestRecordFlag eq 'Y'" +
        " and client eq '${client}'" +
        " and objectStatus ne 'G'"

    // This exact property is used by the FDM OData adapter
    message.setProperty("FDM_Query", fdmQuery)
    message.setHeader("FDM_Query", fdmQuery)
    message.setProperty(
        "LAAS_FMNO_List",
        fmnoList.join(",")
    )

    message.setProperty(
        "LAAS_FMNO_Count",
        fmnoList.size().toString()
    )

    def log = messageLogFactory.getMessageLog(message)

    if (log) {
        log.addAttachmentAsString(
            "Generated_FDM_Query",
            fdmQuery,
            "text/plain"
        )
    }

    return message
}