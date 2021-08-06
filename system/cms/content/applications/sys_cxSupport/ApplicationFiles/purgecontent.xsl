<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
		<!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES_Latin_1_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLlat1x.ent">
		%HTMLlat1;
		<!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES_Symbols_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
		<!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES_Special_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLspecialx.ent">
		%HTMLspecial;
]>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:psxi18n="urn:www.percussion.com/i18n" exclude-result-prefixes="psxi18n" >
	<xsl:import href="file:sys_resources/stylesheets/sys_I18nUtils.xsl"/>
	<xsl:variable name="lang" select="//@xml:lang"/>
	<xsl:variable name="rximagepath">
		<xsl:choose>
			<xsl:when test="$lang and $lang!=''">
				<xsl:text>../rx_resources/images/en-us/</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat('../rx_resources/images/',$lang,'/')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/">
		<html>
			<head>
				<title>
					<xsl:call-template name="getLocaleString">
						<xsl:with-param name="key" select="'psx.sys_cxSupport.purgecontent@Rhythmyx - Purge Confirmation'"/>
						<xsl:with-param name="lang" select="$lang"/>
					</xsl:call-template>
				</title>
				<link rel="stylesheet" type="text/css" href="../sys_resources/css/templates.css"/>
				<script language="javascript">
				   function Continue_onclick()
				   {
				      purgeurl = "<xsl:value-of select="//@purgeurl"/>";
				      var locurl = document.location.href;
				      purgeurl += "?" + locurl.split("?")[1];
				      document.location.href=purgeurl;
				      
				   }
				</script>
			</head>
			<body class="backgroundcolor" topmargin="0" leftmargin="0" marginheight="0" marginwidth="0">
				<form name="purgecontent">
					<table width="100%" height="100%" cellpadding="0" cellspacing="0" border="0">
						<tr>
							<td height="75">
								<table width="100%" height="75" cellpadding="0" cellspacing="0" border="0">
									<tr class="bannerbackground">
										<td width="315" valign="top" align="left">
											<img src="{concat($rximagepath,'banner_longlogo.jpg')}" width="640" height="75" border="0" alt="Rhythmyx Content Manager"/>
										</td>
										<td height="75" align="left" class="tabs" width="100%">
											<table width="100%" border="0" cellspacing="0" cellpadding="0" height="75" background="{concat($rximagepath,'banner_bg_noline.gif')}">
												<tr>
													<td align="left" valign="bottom">
														<img src="../sys_resources/images/spacer.gif"/>
													</td>
												</tr>
											</table>
										</td>
									</tr>
								</table>
							</td>
						</tr>
						<tr class="outerboxcell">
							<td height="100%">
								<table width="100%" cellpadding="0" cellspacing="20" border="0">
									<tr class="outerboxcell">
										<td class="outerboxcellfont" align="center" valign="middle">
											<xsl:call-template name="getLocaleString">
												<xsl:with-param name="key" select="'psx.sys_cxSupport.purgecontent@Purge actions can not be undone. Do you want to proceed?'"/>
												<xsl:with-param name="lang" select="$lang"/>
											</xsl:call-template>
										</td>
									</tr>
									<tr class="outerboxcell">
										<td class="outerboxcellfont" align="center" valign="middle">
											<input type="button" name="Continue" value="Continue" language="javascript" onclick="javascript:Continue_onclick()">
												<xsl:attribute name="value"><xsl:call-template name="getLocaleString"><xsl:with-param name="key" select="'psx.sys_cxSupport.purgecontent@Continue'"/><xsl:with-param name="lang" select="$lang"/></xsl:call-template></xsl:attribute>
											</input>&nbsp;
                        <input type="button" name="Cancel" value="Cancel" language="javascript" onclick="javascript:self.close()">
												<xsl:attribute name="value"><xsl:call-template name="getLocaleString"><xsl:with-param name="key" select="'psx.sys_cxSupport.purgecontent@Cancel'"/><xsl:with-param name="lang" select="$lang"/></xsl:call-template></xsl:attribute>
											</input>&nbsp;
                           </td>
									</tr>
								</table>
							</td>
						</tr>
					</table>
				</form>
			</body>
		</html>
	</xsl:template>
	<psxi18n:lookupkeys>
		<key name="psx.sys_cxSupport.purgecontent@Rhythmyx - Purge Confirmation">Title for the Purge confirmation.</key>
		<key name="psx.sys_cxSupport.purgecontent@Purge actions can not be undone. Do you want to proceed?">Warning message appears while purging the content.</key>
		<key name="psx.sys_cxSupport.purgecontent@Continue">Label for Continue button.</key>
		<key name="psx.sys_cxSupport.purgecontent@Cancel">Label for Cancel button.</key>
	</psxi18n:lookupkeys>
</xsl:stylesheet>
