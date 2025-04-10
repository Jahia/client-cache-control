describe('Cache Control config tests', () => {
    it.skip('TestCase 1 (graphql): List available rules', () => {
        cy.login();
        cy.log('Getting rules list from graphql to check configuration');
        cy.apollo({
            queryFile: 'listRules.graphql'
        }).then(response => {
            cy.log(JSON.stringify(response));
            const rules = response?.data?.admin?.jahia?.clientCacheControl?.rules;
            expect(rules).to.not.be.empty;
            expect(rules.length).to.be.greaterThan(0);
            expect(rules[0].priority).to.be.equal('1.0');
            expect(rules[0].urlRegexp).to.be.equal('(?:/[^/]+)?/cms/render/live/.*');
            expect(rules[8].priority).to.be.equal('8.9');
            expect(rules[8].header).to.be.equal('public, plop, tagada');
        });
        cy.logout();
    });

    it('TestCase 1 (ssh): List available rules', () => {
        cy.task('sshCommand', ['jahia:client-cache-list-rules json']).then((result: string) => {
            const rules = JSON.parse(result);
            cy.log(JSON.stringify(rules));
            expect(rules).to.not.be.empty;
            expect(rules.length).to.be.greaterThan(0);
            expect(rules[0].priority).to.be.equal(1);
            expect(rules[0].urlRegexp).to.be.equal('(?:/[^/]+)?/cms/render/live/.*');
            expect(rules[8].priority).to.be.equal(8.9);
            expect(rules[8].header).to.be.equal('public, plop, tagada');
        });
    });
});
