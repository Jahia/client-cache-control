import {addNode, createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';
import {addSimplePage} from '../../utils/Utils';

describe('Render Chain Client Cache Policy tests', () => {
    const targetSiteKey = 'cacheTestSite';
    before('Create target test site', () => {
        cy.log('Create site ' + targetSiteKey + ' for cache-control tests');
        createSite(targetSiteKey, {locale: 'en', templateSet: 'client-cache-control-test-template', serverName: 'localhost'});
        addNode({parentPathOrId: `/sites/${targetSiteKey}/home`,
            primaryNodeType: 'jnt:contentList',
            name: 'pagecontent'
        }).then(() => {
            addNode({
                parentPathOrId: `/sites/${targetSiteKey}/home/pagecontent`,
                primaryNodeType: 'ccc:simpleType',
                name: 'simple-type',
                properties: [{name: 'j:view', value: 'displayParamValues'}],
                mixins: ['jmix:renderable']
            }).then(() => {
                publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
            });
        });
    });

    // Test case 1 : Verify that a page without any specific content is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a public strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 1', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase1', 'Test case 1', 'en', 'simple').then(() => {
            publishAndWaitJobEnding(`/sites/${targetSiteKey}/home`);
            cy.log('The page should contains client cache strategy for testCase1');
            cy.request({
                url: '/en/sites/' + targetSiteKey + '/home/testCase1.html',
                followRedirect: true,
                failOnStatusCode: false
            }).then(response => {
                expect(response.status).to.eq(200);
                expect(response.body).to.contain('bodywrapper');
                const cache = response.headers['x-jahia-client-cache-policy'];
                expect(cache).to.eq('private');
            });
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('public');
        });
    });

    // Test case 2 : Verify that a page with a user-logged-info section using cache.perUser is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a public strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 2', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase2', 'Test case 2', 'en', 'user-info').then(() => {
            publishAndWaitJobEnding(`/sites/${targetSiteKey}/home`);
            cy.log('The page should contains client cache strategy for testCase2');
            cy.request({
                url: '/en/sites/' + targetSiteKey + '/home/testCase2.html',
                followRedirect: true,
                failOnStatusCode: false
            }).then(response => {
                expect(response.status).to.eq(200);
                expect(response.body).to.contain('You are authenticated');
                const cache = response.headers['x-jahia-client-cache-policy'];
                expect(cache).to.eq('private');
            });
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase2.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('You are authenticated');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('public');
        });
    });

    // Test case 3 : Verify that a page with a simple article is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a public strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 3', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase3', 'Test case 3', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/testCase3`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/testCase3/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'default'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains client cache strategy for testCase3');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase3.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Default');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase3.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Default');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('public');
        });
    });

    // Test case 4 : Verify that a page with a private article is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a private strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 4', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase4', 'Test case 4', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/testCase4`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/testCase4/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'private'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains client cache strategy for testCase4');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase4.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Private');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase4.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Private');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
    });

    // Test case 5 : Verify that a page with a time content (use of cache.private) is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a private strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 5', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase5', 'Test case 5', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/testCase5`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/testCase5/pagecontent`,
                    primaryNodeType: 'ccc:time',
                    name: 'time',
                    properties: [{name: 'j:view', value: 'default'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains client cache strategy for testCase5');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase5.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Time Default');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase5.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Time Default');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
    });

    // Test case 6 : Verify that a page with an article authored (use of cache.expiration=6°) is flagged with :
    //  - a private strategy (when accessed as authenticated user)
    //  - a custom strategy (when accessed as guest)
    it('should find x-jahia-client-cache-policy header according to render client policy test case 6', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'testCase6', 'Test case 6', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/testCase6`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/testCase6/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains client cache strategy for testCase6');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase6.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('private');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/testCase6.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            const cache = response.headers['x-jahia-client-cache-policy'];
            expect(cache).to.eq('custom');
        });
    });

    after('Clean', () => {
        deleteSite(targetSiteKey);
    });
});
