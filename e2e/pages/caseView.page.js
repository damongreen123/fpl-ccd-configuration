const { I } = inject();
const assert = require('assert');
const output = require('codeceptjs').output;

const getTabSelector = tab => `//*[@role="tab"][child::div[text() = "${tab}"]]`;

module.exports = {

  file: 'mockFile.txt',
  tabs: {
    summary: 'Summary',
    history: 'History',
    orders: 'Orders',
    draftOrders: 'Draft orders',
    hearings: 'Hearings',
    casePeople: 'People in the case',
    changeOfRepresentatives: 'Change of representatives',
    legalBasis: 'Legal basis',
    documentsSentToParties: 'Documents sent to parties',
    c2: 'C2',
    confidential: 'Confidential information',
    placement: 'Placement',
    paymentHistory: 'Payment History',
    notes: 'Notes',
    expertReports: 'Expert Reports',
    overview: 'Overview',
    viewApplication: 'View application',
    startApplication: 'Start application',
    correspondence: 'Correspondence',
    courtBundle: 'Court bundle',
    judicialMessages: 'Judicial messages',
    otherApplications: 'Other applications',
    furtherEvidence: 'Documents',
  },
  actionsDropdown: '.ccd-dropdown',
  goButton: 'Go',
  caseTitle: '.case-title .markdown',
  tasksErrorsTitle: 'Why can\'t I submit my application?',

  async goToNewActions(actionSelected) {
    const currentUrl = await I.grabCurrentUrl();
    await I.retryUntilExists(async () => {
      if(await I.waitForSelector(this.actionsDropdown, 30) != null) {
        await I.scrollToElement(this.actionsDropdown);
        I.selectOption(this.actionsDropdown, actionSelected);
        I.click(this.goButton);
      } else {
        const newUrl = await I.grabCurrentUrl();
        if(newUrl === currentUrl || !newUrl.includes('http')){
          output.print('Page refresh');
          I.refreshPage();
        }
      }
    }, 'ccd-case-event-trigger', false);
  },

  async checkActionsAreAvailable(actions) {
    I.waitForElement(this.actionsDropdown, 10);
    await within(this.actionsDropdown, () => {
      for (const action of actions) {
        I.seeElementInDOM(`//option[text()="${action}"]`);
      }
    });
  },

  async checkActionsAreNotAvailable(actions) {
    I.waitForElement(this.actionsDropdown, 10);
    await within(this.actionsDropdown, () => {
      for (const action of actions) {
        I.dontSeeElementInDOM(`//option[text()="${action}"]`);
      }
    });
  },

  checkTaskStatus(task, status) {
    I.waitForElement(locate(`//p/a[text()="${task}"]`), 10);
    if(status) {
      I.seeElement(locate(`//p/a[text()="${task}"]/../img`).withAttr({title: status}));
    } else {
      I.seeElement(locate(`//p/a[text()="${task}"]`));
      I.dontSeeElement(locate(`//p/a[text()="${task}"]/../img`));
    }
  },

  checkTaskIsFinished(task) {
    this.checkTaskStatus(task, 'Finished');
  },

  checkTaskIsCompleted(task) {
    this.checkTaskStatus(task, 'Information added');
  },

  checkTaskIsInProgress(task) {
    this.checkTaskStatus(task, 'In progress');
  },

  checkTaskIsNotStarted(task) {
    this.checkTaskStatus(task, undefined);
  },

  async checkTaskIsAvailable(task) {
    await I.retryUntilExists(() => {
      I.click(task);
    }, 'ccd-case-event-trigger');
    await I.retryUntilExists(() => {
      I.click('Cancel');
    }, this.caseTitle);
  },

  async checkTaskIsUnavailable(task) {
    this.checkTaskStatus(task, 'Cannot send yet');
    const taskTarget = await I.grabAttribute(`//p/a[text()="${task}"]`, 'href');
    assert.strictEqual(!!taskTarget, false);
  },

  async checkTasksHaveErrors(tasksErrors) {
    I.see(this.tasksErrorsTitle);
    I.click(`//p[text() = "${this.tasksErrorsTitle}"]`);

    const errors = (await I.grabTextFrom('details div'))
      .replace('\n\n','\n')
      .split('\n')
      .filter(item => item);

    assert.deepStrictEqual(errors, tasksErrors);
  },

  async checkTasksHaveNoErrors() {
    I.dontSee(this.tasksErrorsTitle);
  },

  async startTask(task) {
    await I.retryUntilExists(() => {
      I.click(task);
    }, 'ccd-case-event-trigger');
    await I.runAccessibilityTest();
  },

  checkTabIsNotPresent(tab) {
    I.dontSee(getTabSelector(tab));
  },

  async selectTab(tab){
    const tabSelector = getTabSelector(tab);

    const numberOfElements = await I.grabNumberOfVisibleElements('//*[@role="tab"]');

    for(let i=0; i<numberOfElements; i++){
      if((await I.canClick(tabSelector))){
        break;
      }
      output.debug(`Scrolling to tab '${tab}'`);
      I.click('.mat-tab-header-pagination-after');
    }

    I.click(tabSelector);

    const id = await I.grabAttributeFrom(tabSelector, 'id');

    return I.waitForVisible(`//mat-tab-body[contains(@class, "mat-tab-body-active")][@aria-labelledby="${id}"]//table`);
  },

  seeInCaseTitle(titleValue) {
    I.seeElement(locate(this.caseTitle).withText(titleValue));
  },
};
