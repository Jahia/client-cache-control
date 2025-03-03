<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>

<h1>User Detail</h1>
<div className="user">
    <span className="username">${currentNode.properties['firstname'].string} ${currentNode.properties['lastname'].string}</span>
    <span className="userage">member since ${currentNode.properties['jcr:created'].string}</span>
</div>
