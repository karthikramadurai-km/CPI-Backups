/* Script Author: Shweta Rana
Created Date: 3rd Oct, 2022*/

import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.xml.MarkupBuilder;
import groovy.xml.StreamingMarkupBuilder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import groovy.xml.*;
import java.text.SimpleDateFormat;

def Message processData(Message message){
    def body = message.getBody(java.lang.String) as String;
    def root = new XmlParser().parseText(body);
    def FileName = '';
    FileName = message.getProperty('coupaFileName');

    def JEBulkRequestHeader = '<ns1:JournalEntryBulkCreateRequest xmlns:ns1 = "http://sap.com/xi/SAPSCORE/SFIN">' + '\n' +
                        msgHeaderBulk() + '\n';
    def JERequest = '';
    root.Invoice.each{
        it1 ->
        JERequest = JERequest +
                    '<JournalEntryCreateRequest>' + '\n' +
                       msgHeaderJE(it1) + '\n' +
                       '<JournalEntry>' + '\n' +
                         journalEntryHeader(it1, FileName) + '\n';

                    if(it1.system_calculated_header_tax.text() == ''){
                      it1.LineItems.LineItem.each{
                        li1->
        JERequest =      JERequest + lineItem(it1, li1, '') + '\n';                 
                        }
                    }else if(it1.system_calculated_header_tax.text() != '')
        JERequest =      JERequest + taxScenarios(it1) + '\n';

        JERequest =      JERequest + creditorItem(it1) + '\n' +
                         withHoldingTaxItem(it1) + '\n' +
                       '</JournalEntry>' + '\n' +
                    '</JournalEntryCreateRequest>' + '\n';        
                                
                                
    }
    
    def JEBulkRequest = JEBulkRequestHeader + JERequest + '</ns1:JournalEntryBulkCreateRequest>';
    JEBulkRequest = JEBulkRequest.replaceAll('<bind>', '').replaceAll('</bind>', '').replaceAll("(?m)^[ \t]*\r?\n", ""); // remove unnecessary tags and then remove emptylines created in place of removed tags
                        
    message.setBody(JEBulkRequest);
    return message;
}

def msgHeaderBulk(){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer);
    def date = new Date()
    def sdf2 = new SimpleDateFormat("yyyy-MM-dd")
    def sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

     builder.'MessageHeader'() {
            'ID'("MSG_" + sdf2.format(date))
            'CreationDateTime'(sdf1.format(date))
        }
    return writer.toString();    
}
def msgHeaderJE(it1){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer);
    def date = new Date()
    def sdf1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'")

    builder.'MessageHeader'() {
                    'ID'(it1.id.text())
                    'ReferenceID'('')
                    'CreationDateTime'(sdf1.format(date))
                }   
    return writer.toString();
}

def journalEntryHeader(it1, FileName){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer);
   def sdf3 = new SimpleDateFormat("yyyyMMdd")

    def remitToAddressCode = it1.remit_to_address_code.text();
    List<String[]> splittedRemitToCode = remitToAddressCode.split("\\|");  // remit_to_address_code = Business Partner/Vendor|Company Code|Payment Method|Payment Terms|BankAcc|BankID
          
    builder.bind{
        'OriginalReferenceDocumentType'('')
        'OriginalReferenceDocument'()
        'OriginalReferenceDocumentLogicalSystem'()
        'BusinessTransactionType'('')

        if(it1.document_type.text() == 'Invoice')
            'AccountingDocumentType'('ZI')
        else if(it1.document_type.text() == "Credit Note")
            'AccountingDocumentType'('ZC')
        else
            'AccountingDocumentType'('')

        if(it1.invoice_number.text().length() > 16)
            'DocumentReferenceID'(it1.invoice_number.text().substring(0,16))
        else
            'DocumentReferenceID'(it1.invoice_number.text())

        'DocumentHeaderText'("COUPA")
        'CreatedByUser'("IS_COD_SD200")
        'CompanyCode'(it1.account_type_name.text())
        'DocumentDate'(it1.invoice_date.text().substring(0,10))

        if(FileName != "null" && FileName != '' )
            'PostingDate'(FileName.substring(9,17))
        else
            'PostingDate'(sdf3.format(date))

        if(it1.payment_term.text() != '')
            'Reference1InDocumentHeader'(splittedRemitToCode[5]+"|"+it1.payment_term.text()) //Bank Key|Payment Term
        else
            'Reference1InDocumentHeader'(splittedRemitToCode[5]+"|"+splittedRemitToCode[3])

        'Reference2InDocumentHeader'(it1.date_of_supply.text()) //probably it is replacement of goods received date field
   
    }
    
    return writer.toString();
}

def creditorItem(it1){
  def writer = new StringWriter();
  def builder = new MarkupBuilder(writer);   

  def remitToAddressCode = it1.remit_to_address_code.text();
  List<String[]> splittedRemitToCode = remitToAddressCode.split("\\|");  // remit_to_address_code = Business Partner/Vendor|Company Code|Payment Method|Payment Terms|BankAcc|BankID

  def custom14 = ''
  custom14 = it1.custom_field_14.text();
  List<String[]> splittedCustom14 = custom14.split("\\|");  //custom_field_14 = Entry Office|Ship To

  builder.'CreditorItem'(){
      'ReferenceDocumentItem'('')
      'Creditor'(splittedRemitToCode[0])
      'AmountInTransactionCurrency'('currencyCode': it1.currency.text(), Double.parseDouble(it1.total.text()) * -1)
      
      if(it1.document_type.text() == "Invoice")
        'DebitCreditCode'('H')
      else if(it1.document_type.text() == "Credit Note")
        'DebitCreditCode'('S')
      else
        'DebitCreditCode'('')
      
      'DocumentItemText'(it1.description.text())
      'AssignmentReference'(it1.barcode.text()) //Assignment number in S4

      if(it1.account_type_name.text() == 'BR10') // Brazil NCM Requirement: Operation Code, for BR10 hsnsac field brings operation code
        'Reference1IDByBusinessPartner'(it1.LineItems.LineItem[0].hsnsac.text()) // Earlier mapped with FMNO: it1.LineItems.LineItem.segment_1[0].text()
      
      'Reference2IDByBusinessPartner'('') //to be mapped with goods received date; 20th dec: LAAS invoice requirement: passing personal charge||  4th Jan 22023: earlier mapped with it1.LineItems.LineItem.personal_charge.text()
      'Reference3IDByBusinessPartner'(it1.charge_id.text()) //charge ID
      'PaymentDetails'(){
          if(it1.payment_method_name.text() != '')
            'PaymentMethod'(it1.payment_method_name.text())
          else
            'PaymentMethod'(splittedRemitToCode[2])
          'PaymentReference'('') // Earlier mapped with ESR number: it1.esr_number.text()
      }
      if(it1.account_type_name.text() == 'IN10'){
          'BranchAccount'(splittedCustom14[0])
          'BusinessPlace'(splittedCustom14[1]) //Section Code field of S4
      }else if(it1.account_type_name.text() == 'BR10') //Brazil NCM Requirement field: Branch code field
          'BusinessPlace'(it1.custom_field_14.text())
        
      'ns3:ZMCKSTP_Pymnt_Ref_NUM'('xmlns:ns3': "http://SAPCustomFields.com/ZMCK", it1.esr_number.text())
      'ns3:ZMCKSTP_ORG_INV_NUM'('xmlns:ns3': "http://SAPCustomFields.com/ZMCK", it1.invoice_number.text())
      'ns3:ZMCKSTP_UNQ_IDT_NUM'('xmlns:ns3': "http://SAPCustomFields.com/ZMCK")    

  }

  return writer.toString();
}

def withHoldingTaxItem(it1){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer); 

    it1.WithholdingTaxLines.WithholdingTaxLine.each{
        it2 ->
        builder.'WithholdingTaxItem'(){
            'WithholdingTaxType'(it2.code.text())
            'WithholdingTaxCode'(it2.code.text())
            'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), '0.00')
            'TaxBaseAmountInTransCrcy'('currencyCode':it1.currency.text(), it2.Withholding_Base.text())
            'TaxIsToBeCalculated'('true')
        }
    }
    
    return writer.toString()
}

def taxScenarios(it1){
    def taxLines = ''
    def lines = ''
    //Calculating difference in Header Tax amount and Native tax line amount
    def taxLineTotal = 0.00;

    if(it1.line_level_taxation.text() == 'true'){
        it1.LineItems.LineItem.each{
            it2 ->
            taxLineTotal = taxLineTotal + Double.parseDouble(it2.TaxLines.TaxLine.amount[0].text());
        }
    }else if(it1.line_level_taxation.text() == 'false'){
        it1.TaxLines.Taxline.each{
            it2 ->
            taxLineTotal = taxLineTotal + Double.parseDouble(it2.amount.text());
        }
    }    
    def taxAmountDifference = taxLineTotal - Double.parseDouble(it1.system_calculated_header_tax.text())
   
    println("tolerance failure:" + it1.system_calculated_tolerance_failure.text())
    println("taxAmountDifference: " + taxAmountDifference)
    //println("taxLineTotal:" + taxLineTotal)

    //Identifying If System Generated Tax codes at each GL line are same or not
    List<String> taxCodesArray = new ArrayList<>()
    def sameTaxCodesIndicator = '';
    it1.LineItems.LineItem.each{
        li ->
        taxCodesArray.add(li.system_calculated_tax_code.text())
    }
    def taxCodesSet = taxCodesArray.toSet();
    if(taxCodesSet.size() == 1)
        sameTaxCodesIndicator = 'true';
    else if(taxCodesSet.size() > 1)
        sameTaxCodesIndicator = 'false';

    println "sameTaxCodesIndicator:" + sameTaxCodesIndicator

    //Tax Scenarios
    switch(true){
        //CASE 1: Generate tax lines by reading the “system_calculated_tax_code” and “system_calculated_line_tax_amount” found on the Line rows
        case((taxAmountDifference == 0.0 && it1.system_calculated_tolerance_failure.text() == "false")):
            println "case 1";
            it1.LineItems.LineItem.each{
                li1 ->
                lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                taxLines = taxLines + taxLineItem(it1, li1.system_calculated_tax_code.text(), li1.system_calculated_tax_rate.text(), li1.system_calculated_line_tax_amount.text(), li1.total.text()) + '\n'
            }
            break;
        //CASE 2: Generate tax lines by reading the tax details found on the TaxLine row(s)
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "true")): 
            println "case 2";
            if(it1.line_level_taxation.text() == 'true'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.TaxLines.TaxLine.code.text()) + '\n'
                    taxLines = taxLines + taxLineItem(it1, li1.TaxLines.TaxLine.code.text(), li1.TaxLines.TaxLine.rate.text(), li1.TaxLines.TaxLine.amount.text(), li1.total.text()) + '\n'
                }
            }else if(it1.line_level_taxation.text() == 'false'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, it1.TaxLines.Taxline.code.text()) + '\n'
                }
                taxLines = taxLines + taxLineItem(it1, it1.TaxLines.Taxline.code.text(), it1.TaxLines.Taxline.rate.text(), it1.TaxLines.Taxline.amount.text(), it1.accounting_total_amount.text()) + '\n'
            }
            
            break;
        //CASE 3a: Generate tax lines by reading the “system_calculated_tax_code” found on the Line row but read the tax amount on the TaxLine row
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "false" && sameTaxCodesIndicator == "true")):
            println "case 3a";
            if(it1.line_level_taxation.text() == 'true'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                    taxLines = taxLines + taxLineItem(it1, li1.system_calculated_tax_code.text(), li1.system_calculated_tax_rate.text(), li1.TaxLines.TaxLine.amount.text(), li1.total.text()) + '\n'
                }
            }else if(it1.line_level_taxation.text() == 'false'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                } 
                taxLines = taxLines + taxLineItem(it1, it1.LineItems.LineItem[0].system_calculated_tax_code.text(), it1.LineItems.LineItem[0].system_calculated_tax_rate.text(), it1.TaxLines.Taxline[0].amount.text(), it1.accounting_total_amount.text()) + '\n'
            }
            
            break;
        //CASE 3b:
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "false" && sameTaxCodesIndicator == "false")):
            println "case 3b";
            it1.LineItems.LineItem.each{
                li1 ->
                lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                taxLines = taxLines + taxLineItem(it1, li1.system_calculated_tax_code.text(), li1.system_calculated_tax_rate.text(), li1.system_calculated_line_tax_amount.text(), li1.total.text()) + '\n'
            }
                lines = lines + balancingLine(it1, taxAmountDifference) + '\n'
            break;
        default: 
            break;
    }
    return lines + taxLines

}

def lineItem(it1, li1, taxCode){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer); 

    def segment1 = ''
    segment1 = li1.segment_1.text()
    List<String[]> splittedSegment1 = segment1.split("\\:"); // segment_1 = FMNO:CostCenter

    def segment2 = ''
    segment2 = li1.segment_2.text()
    List<String[]> splittedSegment2 = segment2.split("\\:");    // segment_2 = CompanyCode:CostCenter 
    
    List<String[]> splittedTaxCode = taxCode.split("\\-")  //taxCode = TaxCode-AccountingKey

    if(li1.SplitLines.SplitLine.isEmpty()){
        builder.Item(){
            'ReferenceDocumentItem'('1')

             if(it1.account_type_name.text() == splittedSegment2[0]) // Logic for Intercompany Invoices: When there is Company code at line level which is not same as company code at header level map company code at item level = company code at line in coupa file.
                'CompanyCode'(it1.account_type_name.text())
             else
                'CompanyCode'(splittedSegment2[0])

             'GLAccount'(li1.GL_Account.text())
             'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), invoiceTotalPlusOverhead(li1))
             
             if(it1.document_type.text() == "Invoice")
                'DebitCreditCode'('S')
             else if(it1.document_type.text() == "Credit Note")
                'DebitCreditCode'('H')
             else
                'DebitCreditCode'('')

             'DocumentItemText'(li1.description.text())
             'AssignmentReference'(splittedSegment1[0] + "|" + li1.Internal_Order.text()) //earlier mapped with FMNO|Internal Order : 'AssignmentReference'(splittedSegment1[0] + "|" + li1.Internal_Order.text())
             'Tax'(){
                 'TaxCode'(splittedTaxCode[0]) // Tax Code decided as per tax scenarios
             }

             if(it1.bill_to_address_country_code.text() == 'PL')
                'Reference1IDByBusinessPartner'(li1.Asset_Class_2.text())
             else if(it1.bill_to_address_country_code.text() == 'DE')
                'Reference1IDByBusinessPartner'(li1.Asset_Class_3.text())
             else
                'Reference1IDByBusinessPartner'(li1.Asset_Class_1.text())

             'Reference2IDByBusinessPartner'(li1.personal_charge.text()) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())

             if(it1.account_type_name.text() == 'IN10')
             'Reference3IDByBusinessPartner'(li1.hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
             else
             'Reference3IDByBusinessPartner'('')
             'AccountAssignment'(){
                 if(li1.GL_Account.text() != ''){
                    if(li1.GL_Account.text().charAt(0) == '1' || li1.GL_Account.text().charAt(0) == '5'){ //Cost Center derivation logic: if GL account is of 1 or 5 series use cost center in segment1 : FMNO:cost Center else read cost center from segment 2 = CompanyCode:CostCenter
                     'CostCenter'(splittedSegment1[1])
                     'WBSElement'('')
                    }else{
                     'CostCenter'(splittedSegment2[1])
                     'WBSElement'(li1.segment_3.text())
                    }
                 }else{
                    'CostCenter'(splittedSegment2[1])
                    'WBSElement'(li1.segment_3.text())
                 }
                 
             }
              if(li1.quantity.text() != '')
             'QuantityInEntryUnit'('unitCode': "PCE", li1.quantity.text())
        }
    }else if(!(li1.SplitLines.SplitLine.isEmpty())){
        li1.SplitLines.SplitLine.each{
            li2 ->

            def segment1_sp = ''
            segment1_sp = li2.segment_1.text()  // segment_1 = FMNO:CostCenter
            List<String[]> splittedSegment1_sp = segment1_sp.split("\\:");

            def segment2_sp = ''
            segment2_sp = li2.segment_2.text() // segment_2 = CompanyCode:CostCenter
            List<String[]> splittedSegment2_sp = segment2_sp.split("\\:");

            builder.Item(){
              'ReferenceDocumentItem'('1')
              
              if(it1.account_type_name.text() == splittedSegment2_sp[0]) // Logic for Intercompany Invoices: When there is Company code at line level which is not same as company code at header level map company code at item level = company code at line in coupa file.
                'CompanyCode'(it1.account_type_name.text())
              else
                'CompanyCode'(splittedSegment2_sp[0])

              'GLAccount'(li1.GL_Account.text())
              'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), invoiceTotalPlusOverhead(li2))
             
              if(it1.document_type.text() == "Invoice")
                'DebitCreditCode'('S')
              else if(it1.document_type.text() == "Credit Note")
                'DebitCreditCode'('H')
              else
                'DebitCreditCode'('')

              'DocumentItemText'(li1.description.text())
              'AssignmentReference'(splittedSegment1_sp[0] + "|" + li1.Internal_Order.text()) //FMNO|Internal Order
              'Tax'(){
                 'TaxCode'(splittedTaxCode[0]) // Tax Code decided as per tax scenarios
              }

              if(it1.bill_to_address_country_code.text() == 'PL')
                'Reference1IDByBusinessPartner'(li1.Asset_Class_2.text())
              else if(it1.bill_to_address_country_code.text() == 'DE')
                'Reference1IDByBusinessPartner'(li1.Asset_Class_3.text())
              else
                'Reference1IDByBusinessPartner'(li1.Asset_Class_1.text())

              'Reference2IDByBusinessPartner'(li1.personal_charge.text()) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
              'Reference3IDByBusinessPartner'(li2.hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
              'AccountAssignment'(){
                 if(li1.GL_Account.text() != ''){
                    if(li1.GL_Account.text().charAt(0) == '1' || li1.GL_Account.text().charAt(0) == '5'){ //Cost Center derivation logic: if GL account is of 1 or 5 series use cost center in segment1 : FMNO:cost Center else read cost center from segment 2
                     'CostCenter'(splittedSegment1_sp[1])
                     'WBSElement'()
                    }else{
                     'CostCenter'(splittedSegment2_sp[1])
                     'WBSElement'(li2.segment_3.text())
                    }
                 }else{
                    'CostCenter'(splittedSegment2_sp[1])
                    'WBSElement'(li2.segment_3.text())
                  }
             }
             if(li1.quantity.text() != '')
             'QuantityInEntryUnit'('unitCode': "PCE", li2.quantity.text())
            }
        }
    }

    return writer.toString() 
}

def invoiceTotalPlusOverhead(li1){
    def invoiceTotal = 0.00

    if(li1.total.text() != '')
        invoiceTotal = invoiceTotal + Double.parseDouble(li1.total.text())
    else  
        invoiceTotal = invoiceTotal + 0.00

    if(li1.distributed_shipping_amount.text() != '')
        invoiceTotal = invoiceTotal + Double.parseDouble(li1.distributed_shipping_amount.text())
    else
        invoiceTotal = invoiceTotal + 0.00

    if(li1.distributed_handling_amount.text() != '')
        invoiceTotal = invoiceTotal + Double.parseDouble(li1.distributed_handling_amount.text())
    else
        invoiceTotal = invoiceTotal + 0.00
                                  
    if(li1.distributed_misc_amount.text() != '')
        invoiceTotal = invoiceTotal + Double.parseDouble(li1.distributed_misc_amount.text())
    else
        invoiceTotal = invoiceTotal +0.00

    return invoiceTotal
}

def balancingLine(it1, taxAmountDifference){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer); 
    
    def segment1 = ''
    segment1 = it1.LineItems.LineItem[0].segment_1.text()
    List<String[]> splittedSegment1 = segment1.split("\\:"); // segment_1 = FMNO:CostCenter
  
    def segment2 = ''
    segment2 = it1.LineItems.LineItem[0].segment_2.text()
    List<String[]> splittedSegment2 = segment2.split("\\:");    // segment_2 = CompanyCode:CostCenter

    def taxCode = ''
    taxCode = it1.LineItems.LineItem[0].system_calculated_tax_code.text() // Tax Code decided as per tax scenarios 
    List<String[]> splittedtaxCode = taxCode.split("\\-"); //TacCode = TacCode-AccountingKey

    builder.Item(){
       'ReferenceDocumentItem'('1')

        if(it1.account_type_name.text() == splittedSegment2[0]) // Logic for Intercompany Invoices: When there is Company code at line level which is not same as company code at header level map company code at item level = company code at line in coupa file.
                'CompanyCode'(it1.account_type_name.text())
             else
                'CompanyCode'(splittedSegment2[0])

             'GLAccount'(it1.LineItems.LineItem[0].GL_Account.text())
             'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), taxAmountDifference) 
             
             if(taxAmountDifference > 0)
                'DebitCreditCode'('S')
             else if(taxAmountDifference < 0)
                'DebitCreditCode'('H')
             else
                'DebitCreditCode'('')

             'DocumentItemText'(it1.LineItems.LineItem[0].description.text())
             'AssignmentReference'(splittedSegment1[0] + "|" + it1.LineItems.LineItem[0].Internal_Order.text()) //FMNO|Internal Order
             'Tax'(){
                 'TaxCode'('') 
             }

             if(it1.bill_to_address_country_code.text() == 'PL')
                'Reference1IDByBusinessPartner'(it1.LineItems.LineItem[0].Asset_Class_2.text())
             else if(it1.bill_to_address_country_code.text() == 'DE')
                'Reference1IDByBusinessPartner'(it1.LineItems.LineItem[0].Asset_Class_3.text())
             else
                'Reference1IDByBusinessPartner'(it1.LineItems.LineItem[0].Asset_Class_1.text())

             'Reference2IDByBusinessPartner'(it1.LineItems.LineItem[0].personal_charge.text()) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
             'Reference3IDByBusinessPartner'(it1.LineItems.LineItem[0].hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
             'AccountAssignment'(){
                 if(it1.LineItems.LineItem[0].GL_Account.text() != ''){
                    if(it1.LineItems.LineItem[0].GL_Account.text().charAt(0) == '1' || it1.LineItems.LineItem[0].GL_Account.text().charAt(0) == '5'){ //Cost Center derivation logic: if GL account is of 1 or 5 series use cost center in segment1 : FMNO:cost Center else read cost center from segment 2 = CompanyCode:CostCenter
                     'CostCenter'(splittedSegment1[1])
                     'WBSElement'('')
                    }else{
                     'CostCenter'(splittedSegment2[1])
                     'WBSElement'(it1.LineItems.LineItem[0].segment_3.text())
                    }
                 }else{
                    'CostCenter'(splittedSegment2[1])
                    'WBSElement'(it1.LineItems.LineItem[0].segment_3.text())
                 }
             }
             if(it1.LineItems.LineItem[0].quantity.text() != '')
             'QuantityInEntryUnit'('unitCode': "PCE", it1.LineItems.LineItem[0].quantity.text())
   }
   return writer.toString()
}

def taxLineItem(it1, taxCode, taxRate, taxAmount, taxBaseAmount){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer); 
    
     List<String[]> splittedTaxCode = taxCode.split("\\-")  //taxCode = TaxCode-AccountingKey

    builder.'ProductTaxItem'(){
        'TaxCode'(splittedTaxCode[0])
        'TaxRate'(taxRate)
        'ConditionType'('ZITR')
        'TaxItemClassification'(splittedTaxCode[1])
        'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), taxAmount)
        
        if(it1.document_type.text() == "Invoice")
            'DebitCreditCode'('S')
        else if(it1.document_type.text() == "Credit Note")
            'DebitCreditCode'('H')
        else
        'DebitCreditCode'('')
        'TaxBaseAmountInTransCrcy'('currencyCode':it1.currency.text(), taxBaseAmount)                                   
    }
    return writer.toString()
}

//Header RefKey2 holds date of supply, Vendor Line RefKey2 : said to be mapped with goods received date
// Are date of supply (located at Lineitem level in coupa file) and Goods received (located at header level in Coupa file)  date same or different
//is any update yet to come for date of supply and goods received date mapping. Amr upadted goods received date field will be removed from coupa file