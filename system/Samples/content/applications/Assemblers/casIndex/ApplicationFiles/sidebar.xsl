<?xml version='1.0' encoding='UTF-8'?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES_Latin_1_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLlat1x.ent">
        %HTMLlat1;
        <!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES_Symbols_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLsymbolx.ent">
        %HTMLsymbol;
        <!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES_Special_for_XHTML//EN" "https://www.percussion.com/DTD/HTMLspecialx.ent">
        %HTMLspecial;
]>


<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml" xmlns:psxi18n="com.percussion.i18n"
                extension-element-prefixes="psxi18n" exclude-result-prefixes="psxi18n">
  <xsl:import href="file:sys_resources/stylesheets/assemblers/sys_InlineLinks.xsl"/>
  <xsl:import href="file:rx_resources/stylesheets/assemblers/rx_InlineLinks.xsl"/>
  <xsl:import href="file:sys_resources/stylesheets/assemblers/sys_Slots.xsl"/>
  <xsl:import href="file:rx_resources/stylesheets/assemblers/rx_Slots.xsl"/>
  <xsl:import href="file:sys_resources/stylesheets/assemblers/sys_ContextTemplates.xsl"/>
  <xsl:import href="file:sys_resources/stylesheets/assemblers/sys_Globals.xsl"/>
  <xsl:import href="file:rx_resources/stylesheets/assemblers/rx_Globals.xsl"/>
<!-- begin XSL -->
<xsl:output method="xml" omit-xml-declaration="yes" />
<!-- end XSL -->
  <xsl:variable name="related" select="/*/sys_AssemblerInfo/RelatedContent"/>
  <xsl:variable name="syscommand" select="//@sys_command"/>
  <xsl:variable name="this" select="/"/>
  <xsl:template match="/">
    <html>
      <head>
        <meta content="Percussion XSpLit" name="generator"/>
        <title>Manual Index Snippet</title>
      </head>

      <body>
        <xsl:apply-templates select="*/sys_AssemblerInfo"/>
        <xsl:variable name="rxslot-9">
          <rxslot psxeditslot="yes" slotname="Related Links" template="SimpleTableSlot">
            <xsl:copy-of select="$related/linkurl[@slotname=&apos;Related Links&apos;]"/>
          </rxslot>

        </xsl:variable>

        <xsl:apply-templates mode="rxslot" select="$rxslot-9"/>
      </body>

    </html>

  </xsl:template>

  <xsl:template match="*">
    <xsl:choose>
      <xsl:when test="text()">
        <xsl:choose>
          <xsl:when test="@no-escaping">
            <xsl:value-of disable-output-escaping="yes" select="."/>
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

  <xsl:template match="*[div/@class=&apos;rxbodyfield&apos;]">
    <xsl:apply-templates mode="rxbodyfield" select="*"/>
  </xsl:template>

  <xsl:template match="sys_AssemblerInfo"/>
</xsl:stylesheet>
