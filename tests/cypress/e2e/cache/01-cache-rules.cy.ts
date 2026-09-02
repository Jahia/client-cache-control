// The command returns every rule registered on the server, from the test rulesets and from any
// module that ships one of its own, so a rule is looked up by what identifies it rather than by
// the position it happens to hold.
describe('Cache Control config tests', () => {
    const LIVE_RENDER_REGEXP = '(?:/[^/]+)?/cms/render/live/.*';
    const CUSTOM_RULE_HEADER = 'public, plop, tagada';

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

            const liveRenderRule = rules.find(rule => rule.urlRegexp === LIVE_RENDER_REGEXP);
            expect(liveRenderRule, `the default ruleset rule matching ${LIVE_RENDER_REGEXP} must be registered`).to.not.be.undefined;
            expect(liveRenderRule.priority).to.be.equal('1.0');

            const customRule = rules.find(rule => rule.header === CUSTOM_RULE_HEADER);
            expect(customRule, 'the custom ruleset rule must be registered').to.not.be.undefined;
            expect(customRule.priority).to.be.equal('8.9');
        });
        cy.logout();
    });

    it('TestCase 1 (ssh): List available rules', () => {
        cy.task('sshCommand', ['jahia:client-cache-list-rules json']).then((result: string) => {
            const rules = JSON.parse(result);
            cy.log(JSON.stringify(rules));
            expect(rules).to.not.be.empty;
            expect(rules.length).to.be.greaterThan(0);

            const liveRenderRule = rules.find(rule => rule.urlRegexp === LIVE_RENDER_REGEXP);
            expect(liveRenderRule, `the default ruleset rule matching ${LIVE_RENDER_REGEXP} must be registered`).to.not.be.undefined;
            expect(liveRenderRule.priority).to.be.equal(1);

            const customRule = rules.find(rule => rule.header === CUSTOM_RULE_HEADER);
            expect(customRule, 'the custom ruleset rule must be registered').to.not.be.undefined;
            expect(customRule.priority).to.be.equal(8.9);

            const priorities = rules.map(rule => rule.priority);
            expect(priorities, 'the command must return the rules in the order they are evaluated').to.deep.equal([...priorities].sort((a, b) => a - b));
        });
    });
});
