const I = actor();

module.exports = {

  fields: {
    consentCheckbox: '#submissionConsent-agree', 
  },
  
  giveConsent() {
    I.checkOption(this.fields.consentCheckbox);
  },
};
