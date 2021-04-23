const config = require('../config.js');
const mandatorySubmissionFields = require('../fixtures/caseData/mandatorySubmissionFields.json');

let caseId;
const referenceErrorMessage = 'Enter an online case reference number that exactly matches the case details';
const detailsErrorMessage = 'Enter the client details exactly as they’re written on the case, including any mistakes';

Feature('Notice of change');

BeforeSuite(async ({I}) => {
  caseId = await I.submitNewCaseWithData(mandatorySubmissionFields);
});

// To be removed once ExUI have added their own field validation testing:
// https://tools.hmcts.net/jira/browse/EUI-3837
Scenario('Private solicitor sees errors when attempting access through NoC with invalid details', async ({I, noticeOfChangePage}) => {
  await I.signIn(config.privateSolicitorOne);
  I.navigateToCaseList();
  noticeOfChangePage.navigate();
  await noticeOfChangePage.enterCaseReference('1111-2222-3333-4444');
  I.click('Continue');
  I.waitForText(referenceErrorMessage);
  await noticeOfChangePage.enterCaseReference(caseId);
  await I.retryUntilExists(() => I.click('Continue'), noticeOfChangePage.fields.applicantName);
  await noticeOfChangePage.enterApplicantName('Wrong detail');
  noticeOfChangePage.enterRespondentName('Joe', 'Bloggs');
  I.click('Continue');
  I.waitForText(detailsErrorMessage);
  await noticeOfChangePage.enterApplicantName('Swansea City Council');
  noticeOfChangePage.enterRespondentName('Wrong', 'detail');
  I.click('Continue');
  I.waitForText(detailsErrorMessage);
});

Scenario('Private solicitor obtains case access through NoC', async ({I, caseListPage, noticeOfChangePage}) => {
  await I.signIn(config.privateSolicitorOne);
  I.navigateToCaseList();
  caseListPage.searchForCasesWithId(caseId);
  I.dontSeeCaseInSearchResult(caseId);
  noticeOfChangePage.navigate();
  await noticeOfChangePage.enterCaseReference(caseId);
  await I.retryUntilExists(() => I.click('Continue'), noticeOfChangePage.fields.applicantName);
  await noticeOfChangePage.enterApplicantName('Swansea City Council');
  noticeOfChangePage.enterRespondentName('Joe', 'Bloggs');
  await I.retryUntilExists(() => I.click('Continue'), noticeOfChangePage.fields.confirmNoC);
  noticeOfChangePage.confirmNoticeOfChange();
  await I.retryUntilExists(() => I.click('Submit'), '.govuk-panel--confirmation');
  I.see('Notice of change successful');
  I.navigateToCaseList();
  caseListPage.searchForCasesWithId(caseId);
  I.seeCaseInSearchResult(caseId);
});
