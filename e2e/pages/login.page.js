const { I } = inject();

module.exports = {

  fields: {
    username: '#username',
    password: '#password',
  },
  submitButton: 'input[value="Sign in"]',

  async signIn(user) {
    console.log('login page signIn');
    await I.waitForSelector(this.fields.username);
    await I.grabCurrentUrl();
    I.fillField(this.fields.username, user.email);
    I.fillField(this.fields.password, user.password);

    await I.waitForSelector(this.submitButton);
    I.click(this.submitButton);
    await I.grabCurrentUrl();
  },

};
