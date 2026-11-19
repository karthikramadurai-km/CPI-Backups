/* Script Author: Shweta Rana
Info: This script converts Coupa CSV file into XML format*/

import groovy.xml.*;
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
def Message processData(Message message) {
    def body = message.getBody(java.lang.String) as String;
    def messageLog = messageLogFactory.getMessageLog(message);
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    def rows = getRows(body)
    def listNodes = getListNodes(rows)
    generateXML(xml, listNodes)
    def output = writer.toString()
    output = output.replace("\"", "");
    message.setBody(output);
    return message
}


def getRows(body) {
     // body = body.replace("\""+"\"", "blank");
      //body = body.replace("\"", ""); 

    String[] lines = body.split("\\r?\\n").drop(4);
    List<String[]> rows = lines.collect {it.split(',(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)', -1)};
    return rows;
}

def getListNodes(input) {
    def listOfNodes = [];
    for (int i = 0; i < input.size(); i++) {
        if (input[i][0] == "Header") { 
            def HeaderID = input[i][1];
            def obj = [
                          InvoiceHeader: input[i],
                          LineItem : [],
                          TaxLine : []
                      ]
            
            for (int j = i + 1; j < input.size(); j++) {
                if (input[j][0] == "Line" && input[j][1] == HeaderID) {
                    obj.LineItem.add(input[j])
                } 
                else if(input[j][0] == "TaxLine" && input[j][1] == HeaderID) {
                    obj.TaxLine.add(input[j])
                }
                else {
                    i = j - 1;
                    break;
                }
            }
            listOfNodes.add(obj)
        }
    }

    return listOfNodes
}

def generateXML(xml, listNodes) {
    listNodes.each {it ->
                    def currHeader = it.get('InvoiceHeader')
                    def currNode = it.get('LineItem')
                    def taxNode = it.get('TaxLine')

    xml.Invoice {                          
           type(currHeader[0]);id(currHeader[1]);created_at(currHeader[2]);updated_at(currHeader[3]);status(currHeader[4]);internal_note(currHeader[5]);invoice_date(currHeader[6]);invoice_number(currHeader[7]);line_level_taxation(currHeader[8]);supplier_note(currHeader[9]);header_tax_amount(currHeader[10]);header_tax_rate(currHeader[11]);header_tax_code(currHeader[12]);paid(currHeader[13]);payment_date(currHeader[14]);payment_notes(currHeader[15]);exported(currHeader[16]);account_type_id(currHeader[17]);account_type_name(currHeader[18]);payment_term(currHeader[19]);handling_amount(currHeader[20]);misc_amount(currHeader[21]);shipping_amount(currHeader[22]);total(currHeader[23]);currency(currHeader[24]);accounting_total_amount(currHeader[25]);accounting_total_currency(currHeader[26]);supplier_id(currHeader[27]);supplier_name(currHeader[28]);supplier_number(currHeader[29]);created_by_email(currHeader[30]);created_by_employee_number(currHeader[31]);created_by_login(currHeader[32]);updated_by_email(currHeader[33]);updated_by_employee_number(currHeader[34]);updated_by_login(currHeader[35]);attachment_texts(currHeader[36]);comments(currHeader[37]);remit_to_address_code(currHeader[38]);remit_to_address_attention(currHeader[39]);remit_to_address_street1(currHeader[40]);remit_to_address_street2(currHeader[41]);remit_to_address_city(currHeader[42]);remit_to_address_state(currHeader[43]);remit_to_address_postal_code(currHeader[44]);remit_to_address_country_code(currHeader[45]);remit_to_address_country_name(currHeader[46]);remit_to_address_name(currHeader[47]);remit_to_address_vat_number(currHeader[48]);remit_to_address_vat_country_code(currHeader[49]);remit_to_address_vat_country_name(currHeader[50]);supplier_vat_number(currHeader[51]);supplier_vat_country_code(currHeader[52]);document_type(currHeader[53]);original_invoice_number(currHeader[54]);original_invoice_date(currHeader[55]);bill_to_address_legal_entity_name(currHeader[56]);bill_to_address_attention(currHeader[57]);bill_to_address_street1(currHeader[58]);bill_to_address_street2(currHeader[59]);bill_to_address_city(currHeader[60]);bill_to_address_state(currHeader[61]);bill_to_address_postal_code(currHeader[62]);bill_to_address_country_code(currHeader[63]);bill_to_address_country_name(currHeader[64]);bill_to_address_name(currHeader[65]);ship_to_address_attention(currHeader[66]);ship_to_address_street1(currHeader[67]);ship_to_address_street2(currHeader[68]);ship_to_address_city(currHeader[69]);ship_to_address_state(currHeader[70]);ship_to_address_postal_code(currHeader[71]);ship_to_address_country_code(currHeader[72]);ship_to_address_country_name(currHeader[73]);ship_to_address_name(currHeader[74]);supplier_remit_to_code(currHeader[75]);supplier_remit_to_supplier_name(currHeader[76]);supplier_remit_to_street1(currHeader[77]);supplier_remit_to_street2(currHeader[78]);supplier_remit_to_city(currHeader[79]);supplier_remit_to_state(currHeader[80]);supplier_remit_to_postal_code(currHeader[81]);supplier_remit_to_country_code(currHeader[82]);supplier_remit_to_country_name(currHeader[83]);discount_due_date(currHeader[84]);net_due_date(currHeader[85]);discount_amount(currHeader[86]);margin_scheme(currHeader[87]);cash_accounting_scheme_reference(currHeader[88]);registered_company_type(currHeader[89]);registered_company_managing_directors(currHeader[90]);esr_number(currHeader[91]);gl_date(currHeader[92]);barcode(currHeader[93]);description(currHeader[94]);payment_method(currHeader[95]);contact_name(currHeader[96]);last_approver(currHeader[97]);facta(currHeader[98]);charge_id(currHeader[99]);image_now_id(currHeader[100]);custom_field_11(currHeader[101]);custom_field_12(currHeader[102]);custom_field_13(currHeader[103]);custom_field_14(currHeader[104]);please_enter_your_name(currHeader[105]);oracle_invoice_status(currHeader[106]);exception_routing(currHeader[107]);emergency(currHeader[108]);po(currHeader[109]);invoice_total(currHeader[110]);approver_charge_code(currHeader[111]);approver_fmno(currHeader[112]);ed_for_approvals(currHeader[113]);exported_to_amex(currHeader[114]);Channel(currHeader[115]);
        
        LineItems{
            currNode.each {
                it1 ->

                LineItem {
                    type(it1[0]);invoice_id(it1[1]);invoice_line_id(it1[2]);created_at(it1[3]);updated_at(it1[4]);total(it1[5]);accounting_total(it1[6]);accounting_total_currency(it1[7]);line_num(it1[8]);description(it1[9]);order_header_num(it1[10]);order_header_requested_by_email(it1[11]);order_header_requested_by_employee_number(it1[12]);order_header_requested_by_login(it1[13]);order_line_id(it1[14]);order_line_num(it1[15]);order_line_commodity(it1[16]);price(it1[17]);quantity(it1[18]);uom(it1[19]);status(it1[20]);tax_rate(it1[21]);tax_location(it1[22]);tax_code(it1[23]);tax_amount(it1[24]);tax_description(it1[25]);tax_supply_date(it1[26]);account_code(it1[27]);account_active(it1[28]);billing_notes(it1[29]);segment_1(it1[30]);segment_2(it1[31]);segment_3(it1[32]);segment_4(it1[33]);segment_5(it1[34]);segment_6(it1[35]);segment_7(it1[36]);segment_8(it1[37]);segment_9(it1[38]);segment_10(it1[39]);segment_11(it1[40]);segment_12(it1[41]);segment_13(it1[42]);segment_14(it1[43]);segment_15(it1[44]);segment_16(it1[45]);segment_17(it1[46]);segment_18(it1[47]);segment_19(it1[48]);segment_20(it1[49]);account_name(it1[50]);created_by_email(it1[51]);created_by_employee_number(it1[52]);created_by_login(it1[53]);updated_by_email(it1[54]);updated_by_employee_number(it1[55]);updated_by_login(it1[56]);distributed_tax_amount(it1[57]);distributed_shipping_amount(it1[58]);distributed_handling_amount(it1[59]);distributed_misc_amount(it1[60]);personal_charge(it1[61]);charge_code(it1[62]);fmno(it1[63]);asset_model_number(it1[64]);serial(it1[65]);warranty_number(it1[66]);cip(it1[67]);custom_field_8(it1[68]);custom_field_9 (it1[69]);custom_field_10(it1[70]);tax_jurisdiction_code(it1[71]);
                }
            }
        }
        
        TaxLines{
            taxNode.each {
                it2 ->

                Taxline {
                    type(it2[0]);invoice_id(it2[1]);invoice_line_id(it2[2]);line_num(it2[3]);order_header_num(it2[4]);order_line_id(it2[5]);order_line_num(it2[6]);account_allocation_id(it2[7]);account_allocation_sequence(it2[8]);account_allocation_amount(it2[9]);account_allocation_percent(it2[10]);account_code(it2[11]);account_active(it2[12]);billing_notes(it2[13]);segment_1(it2[14]);segment_2(it2[15]);segment_3(it2[16]);segment_4(it2[17]);segment_5(it2[18]);segment_6(it2[19]);segment_7(it2[20]);segment_8(it2[21]);segment_9(it2[22]);segment_10(it2[23]);segment_11(it2[24]);segment_12(it2[25]);segment_13(it2[26]);segment_14(it2[27]);segment_15(it2[28]);segment_16(it2[29]);segment_17(it2[30]);segment_18(it2[31]);segment_19(it2[32]);segment_20(it2[33]);account_name(it2[34]);currency_code(it2[35]);accounting_total(it2[36]);accounting_currency(it2[37]);distributed_tax_amount(it2[38]);distributed_shipping_amount(it2[39]);distributed_handling_amount(it2[40]);distributed_misc_amount(it2[41]);custom_field_1(it2[42]);custom_field_2(it2[43]);custom_field_3(it2[44]);custom_field_4(it2[45]);custom_field_5(it2[46]);custom_field_6(it2[47]);custom_field_7(it2[48]);custom_field_8(it2[49]);custom_field_9(it2[50]);custom_field_10(it2[51]);
                }
            }
        }
        
        
    }
                   }
                  
}
