const config = require('../config.js');
const mandatoryWithMaxChildren = require('../fixtures/caseData/mandatoryWithMaxChildren.json');
const apiHelper = require('../helpers/api_helper.js');
const moment = require('moment');

const solicitor1 = config.privateSolicitorOne;
const solicitor2 = config.hillingdonLocalAuthorityUserOne;
const solicitor3 = config.wiltshireLocalAuthorityUserOne;
const children = mandatoryWithMaxChildren.caseData.children1;

const unregisteredSolicitor = {
  forename: 'Rupert',
  surname: 'Bear',
  email: 'rupert@bear.com',
  unregisteredOrganisation: {
    name: 'Swansea Managing Office',
    address: {
      lookupOption: 'The Tower, Trawler Rd, Maritime Quarter, Swansea',
      buildingAndStreet: {
        lineOne: 'The Tower',
        lineTwo: 'Trawlery Rd',
        lineThree: 'Maritime Quarter',
      },
      town: 'Swansea',
      postcode: 'SA1 1JW',
      country: 'United Kingdom',
    },
  },
};

let caseId;

Feature('Child solicitors');

async function setupScenario(I) {
  if (!caseId) {
    caseId = await I.submitNewCaseWithData(mandatoryWithMaxChildren);
  }
  if (!solicitor1.details) {
    solicitor1.details = await apiHelper.getUser(solicitor1);
    solicitor1.details.organisation = 'Private solicitors';
  }
  if (!solicitor2.details) {
    solicitor2.details = await apiHelper.getUser(solicitor2);
    solicitor2.details.organisation = 'London Borough Hillingdon';
  }
  if (!solicitor3.details) {
    solicitor3.details = await apiHelper.getUser(solicitor3);
    solicitor3.details.organisation = 'Wiltshire County Council';
  }
}

Scenario('Solicitor cannot request representation before case submission and Cafcass solicitor is set', async ({I, caseListPage, caseViewPage, submitApplicationEventPage, noticeOfChangePage}) => {
  await setupScenario(I);
  await I.signIn(solicitor1);
  caseListPage.verifyCaseIsNotAccessible(caseId);

  await noticeOfChangePage.navigate();
  await noticeOfChangePage.enterCaseReference(caseId);
  I.click('Continue');
  I.see('Your notice of change request has not been submitted');

  await I.navigateToCaseDetailsAs(config.swanseaLocalAuthorityUserOne, caseId);
  await caseViewPage.goToNewActions(config.applicationActions.submitCase);
  await submitApplicationEventPage.giveConsent();
  await I.completeEvent('Submit', null, true);

  await attemptAndFailNoticeOfChange(I, noticeOfChangePage, solicitor2, children[0]);
});

Scenario('HMCTS Confirm that a main solicitor in not assigned for all the children yet', async ({I, caseViewPage, enterChildrenEventPage, noticeOfChangePage}) => {
  await setupScenario(I);
  await I.navigateToCaseDetailsAs(config.hmctsAdminUser, caseId);
  await caseViewPage.goToNewActions(config.administrationActions.amendChildren);
  await I.goToNextPage();
  enterChildrenEventPage.selectAnyChildHasLegalRepresentation(enterChildrenEventPage.fields().mainSolicitor.childrenHaveLegalRepresentation.options.no);
  await I.completeEvent('Save and continue');
  I.seeEventSubmissionConfirmation(config.administrationActions.amendChildren);
  await attemptAndFailNoticeOfChange(I, noticeOfChangePage, solicitor2, children[0]);
});

Scenario('HMCTS assign a main solicitor for all the children', async ({I, caseViewPage, enterChildrenEventPage}) => {
  await setupScenario(I);
  await I.navigateToCaseDetailsAs(config.hmctsAdminUser, caseId);
  await caseViewPage.goToNewActions(config.administrationActions.amendChildren);
  await I.goToNextPage();
  enterChildrenEventPage.selectAnyChildHasLegalRepresentation(enterChildrenEventPage.fields().mainSolicitor.childrenHaveLegalRepresentation.options.yes);
  enterChildrenEventPage.enterChildrenMainRepresentation(solicitor1);
  await enterChildrenEventPage.enterRegisteredOrganisation(solicitor1);
  await I.goToNextPage();
  enterChildrenEventPage.selectChildrenHaveSameRepresentation(enterChildrenEventPage.fields().mainSolicitor.childrenHaveSameRepresentation.options.yes);
  await I.completeEvent('Save and continue');
  I.seeEventSubmissionConfirmation(config.administrationActions.amendChildren);
  caseViewPage.selectTab(caseViewPage.tabs.casePeople);
  mandatoryWithMaxChildren.caseData.children1.forEach((element, index) => assertChild(I, index + 1, element.value, solicitor1));
});

Scenario('HMCTS assign a different solicitor for some of the children', async ({I, caseViewPage, enterChildrenEventPage}) => {
  await setupScenario(I);

  await I.goToNextPage();
  enterChildrenEventPage.selectAnyChildHasLegalRepresentation(enterChildrenEventPage.fields().mainSolicitor.childrenHaveLegalRepresentation.options.yes);
  enterChildrenEventPage.enterChildrenMainRepresentation(solicitor1);
  await enterChildrenEventPage.enterRegisteredOrganisation(solicitor1);
  await I.runAccessibilityTest();
  await I.goToNextPage();

  enterChildrenEventPage.selectChildrenHaveSameRepresentation(enterChildrenEventPage.fields().mainSolicitor.childrenHaveSameRepresentation.options.no);
  for (const [index, child] of children.entries()) {
    await enterChildrenEventPage.selectChildUseMainRepresentation(enterChildrenEventPage.fields(index).childSolicitor.useMainSolicitor.options.yes, index, child.value.party);
  }

  const childWithDifferentRegisteredSolicitorIdx = 2;
  const childWithUnregisteredSolicitorIdx = 3;
  await setSpecificRepresentative(enterChildrenEventPage, childWithDifferentRegisteredSolicitorIdx, children[childWithDifferentRegisteredSolicitorIdx].value.party, solicitor2);
  await setSpecificRepresentative(enterChildrenEventPage, childWithUnregisteredSolicitorIdx, children[childWithUnregisteredSolicitorIdx].value.party, unregisteredSolicitor);
  await I.runAccessibilityTest();
  await I.completeEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.administrationActions.amendChildren);
  caseViewPage.selectTab(caseViewPage.tabs.casePeople);

  for (const [index, child] of children.entries()) {
    const solicitor = index === childWithDifferentRegisteredSolicitorIdx ? solicitor2 : (index === childWithUnregisteredSolicitorIdx ? unregisteredSolicitor : solicitor1);
    assertChild(I, index + 1, child.value, solicitor);
  }
});

Scenario('Solicitor can request representation only after case submission and Cafcass solicitor is set', async ({I, caseListPage, noticeOfChangePage}) => {
  await setupScenario(I);
  await I.signIn(solicitor1);
  caseListPage.verifyCaseIsNotAccessible(caseId);

  await noticeOfChangePage.userCompletesNoC(caseId, 'Swansea City Council', children[0].value.party.firstName, children[0].value.party.lastName);
});

async function setSpecificRepresentative(enterChildrenEventPage, idx, child, solicitor) {
  await enterChildrenEventPage.selectChildUseMainRepresentation(enterChildrenEventPage.fields(idx).childSolicitor.useMainSolicitor.options.no, idx, child);
  enterChildrenEventPage.enterChildrenSpecificRepresentation(idx, solicitor);
  if (solicitor.unregisteredOrganisation) {
    await enterChildrenEventPage.enterSpecificUnregisteredOrganisation(idx, solicitor);
  } else {
    await enterChildrenEventPage.enterSpecificRegisteredOrganisation(idx, solicitor);
  }
}

function assertChild(I, idx, child, solicitor) {
  const childElement = `Child ${idx}`;

  I.seeInTab([childElement, 'Party', 'First name'], child.party.firstName);
  I.seeInTab([childElement, 'Party', 'Last name'], child.party.lastName);
  I.seeInTab([childElement, 'Party', 'Date of birth'], moment(child.party.dateOfBirth, 'YYYY-MM-DD').format('D MMM YYYY'));
  I.seeInTab([childElement, 'Party', 'Gender'], child.party.gender);

  if (solicitor) {
    I.seeInTab([childElement, 'Representative', 'Representative\'s first name'], solicitor.details.forename);
    I.seeInTab([childElement, 'Representative', 'Representative\'s last name'], solicitor.details.surname);
    I.seeInTab([childElement, 'Representative', 'Email address'], solicitor.details.email);
    if (solicitor.unregisteredOrganisation) {
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation name'], solicitor.unregisteredOrganisation.name);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Building and Street'], solicitor.unregisteredOrganisation.address.buildingAndStreet.lineOne);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Address Line 2'], solicitor.unregisteredOrganisation.address.buildingAndStreet.lineTwo);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Address Line 3'], solicitor.unregisteredOrganisation.address.buildingAndStreet.lineThree);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Town or City'], solicitor.unregisteredOrganisation.address.town);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Postcode/Zipcode'], solicitor.unregisteredOrganisation.address.postcode);
      I.seeInTab([childElement, 'Representative', 'Organisation (unregistered)', 'Organisation address', 'Country'], solicitor.unregisteredOrganisation.address.country);
    } else {
      I.waitForText(solicitor.organisation, 40);
      I.seeOrganisationInTab([childElement, 'Representative', 'Name'], solicitor.details.organisation);
    }
  }
}

async function attemptAndFailNoticeOfChange(I, noticeOfChangePage, solicitor, child) {
  await I.signIn(solicitor);
  await noticeOfChangePage.userFillsNoC(caseId, 'Swansea City Council', child.value.party.firstName, child.value.party.lastName);
  I.click('Continue');
  I.see('Enter the client details exactly as they’re written on the case, including any mistakes');
}
