<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" />

	<xsl:variable name="indentStr">
		<xsl:text>--</xsl:text>
	</xsl:variable>

	<xsl:template name="substringCountIndent">
		<xsl:param name="testStr" />
		<xsl:param name="checkStr" />
		<xsl:param name="spacingStr" />
		<xsl:choose>
			<xsl:when test="contains($testStr,$checkStr)">
				<xsl:call-template name="substringCountIndent">
					<xsl:with-param name="testStr"
						select="substring-after($testStr,$checkStr)" />
					<xsl:with-param name="checkStr" select="$checkStr" />
					<xsl:with-param name="spacingStr"
						select="concat($spacingStr,$indentStr)" />
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$spacingStr" />
				<xsl:text>&gt;</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="rowColor">
		<xsl:param name="rowNumber" />
		<xsl:param name="subPosition" />
		<xsl:if test="rowNumber mod 2 = 0">
			<xsl:text>odd</xsl:text>
		</xsl:if>
		<xsl:if test="rowNumber mod 2 = 1">
			<xsl:text>even</xsl:text>
		</xsl:if>
	</xsl:template>

	<xsl:template match="/">
		<html>
			<head>
				<style type="text/css">
					table,th,td
					{
					border:1px solid #dddddd;
					border-collapse:collapse;
					padding:2px;
					white-space:nowrap;
					}
					th
					{
					background-color:#888888;
					font:bold 12px arial,sans-serif;
					color:
					#ffffff;
					}
					table,td
					{
					font:12px
					arial,sans-serif;
					}
					#caseName
					{
					width: 55%;
					}
					#total
					{
					width: 9%;
					}
					#pass
					{
					width: 9%;
					}
					#failure
					{
					width: 9%;
					}
					#passrate
					{
					width:9%;
					}
					#excutetime
					{
					width:9%;
					}
					#duration
					{
					width:9%;
					}
					#result_table_header
					{
					background-color:#eeeeee;
					width: 100%;
					}
					#result_table
					{
					width: 100%;
					}
					#even
					{
					background-color:#eeeeee;
					}
					#col01
					{
					width: 78%;
					word-break:
					break-all;
					}
					#col02
					{
					width: 9%;
					word-break:
					break-all;
					}
					#col03
					{
					width:9%;
					word-break: break-all;
					}
					#col1
					{
					width:
					40%;
					word-break:
					break-all;
					}
					#col2
					{
					width: 40%;
					word-break:
					break-all;
					}
					#col3
					{
					width: 20%;
					word-break: break-all;
					}
					#col3_body
					{
					width: 15%;
					word-break: break-all;
					background-color:#FF0000;
					}
					#col4
					{
					width: 10%;
					word-break: break-all;
					}
					#col5
					{
					width: 10%;
					word-break:
					break-all;
					}
				</style>
				<title>Cloud Test Report</title>
			</head>
			<script language="javascript">
				function showHide(bodyid) {
				var body =
				document.getElementById(bodyid);
				var display = body.style.display;
				if
				(display == "none" || display == "") {
				body.style.display = "block";
				} else {
				body.style.display = "none";
				}
				}
			</script>
			<body>
				<table id="result_table_header">
					<tr>
						<th id="caseName" align="left">Case Name</th>
						<th id="total" align="left">Total</th>
						<th id="pass" align="left">Pass</th>
						<th id="failure" align="left">Failure</th>
						<th id="passrate" align="left">Pass Rate</th>
						<th id="execution" align="left">Run Time</th>
					</tr>
				</table>
				<xsl:for-each
					select="cloudTestReportXML/testCaseResults">
					<xsl:variable name="changeType" select="@changeType" />
					<xsl:variable name="group" select="@group" />
					<xsl:variable name="total"
						select="count(testCaseResult)" />
					<xsl:variable name="pass"
						select="count(testCaseResult[@status='true'])" />
					<xsl:variable name="failure"
						select="count(testCaseResult[@status='false'])" />
					<xsl:variable name="executionTime"
						select="format-number(sum(testCaseResult/@executionTime),'#.##')" />


					<table id="result_table">
						<tr style="font-weight:bold" align="left">
							<td id="caseName">
								<a href="#" onclick="showHide('{$group}'); return false">
									<xsl:if test="$changeType !='0'">
										<font color="{$changeType}">
											<b>
												<xsl:value-of select="$group" />
											</b>
										</font>
									</xsl:if>
									<xsl:if test="$changeType ='0'">
										<xsl:value-of select="$group" />
									</xsl:if>
								</a>
							</td>
							<td id="total">
								<xsl:value-of select="$total" />
							</td>
							<td id="pass">
								<xsl:value-of select="$pass" />
							</td>
							<td id="failure">
								<!-- bgcolor="#FF0000" -->
								<xsl:value-of select="$failure" />
							</td>
							<xsl:if test="$failure >0">
								<td id="passrate" bgcolor="red">
									<xsl:if test="$pass=0">
										0%
									</xsl:if>
									<xsl:if test="$pass>0">
										<xsl:value-of
											select="format-number($pass div $total * 100, '#.##')" />
										%
									</xsl:if>
								</td>
							</xsl:if>
							<xsl:if test="$failure =0">
								<td id="passrate" bgcolor="green">
									<xsl:if test="$pass=0">
										0%
									</xsl:if>
									<xsl:if test="$pass>0">
										<xsl:value-of
											select="format-number($pass div $total * 100, '#.##')" />
										%
									</xsl:if>
								</td>
							</xsl:if>
							<td id="execution">
								<xsl:value-of select="$executionTime" />
							</td>
						</tr>
					</table>

					<div id="{$group}" style="width:97%;float:right;display:none;">
						<table id="result_table_header">
							<td id="col01"><b>Case ID</b></td>
							<td id="col02"><b>Status</b></td>
							<td id="col03"><b>Run Time</b></td>
						</table>
						<xsl:for-each select="testCaseResult">
							<xsl:variable name="changeType" select="@changeType" />
							<xsl:variable name="caseId" select="@caseId" />
							<xsl:variable name="caseName"
								select="concat($caseId,'@',$group )" />
							<xsl:variable name="status" select="@status" />
							<xsl:variable name="testCase" select="testCase" />
							<xsl:variable name="errorMessage"
								select="errorMessage" />
							<xsl:variable name="failedAssertResults"
								select="failedAssertResults" />
							<xsl:variable name="executionTime"
								select="format-number(@executionTime,'#.##')" />
							<table id="result_table">

								<xsl:if test="$status ='false'">
									<tr>
										<td id="col01">
											<a href="#" onclick="showHide('{$caseName}'); return false">
												<xsl:if test="$changeType !='0'">
													<font color="{$changeType}">
														<b>
															<xsl:value-of select="$caseId" />
														</b>
													</font>
												</xsl:if>
												<xsl:if test="$changeType ='0'">
													<xsl:value-of select="$caseId" />
												</xsl:if>
											</a>
										</td>
										<td id="col02" bgcolor="#FF0000">
											<xsl:value-of select="$status" />
										</td>

										<td id="col03">
											<xsl:value-of select="$executionTime" />
										</td>

									</tr>
								</xsl:if>
								<xsl:if test="$status ='true'">
									<tr>
										<td id="col01">
											<xsl:if test="$changeType !='0'">
												<font color="{$changeType}">
													<b>
														<xsl:value-of select="$caseId" />
													</b>
												</font>
											</xsl:if>
											<xsl:if test="$changeType ='0'">
												<xsl:value-of select="$caseId" />
											</xsl:if>
										</td>
										<td id="col02" bgcolor="green">
											<xsl:value-of select="$status" />
										</td>

										<td id="col03">
											<xsl:value-of select="$executionTime" />
										</td>

									</tr>
								</xsl:if>
							</table>
							<xsl:if test="$status ='false'">

							</xsl:if>
							<div id="{$caseName}"
								style="width:97%;float:right;display:none;">
								<table id="result_table_header">
									<td id="col1"><b>Test Case</b></td>
								</table>
								<table id="result_table">
									<tr>
										<td id="col1">
											<pre>
												<xsl:value-of select="$testCase" />
											</pre>
										</td>
									</tr>
								</table>
								<xsl:if test="string-length($failedAssertResults)&gt;0">
									<table id="result_table_header">
										<td id="col3"><b>Failed Assertion Result</b></td>
									</table>
									<table id="result_table">
										<tr>
											<td id="col3">
												<pre>
													<xsl:value-of select="$failedAssertResults" />
												</pre>
											</td>

										</tr>
									</table>
								</xsl:if>
								<xsl:if test="string-length($errorMessage)&gt;0">
									<table id="result_table_header">
										<td id="col2"><b>Error Message</b></td>
									</table>
									<table id="result_table">
										<tr>
											<td id="col2">
												<pre>
													<xsl:value-of select="$errorMessage" />
												</pre>
											</td>
										</tr>
									</table>
								</xsl:if>
							</div>
						</xsl:for-each>
					</div>

				</xsl:for-each>
				<div
					style="font:10px arial,sans-serif;text-align:right;width: 100%;height:60px;line-height:60px;">
					Copyright©2012-
					<SCRIPT>
						today = new Date();
						document.write( today.getFullYear());
					</SCRIPT>
					CloudTest, Licensed under the Apache License, Version 2.0
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>