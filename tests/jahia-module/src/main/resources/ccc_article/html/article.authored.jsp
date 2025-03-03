<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>

<div>
    <h1>Article Authored</h1>
    <h2>${currentNode.properties['title'].string}</h2>
    <p>${currentNode.properties['body'].string}</p>
    <div>
        <p>${currentNode.properties['footer'].string}</p>
    </div>
    <div>Authored by:
        <Render path={'/users/' + author} view="detail"/>
    </div>
</div>
