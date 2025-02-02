<?xml version="1.0" encoding="UTF-8"?>


<!DOCTYPE xsl:stylesheet [
		<!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES_Latin_1_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLlat1x.ent">
		%HTMLlat1;
		<!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES_Symbols_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
		<!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES_Special_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLspecialx.ent">
		%HTMLspecial;
		<!ENTITY % w3centities-f PUBLIC
				"-//W3C//ENTITIES Combined Set//EN//XML"
				"http://www.w3.org/2003/entities/2007/w3centities-f.ent"
				>
		%w3centities-f;
		]>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml" xmlns:psxi18n="com.percussion.i18n"
                extension-element-prefixes="psxi18n" exclude-result-prefixes="psxi18n">
	<xsl:import href="file:sys_resources/stylesheets/sys_bannerTemplate.xsl"/>
	<xsl:include href="file:sys_wfLookups/workflowactionlistbox.xsl"/>
	<xsl:include href="file:sys_wfLookups/exttransitionnotifs.xsl"/>
	<xsl:variable name="this" select="/"/>
	<xsl:variable name="bannerinclude" select="/*/bannerincludeurl"/>
	<xsl:variable name="userstatusinclude" select="/*/userstatusincludeurl"/>
	<xsl:variable name="relatedlinks" select="/*/relatedlinks"/>
	<xsl:template match="/">
		<html>
			<head>
				<meta name="generator" content="Percussion XSpLit Version 3.5"/>
				<meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
				<title>Rhythmyx - Workflow Administrator</title>
				<link rel="stylesheet" type="text/css" href="/sys_resources/css/templates.css"/>
				<link rel="stylesheet" type="text/css" href="/rx_resources/css/templates.css"/>
				<link href="../sys_resources/css/tabs.css" rel="stylesheet" type="text/css"/>
				<script language="JavaScript" src="../sys_resources/js/checkrequired.js"><![CDATA[
]]></script>
				<script language="JavaScript" src="../sys_resources/js/formValidation.js"><![CDATA[
			]]></script>
				<script id="clientEventHandlersJS" language="javascript"><![CDATA[
				function save_onclick() {
						document.UpdateTransitionRole.roleid.value = document.UpdateTransitionRole.role[document.UpdateTransitionRole.role.selectedIndex].value;
						document.UpdateTransitionRole.submit();
				}
			]]></script>
			<script language="javascript1.2">

			   function cancelFunc()
			   {			      
			      
			      document.location.href = '<xsl:value-of select="//cancelurl"/>';
			      
			   }
						
			</script>
			</head>
			<body class="backgroundcolour" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
				<!--   BEGIN Banner and Login Details   -->
				<xsl:call-template name="bannerAndUserStatus"/>
				<!--   END Banner and Login Details   -->
				<table width="100%" height="100%" cellpadding="0" cellspacing="1" border="0">
					<tr>
						<td align="middle" valign="top" width="150" height="100%" class="outerboxcell">
							<!--   start left nav slot   -->
							<!--   start left nav slot   -->
							<xsl:for-each select="document($relatedlinks)/*/component[@slotname='slt_wf_nav']">
								<xsl:copy-of select="document(url)/*/body/*"/>
							</xsl:for-each>
							<!--   end left nav slot   -->
						</td>
						<td align="middle" width="100%" valign="top" height="100%" class="outerboxcell">
							<!--   start main body slot   -->
							<!--   start main body slot   -->
							<form name="UpdateTransitionRole" action="UpdateTransitionRole.htm" method="get" onsubmit="return save_onclick()">
								<!--   BEGIN Banner and Login Details   -->
								<xsl:apply-templates select="*" mode="mode26"/>
							</form>
						</td>
					</tr>
				</table>
			</body>
		</html>
	</xsl:template>
	<xsl:template match="*">
		<xsl:choose>
			<xsl:when test="text()">
				<xsl:choose>
					<xsl:when test="@no-escaping">
						<xsl:value-of select="." disable-output-escaping="yes"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="."/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>&nbsp;</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="not(position()=last())">
			<br id="XSpLit"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="attribute::*">
		<xsl:value-of select="."/>
		<xsl:if test="not(position()=last())">
			<br id="XSpLit"/>
		</xsl:if>
	</xsl:template>
	<xsl:template match="*" mode="mode0">
		<xsl:variable name="transitioninfo" select="document(/*/transitioninfo)/*"/>
		<xsl:for-each select=".">
			<tr>
				<td class="outerboxcell" align="right" valign="top">
					<span class="outerboxcellfont">
						<a>
							<xsl:attribute name="href"><xsl:value-of select="$transitioninfo/workflowslink"/></xsl:attribute>
				Workflows
			</a> &gt;
			<a>
							<xsl:attribute name="href"><xsl:value-of select="$transitioninfo//workflowlink"/></xsl:attribute>
							<xsl:apply-templates select="$transitioninfo/@workflowname"/>
						</a> &gt;
			<a>
							<xsl:attribute name="href"><xsl:value-of select="$transitioninfo/statelink"/></xsl:attribute>
							<xsl:apply-templates select="$transitioninfo/@statename"/>
						</a> &gt;
			<a>
							<xsl:attribute name="href"><xsl:value-of select="$transitioninfo/transitionlink"/></xsl:attribute>
							<xsl:apply-templates select="$transitioninfo/@transitionlabel"/>
						</a> &gt;Transition Role
          </span>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="*" mode="mode25">
		<xsl:for-each select=".">
			<tr>
				<td valign="top">
					<!--   View Start   -->
					<input name="sys_componentname" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="componentname"/></xsl:attribute>
					</input>
					<input name="workflowid" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="workflowid"/></xsl:attribute>
					</input>
					<input name="stateid" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="stateid"/></xsl:attribute>
					</input>
					<input name="transitionid" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="transitionid"/></xsl:attribute>
					</input>
					<input name="sys_isaging" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="isaging"/></xsl:attribute>
					</input>
					<input name="rxorigin" type="hidden" value="edittransrole"/>
					<input name="roleid" type="hidden" value=""/>
					<table width="100%" cellpadding="0" cellspacing="1" border="0">
						<xsl:choose>
							<xsl:when test="count(role)=1 and role/id=''">
								<tr class="datacell1">
									<td align="center" colspan="2" class="datacellnoentriesfound">
				No entries found.&nbsp;
			</td>
								</tr>
							</xsl:when>
							<xsl:otherwise>
								<tr class="datacell1">
									<td align="left" class="headercellfont" width="30%">Role:
					 <img src="/sys_resources/images/invis.gif" height="1" width="100" border="0"/>
									</td>
									<td width="70%" align="left" class="datacell1font">
										<select name="role">
											<xsl:for-each select="role">
												<option value="{id}"><xsl:value-of select="name"/></option>
											</xsl:for-each>
										</select>
									</td>
								</tr>
								<tr class="datacell2">
									<td colspan="2" align="left" class="datacell1font">
										<input type="hidden" class="nav_body" name="DBActionType" value="INSERT"/>
										<input type="button" class="nav_body" name="save" value="Save" onclick="javascript:save_onclick();"/>&nbsp;
										<input type="button" value="Cancel" name="cancel" language="javascript" onclick="cancelFunc();"/>
									</td>
								</tr>
							</xsl:otherwise>
						</xsl:choose>
					</table>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	<xsl:template match="*" mode="mode26">
		<xsl:for-each select=".">
			<table width="100%" height="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td id="XSpLit"/>
				</tr>
				<xsl:apply-templates select="." mode="mode0"/>
				<tr>
					<td width="100%" class="headercell">&nbsp;
              </td>
				</tr>
				<xsl:apply-templates select="." mode="mode25"/>
				<tr class="headercell">
					<td height="100%">&nbsp;</td>
					<!--   Fill down to the bottom   -->
				</tr>
			</table>
			<!--   Main View Area End   -->
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
