<p:declare-step type="px:text-to-ssml" version="1.0" name="main"
		xmlns:p="http://www.w3.org/ns/xproc"
		xmlns:px="http://www.daisy.org/ns/pipeline/xproc"
		xmlns:cx="http://xmlcalabash.com/ns/extensions"
		xmlns:xml="http://www.w3.org/XML/1998/namespace"
		xmlns:ssml="http://www.w3.org/2001/10/synthesis"
		exclude-inline-prefixes="#all">

  <p:documentation>
    Generate the TTS input, as SSML snippets.
  </p:documentation>

  <p:input port="fileset.in" sequence="false"/>
  <p:input port="content.in"  sequence="false" primary="true">
    <p:documentation>The content document (e.g. a Zedai document, a DTBook)</p:documentation>
  </p:input>
  <p:input port="sentence-ids" sequence="false" >
    <p:documentation>The list of the sentence ids, generated by the lexers.</p:documentation>
  </p:input>

  <p:output port="result" sequence="true" primary="true">
    <p:documentation>The SSML output.</p:documentation>
  </p:output>

  <p:option name="aural-sheet-uri" required="false" select="''">
      <p:documentation>An additional CSS Speech stylesheet that is not
      already referred by the content documents. If such a stylesheet
      is provided, the other stylesheets must not contain any
      'cue-before', 'cue-after', or 'cue' properties with relative
      paths.
      </p:documentation>
  </p:option>

  <p:option name="section-elements" required="true">
    <p:documentation>Elements used to identify threadable groups,
    together with its attribute 'section-attr'.</p:documentation>
  </p:option>
  <p:option name="section-attr" required="false" select="''">
    <p:documentation>If provided, there should be only one section
    element and the option 'section-attr-val' must be provided
    too.</p:documentation>
  </p:option>
  <p:option name="section-attr-val" required="false" select="''"/>

  <p:option name="word-element" required="true">
    <p:documentation>Element used to identify words within sentences,
    together with its attribute 'word-attr'.</p:documentation>
  </p:option>
  <p:option name="word-attr" required="false" select="''"/>
  <p:option name="word-attr-val" required="false" select="''"/>

  <p:option name="skippable-elements" required="false" select="''">
    <p:documentation>The list of elements that will be synthesized in
    separate sections when the corresponding option is enable.
    </p:documentation>
  </p:option>
  <p:option name="separate-skippable" required="false" select="'false'">
    <p:documentation>Whether or not the skippable elements must be all
    synthesized in separate sections.
    </p:documentation>
  </p:option>

  <p:option name="link-element" required="false" select="'true'">
    <p:documentation>Whether or not an HTML-like 'link' element exists,
    such as in EPUB3 and DTBook. Used for retrieving the CSS
    stylesheets.</p:documentation>
  </p:option>

  <p:option name="ssml-of-lexicons-uris" required="false" select="''">
    <p:documentation>URI of a SSML file containing a list of pointers
    to custom lexicons. The phonemes will owerwrite those defined in
    the builtin lexicons.</p:documentation>
  </p:option>
  
  
  <p:import href="http://www.daisy.org/pipeline/modules/fileset-utils/library.xpl"/>
  <p:import href="http://www.daisy.org/pipeline/modules/css-speech/inline-css.xpl"/>
  <p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl" />
  <p:import href="styled-text-to-ssml.xpl" />
  <p:import href="skippable-to-ssml.xpl" />

  <p:variable name="style-ns" select="'http://www.daisy.org/ns/pipeline/tmp'"/>

  <!-- Get the CSS stylesheets -->
  <p:try>
    <p:group>
      <p:output port="result"/>
      <p:variable name="fileset-base" select="base-uri(/*)">
	<p:pipe port="fileset.in" step="main"/>
      </p:variable>
      <p:xslt name="get-css">
	<p:with-param name="link-element" select="$link-element"/>
	<p:input port="source">
	  <p:pipe port="content.in" step="main"/>
	</p:input>
	<p:input port="stylesheet">
	  <p:document href="http://www.daisy.org/pipeline/modules/css-utils/xml-to-css-uris.xsl"/>
	</p:input>
      </p:xslt>
      <p:viewport match="//*[@href]">
	<p:add-attribute attribute-name="original-href" match="/*">
	  <p:with-option name="attribute-value" select="resolve-uri(/*/@href, $fileset-base)"/>
	</p:add-attribute>
      </p:viewport>
    </p:group>
    <p:catch>
      <p:output port="result"/>
      <cx:message message="CSS stylesheet URI(s) are malformed."/>
      <p:identity>
	<p:input port="source">
	  <p:empty/>
	</p:input>
      </p:identity>
    </p:catch>
  </p:try>

  <p:group>
    <p:output port="result" sequence="true">
      <p:pipe port="result" step="skippable-to-ssml"/>
      <p:pipe port="result" step="content-to-ssml"/>
    </p:output>

    <p:variable name="sheet-uri-list" select="string-join(//*[@original-href]/@original-href, ',')"/>
    <p:variable name="all-sheet-uris"
		select="if ($aural-sheet-uri) then concat($sheet-uri-list, ',', $aural-sheet-uri) else $sheet-uri-list">
      <p:empty/>
    </p:variable>
    <p:variable name="first-sheet-uri"
		select="if ($aural-sheet-uri) then $aural-sheet-uri else //*[@original-href][1]/@original-href"/>
    <p:choose name="inlining">
      <p:when test="$all-sheet-uris != '' and $all-sheet-uris != ','">
	<p:output port="result" sequence="true"/>
	<p:identity>
	  <p:input port="source">
	    <p:pipe port="content.in" step="main"/>
	  </p:input>
	</p:identity>
	<px:inline-css>
	  <p:with-option name="stylesheet-uri" select="$all-sheet-uris">
	    <p:empty/> <!-- More than one document in context. -->
	  </p:with-option>
	  <p:with-option name="style-ns" select="$style-ns">
	    <p:empty/>
	  </p:with-option>
	</px:inline-css>
	<cx:message message="CSS speech inlined"/>
      </p:when>
      <p:otherwise>
	<p:output port="result"/>
	<p:identity>
	  <p:input port="source">
	    <p:pipe port="content.in" step="main"/>
	  </p:input>
	</p:identity>
	<cx:message message="No CSS sheet found"/>
      </p:otherwise>
    </p:choose>

    <!-- Replace the sentences and the words with their SSML counterpart so that it -->
    <!-- will be much simpler and faster to apply transformations after.  -->
    <p:xslt name="normalize">
      <p:with-param name="word-element" select="$word-element"/>
      <p:with-param name="word-attr" select="$word-attr"/>
      <p:with-param name="word-attr-val" select="$word-attr-val"/>
      <p:input port="source">
	<p:pipe port="result" step="inlining"/>
	<p:pipe port="sentence-ids" step="main"/>
      </p:input>
      <p:input port="stylesheet">
	<p:document href="../xslt/normalize.xsl"/>
      </p:input>
    </p:xslt>
    <cx:message message="Lexing information normalized"/>

    <p:xslt name="separate">
      <p:with-param name="skippable-elements" select="$skippable-elements"/>
      <p:input port="stylesheet">
	<p:document href="../xslt/extract-skippable.xsl"/>
      </p:input>
    </p:xslt>

    <px:skippable-to-ssml name="skippable-to-ssml">
      <p:input port="content.in">
	<p:pipe port="secondary" step="separate"/>
      </p:input>
      <p:with-option name="skippable-elements" select="$skippable-elements"/>
      <p:with-option name="style-ns" select="$style-ns"/>
    </px:skippable-to-ssml>

    <!-- Load the SSML file containing user's lexicons -->
    <p:choose name="ssml-of-lexicons-uris">
      <p:xpath-context>
	<p:empty/>
      </p:xpath-context>
      <p:when test="$ssml-of-lexicons-uris != ''">
	<p:output port="result"/>
	<p:load>
	  <p:with-option name="href" select="$ssml-of-lexicons-uris"/>
	</p:load>
      </p:when>
      <p:otherwise>
	<p:output port="result"/>
	<p:identity>
	  <p:input port="source">
	    <p:empty/>
	  </p:input>
	</p:identity>
      </p:otherwise>
    </p:choose>

    <px:styled-text-to-ssml name="content-to-ssml">
      <p:input port="ssml-of-lexicons-uris">
	<p:pipe port="result" step="ssml-of-lexicons-uris"/>
      </p:input>
      <p:input port="content.in">
	<p:pipe port="result" step="separate"/>
      </p:input>
      <p:input port="fileset.in">
	<p:pipe port="fileset.in" step="main"/>
      </p:input>
      <p:with-option name="section-elements" select="$section-elements"/>
      <p:with-option name="section-attr" select="$section-attr"/>
      <p:with-option name="section-attr-val" select="$section-attr-val"/>
      <p:with-option name="first-sheet-uri" select="$first-sheet-uri"/>
      <p:with-option name="style-ns" select="$style-ns"/>
    </px:styled-text-to-ssml>
  </p:group>

</p:declare-step>
