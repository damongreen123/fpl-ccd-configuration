const {I} = inject();

module.exports = {
  fields: function (index) {

    return {
      legalCounsellor: {
        firstName: `#listOfLegalCounsellors_${index}_firstName`,
        lastName: `#listOfLegalCounsellors_${index}_lastName`,
        organisation: `#listOfLegalCounsellors_${index}_organisation`,
        email: `#listOfLegalCounsellors_${index}_email`,
        telephone: `[id="listOfLegalCounsellors_${index}_telephoneNumber"]`,
      },
    };
  },

  async addLegalCounsellor(legalRepresentative) {

    const elementIndex = await this.getActiveElementIndex();

    if(legalRepresentative.firstName) {
      I.fillField(this.fields(elementIndex).legalCounsellor.firstName, legalRepresentative.firstName);
    }
    if(legalRepresentative.lastName) {
      I.fillField(this.fields(elementIndex).legalCounsellor.lastName, legalRepresentative.lastName);
    }
    if(legalRepresentative.organisation) {
      I.fillField(this.fields(elementIndex).legalCounsellor.organisation, legalRepresentative.organisation);
    }
    if(legalRepresentative.email) {
      I.fillField(this.fields(elementIndex).legalCounsellor.email, legalRepresentative.email);
    }
    if(legalRepresentative.telephone) {
      I.fillField(this.fields(elementIndex).legalCounsellor.telephone, legalRepresentative.telephone);
    }
    await I.runAccessibilityTest();
  },

  async getActiveElementIndex() {
    return await I.grabNumberOfVisibleElements('//button[text()="Remove"]') - 1;
  },
};
