/* Script Author: Shweta Rana
Created Date: 27th Apr, 2023*/

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
    String user = message.getProperty('createdByUser');

    def JEBulkRequestHeader = '<ns1:JournalEntryBulkCreateRequest xmlns:ns1 = "http://sap.com/xi/SAPSCORE/SFIN">' + '\n' +
                        msgHeaderBulk() + '\n';
    def JERequest = '';
    root.Invoice.each{
        it1 ->
        JERequest = JERequest +
                    '<JournalEntryCreateRequest>' + '\n' +
                       msgHeaderJE(it1) + '\n' +
                       '<JournalEntry>' + '\n' +
                         journalEntryHeader(it1, FileName, user) + '\n';

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
                    'ReferenceID'(it1.id.text())
                    'CreationDateTime'(sdf1.format(date))
                }   
    return writer.toString();
}

def journalEntryHeader(it1, FileName, user){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer);
    def date = new Date()
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
            'DocumentReferenceID'(it1.invoice_number.text().substring(it1.invoice_number.text().length() - 16, it1.invoice_number.text().length())) //Get last 16 characters of invoice number
        else
            'DocumentReferenceID'(it1.invoice_number.text())

        'DocumentHeaderText'("COUPA")
        if(user != "null" && user != '')
        'CreatedByUser'(user)
        else
        'CreatedByUser'('')
        'CompanyCode'(it1.account_type_name.text())
        'DocumentDate'(it1.invoice_date.text().substring(0,10))

        if(FileName != "null" && FileName != '' )
            'PostingDate'(FileName.substring(9,17))
        else
            'PostingDate'(sdf3.format(date))
        'InvoiceReceiptDate'(it1.created_at.text().substring(0,10))
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
      
      if(Double.parseDouble(it1.total.text()) * -1 < 0)
        'DebitCreditCode'('H')
      else
        'DebitCreditCode'('S')
        
      if(it1.description.text().length() > 50)
        'DocumentItemText'(it1.description.text().substring(0,50))
      else
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
      
      'BusinessPlace'(splittedCustom14[splittedCustom14.size - 1]) //Section Code field of S4 updated on March 27th 23, business place will be passed for all the countries if it comes from coupa
    //   if(it1.account_type_name.text() == 'IN10'){
    //       'BranchAccount'(splittedCustom14[0])
    //       'BusinessPlace'(splittedCustom14[1]) //Section Code field of S4
    //   }else                                   //Changed on 9th Feb, 2023:  else if(it1.account_type_name.text() == 'BR10') //Brazil NCM Requirement field: Branch code field
    //       'BusinessPlace'(it1.custom_field_14.text()) // business place will be passed for all the countries if it comes from coupa
        
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

    if(it1.TaxLines.Taxline.isEmpty() && it1.LineItems.LineItem.TaxLines.TaxLine.isEmpty() && it1.system_calculated_tolerance_failure.text() == "false"){
        it1.LineItems.LineItem.each{
            li1 ->
            lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
            def taxCode = ''
            def taxRate = '';
            def taxAmount = '';
            taxCode = li1.system_calculated_tax_code.text()
            taxRate = li1.system_calculated_tax_rate.text();
            taxAmount = li1.system_calculated_line_tax_amount.text();
            List<String[]> splittedTaxCode = taxCode.split("\\|");
            List<String[]> splittedTaxRate = taxRate.split("\\|"); 
            List<String[]> splittedTaxAmount = taxAmount.split("\\|"); 

            for(int i=0; i < splittedTaxCode.size(); i++){
                taxLines = taxLines + taxLineItem(it1, splittedTaxCode[i], splittedTaxRate[i], splittedTaxAmount[i], li1.total.text()) + '\n'
            }
        }
    }else{
        //Calculating difference in system_generated_Header_Tax_amount and total of Native tax lines amount
        def taxLineTotal = 0.00;

        if(it1.line_level_taxation.text() == 'true'){
            it1.LineItems.LineItem.each{
                it2 ->
                it2.TaxLines.TaxLine.each{
                    it3->
                    taxLineTotal = taxLineTotal + Double.parseDouble(it3.amount.text());
                }
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
    println("taxLineTotal:" + taxLineTotal)

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
        //CASE 1: Generate tax lines by reading the “system_calculated_tax_code”, “system_calculated_tax_rate” and “system_calculated_line_tax_amount” found on the Line rows
        case((taxAmountDifference == 0.0 && it1.system_calculated_tolerance_failure.text() == "false")):
            println "case 1";
            it1.LineItems.LineItem.each{
                li1 ->
                lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n' 
                //lines = lines + taxBalancingLine(it1, it1.LineItems.LineItem[0], '20', "")
                //Handling multiple tax entries in Gl Line/system calculated fields, which may bring concatenated values as code1-conditionType1|code2-conditionType2, rate1|rate2, amount1|amount2; while there will be as many native tax lines as many pipe separted values we have 
                def taxCode = ''
                def taxRate = '';
                def taxAmount = '';

                taxCode = li1.system_calculated_tax_code.text()
                taxRate = li1.system_calculated_tax_rate.text();
                taxAmount = li1.system_calculated_line_tax_amount.text();

                List<String[]> splittedTaxCode = taxCode.split("\\|"); //tax values may come pipe separated in system calculated code, rate and amount on GL line as mentioned in above comment
                List<String[]> splittedTaxRate = taxRate.split("\\|"); 
                List<String[]> splittedTaxAmount = taxAmount.split("\\|");
                   
                for(int i = 0; i < splittedTaxCode.size(); i++){
                    if(splittedTaxCode[i].contains('NVV')){
                        lines = lines + taxBalancingLine(it1, li1, splittedTaxAmount[i], splittedTaxCode[i]) + '\n'
                        taxLines = taxLines + taxLineItem(it1, splittedTaxCode[i], splittedTaxRate[i], "0", li1.total.text()) + '\n'
                    }
                     else 
                    taxLines = taxLines + taxLineItem(it1, splittedTaxCode[i], splittedTaxRate[i], splittedTaxAmount[i], li1.total.text()) + '\n'
                }
                   
            }
            break;
        //CASE 2: Generate tax lines by reading the tax details found on the TaxLine row(s)
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "true")): 
            println "case 2";
            if(it1.line_level_taxation.text() == 'true'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.TaxLines.TaxLine[0].code.text()) + '\n'
                    li1.TaxLines.TaxLine.each{
                        tl1 ->
                        if(tl1.code.text().contains('NVV')){
                            lines = lines + taxBalancingLine(it1, li1, tl1.amount.text(), tl1.code.text()) + '\n'
                            taxLines = taxLines + taxLineItem(it1, tl1.code.text(), tl1.rate.text(), "0", li1.total.text()) + '\n'
                        }
                        else
                        taxLines = taxLines + taxLineItem(it1, tl1.code.text(), tl1.rate.text(), tl1.amount.text(), li1.total.text()) + '\n'
                    }
                }
            }else if(it1.line_level_taxation.text() == 'false'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, it1.TaxLines.Taxline[0].code.text()) + '\n'
                }
                it1.TaxLines.Taxline.each{
                    tl1 ->
                    if(tl1.code.text().contains('NVV')){
                        lines = lines + taxBalancingLine(it1, it1.LineItems.LineItem[0], tl1.amount.text(), tl1.code.text()) + '\n'
                        taxLines = taxLines + taxLineItem(it1, tl1.code.text(), tl1.rate.text(), "0", it1.accounting_total_amount.text()) + '\n'
                    }
                     else
                    taxLines = taxLines + taxLineItem(it1, tl1.code.text(), tl1.rate.text(), tl1.amount.text(), it1.accounting_total_amount.text()) + '\n'
                }
            }
            
            break;
        //CASE 3a: Generate tax lines by reading the “system_calculated_tax_code” & "system_calculated_tax_rate" found on the Line row but read the tax amount on Native TaxLine
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "false" && sameTaxCodesIndicator == "true")):
            println "case 3a";
            if(it1.line_level_taxation.text() == 'true'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                     //Handling dual taxes
                     def taxCode = ''
                     taxCode = li1.system_calculated_tax_code.text()
                     List<String[]> splittedTaxCode = taxCode.split("\\|"); //For dual taxes, tax values comes pipe separated in system calculated code, rate and amount on GL line
                     if(splittedTaxCode.size() > 1){
                        def taxRate = '';
                        taxRate = li1.system_calculated_tax_rate.text();
                        List<String[]> splittedTaxRate = taxRate.split("\\|"); 

                        if(splittedTaxRate[0] == li1.TaxLines.TaxLine[0].rate.text()){
                            taxLines = taxLines + taxLineItem(it1, splittedTaxCode[0], splittedTaxRate[0], li1.TaxLines.TaxLine[0].amount.text(), li1.total.text()) + '\n'
                            taxLines = taxLines + taxLineItem(it1, splittedTaxCode[1], splittedTaxRate[1], li1.TaxLines.TaxLine[1].amount.text(), li1.total.text()) + '\n'
                        }else{
                            taxLines = taxLines + taxLineItem(it1, splittedTaxCode[0], splittedTaxRate[0], li1.TaxLines.TaxLine[1].amount.text(), li1.total.text()) + '\n'
                            taxLines = taxLines + taxLineItem(it1, splittedTaxCode[1], splittedTaxRate[1], li1.TaxLines.TaxLine[0].amount.text(), li1.total.text()) + '\n' 
                        }
                    }
                else
                    taxLines = taxLines + taxLineItem(it1, li1.system_calculated_tax_code.text(), li1.system_calculated_tax_rate.text(), li1.TaxLines.TaxLine.amount.text(), li1.total.text()) + '\n'
                }
            }else if(it1.line_level_taxation.text() == 'false'){
                it1.LineItems.LineItem.each{
                    li1 ->
                    lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                } 
                //Handling dual taxes
                def taxCode = ''
                taxCode = it1.LineItems.LineItem[0].system_calculated_tax_code.text()
                List<String[]> splittedTaxCode = taxCode.split("\\|"); //For dual taxes, tax values comes pipe separated in system calculated code, rate and amount on GL line
                if(splittedTaxCode.size() > 1){
                    def taxRate = '';
                    taxRate = it1.LineItems.LineItem[0].system_calculated_tax_rate.text();
                    List<String[]> splittedTaxRate = taxRate.split("\\|"); 

                    if(splittedTaxRate[0] == it1.TaxLines.Taxline[0].rate.text()){
                        taxLines = taxLines + taxLineItem(it1, splittedTaxCode[0], splittedTaxRate[0], it1.TaxLines.Taxline[0].amount.text(), it1.accounting_total_amount.text()) + '\n'
                        taxLines = taxLines + taxLineItem(it1, splittedTaxCode[1], splittedTaxRate[1], it1.TaxLines.Taxline[1].amount.text(), it1.accounting_total_amount.text()) + '\n'
                     }else{
                        taxLines = taxLines + taxLineItem(it1, splittedTaxCode[0], splittedTaxRate[0], it1.TaxLines.Taxline[1].amount.text(), it1.accounting_total_amount.text()) + '\n'
                        taxLines = taxLines + taxLineItem(it1, splittedTaxCode[1], splittedTaxRate[1], it1.TaxLines.Taxline[0].amount.text(), it1.accounting_total_amount.text()) + '\n' 
                      }
                    }
                else
                    taxLines = taxLines + taxLineItem(it1, it1.LineItems.LineItem[0].system_calculated_tax_code.text(), it1.LineItems.LineItem[0].system_calculated_tax_rate.text(), it1.TaxLines.Taxline[0].amount.text(), it1.accounting_total_amount.text()) + '\n'    
            }
            
            break;
        //CASE 3b: Generate tax line by reading system calculated tax fields at GL line, and create a balancing entry which details same as GL Line1 with amount = total of tax line amount - system generated header tax amount
        case((taxAmountDifference != 0.0 && it1.system_calculated_tolerance_failure.text() == "false" && sameTaxCodesIndicator == "false")):
            println "case 3b";
            it1.LineItems.LineItem.each{
                li1 ->
                lines = lines + lineItem(it1, li1, li1.system_calculated_tax_code.text()) + '\n'
                //Handling Dual Taxes
                def taxCode = ''
                taxCode = li1.system_calculated_tax_code.text()
                List<String[]> splittedTaxCode = taxCode.split("\\|"); //For dual taxes, tax values comes pipe separated in system calculated code, rate and amount on GL line
                if(splittedTaxCode.size() > 1){
                   def taxRate = '';
                   def taxAmount = '';
                   taxRate = li1.system_calculated_tax_rate.text();
                   taxAmount = li1.system_calculated_line_tax_amount.text();
                   List<String[]> splittedTaxRate = taxRate.split("\\|"); 
                   List<String[]> splittedTaxAmount = taxAmount.split("\\|"); 

                   taxLines = taxLines + taxLineItem(it1, splittedTaxCode[0], splittedTaxRate[0], splittedTaxAmount[0], li1.total.text()) + '\n'
                   taxLines = taxLines + taxLineItem(it1, splittedTaxCode[1], splittedTaxRate[1], splittedTaxAmount[1], li1.total.text()) + '\n'
                }
                else
                   taxLines = taxLines + taxLineItem(it1, li1.system_calculated_tax_code.text(), li1.system_calculated_tax_rate.text(), li1.system_calculated_line_tax_amount.text(), li1.total.text()) + '\n'
            }
                lines = lines + taxBalancingLine(it1, it1.LineItems.LineItem[0], taxAmountDifference, "") + '\n'
            break;
        default: 
            break;
    }
    }
    return lines + taxLines

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
        
        // if(it1.document_type.text() == "Invoice")
        //     'DebitCreditCode'('S')
        // else if(it1.document_type.text() == "Credit Note")
        //     'DebitCreditCode'('H')
        // else
        // 'DebitCreditCode'('')
        
         if(Double.parseDouble(taxAmount) >= 0)
            'DebitCreditCode'('S')
         else
            'DebitCreditCode'('H')
        
        'TaxBaseAmountInTransCrcy'('currencyCode':it1.currency.text(), taxBaseAmount)                                   
    }
    return writer.toString()
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
    
    def segment3 = ''
    segment3 = li1.segment_3.text()
    List<String[]> splittedSegment3 = segment3.split("\\:");    // segment_3 = CompanyCode:WBSElement

    List<String[]> splittedTaxCode = taxCode.split("\\-")
    
    if(li1.SplitLines.SplitLine.isEmpty()){
        builder.Item(){
            'ReferenceDocumentItem'('1')
            'GLAccount'(li1.GL_Account.text())
            'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), invoiceTotalPlusOverhead(li1))
            
            //  if(it1.document_type.text() == "Invoice")
            //     'DebitCreditCode'('S')
            //  else if(it1.document_type.text() == "Credit Note")
            //     'DebitCreditCode'('H')
            //  else
            //     'DebitCreditCode'('')
            
             if(invoiceTotalPlusOverhead(li1) >= 0)
                'DebitCreditCode'('S')
             else
                'DebitCreditCode'('H')
                
             if(li1.description.text().length() > 50)
                'DocumentItemText'(li1.description.text().substring(0,50))
             else
                'DocumentItemText'(li1.description.text())

             'AssignmentReference'(splittedSegment1[0] + "|" + li1.Internal_Order.text()) //earlier mapped with FMNO|Internal Order : 'AssignmentReference'(splittedSegment1[0] + "|" + li1.Internal_Order.text())
             'Tax'(){
                 'TaxCode'(splittedTaxCode[0]) // Tax Code decided as per tax scenarios
             }
             'Reference1IDByBusinessPartner'(li1.Asset_Class.text())

            if(li1.personal_charge.text().length() > 12)
                'Reference2IDByBusinessPartner'(li1.personal_charge.text().substring(0,12)) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
             else
                'Reference2IDByBusinessPartner'(li1.personal_charge.text())

             if(it1.account_type_name.text() == 'IN10')
             'Reference3IDByBusinessPartner'(li1.hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
             else
             'Reference3IDByBusinessPartner'('')
             
             if(li1.GL_Account.text() != '' && (li1.GL_Account.text() == '14000000' || li1.GL_Account.text() == '16100004' || li1.GL_Account.text() == '16100005' || li1.GL_Account.text() == '52000000' || li1.GL_Account.text() == '52000001' || li1.GL_Account.text() == '52000002' || li1.GL_Account.text() == '53000000')){ //Cost Center derivation logic: if GL account is in the given list use cost center in segment1 : FMNO:CostCenter
             
                'CompanyCode'(it1.account_type_name.text()) // If cost center derivation logic applies read Company code at Header level
                'AccountAssignment'(){
                    'CostCenter'(splittedSegment1[1]) //segment1 : FMNO:CostCenter
                    'WBSElement'('')
                    }
                }else if(splittedSegment2[1] != null){ //segment_2 = CompanyCode:CostCenter
                    'CompanyCode'(splittedSegment2[0])
                    'AccountAssignment'(){
                      'CostCenter'(splittedSegment2[1])
                      'WBSElement'('')
                     }
                 }else if(splittedSegment3[1] != null){ //segment_3 = CompanyCode:WBSElement
                    'CompanyCode'(splittedSegment3[0])
                    'AccountAssignment'(){
                     'CostCenter'('')
                     'WBSElement'(splittedSegment3[1])
                     }
                  }else {
                    'CompanyCode'(it1.account_type_name.text())
                    'AccountAssignment'(){
                     'CostCenter'(splittedSegment2[1])
                     'WBSElement'('')
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
            
            def segment3_sp = ''
            segment3_sp = li2.segment_3.text() // segment_3 = CompanyCode:WBSElement
            List<String[]> splittedSegment3_sp = segment3_sp.split("\\:");

            builder.Item(){
              'ReferenceDocumentItem'('1')
              
              if(splittedSegment2_sp[1] != null) // segment_2 = CompanyCode:CostCenter  If CostCenter Exists send Company code sent along cost center
                'CompanyCode'(splittedSegment2_sp[0])
              else if(splittedSegment3_sp[1] != null) // segment_3 = CompanyCode:WBSElement If WBSElement Exists send Company code sent along WBSElementr
                'CompanyCode'(splittedSegment3_sp[0])
              else
                'CompanyCode'(it1.account_type_name.text()) //If both segment2 and segment3 do not have CostCenter and WBSElement send Company code at Header level

              'GLAccount'(li1.GL_Account.text())
              'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), invoiceTotalPlusOverhead(li2))
             
            //   if(it1.document_type.text() == "Invoice")
            //     'DebitCreditCode'('S')
            //   else if(it1.document_type.text() == "Credit Note")
            //     'DebitCreditCode'('H')
            //   else
            //     'DebitCreditCode'('')
            
             if(invoiceTotalPlusOverhead(li2) >= 0)
                'DebitCreditCode'('S')
             else
                'DebitCreditCode'('H')
                
             if(li1.description.text().length() > 50)
                'DocumentItemText'(li1.description.text().substring(0,50))
             else
                'DocumentItemText'(li1.description.text())
                
              'AssignmentReference'(splittedSegment1_sp[0] + "|" + li1.Internal_Order.text()) //FMNO|Internal Order
              'Tax'(){
                 'TaxCode'(taxCode) // Tax Code whose accounting key != NVV; for NVV accounting key taxcode = ''
              }
              
             'Reference1IDByBusinessPartner'(li1.Asset_Class.text())
              
            if(li1.personal_charge.text().length() > 12)
                'Reference2IDByBusinessPartner'(li1.personal_charge.text().substring(0,12)) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
             else
                'Reference2IDByBusinessPartner'(li1.personal_charge.text())
              
              'Reference3IDByBusinessPartner'(li2.hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
              
               if(li1.GL_Account.text() != '' && (li1.GL_Account.text() == '14000000' || li1.GL_Account.text() == '16100004' || li1.GL_Account.text() == '16100005' || li1.GL_Account.text() == '52000000' || li1.GL_Account.text() == '52000001' || li1.GL_Account.text() == '52000002' || li1.GL_Account.text() == '53000000')){ //Cost Center derivation logic: if GL account is in the given list use cost center in segment1 : FMNO:CostCenter
               
                'CompanyCode'(it1.account_type_name.text()) // If cost center derivation logic applies read Company code at Header level
                'AccountAssignment'(){
                    'CostCenter'(splittedSegment1_sp[1]) //segment1 : FMNO:CostCenter
                    'WBSElement'('')
                    }
                }else if(splittedSegment2_sp[1] != null){ //segment_2 = CompanyCode:CostCenter
                    'CompanyCode'(splittedSegment2_sp[0])
                    'AccountAssignment'(){
                      'CostCenter'(splittedSegment2_sp[1])
                      'WBSElement'('')
                     }
                 }else if(splittedSegment3_sp[1] != null){ //segment_3 = CompanyCode:WBSElement
                    'CompanyCode'(splittedSegment3_sp[0])
                    'AccountAssignment'(){
                     'CostCenter'('')
                     'WBSElement'(splittedSegment3_sp[1])
                     }
                  }
             if(li1.quantity.text() != '')
             'QuantityInEntryUnit'('unitCode': "PCE", li2.quantity.text())
            }
        }
    }
 
    return writer.toString() ;
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

def taxBalancingLine(it1, li1, nonDeductibleTaxAmount, taxCode){
    def writer = new StringWriter();
    def builder = new MarkupBuilder(writer); 
    
    def segment1 = ''
    segment1 = li1.segment_1.text()
    List<String[]> splittedSegment1 = segment1.split("\\:"); // segment_1 = FMNO:CostCenter
  
    def segment2 = ''
    segment2 = li1.segment_2.text()
    List<String[]> splittedSegment2 = segment2.split("\\:");    // segment_2 = CompanyCode:CostCenter

    def segment3 = ''
    segment3 = li1.segment_3.text()
    List<String[]> splittedSegment3 = segment3.split("\\:");    // segment_3 = CompanyCode:WBSElement

    List<String[]> splittedTaxCode = taxCode.split("\\-")

    builder.Item(){
        'ReferenceDocumentItem'('1')
        'GLAccount'(li1.GL_Account.text())
        'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), nonDeductibleTaxAmount) 

        if(nonDeductibleTaxAmount.class == String)
        nonDeductibleTaxAmount = Double.parseDouble(nonDeductibleTaxAmount)
             
        if(nonDeductibleTaxAmount >= 0)
            'DebitCreditCode'('S')
        else
            'DebitCreditCode'('H')
        
        if(li1.description.text().length() > 50)
            'DocumentItemText'(li1.description.text().substring(0,50))
         else
            'DocumentItemText'(li1.description.text())
            
        'AssignmentReference'(splittedSegment1[0] + "|" + li1.Internal_Order.text()) //FMNO|Internal Order
        'Tax'(){
            'TaxCode'(splittedTaxCode[0]) 
         }

        'Reference1IDByBusinessPartner'(li1.Asset_Class.text())
        
        if(li1.personal_charge.text().length() > 12) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
            'Reference2IDByBusinessPartner'(li1.personal_charge.text().substring(0,12))
        else
            'Reference2IDByBusinessPartner'(li1.personal_charge.text())
        
        'Reference3IDByBusinessPartner'(li1.hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
        
        if(li1.GL_Account.text() != '' && (li1.GL_Account.text() == '14000000' || li1.GL_Account.text() == '16100004' || li1.GL_Account.text() == '16100005' || li1.GL_Account.text() == '52000000' || li1.GL_Account.text() == '52000001' || li1.GL_Account.text() == '52000002' || li1.GL_Account.text() == '53000000')){ //Cost Center derivation logic: if GL account is in the given list use cost center in segment1 : FMNO:CostCenter
             
            'CompanyCode'(it1.account_type_name.text()) // If cost center derivation logic applies read Company code at Header level
            'AccountAssignment'(){
                'CostCenter'(splittedSegment1[1]) //segment1 : FMNO:CostCenter
                'WBSElement'('')
                 }
            }else if(splittedSegment2[1] != null){ //segment_2 = CompanyCode:CostCenter
                'CompanyCode'(splittedSegment2[0])
                'AccountAssignment'(){
                    'CostCenter'(splittedSegment2[1])
                    'WBSElement'('')
                     }
            }else if(splittedSegment3[1] != null){ //segment_3 = CompanyCode:WBSElement
                'CompanyCode'(splittedSegment3[0])
                'AccountAssignment'(){
                    'CostCenter'('')
                    'WBSElement'(splittedSegment3[1])
                     }
                }
        if(li1.quantity.text() != '')
        'QuantityInEntryUnit'('unitCode': "PCE", li1.quantity.text())
   }
   return writer.toString()
}

//Header RefKey2 holds date of supply, Vendor Line RefKey2 : said to be mapped with goods received date
// Are date of supply (located at Lineitem level in coupa file) and Goods received (located at header level in Coupa file)  date same or different
//is any update yet to come for date of supply and goods received date mapping. Amr upadted goods received date field will be removed from coupa file

// def balancingLine(it1, taxAmountDifference, taxCode){
//     def writer = new StringWriter();
//     def builder = new MarkupBuilder(writer); 
    
//     def segment1 = ''
//     segment1 = it1.LineItems.LineItem[0].segment_1.text()
//     List<String[]> splittedSegment1 = segment1.split("\\:"); // segment_1 = FMNO:CostCenter
  
//     def segment2 = ''
//     segment2 = it1.LineItems.LineItem[0].segment_2.text()
//     List<String[]> splittedSegment2 = segment2.split("\\:");    // segment_2 = CompanyCode:CostCenter

//     def segment3 = ''
//     segment3 = it1.LineItems.LineItem[0].segment_3.text()
//     List<String[]> splittedSegment3 = segment3.split("\\:");    // segment_3 = CompanyCode:WBSElement

//     taxAmountDifference = Double.parseDouble(taxAmountDifference)

//     builder.Item(){
//         'ReferenceDocumentItem'('1')
//         'GLAccount'(it1.LineItems.LineItem[0].GL_Account.text())
//         'AmountInTransactionCurrency'('currencyCode':it1.currency.text(), taxAmountDifference) 
             
//         if(taxAmountDifference >= 0)
//             'DebitCreditCode'('S')
//         else
//             'DebitCreditCode'('H')
        
//         if(it1.LineItems.LineItem[0].description.text().length() > 50)
//             'DocumentItemText'(it1.LineItems.LineItem[0].description.text().substring(0,50))
//          else
//             'DocumentItemText'(it1.LineItems.LineItem[0].description.text())
            
//         'AssignmentReference'(splittedSegment1[0] + "|" + it1.LineItems.LineItem[0].Internal_Order.text()) //FMNO|Internal Order
//         'Tax'(){
//             'TaxCode'(taxCode) 
//          }

//         'Reference1IDByBusinessPartner'(it1.LineItems.LineItem[0].Asset_Class.text())
        
//         if(it1.LineItems.LineItem[0].personal_charge.text().length() > 12) //LAAS invoice requirement, send PErsonal Charge.  Earlier it was mapped with Quantity: (li1.quantity.text())
//             'Reference2IDByBusinessPartner'(it1.LineItems.LineItem[0].personal_charge.text().substring(0,12))
//         else
//             'Reference2IDByBusinessPartner'(it1.LineItems.LineItem[0].personal_charge.text())
        
//         'Reference3IDByBusinessPartner'(it1.LineItems.LineItem[0].hsnsac.text()) //HSN/SAC, have to be cleared by ABAP and substituted with original cost center in S4
        
//         if(it1.LineItems.LineItem[0].GL_Account.text() != '' && (it1.LineItems.LineItem[0].GL_Account.text() == '14000000' || it1.LineItems.LineItem[0].GL_Account.text() == '16100004' || it1.LineItems.LineItem[0].GL_Account.text() == '16100005' || it1.LineItems.LineItem[0].GL_Account.text() == '52000000' || it1.LineItems.LineItem[0].GL_Account.text() == '52000001' || it1.LineItems.LineItem[0].GL_Account.text() == '52000002' || it1.LineItems.LineItem[0].GL_Account.text() == '53000000')){ //Cost Center derivation logic: if GL account is in the given list use cost center in segment1 : FMNO:CostCenter
             
//             'CompanyCode'(it1.account_type_name.text()) // If cost center derivation logic applies read Company code at Header level
//             'AccountAssignment'(){
//                 'CostCenter'(splittedSegment1[1]) //segment1 : FMNO:CostCenter
//                 'WBSElement'('')
//                  }
//             }else if(splittedSegment2[1] != null){ //segment_2 = CompanyCode:CostCenter
//                 'CompanyCode'(splittedSegment2[0])
//                 'AccountAssignment'(){
//                     'CostCenter'(splittedSegment2[1])
//                     'WBSElement'('')
//                      }
//             }else if(splittedSegment3[1] != null){ //segment_3 = CompanyCode:WBSElement
//                 'CompanyCode'(splittedSegment3[0])
//                 'AccountAssignment'(){
//                     'CostCenter'('')
//                     'WBSElement'(splittedSegment3[1])
//                      }
//                 }
//         if(it1.LineItems.LineItem[0].quantity.text() != '')
//         'QuantityInEntryUnit'('unitCode': "PCE", it1.LineItems.LineItem[0].quantity.text())
//    }
//    return writer.toString()
// }