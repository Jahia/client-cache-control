<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>

<jsp:useBean id="date" class="java.util.Date" />
<h1>Time Default</h1>
<div className="time">${currentNode.properties['label'].string}: <fmt:formatDate value="${date}" pattern="yyyy" /></div>
