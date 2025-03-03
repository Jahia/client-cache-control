<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>

<div>
    <h1>Article Default</h1>
    <h2>${currentNode.properties['title'].string}</h2>
    <p>${currentNode.properties['body'].string}</p>
    <div>
        <p>${currentNode.properties['footer'].string}</p>
    </div>
</div>
