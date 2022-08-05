<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:xs="http://www.w3.org/2001/XMLSchema"
				xmlns:mml="http://www.w3.org/1998/Math/MathML"
				xmlns:xlink="http://www.w3.org/1999/xlink"
				xmlns:tei="http://www.tei-c.org/ns/1.0"
				exclude-result-prefixes="xlink xs mml tei"
				version="1.0">

	<xsl:template match="/">
		<xsl:variable name="lang" select="tei:TEI/tei:teiHeader/@xml:lang"/>
		<article article-type="research-article" xml:lang="{$lang}">
			<xsl:apply-templates select="tei:TEI/tei:teiHeader"/>
			<xsl:apply-templates select="tei:TEI/tei:text/tei:body"/>
			<xsl:apply-templates select="tei:TEI/tei:text/tei:back"/>

		</article>
	</xsl:template>

	<xsl:template match="tei:teiHeader">
		<xsl:variable name="article-lang" select="@xml:lang"/>
		<front>
			<journal-meta>
				<journal-title>
					<!--    			<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:monogr/tei:title"/>-->
				</journal-title>
				<issn><xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:monogr/tei:idno[@type='ISSN']"/></issn>
			</journal-meta>
			<article-meta>
				<xsl:if test="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:idno[@type='DOI']">
					<article-id pub-id-type="doi">
						<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:idno[@type='DOI']"/>
					</article-id>
				</xsl:if>

				<title-group>
					<xsl:if test="$article-lang = 'en'">
						<article-title xml:lang="en">
							<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='en']"/>
						</article-title>
						<xsl:if test="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='ko']">
							<trans-title-group xml:lang="ko">
								<trans-title>
									<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='ko']"/>
								</trans-title>
							</trans-title-group>
						</xsl:if>
					</xsl:if>
					<xsl:if test="$article-lang = 'ko'">
						<article-title xml:lang="ko">
							<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='ko']"/>
						</article-title>
						<xsl:if test="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='en']">
							<trans-title-group xml:lang="en">
								<trans-title>
									<xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:title[@xml:lang='en']"/>
								</trans-title>
							</trans-title-group>
						</xsl:if>
					</xsl:if>
				</title-group>
				
				<xsl:choose>
					<xsl:when test="@xml:lang='ko'">
						<xsl:if test="tei:profileDesc/tei:abstract[@xml:lang='ko']">
							<abstract xml:lang="ko">
								<p>
									<xsl:apply-templates select="tei:profileDesc/tei:abstract[@xml:lang='ko']"/>
								</p>
							</abstract>
						</xsl:if>
						<xsl:if test="tei:profileDesc/tei:abstract[@xml:lang='en']">
							<trans-abstract xml:lang="en">
								<p>
									<xsl:apply-templates select="tei:profileDesc/tei:abstract[@xml:lang='en']"/>
								</p>
							</trans-abstract>
						</xsl:if>
					</xsl:when>
					<xsl:otherwise>
						<abstract xml:lang="en">
							<p>
								<xsl:apply-templates select="tei:profileDesc/tei:abstract[@xml:lang='en']"/>
							</p>
						</abstract>
						<xsl:if test="tei:profileDesc/tei:abstract[@xml:lang='ko']">
							<trans-abstract xml:lang="ko">
								<p>
									<xsl:apply-templates select="tei:profileDesc/tei:abstract[@xml:lang='ko']"/>
								</p>
							</trans-abstract>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>

				<contrib-group>
					<xsl:for-each select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:analytic/tei:author">
						<xsl:variable name="lang" select="@xml:lang"/>
						<xsl:if test="tei:persName">
							<contrib contrib-type="author">
								<xsl:choose>
									<xsl:when test="($article-lang='ko' and $lang='kr') or ($article-lang=$lang)">
										<name>
											<surname>
												<xsl:value-of select="tei:persName/tei:surname"/>
											</surname>
											<given-names>
												<xsl:for-each select="tei:persName/tei:forename">
													<xsl:value-of select="string(.)"/>
												</xsl:for-each>
											</given-names>
										</name>	
									</xsl:when>
									<xsl:otherwise>
										<name xml:lang="{$lang}">
											<surname>
												<xsl:value-of select="tei:persName/tei:surname"/>
											</surname>
											<given-names>
												<xsl:for-each select="tei:persName/tei:forename">
													<xsl:value-of select="string(.)"/>
												</xsl:for-each>
											</given-names>
										</name>
									</xsl:otherwise>
								</xsl:choose>
								
								<xsl:if test="tei:affiliation">
									<aff>
										<named-content content-type="inst">
											<xsl:value-of select="tei:affiliation/tei:orgName[@type='institution']"/>
										</named-content>
										<named-content content-type="dept">
											<xsl:value-of select="tei:affiliation/tei:orgName[@type='department']"/>
										</named-content>
									</aff>
								</xsl:if>
							</contrib>
						</xsl:if>
					</xsl:for-each>
				</contrib-group>
				<xsl:variable name="pubDate" select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:monogr/tei:imprint/tei:date/@when"/>
				<xsl:variable name="pubYear" select="substring($pubDate, 0,5)"/>
				<xsl:variable name="pubMonth" select="substring($pubDate, 6,2)"/>
				<xsl:variable name="pubDay" select="substring($pubDate, 9,2)"/>
				<xsl:if test="$pubDate">
					<pub-date date-type="pub" publication-format="print" iso-8601-date="{$pubDate}">
						<year><xsl:value-of select="$pubYear"/></year>
						<xsl:if test="string-length($pubDate) > 6">
							<month><xsl:value-of select="$pubMonth"/></month>
						</xsl:if>
						<xsl:if test="string-length($pubDate) > 9">
							<day><xsl:value-of select="$pubDay"/></day>
						</xsl:if>
					</pub-date>
				</xsl:if>

				<permissions>
					<copyright-statement><xsl:value-of select="tei:fileDesc/tei:sourceDesc/tei:biblStruct/tei:note[@type='copyright']"/></copyright-statement>
				</permissions>

				<history>
					<!--				submission.. 어떻게 날짜 나눌까-->
				</history>

				<kwd-group>
					<xsl:for-each select="tei:profileDesc/tei:textClass/tei:keywords/tei:term">
						<kwd>
							<xsl:value-of select="text()"/>
						</kwd>
					</xsl:for-each>
				</kwd-group>
			</article-meta>
		</front>
	</xsl:template>

	<xsl:template match="tei:text/tei:body">
		<body>
			<xsl:apply-templates/>
<!--			<xsl:apply-templates select="tei:div"/>-->
<!--			<xsl:apply-templates select="tei:figure"/>-->
		</body>
	</xsl:template>
	
	<xsl:template match="tei:text/tei:body/tei:div">
		<sec>
			<title>
				<bold>
					<xsl:value-of select="tei:head"/>
				</bold>
			</title>
			<p>
				<xsl:apply-templates select="tei:p"/>
			</p>
		</sec>
	</xsl:template>

	<xsl:template match="tei:text/tei:back">
		<back>
			<xsl:if test="tei:div[@type='acknowledgement']/tei:div/tei:p">
				<fn-group>
					<fn fn-type="ack">
						<xsl:value-of select="tei:div[@type='acknowledgement']/tei:div/tei:p"/>
					</fn>
				</fn-group>
			</xsl:if>
			<ref-list>
				<xsl:apply-templates select="tei:div/tei:listBibl/tei:biblStruct"/>
			</ref-list>

		</back>
	</xsl:template>

	<xsl:template match="tei:biblStruct">
		<xsl:variable name="id" select="@xml:id"/>
		<xsl:variable name="pubType" select="tei:analytic/tei:title/@level"/>
		<xsl:variable name="title" select="tei:analytic/tei:title"/>
		<xsl:variable name="index" select="position()"/>
		<xsl:variable name="year" select="tei:monogr/tei:imprint/tei:date"/>

		<ref id="{$id}">
			<label>
				<xsl:value-of select="$index"/>
			</label>
			<element-citation publication-type="journal" publication-format="print">
				<xsl:if test="tei:monogr/tei:author">
					<person-group person-group-type="author">
						<xsl:apply-templates select="tei:monogr/tei:author"/>
					</person-group>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:editor">"
					<person-group person-group-type="editor">
						<xsl:apply-templates select="tei:monogr/tei:editor"/>
					</person-group>
				</xsl:if>
				<xsl:if test="tei:analytic/tei:author">
					<person-group person-group-type="author">
						<xsl:apply-templates select="tei:analytic/tei:author"/>
					</person-group>
				</xsl:if>
				<xsl:if test="tei:analytic/tei:editor">"
					<person-group person-group-type="editor">
						<xsl:apply-templates select="tei:analytic/tei:editor"/>
					</person-group>
				</xsl:if>
				<article-title>
					<xsl:choose>
						<xsl:when test="not($title)">
							<xsl:value-of select="tei:monogr/tei:title[@level!='j']"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$title"/>
						</xsl:otherwise>
					</xsl:choose>
				</article-title>
				<source>
					<xsl:value-of select="tei:monogr/tei:title[@level='j']"/>
				</source>
				<xsl:if test="$year">
					<year iso-8601-date="{$year}">
						<xsl:value-of select="$year"/>
					</year>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:imprint/tei:biblScope[@unit='volume']">
					<volume><xsl:value-of select="tei:monogr/tei:imprint/tei:biblScope[@unit='volume']"/></volume>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:imprint/tei:biblScope[@unit='issue']">
					<issue><xsl:value-of select="tei:monogr/tei:imprint/tei:biblScope[@unit='issue']"/></issue>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:imprint/tei:biblScope[@unit='page']">
					<fpage><xsl:value-of select="tei:monogr/tei:imprint/tei:biblScope[@unit='page']/@from"/></fpage>
					<lpage><xsl:value-of select="tei:monogr/tei:imprint/tei:biblScope[@unit='page']/@to"/></lpage>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:imprint/tei:pubPlace">
					<publisher-loc><xsl:value-of select="tei:monogr/tei:imprint/tei:pubPlace"/></publisher-loc>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:imprint/tei:publisher">
					<publisher-name><xsl:value-of select="tei:monogr/tei:imprint/tei:publisher"/></publisher-name>
				</xsl:if>
				<xsl:if test="tei:monogr/tei:ptr">
					<uri>
						<xsl:value-of select="tei:monogr/tei:ptr"/>
					</uri>
				</xsl:if>

			</element-citation>
		</ref>
	</xsl:template>


	<xsl:template match="tei:author">
		<name>
			<surname>
				<xsl:value-of select="tei:persName/tei:surname"/>
			</surname>
			<given-names>
				<xsl:for-each select="tei:persName/tei:forename">
					<xsl:value-of select="string( )"/>
				</xsl:for-each>
			</given-names>
		</name>
	</xsl:template>
	
	<xsl:template match="tei:p">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="tei:ref">
		<xsl:variable name="ref-type" select="@type"/>
		<xsl:variable name="target" select="@target"/>
		<xsl:choose>
			<xsl:when test="$ref-type ='figure'">
				<xref rid="{$target}" ref-type="fig">
					<xsl:apply-templates/>
				</xref>
			</xsl:when>
			<xsl:otherwise>
				<xref rid="{$target}" ref-type="{$ref-type}">
					<xsl:apply-templates/>
				</xref>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="tei:text/tei:body/tei:figure">
		<xsl:if test="not(@type)">
			<xsl:variable name="id" select="@xml:id"/>
			<xsl:variable name="url" select="tei:graphic/@url"/>
			<fig id="#{$id}">
				<label>
					<xsl:value-of select="tei:figDesc"/>
				</label>
				<caption>
					<xsl:value-of select="tei:figDesc"/>
				</caption>
				<graphic xlink:href="{$url}"/>
			</fig>
		</xsl:if>
		<xsl:if test="@type='table'">
			<xsl:variable name="id" select="@xml:id"/>
			<table-wrap id="#{$id}">
				<caption>
					<p>
						<xsl:value-of select="tei:head"/>
						<xsl:text> </xsl:text>
						<xsl:value-of select="tei:figDesc"/>
					</p>
				</caption>
                <xsl:if test="@validated='true'">
                    <table>
                        <tr>
                            <xsl:for-each select="tei:table/tei:row[1]/tei:cell">
                                <xsl:variable name="cols" select="@cols"/>
                                <td colspan="{$cols}">
                                    <bold>
                                        <xsl:value-of select="."/>
                                    </bold>
                                </td>
                            </xsl:for-each>
                        </tr>
                        <xsl:variable name="n" select="2"/>
                        <xsl:variable name="row_count" select="count(tei:table/tei:row)"/>
    
                        <xsl:if test="$n &lt;= $row_count">
                            <xsl:call-template name="td-repeat">
                                <xsl:with-param name="total" select="$row_count"/>
                            </xsl:call-template>
                        </xsl:if>
                    </table>
                </xsl:if>
                <xsl:if test="@validated='false'">
                    <table>
                        <tbody>
                            <tr>
                                <td>
                                    <xsl:value-of select="tei:table"/>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </xsl:if>                
				<table-wrap-foot>
					<fn>
						<p>
							<xsl:value-of select="tei:note"/>
						</p>
					</fn>
				</table-wrap-foot>
			</table-wrap>
		</xsl:if>
	</xsl:template>

	<xsl:template name="td-repeat">
		<xsl:param name="index" select="2" />
		<xsl:param name="total"/>

		<tr>
			<xsl:for-each select="tei:table/tei:row[$index]/tei:cell">
				<xsl:variable name="cols" select="@cols"/>
				<td colspan="{$cols}" rowspan="1">
					<xsl:value-of select="."/>
				</td>
			</xsl:for-each>
		</tr>

		<xsl:if test="not($index = $total)">
			<xsl:call-template name="td-repeat">
				<xsl:with-param name="index" select="$index + 1"/>
				<xsl:with-param name="total" select="$total"/>
			</xsl:call-template>
		</xsl:if>


	</xsl:template>



	<xsl:template match="tei:title">
		<xsl:apply-templates select="node()|@*"/>
	</xsl:template>

	<xsl:template match="tei:p">
		<p>
			<xsl:apply-templates select="node()|@*"/>
		</p>
	</xsl:template>


</xsl:stylesheet>