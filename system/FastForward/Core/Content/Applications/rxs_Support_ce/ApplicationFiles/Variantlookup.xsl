<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~     Percussion CMS
  ~     Copyright (C) 1999-2020 Percussion Software, Inc.
  ~
  ~     This program is free software: you can redistribute it and/or modify
  ~     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  ~
  ~     This program is distributed in the hope that it will be useful,
  ~     but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~     GNU Affero General Public License for more details.
  ~
  ~     Mailing Address:
  ~
  ~      Percussion Software, Inc.
  ~      PO Box 767
  ~      Burlington, MA 01803, USA
  ~      +01-781-438-9900
  ~      support@percussion.com
  ~      https://www.percussion.com
  ~
  ~     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
  -->

<!DOCTYPE xsl:stylesheet [
		<!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES_Latin_1_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLlat1x.ent">
		%HTMLlat1;
		<!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES_Symbols_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLsymbolx.ent">
		%HTMLsymbol;
		<!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES_Special_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLspecialx.ent">
		%HTMLspecial;
]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:variable name="this" select="/"/>
	<xsl:template match="/">
		<html>
			<head>
				<meta name="generator" content="Percussion XSpLit Version 3.0"/>
				<meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
				<title>VARIANTLOOKUP Page</title>
			</head>
			<body>
				<table>
					<xsl:apply-templates select="*/item" mode="mode0"/>
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
	<xsl:template match="*/item" mode="mode0">
		<xsl:for-each select=".">
			<tr>
				<td>
					<xsl:apply-templates select="value"/>
				</td>
				<td>
					<xsl:apply-templates select="display"/>
				</td>
				<td>
					<xsl:apply-templates select="contenttypeid"/>
				</td>
				<td>
					<xsl:apply-templates select="stylesheetname"/>
				</td>
				<td>
					<xsl:apply-templates select="assemblyurl"/>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
