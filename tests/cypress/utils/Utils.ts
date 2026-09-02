import {addPage} from '@jahia/cypress';

export const addSimplePage = (parentPathOrId: string, pageName: string, pageTitle: string, language: string, template = 'home', children = []) => addPage({
    parentPathOrId: parentPathOrId,
    name: pageName,
    title: pageTitle,
    language: language,
    template: template,
    children: children.length > 0 ? children : [{
        name: 'area-main',
        primaryNodeType: 'jnt:contentList',
        children: [{
            name: 'text',
            primaryNodeType: 'jnt:text',
            properties: [{language: language, name: 'text', type: 'STRING', value: pageName}]
        }]
    }]
});

export const deleteDownloadFolder = () => {
    const downloadsFolder = Cypress.config('downloadsFolder');
    cy.task('deleteFolder', downloadsFolder);
};
