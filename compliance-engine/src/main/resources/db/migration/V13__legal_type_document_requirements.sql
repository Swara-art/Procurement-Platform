CREATE TABLE business_legal_types (
    code VARCHAR(40) PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE document_types (
    code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL UNIQUE,
    description VARCHAR(500),
    upload_category VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE legal_type_document_requirements (
    legal_type_code VARCHAR(40) NOT NULL REFERENCES business_legal_types(code),
    document_type_code VARCHAR(50) NOT NULL REFERENCES document_types(code),
    requirement_level VARCHAR(20) NOT NULL CHECK (requirement_level IN ('REQUIRED','CONDITIONAL','TENDER_SPECIFIC')),
    condition_expression VARCHAR(200),
    alternative_group VARCHAR(80),
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (legal_type_code, document_type_code)
);
CREATE INDEX idx_legal_requirements_type ON legal_type_document_requirements(legal_type_code, active, sort_order);

ALTER TABLE bidder_profiles ADD COLUMN llpin VARCHAR(8);
ALTER TABLE bidder_profiles ADD COLUMN government_owned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bidder_profiles ADD COLUMN firm_registered BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bidder_profiles ADD COLUMN representative_is_signatory BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE bidder_profiles ADD COLUMN msme_benefit_claimed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bidder_profiles ADD COLUMN shop_registration_applicable BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bidder_profiles ADD COLUMN business_license_available BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bidder_profiles ADD COLUMN incorporated_entity BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE bidder_profiles ADD COLUMN pan_applicable BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE bidder_profiles ADD COLUMN section8_tax_registration_applicable BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO business_legal_types(code,display_name,description,sort_order) VALUES
 ('PRIVATE_LIMITED','Private Limited Company','Private company incorporated under company law.',1),
 ('PUBLIC_LIMITED','Public Limited Company','Public company; stock exchange listing is not implied.',2),
 ('OPC','One Person Company','A one person incorporated company.',3),
 ('LLP','Limited Liability Partnership','Limited liability partnership.',4),
 ('PARTNERSHIP','Partnership Firm','Registered or unregistered partnership firm.',5),
 ('SOLE_PROPRIETORSHIP','Sole Proprietorship','Business operated by an individual proprietor.',6),
 ('SECTION_8','Section 8 Company','Company registered for not-for-profit objectives.',7),
 ('GOVERNMENT_ENTITY','Government Entity / PSU','Government controlled company or statutory entity; legal structure/ownership may be recorded separately.',8);

INSERT INTO document_types(code,display_name,description,upload_category) VALUES
 ('CERTIFICATE_OF_INCORPORATION','Certificate of Incorporation','Company or LLP incorporation evidence.','CERTIFICATE_OF_INCORPORATION'),
 ('COMPANY_PAN','Business PAN','Company, firm, or proprietor PAN evidence.','COMPANY_PAN'),
 ('MOA','Memorandum of Association','Memorandum of Association.','MOA'),
 ('AOA','Articles of Association','Articles of Association.','AOA'),
 ('GST_CERTIFICATE','GST Registration Certificate','GST registration evidence where applicable.','GST_CERTIFICATE'),
 ('AUTHORIZATION_LETTER','Board / Partner Authorization','Authority for the submitting representative.','AUTHORIZATION_LETTER'),
 ('BANK_ACCOUNT_PROOF','Business Bank Account Proof','Proof of business bank account.','BANK_ACCOUNT_PROOF'),
 ('REGISTERED_ADDRESS_PROOF','Registered / Business Address Proof','Proof of registered or business address.','REGISTERED_ADDRESS_PROOF'),
 ('SIGNATORY_IDENTITY_PROOF','Authorized Signatory Identity Proof','Identity evidence for the authorized signatory.','SIGNATORY_IDENTITY_PROOF'),
 ('AUDITED_FINANCIAL_STATEMENTS','Audited Financial Statements','Financial statements when required by a tender.','AUDITED_FINANCIAL_STATEMENTS'),
 ('UDYAM_CERTIFICATE','Udyam / MSME Certificate','Optional MSME evidence or tender-specific benefit claim.','UDYAM_CERTIFICATE'),
 ('LLP_AGREEMENT','LLP Agreement','LLP agreement evidence.','LLP_AGREEMENT'),
 ('DESIGNATED_PARTNER_ID','Designated Partner Identity Proof','Identity evidence for a designated partner.','DESIGNATED_PARTNER_ID'),
 ('PARTNERSHIP_DEED','Partnership Deed','Partnership deed.','PARTNERSHIP_DEED'),
 ('FIRM_REGISTRATION_CERTIFICATE','Firm Registration Certificate','Evidence of firm registration, where registered.','FIRM_REGISTRATION_CERTIFICATE'),
 ('PARTNER_IDENTITY_PROOF','Signing Partner Identity Proof','Identity evidence for signing partner.','PARTNER_IDENTITY_PROOF'),
 ('PROPRIETOR_IDENTITY_PROOF','Proprietor Identity Proof','Identity evidence for proprietor.','PROPRIETOR_IDENTITY_PROOF'),
 ('BUSINESS_EXISTENCE_EVIDENCE','Proof of Business Existence','Provide one acceptable proof in this alternative group.','BUSINESS_EXISTENCE_EVIDENCE'),
 ('SHOP_ESTABLISHMENT_CERTIFICATE','Shop & Establishment Registration','Where applicable; may also satisfy business existence evidence.','SHOP_ESTABLISHMENT_CERTIFICATE'),
 ('BUSINESS_LICENSE','Business / Trade Licence','Recognized business or trade licence.','BUSINESS_LICENSE'),
 ('SECTION_8_APPROVAL','Section 8 Licence / Approval','Applicable Section 8 approval evidence.','SECTION_8_APPROVAL'),
 ('12AB_80G_CERTIFICATE','12AB / 80G Registration','Only where applicable or claimed.','12AB_80G_CERTIFICATE'),
 ('GOVERNMENT_ESTABLISHMENT_PROOF','Government Establishment / PSU Status Proof','Establishment order or proof supporting claimed government status.','GOVERNMENT_ESTABLISHMENT_PROOF'),
 ('OFFICER_IDENTITY_PROOF','Authorized Government Officer Identity Proof','Identity evidence for authorized officer.','OFFICER_IDENTITY_PROOF'),
 ('DELEGATION_LETTER','Authorization / Delegation Letter','Delegation or authorization evidence.','DELEGATION_LETTER'),
 ('INDUSTRY_LICENSE','Applicable Industry Licence','Tender, sector, or policy-specific licence.','INDUSTRY_LICENSE'),
 ('CAPABILITY_DECLARATION','Company Capability Declaration','Bidder capability declaration.','CAPABILITY_DECLARATION');

-- Company requirements are platform onboarding recommendations; tender-specific items remain separate.
INSERT INTO legal_type_document_requirements(legal_type_code,document_type_code,requirement_level,condition_expression,alternative_group,sort_order) VALUES
 ('PRIVATE_LIMITED','CERTIFICATE_OF_INCORPORATION','REQUIRED',NULL,NULL,1),('PRIVATE_LIMITED','COMPANY_PAN','REQUIRED',NULL,NULL,2),('PRIVATE_LIMITED','MOA','REQUIRED',NULL,NULL,3),('PRIVATE_LIMITED','AOA','REQUIRED',NULL,NULL,4),('PRIVATE_LIMITED','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,5),('PRIVATE_LIMITED','AUTHORIZATION_LETTER','REQUIRED',NULL,NULL,6),('PRIVATE_LIMITED','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('PRIVATE_LIMITED','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('PRIVATE_LIMITED','SIGNATORY_IDENTITY_PROOF','REQUIRED',NULL,NULL,9),('PRIVATE_LIMITED','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,10),('PRIVATE_LIMITED','UDYAM_CERTIFICATE','CONDITIONAL','msmeBenefitClaimed',NULL,11),
 ('PUBLIC_LIMITED','CERTIFICATE_OF_INCORPORATION','REQUIRED',NULL,NULL,1),('PUBLIC_LIMITED','COMPANY_PAN','REQUIRED',NULL,NULL,2),('PUBLIC_LIMITED','MOA','REQUIRED',NULL,NULL,3),('PUBLIC_LIMITED','AOA','REQUIRED',NULL,NULL,4),('PUBLIC_LIMITED','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,5),('PUBLIC_LIMITED','AUTHORIZATION_LETTER','REQUIRED',NULL,NULL,6),('PUBLIC_LIMITED','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('PUBLIC_LIMITED','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('PUBLIC_LIMITED','SIGNATORY_IDENTITY_PROOF','REQUIRED',NULL,NULL,9),('PUBLIC_LIMITED','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,10),('PUBLIC_LIMITED','INDUSTRY_LICENSE','CONDITIONAL','sectorOrPolicyRequiresLicence',NULL,11),
 ('OPC','CERTIFICATE_OF_INCORPORATION','REQUIRED',NULL,NULL,1),('OPC','COMPANY_PAN','REQUIRED',NULL,NULL,2),('OPC','MOA','REQUIRED',NULL,NULL,3),('OPC','AOA','REQUIRED',NULL,NULL,4),('OPC','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,5),('OPC','SIGNATORY_IDENTITY_PROOF','REQUIRED',NULL,NULL,6),('OPC','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('OPC','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('OPC','AUTHORIZATION_LETTER','CONDITIONAL','representativeIsDifferent',NULL,9),('OPC','UDYAM_CERTIFICATE','CONDITIONAL','msmeBenefitClaimed',NULL,10),
 ('LLP','CERTIFICATE_OF_INCORPORATION','REQUIRED',NULL,NULL,1),('LLP','LLP_AGREEMENT','REQUIRED',NULL,NULL,2),('LLP','COMPANY_PAN','REQUIRED',NULL,NULL,3),('LLP','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,4),('LLP','DESIGNATED_PARTNER_ID','REQUIRED',NULL,NULL,5),('LLP','AUTHORIZATION_LETTER','CONDITIONAL','representativeIsDifferent',NULL,6),('LLP','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('LLP','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('LLP','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,9),('LLP','UDYAM_CERTIFICATE','CONDITIONAL','msmeBenefitClaimed',NULL,10),
 ('PARTNERSHIP','PARTNERSHIP_DEED','REQUIRED',NULL,NULL,1),('PARTNERSHIP','COMPANY_PAN','REQUIRED',NULL,NULL,2),('PARTNERSHIP','FIRM_REGISTRATION_CERTIFICATE','CONDITIONAL','firmRegistered=true',NULL,3),('PARTNERSHIP','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,4),('PARTNERSHIP','PARTNER_IDENTITY_PROOF','REQUIRED',NULL,NULL,5),('PARTNERSHIP','AUTHORIZATION_LETTER','CONDITIONAL','representativeIsDifferent',NULL,6),('PARTNERSHIP','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('PARTNERSHIP','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('PARTNERSHIP','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,9),('PARTNERSHIP','UDYAM_CERTIFICATE','CONDITIONAL','msmeBenefitClaimed',NULL,10),
 ('SOLE_PROPRIETORSHIP','COMPANY_PAN','REQUIRED',NULL,NULL,1),('SOLE_PROPRIETORSHIP','PROPRIETOR_IDENTITY_PROOF','REQUIRED',NULL,NULL,2),('SOLE_PROPRIETORSHIP','BUSINESS_EXISTENCE_EVIDENCE','REQUIRED',NULL,'BUSINESS_EXISTENCE',3),('SOLE_PROPRIETORSHIP','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true','BUSINESS_EXISTENCE',4),('SOLE_PROPRIETORSHIP','UDYAM_CERTIFICATE','CONDITIONAL','msmeBenefitClaimed','BUSINESS_EXISTENCE',5),('SOLE_PROPRIETORSHIP','SHOP_ESTABLISHMENT_CERTIFICATE','CONDITIONAL','shopRegistrationApplicable','BUSINESS_EXISTENCE',6),('SOLE_PROPRIETORSHIP','BUSINESS_LICENSE','CONDITIONAL','businessLicenseAvailable','BUSINESS_EXISTENCE',7),('SOLE_PROPRIETORSHIP','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,8),('SOLE_PROPRIETORSHIP','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,9),('SOLE_PROPRIETORSHIP','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,10),
 ('SECTION_8','CERTIFICATE_OF_INCORPORATION','REQUIRED',NULL,NULL,1),('SECTION_8','SECTION_8_APPROVAL','REQUIRED',NULL,NULL,2),('SECTION_8','COMPANY_PAN','REQUIRED',NULL,NULL,3),('SECTION_8','MOA','REQUIRED',NULL,NULL,4),('SECTION_8','AOA','REQUIRED',NULL,NULL,5),('SECTION_8','AUTHORIZATION_LETTER','REQUIRED',NULL,NULL,6),('SECTION_8','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,7),('SECTION_8','REGISTERED_ADDRESS_PROOF','REQUIRED',NULL,NULL,8),('SECTION_8','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,9),('SECTION_8','12AB_80G_CERTIFICATE','CONDITIONAL','section8TaxRegistrationApplicable',NULL,10),('SECTION_8','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,11),
 ('GOVERNMENT_ENTITY','GOVERNMENT_ESTABLISHMENT_PROOF','REQUIRED',NULL,NULL,1),('GOVERNMENT_ENTITY','COMPANY_PAN','CONDITIONAL','panApplicable=true',NULL,2),('GOVERNMENT_ENTITY','CERTIFICATE_OF_INCORPORATION','CONDITIONAL','isIncorporated=true',NULL,3),('GOVERNMENT_ENTITY','MOA','CONDITIONAL','isIncorporated=true',NULL,4),('GOVERNMENT_ENTITY','AOA','CONDITIONAL','isIncorporated=true',NULL,5),('GOVERNMENT_ENTITY','OFFICER_IDENTITY_PROOF','REQUIRED',NULL,NULL,6),('GOVERNMENT_ENTITY','DELEGATION_LETTER','REQUIRED',NULL,NULL,7),('GOVERNMENT_ENTITY','BANK_ACCOUNT_PROOF','REQUIRED',NULL,NULL,8),('GOVERNMENT_ENTITY','GST_CERTIFICATE','CONDITIONAL','gstApplicable=true',NULL,9),('GOVERNMENT_ENTITY','AUDITED_FINANCIAL_STATEMENTS','TENDER_SPECIFIC','tenderRequiresFinancials',NULL,10);

-- Preserve existing profiles through aliases; legal_type_code remains separate from government ownership.
UPDATE bidder_profiles SET entity_type = CASE entity_type WHEN 'PROPRIETORSHIP' THEN 'SOLE_PROPRIETORSHIP' WHEN 'PRIVATE_LIMITED' THEN 'PRIVATE_LIMITED' WHEN 'PUBLIC_LIMITED' THEN 'PUBLIC_LIMITED' ELSE entity_type END;
