import {addNode, createSite, deleteSite, getJahiaVersion, publishAndWaitJobEnding, uploadFile} from '@jahia/cypress';
import {addSimplePage} from '../../utils/Utils';
import {compare} from 'compare-versions';

describe('Cache Control header tests', () => {
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
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
    });

    // Test case 1 : Verify that a rendered page without any specific content contains Cache-Control header with :
    //  - a private cache-control (when accessed as authenticated user)
    //  - a private cache-control (when accessed as authenticated user in edit mode)
    //  - a public cache-control (when accessed as guest)
    it('TestCase 1: In basic rendered page, should find private cache-control when root (live and edit) and public when guest ', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page1', 'Page test case 1', 'en', 'simple').then(() => {
            publishAndWaitJobEnding(`/sites/${targetSiteKey}/home`);
        });
        cy.log('The page should contains Cache-Control header for private content when accessed logged in');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('private');
            expect(cache).to.contains('no-cache');
            expect(cache).to.contains('no-store');
            expect(cache).to.contains('max-age=0');
        });
        cy.request({
            url: '/cms/editframe/default/en/sites/' + targetSiteKey + '/home/page1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('private');
            expect(cache).to.contains('no-cache');
            expect(cache).to.contains('no-store');
            expect(cache).to.contains('max-age=0');
        });
        cy.logout();
        cy.log('The page should contains Cache-Control header for public content when not logged');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=60');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 2 : Verify that a rendered page with a private article is flagged with a private strategy when accessed as guest
    it('TestCase2: in rendered page  with private content, should find private cache-control when guest', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page2', 'Page test case 2', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page2`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page2/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'private'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains Cache-Control header for private content when not logged');
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page2.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Private');
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('private');
            expect(cache).to.contains('no-cache');
            expect(cache).to.contains('no-store');
            expect(cache).to.contains('max-age=0');
        });
    });

    // Test case 3 : Verify that a rendered page with an article authored (use of cache.expiration=42) is flagged with a custom strategy when accessed as guest
    it('TestCase 3: in rendered page with custom cache.expiration=42, should find public cache-control with s-maxage=42', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page3', 'Page test case 3', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page3`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page3/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains Cache-Control header for custom content when not logged');
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page3.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=42');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 4 : Verify that accessing files (like images) are flagged with a public-medium strategy
    it('TestCase 4: for images in media library, should find public cache-control with medium ttl value s-maxage=600', () => {
        cy.login();
        uploadFile('clientCache/jahia-logo.jpg', `/sites/${targetSiteKey}/files`, 'jahia-logo.jpg', 'image/jpeg').then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/files');
        });
        cy.log('The page should contains public Cache-Control header with medium ttl');
        cy.logout();
        cy.request({
            url: '/files/live/sites/' + targetSiteKey + '/files/jahia-logo.jpg',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=600');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 5 : Verify that accessing modules resources content are flagged with a public strategy
    //   -> Use a request to simpleTemplateSet module for /css/style2.css
    it('TestCase5: for assets in modules, should find public cache-control with medium ttl value s-maxage=600', () => {
        cy.logout();
        cy.request({
            url: '/modules/client-cache-control-test-template/css/style2.css',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=600');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 6 : Verify that accessing generated resources are flagged with an immutable strategy
    //   ->  use a page that contains resources, parse injected css or js and access the resource directly to check header
    it('TestCase6: for generated-resources, should find immutable cache-control', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page6', 'Page test case 6', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page6`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page6/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page6.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            cy.log('The page should contains Cache-Control header for custom content when not logged');
            const cssHrefMatch = response.body.match(/<link[^>]+id="staticAssetCSS0"[^>]+href="([^"]+)"/);

            if (cssHrefMatch) {
                const cssHref = cssHrefMatch[1]; // L'URL du CSS
                cy.log('The CSS file should contain Cache-Control header for immutable content when not logged, visiting: ' + cssHref);

                cy.request(cssHref).then(response2 => {
                    expect(response2.status).to.eq(200);
                    expect(response2.headers).to.have.property('cache-control');
                    const cache = response2.headers['cache-control'];
                    expect(cache).to.include('public');
                    expect(cache).to.include('max-age=2678400');
                    expect(cache).to.include('s-maxage=2678400');
                    expect(cache).to.contains('stale-while-revalidate=15');
                    expect(cache).to.include('immutable');
                });
            } else {
                throw new Error('CSS link with id "staticAssetCSS0" not found in the response body');
            }
        });
    });

    // Test case 7 : Verify that accessing a rule with a header defined without template is working
    //   -> Use a request to simpleTemplateSet module for /css/style.css
    //   a custom rule is defined in a dedicated ruleset
    it('TestCase7, for url with a dedicated header value rule, should find that value in cache-control', () => {
        cy.logout();
        cy.request({
            url: '/modules/client-cache-control-test-template/css/style.css',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('plop');
        });
    });

    // Test case 8 : Verify that accessing /tools is flagged with a private strategy
    it('TestCase8: for tools, should find a private cache-control', () => {
        cy.login();
        cy.request({
            url: '/tools',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.headers).to.have.property('cache-control');
            const cache = response.headers['cache-control'];
            getJahiaVersion().then(jahiaVersion => {
                cy.log('Test is running on jahia version: ' + jahiaVersion);
                // Depending on Jahia version, client-cache-control is not configured the same way (strict for version 8.2.1.x and allow_overrides for >= 8.2.2)
                if (compare(jahiaVersion.release.replace('-SNAPSHOT', ''), '8.2.2', '<')) {
                    // In version 8.2.1 the mode is strict so header is enforced by the yml config.
                    expect(cache).to.contains('private');
                    expect(cache).to.contains('must-revalidate');
                    expect(cache).to.contains('max-age=0');
                } else {
                    // Until tools dedicated last-urlrewrite.xml is present and mode is overrides the header if populated by the xml file
                    expect(cache).to.contains('no-cache');
                    expect(cache).to.contains('no-store');
                    expect(cache).to.contains('max-age=0');
                }
            });
        });
        cy.logout();
    });

    // Test case 9 : Verify that accessing Csrf module resources are flagged with an immutable strategy

    after('Clean', () => {
        deleteSite(targetSiteKey);
    });
});
