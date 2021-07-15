const defaultPassword = 'Password12';

module.exports = {
  swanseaLocalAuthorityUserOne: {
    email: 'kurt@swansea.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'kurt@swansea.gov.uk',
    surname: '(local-authority)',
  },
  swanseaLocalAuthorityUserTwo: {
    email: 'damian@swansea.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'damian@swansea.gov.uk',
    surname: '(local-authority)',
  },
  hillingdonLocalAuthorityUserOne: {
    email: 'sam@hillingdon.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'sam@hillingdon.gov.uk',
    surname: '(local-authority)',
  },
  hillingdonLocalAuthorityUserTwo: {
    email: 'siva@hillingdon.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'siva@hillingdon.gov.uk',
    surname: '(local-authority)',
  },
  wiltshireLocalAuthorityUserOne: {
    email: 'raghu@wiltshire.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'raghu@wiltshire.gov.uk',
    surname: '(local-authority)',
  },
  wiltshireLocalAuthorityUserTwo: {
    email: 'sam@wiltshire.gov.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
    forename: 'sam@wiltshire.gov.uk',
    surname: '(local-authority)',
  },
  localAuthorityBarristerUserOne: {
    email: 'la-barrister@mailnesia.com',
    password: process.env.LA_BARRISTER_USER_PASSWORD || defaultPassword,
    forename: 'la-barrister@mailnesia.com',
    surname: '(local-authority-barrister)',
  },
  hmctsAdminUser: {
    email: 'hmcts-admin@example.com',
    password: process.env.CA_USER_PASSWORD || defaultPassword,
  },
  hmctsSuperUser: {
    email: 'hmcts-superuser@mailnesia.com',
    password: process.env.SUPER_USER_PASSWORD || defaultPassword,
  },
  cafcassUser: {
    email: 'cafcass@example.com',
    password: process.env.CAFCASS_USER_PASSWORD || defaultPassword,
  },
  gateKeeperUser: {
    email: 'gatekeeper-only@mailnesia.com',
    password: process.env.GATEKEEPER_USER_PASSWORD || defaultPassword,
  },
  judicaryUser: {
    email: 'judiciary-only@mailnesia.com',
    password: process.env.JUDICIARY_USER_PASSWORD || defaultPassword,
  },
  magistrateUser: {
    email: 'magistrate@mailnesia.com',
    password: process.env.MAGISTRATE_USER_PASSWORD || defaultPassword,
  },
  systemUpdateUser: {
    email: process.env.SYSTEM_UPDATE_USER_USERNAME || 'fpl-system-update@mailnesia.com',
    password: process.env.SYSTEM_UPDATE_USER_PASSWORD || defaultPassword,
  },
  smokeTestUser: {
    email: process.env.SMOKE_TEST_LA_USER_USERNAME || 'james@swansea.gov.uk',
    password: process.env.SMOKE_TEST_LA_USER_PASSWORD || defaultPassword,
  },
  hmctsUser: {
    email: process.env.HMCTS_USER_USERNAME,
    password: process.env.HMCTS_USER_PASSWORD,
  },
  privateSolicitorOne: {
    email: 'solicitor1@solicitors.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
  },
  privateSolicitorTwo: {
    email: 'solicitor2@solicitors.uk',
    password: process.env.LA_USER_PASSWORD || defaultPassword,
  },
  localAuthorityPassword: process.env.LA_USER_PASSWORD || defaultPassword,
  baseUrl: process.env.URL || 'http://localhost:3333',
  fplServiceUrl: process.env.CASE_SERVICE_URL || 'http://localhost:4000',
  idamApiUrl: process.env.IDAM_API_URL || 'http://localhost:5000',
  dmStoreUrl: process.env.DM_STORE_URL || 'http://dm-store:8080',
  mockedPayment: (process.env.MOCKED_PAYMENTS || 'true').toLowerCase() === 'true',
  ctscEmail: process.env.CTSC_EMAIL || 'FamilyPublicLaw+ctsc@gmail.com',
  TestOutputDir: process.env.E2E_OUTPUT_DIR || './output',
  TestForAccessibility: process.env.TESTS_FOR_ACCESSIBILITY === 'true',
  definition: {
    jurisdiction: 'PUBLICLAW',
    jurisdictionFullDesc: 'Public Law',
    caseType: 'CARE_SUPERVISION_EPO',
    caseTypeFullDesc: 'Care, supervision and EPOs',
  },
  // actions
  applicationActions: {
    enterOrdersAndDirectionsNeeded: 'Orders and directions sought',
    enterHearingNeeded: 'Hearing urgency',
    enterChildren: 'Child\'s details',
    enterRespondents: 'Respondents\' details',
    enterApplicant: 'Applicant\'s details',
    enterOthers: 'Other people in the case',
    enterGrounds: 'Grounds for the application',
    enterRiskAndHarmToChildren: 'Risk and harm to children',
    enterFactorsAffectingParenting: 'Factors affecting parenting',
    enterInternationalElement: 'International element',
    enterOtherProceedings: 'Other proceedings',
    enterAllocationProposal: 'Allocation proposal',
    enterAllocationDecision: 'Allocation decision',
    enterAttendingHearing: 'Court services needed',
    uploadDocuments: 'Upload documents',
    changeCaseName: 'Change case name',
    submitCase: 'Submit application',
    deleteApplication: 'Delete an application',
    uploadCMO: 'Upload draft orders',
    approveOrders: 'Approve orders',
    allocatedJudge: 'Allocated Judge',
    extend26WeekTimeline: 'Extend 26-week timeline',
    manageLegalRepresentatives: 'Manage legal representatives',
    addApplicationDocuments: 'Application documents',
    manageDocumentsLA: 'Manage documents',
    messageJudge: 'Send and reply to messages',
    removeManagingOrganisation: 'Remove managing organisation',
    addOrRemoveLegalCounsel: 'Add or remove legal counsel',
  },
  administrationActions: {
    addFamilyManCaseNumber: 'Add case number',
    changeCaseName: 'Change case name',
    sendToGatekeeper: 'Send to gatekeeper',
    notifyGatekeeper: 'Notify gatekeeper',
    amendChildren: 'Children',
    amendRespondents: 'Respondents',
    amendOther: 'Others to be given notice',
    amendInternationalElement: 'International element',
    amendOtherProceedings: 'Other proceedings',
    amendAttendingHearing: 'Attending the hearing',
    amendRepresentatives: 'Manage representatives',
    manageHearings: 'Manage hearings',
    createNoticeOfProceedings: 'Create notice of proceedings',
    addStatementOfService: 'Add statement of service (c9)',
    uploadC2Documents: 'Upload a C2',
    addGatekeepingOrder: 'Add the gatekeeping order',
    createOrder: 'Create or upload an order',
    placement: 'Placement',
    handleSupplementaryEvidence: 'Handle supplementary evidence',
    bulkScan: 'Attach scanned docs',
    addNote: 'Add a case note',
    addExpertReportLog: 'Log expert report',
    closeTheCase: 'Close the case',
    returnApplication: 'Return application',
    manageDocuments: 'Manage documents',
    uploadAdditionalApplications: 'Upload additional applications',
    manageOrders: 'Manage orders',
  },
  superUserActions: {
    removeOrdersAndApplications: 'Remove orders and applications',
    changeCaseState: 'Change case state',
  },
  internalActions: {
    updateCase: 'internal-change-UPDATE_CASE',
  },
  // files
  testFile: './e2e/fixtures/testFiles/mockFile.txt',
  testPdfFile: './e2e/fixtures/testFiles/mockFile.pdf',
  testWordFile: './e2e/fixtures/testFiles/mockFile.docx',
  // urls
  presidentsGuidanceUrl: 'https://www.judiciary.uk/wp-content/uploads/2013/03/President%E2%80%99s-Guidance-on-Allocation-and-Gatekeeping.pdf',
  scheduleUrl: 'https://www.judiciary.uk/wp-content/uploads/2013/03/Schedule-to-the-President%E2%80%99s-Guidance-on-Allocation-and-Gatekeeping.pdf',
  otherProposalUrl: '/otherProposal/otherProposal1',
};
