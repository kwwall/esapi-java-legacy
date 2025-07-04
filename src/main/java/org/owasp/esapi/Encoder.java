/**
 * OWASP Enterprise Security API (ESAPI)
 *
 * This file is part of the Open Web Application Security Project (OWASP)
 * Enterprise Security API (ESAPI) project. For details, please see
 * <a href="http://www.owasp.org/index.php/ESAPI">http://www.owasp.org/index.php/ESAPI</a>.
 *
 * Copyright (c) 2007-2019 - The OWASP Foundation
 *
 * The ESAPI is published by OWASP under the BSD license. You should read and accept the
 * LICENSE before you use, modify, and/or redistribute this software.
 *
 * @author Jeff Williams <a href="http://www.aspectsecurity.com">Aspect Security</a>
 * @created 2007
 */
package org.owasp.esapi;

import java.io.IOException;
import java.net.URI;

import org.owasp.esapi.codecs.Codec;
import org.owasp.esapi.errors.EncodingException;


/**
 * The {@code Encoder} interface contains a number of methods for decoding input and encoding output
 * so that it will be safe for a variety of interpreters. Its primary use is to
 * provide <i>output</i> encoding to prevent XSS.
 * <p>
 * To prevent double-encoding, callers should make sure input does not already contain encoded characters
 * by calling one of the {@code canonicalize()} methods. Validator implementations should call
 * {@code canonicalize()} on user input <b>before</b> validating to prevent encoded attacks.
 * </p><p>
 * All of the methods <b>must</b> use an "allow list" or "positive" security model rather
 * than a "deny list" or "negative" security model.  For the encoding methods, this means that
 * all characters should be encoded, except for a specific list of "immune" characters that are
 * known to be safe.
 * </p><p>
 * The {@code Encoder} performs two key functions, encoding (also referred to as "escaping" in this Javadoc)
 * and decoding. These functions rely on a set of codecs that can be found in the
 * {@code org.owasp.esapi.codecs} package. These include:
 * <ul>
 * <li>CSS Escaping</li>
 * <li>HTMLEntity Encoding</li>
 * <li>JavaScript Escaping</li>
 * <li>MySQL Database Escaping</li>
 * <li>Oracle Database Escaping</li>
 * <li>JSON Escaping</li>
 * <li>Percent Encoding (aka URL Encoding)</li>
 * <li>Unix Shell Escaping</li>
 * <li>VBScript Escaping</li>
 * <li>Windows Cmd Escaping</li>
 * <li>LDAP Escaping</li>
 * <li>XML and XML Attribute Encoding</li>
 * <li>XPath Escaping</li>
 * <li>Base64 Encoding</li>
 * </ul>
 * </p><p>
 * The primary use of ESAPI {@code Encoder} is to prevent XSS vulnerabilities by
 * providing output encoding using the various "encodeFor<i>XYZ</i>()" methods,
 * where <i>XYZ</i> is one of CSS, HTML, HTMLAttribute, JavaScript, or URL. When
 * using the ESAPI output encoders, it is important that you use the one for the
 * <b>appropriate context</b> where the output will be rendered. For example, it
 * the output appears in an JavaScript context, you should use {@code encodeForJavaScript}
 * (note this includes all of the DOM JavaScript event handler attributes such as
 * 'onfocus', 'onclick', 'onload', etc.). If the output would be rendered in an HTML
 * attribute context (with the exception of the aforementioned 'onevent' type event
 * handler attributes), you would use {@code encodeForHTMLAttribute}. If you are
 * encoding anywhere a URL is expected (e.g., a 'href' attribute for for &lt;a&gt; or
 * a 'src' attribute on a &lt;img&gt; tag, etc.), then you should use use {@code encodeForURL}.
 * If encoding CSS, then use {@code encodeForCSS}. Etc. This is because there are
 * different escaping requirements for these different contexts. Developers who are
 * new to ESAPI or to defending against XSS vulnerabilities are highly encouraged to
 * <i>first</i> read the
 * <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html" target="_blank" rel="noopener noreferreer">
 * OWASP Cross-Site Scripting Prevention Cheat Sheet</a>.
 * </p><p>
 * Note that in addition to these encoder methods, ESAPI also provides a JSP Tag
 * Library ({@code META-INF/esapi.tld}) in the ESAPI jar. This allows one to use
 * the more convenient JSP tags in JSPs. These JSP tags are simply wrappers for the
 * various these "encodeForX<i>XYZ</i>()" method docmented in this {@code Encoder}
 * interface.
 * </p><p>
 * <b>Some important final words:</b>
 * <ul>
 * <li><b>Where to output encode for HTML rendering:</b>
 * Knowing <i>where</i> to place the output encoding in your code
 * is just as important as knowing which context (HTML, HTML attribute, CSS,
 * JavaScript, or URL) to use for the output encoding and surprisingly the two
 * are often related. In general, output encoding should be done just prior to the
 * output being rendered (that is, as close to the 'sink' as possible) because that
 * is what determines what the appropriate context is for the output encoding.
 * In fact, doing output encoding on untrusted data that is stored and to
 * be used later--whether stored in an HTTP session or in a database--is almost
 * always considered an anti-pattern. An example of this is one gathers and
 * stores some untrusted data item such as an email address from a user. A
 * developer thinks "let's output encode this and store the encoded data in
 * the database, thus making the untrusted data safe to use all the time, thus
 * saving all of us developers all the encoding troubles later on". On the surface,
 * that sounds like a reasonable approach. The problem is how to know what
 * output encoding to use, not only for now, but for all possible <i>future</i>
 * uses? It might be that the current application code base is only using it in
 * an HTML context that is displayed in an HTML report or shown in an HTML
 * context in the user's profile. But what if it is later used in a {@code mailto:} URL?
 * Then instead of HTML encoding, it would need to have URL encoding. Similarly,
 * what if there is a later switch made to use AJAX and the untrusted email
 * address gets used in a JavaScript context? The complication is that even if
 * you know with certainty today all the ways that an untrusted data item is
 * used in your application, it is generally impossible to predict all the
 * contexts that it may be used in the future, not only in your application, but
 * in other applications that could access that data in the database.
 * </li>
 * <li><b>Avoiding multiple <i>nested</i> contexts:</b>
 * A really tricky situation to get correct is when there are multiple nested
 * encoding contexts. But far, the most common place this seems to come up is
 * untrusted URLs used in JavaScript. How should you handle that? Well,
 * the best way is to rewrite your code to avoid it!  An example of
 * this that is well worth reading may be found at
 * <a href="https://lists.owasp.org/pipermail/esapi-dev/2012-March/002090"
 * target="_blank" rel="noopener noreferrer">ESAPI-DEV mailing list archives:
 * URL encoding within JavaScript</a>. Be sure to read the entire thread.
 * The question itself is too nuanced to be answered in Javadoc, but now,
 * hopefully you are at least aware of the potential pitfalls. There is little
 * available research or examples on how to do output encoding when multiple
 * mixed encodings are required, although one that you may find useful is
 * <a href="https://arxiv.org/pdf/1804.01862.pdf" target="_blank"
 * rel="noopener noreferrer">
 * Automated Detecting and Repair of Cross-SiteScripting Vulnerabilities through Unit Testing</a>
 * It at least discusses a few of the common errors involved in multiple mixed
 * encoding contexts.
 * </li><li><b>A word about unit testing:</b>
 * Unit testing this is hard. You may be satisfied with stopped after you have
 * tested against the ubiquitous XSS test case of
 * <pre>
 *      &lt;/script&gt;alert(1)&lt;/script&gt;
 * </pre>
 * or similar simplistic XSS attack payloads and if that is properly encoded
 * (or, you don't see an alert box popped in your browser), you consider it
 * "problem fixed", and consider the unit testing sufficient. Unfortunately, that
 * minimalist testing may not always detect places where you used the wrong output
 * encoder. You need to do better. Fortunately, the aforementioned link,
 * <a href="https://arxiv.org/pdf/1804.01862.pdf" target="_blank"
 * rel="noopener noreferrer">
 * Automated Detecting and Repair of Cross-SiteScripting Vulnerabilities through Unit Testing</a>
 * provides some insight on this. You may also wish to look at the
 * <a href="https://github.com/ESAPI/esapi-java-legacy/blob/develop/src/test/java/org/owasp/esapi/reference/EncoderTest.java"
 * target="_blank" rel="noopener noreferrer">ESAPI Encoder JUnittest cases</a> for ideas.
 * If you are really ambitious, an excellent resource for XSS attack patterns is
 * <a href="https://beefproject.com/" target="_blank" rel="noopener noreferrer">BeEF - The Browser Exploitation Framework Project</a>.
 * </li><li><b>A final note on {@code Encoder} implementation details:</b>
 * Most of the {@code Encoder} methods make extensive use of ESAPI's {@link org.owasp.esapi.codecs.Codec}
 * classes under-the-hood. These {@code Codec} classes are intended for use for encoding and decoding
 * input based on some particular context or specification.  While the OWASP team
 * over the years have made every effort to be cautious--often going to extremes
 * to make "safe harbor" decisions on harmful inputs other similar encoders assume are already safe
 * (we did this to in order to protect the client's users from buggy browsers that don't adhere
 * to the W3C HTML specications)&em;the various {@code Codec} implemtations can offer
 * NO GUARANTEE of safety of the content being encoded or decoded. Therefore,
 * it is highly advised to practice a security-in-depth approach for everything you do.
 * By following that advice, you will minimize the impact and/or likelihood of any
 * vulnerabilities from bugs in the ESAPI code or accidental misuse of the ESAPI
 * library on your part. In particular, whenever there are cases where cients use
 * any of these {@link org.owasp.esapi.codecs.Codec} classes directly, it is highly
 * recommended to perform canonicalization followed by strict input valiation both
 * prior to encoding and after decoding to protect your application from input-based
 * attacks.
 * </li>
 * </ul>
 * </p>
 * @see <a href="https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html">OWASP Cross-Site Scripting Prevention Cheat Sheet</a>
 * @see org.owasp.esapi.Validator
 * @see <a href="https://owasp.org/www-project-proactive-controls/v3/en/c4-encode-escape-data">OWASP Proactive Controls: C4: Encode and Escape Data</a>
 * @see <a href="https://www.onwebsecurity.com/security/properly-encoding-and-escaping-for-the-web.html" target="_blank" rel="noopener noreferrer">Properly encoding and escaping for the web</a>
 * @author Jeff Williams (jeff.williams .at. owasp.org)
 * @since June 1, 2007
 */
public interface Encoder {

    /**
     * This method is equivalent to calling {@code Encoder.canonicalize(input, restrictMultiple, restrictMixed);}.
     *
     * The <i>default</i> values for {@code restrictMultiple} and {@code restrictMixed} come from {@code ESAPI.properties}.
     * <pre>
     * Encoder.AllowMultipleEncoding=false
     * Encoder.AllowMixedEncoding=false
     * </pre>
     * and the default codecs that are used for canonicalization are the list
     * of codecs that comes from:
     * <pre>
     * Encoder.DefaultCodecList=HTMLEntityCodec,PercentCodec,JavaScriptCodec
     * </pre>
     * (If the {@code Encoder.DefaultCodecList} property is null or not set,
     * these same codecs are listed in the same order. Note that you may supply
     * your own codec by using a fully cqualified class name of a class that
     * implements {@code org.owasp.esapi.codecs.Codec<T>}.
     *
     * @see #canonicalize(String, boolean, boolean)
     * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4">W3C specifications</a>
     *
     * @param input the text to canonicalize
     * @return a String containing the canonicalized text
     */
    String canonicalize(String input);

    /**
     * This method is the equivalent to calling {@code Encoder.canonicalize(input, strict, strict);}.
     *
     * @see #canonicalize(String, boolean, boolean)
     * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4">W3C specifications</a>
     *
     * @param input
     *      the text to canonicalize
     * @param strict
     *      true if checking for multiple and mixed encoding is desired, false otherwise
     *
     * @return a String containing the canonicalized text
     */
    String canonicalize(String input, boolean strict);

    /**
     * Canonicalization is simply the operation of reducing a possibly encoded
     * string down to its simplest form. This is important, because attackers
     * frequently use encoding to change their input in a way that will bypass
     * validation filters, but still be interpreted properly by the target of
     * the attack. Note that data encoded more than once is not something that a
     * normal user would generate and should be regarded as an attack.
     * <p>
     * Everyone <a href="http://cwe.mitre.org/data/definitions/180.html">says</a> you shouldn't do validation
     * without canonicalizing the data first. This is easier said than done. The canonicalize method can
     * be used to simplify just about any input down to its most basic form. Note that canonicalize doesn't
     * handle Unicode issues, it focuses on higher level encoding and escaping schemes. In addition to simple
     * decoding, canonicalize also handles:
     * <ul><li>Perverse but legal variants of escaping schemes</li>
     * <li>Multiple escaping (%2526 or &#x26;lt;)</li>
     * <li>Mixed escaping (%26lt;)</li>
     * <li>Nested escaping (%%316 or &amp;%6ct;)</li>
     * <li>All combinations of multiple, mixed, and nested encoding/escaping (%2&#x35;3c or &#x2526gt;)</li></ul>
     * <p>
     * Using canonicalize is simple. The default is just...
     * <pre>
     *     String clean = ESAPI.encoder().canonicalize( request.getParameter("input"));
     * </pre>
     * You need to decode untrusted data so that it's safe for ANY downstream interpreter or decoder. For
     * example, if your data goes into a Windows command shell, then into a database, and then to a browser,
     * you're going to need to decode for all of those systems. You can build a custom encoder to canonicalize
     * for your application like this...
     * <pre>
     *     ArrayList list = new ArrayList();
     *     list.add( new WindowsCodec() );
     *     list.add( new MySQLCodec() );
     *     list.add( new PercentCodec() );
     *     Encoder encoder = new DefaultEncoder( list );
     *     String clean = encoder.canonicalize( request.getParameter( "input" ));
     * </pre>
     * or alternately, you can just customize {@code Encoder.DefaultCodecList} property
     * in the {@code ESAPI.properties} file with your preferred codecs; for
     * example:
     * <pre>
     * Encoder.DefaultCodecList=WindowsCodec,MySQLCodec,PercentCodec
     * </pre>
     * and then use:
     * <pre>
     *     Encoder encoder = ESAPI.encoder();
     *     String clean = encoder.canonicalize( request.getParameter( "input" ));
     * </pre>
     * as you normally would. However, the downside to using the
     * {@code ESAPI.properties} file approach does not allow you to vary your
     * list of codecs that are used each time. The downside to using the
     * {@code DefaultEncoder} constructor is that your code is now timed to
     * specific reference implementations rather than just interfaces and those
     * reference implementations are what is most likely to change in ESAPI 3.x.
     * </p><p>
     * In ESAPI, the {@code Validator} uses the {@code canonicalize} method before it does validation.  So all you need to
     * do is to validate as normal and you'll be protected against a host of encoded attacks.
     * <pre>
     *     String input = request.getParameter( "name" );
     *     String name = ESAPI.validator().isValidInput( "test", input, "FirstName", 20, false);
     * </pre>
     * However, the default canonicalize() method only decodes HTMLEntity, percent (URL) encoding, and JavaScript
     * encoding. If you'd like to use a custom canonicalizer with your validator, that's pretty easy too.
     * <pre>
     *     ... setup custom encoder as above
     *     Validator validator = new DefaultValidator( encoder );
     *     String input = request.getParameter( "name" );
     *     String name = validator.isValidInput( "test", input, "name", 20, false);
     * </pre>
     * Although ESAPI is able to canonicalize multiple, mixed, or nested encoding, it's safer to not accept
     * this stuff in the first place. In ESAPI, the default is "strict" mode that throws an IntrusionException
     * if it receives anything not single-encoded with a single scheme. This is configurable
     * in {@code ESAPI.properties} using the properties:
     * <pre>
     * Encoder.AllowMultipleEncoding=false
     * Encoder.AllowMixedEncoding=false
     * </pre>
     * This method allows you to override the default behavior by directly specifying whether to restrict
     * multiple or mixed encoding. Even if you disable restrictions, you'll still get
     * warning messages in the log about each multiple encoding and mixed encoding received.
     * <pre>
     *     // disabling strict mode to allow mixed encoding
     *     String url = ESAPI.encoder().canonicalize( request.getParameter("url"), false, false);
     * </pre>
     * <b>WARNING #1!!!</b> Please note that this method is incompatible with URLs and if there exist any HTML Entities
     * that correspond with parameter values in a URL such as "&amp;para;" in a URL like
     * "https://foo.com/?bar=foo&amp;parameter=wrong" you will get a mixed encoding validation exception.
     * <p>
     * If you wish to canonicalize a URL/URI use the method {@code Encoder.getCanonicalizedURI(URI dirtyUri);}
     * </p><p>
     * <b>WARNING #2!!!</b> Even if you use {@code WindowsCodec} or {@code UnixCodec}
     * as appropriate, file path names in the {@code input} parameter will <b><i>NOT</i></b>
     * be canonicalized. It the failure of such file path name canonicalization
     * presents a potential security issue, consider using one of the
     * {@code Validator.getValidDirectoryPath()} methods instead of or in addition to this method.
     *
     * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4">W3C specifications</a>
     * @see #canonicalize(String)
     * @see #getCanonicalizedURI(URI dirtyUri)
     * @see org.owasp.esapi.Validator#getValidDirectoryPath(java.lang.String, java.lang.String, java.io.File, boolean)
     *
     * @param input
     *      the text to canonicalize
     * @param restrictMultiple
     *      true if checking for multiple encoding is desired, false otherwise
     * @param restrictMixed
     *      true if checking for mixed encoding is desired, false otherwise
     *
     * @return a String containing the canonicalized text
     */
    String canonicalize(String input, boolean restrictMultiple, boolean restrictMixed);

    /**
     * Encode data for use in Cascading Style Sheets (CSS) content.
     *
     * @see <a href="http://www.w3.org/TR/CSS21/syndata.html#escaped-characters">CSS Syntax [w3.org]</a>
     *
     * @param untrustedData
     *      the untrusted data to output encode for CSS
     *
     * @return the untrusted data safely output encoded for use in a CSS
     */
    String encodeForCSS(String untrustedData);

    /**
     * Encode data for use in HTML using HTML entity encoding
     * <p>
     * Note that the following characters:
     * 00-08, 0B-0C, 0E-1F, and 7F-9F
     * <p>cannot be used in HTML.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Character_encodings_in_HTML">HTML Encodings [wikipedia.org]</a>
     * @see <a href="http://www.w3.org/TR/html4/sgml/sgmldecl.html">SGML Specification [w3.org]</a>
     * @see <a href="http://www.w3.org/TR/REC-xml/#charsets">XML Specification [w3.org]</a>
     *
     * @param untrustedData
     *      the untrusted data to output encode for HTML
     *
     * @return the untrusted data safely output encoded for use in a HTML
     */
    String encodeForHTML(String untrustedData);

    /**
     * Decodes HTML entities.
     * @param input the <code>String</code> to decode
     * @return the newly decoded <code>String</code>
     */
    String decodeForHTML(String input);

    /**
     * Encode data for use in HTML attributes.
     *
     * @param untrustedData
     *      the untrusted data to output encode for an HTML attribute
     *
     * @return the untrusted data safely output encoded for use in a use as an HTML attribute
     */
    String encodeForHTMLAttribute(String untrustedData);


    /**
     * Encode data for insertion inside a data value or function argument in JavaScript. Including user data
     * directly inside a script is quite dangerous. Great care must be taken to prevent including user data
     * directly into script code itself, as no amount of encoding will prevent attacks there.
     *
     * Please note there are some JavaScript functions that can never safely receive untrusted data
     * as input – even if the user input is encoded.
     *
     * For example:
     * <pre>
     *  &lt;script&gt;
     *    &nbsp;&nbsp;window.setInterval('&lt;%= EVEN IF YOU ENCODE UNTRUSTED DATA YOU ARE XSSED HERE %&gt;');
     *  &lt;/script&gt;
     * </pre>
     * @param untrustedData
     *          the untrusted data to output encode for JavaScript
     *
     * @return the untrusted data safely output encoded for use in a use in JavaScript
     */
    String encodeForJavaScript(String untrustedData);

    /**
     * Encode data for insertion inside a data value in a Visual Basic script. Putting user data directly
     * inside a script is quite dangerous. Great care must be taken to prevent putting user data
     * directly into script code itself, as no amount of encoding will prevent attacks there.
     *
     * This method is not recommended as VBScript is only supported by Internet Explorer
     *
     * @param untrustedData
     *      the untrusted data to output encode for VBScript
     *
     * @return the untrusted data safely output encoded for use in a use in VBScript
     */
    String encodeForVBScript(String untrustedData);


    /**
     * Encode input for use in a SQL query, according to the selected codec
     * (appropriate codecs include the {@link org.owasp.esapi.codecs.MySQLCodec}
     * and {@link org.owasp.esapi.codecs.OracleCodec}), but see
     * "<b>SECURITY WARNING</b>" below before using.
     * <p>
     * The this method attempts to ensure make sure any single-quotes are double-quoted
     * (i.e., as '', not double-quotes, as in &quot;). Another possible approach
     * is to use the {escape} syntax described in the JDBC specification in section 1.5.6.
     * However, this syntax does not work with all drivers, and requires
     * modification of all queries.
     * </p><p>
     * <b>SECURITY WARNING:</b> This method is <u>NOT</u> recommended. The use of the {@code PreparedStatement}
     * interface is the preferred approach. However, if for some reason
     * this is impossible, then this method is provided as a significantly weaker
     * alternative. In particular, it should be noted that if all you do to
     * address potential SQL Injection attacks is to use this method to escape
     * parameters, you <i>will</i> fail miserably. According to the
     * <a href="https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html">
     * OWASP SQL Injection Prevention Cheat Sheet</a>, these are the primary
     * defenses against SQL Injection (as of June 2025):
     * <ul>
     * <li>Option 1: Use of Prepared Statements (with Parameterized Queries)</li>
     * <li>Option 2: Use of Properly Constructed Stored Procedures</li>
     * <li>Option 3: Allow-list Input Validation</li>
     * <li>Option 4: STRONGLY DISCOURAGED: Escaping All User Supplied Input</li>
     * </ul>
     * </p><p>
     * According to "Option 4" (which is what this method implements), that OWASP Cheat Sheet
     * states:
     * <blockquote
     * cite="https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html#defense-option-4-strongly-discouraged-escaping-all-user-supplied-input">
     * In this approach, the developer will escape all user input
     * before putting it in a query. It is very database specific
     * in its implementation. This methodology is frail compared
     * to other defenses, and <b>we <i>CANNOT</i> guarantee that this option
     * will prevent all SQL injections in all situations.</b>
     * </blockquote>
     * (Emphasis ours.)
     * </p><p>
     * Note you could give yourself a slightly better chance at success if prior to
     * escaping by this method, you first canonicalize the input and run it through
     * some strong allow-list validation. We will not provide anymore details than
     * that, lest we encourage its misuse; however, it should be noted that resorting
     * to use this method--especially by itself--should rarely, if ever, used. It
     * is intended as a last ditch, emergency, Hail Mary effort. (To be honest, you'd
     * likely have more success setting up a WAF such as
     * <a href="https://modsecurity.org/">OWASP ModSecurity</a> and
     * <a href="https://owasp.org/www-project-modsecurity-core-rule-set/">OWASP CRS</a>
     * if you need a temporary emergency SQLi defense shield, but using {@code PreparedStatement}
     * is still your best option if you have the time and resources.
     * </p><p>
     * <b>Note to AppSec / Security Auditor teams:</b> If see this method being used in
     * application code, the risk of an exploitable SQLi vulnerability is still high. We
     * stress the importance of the first two Options discussed in the
     * <a href="https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html">
     * OWASP SQL Injection Prevention Cheat Sheet</a>. If you allow this, we recommend only
     * doing so for a limited time duration and in the meantime creating some sort of security
     * exception ticket to track it.
     * </p><p>
     * <b>IMPORTANT NOTE:</b> If you really do insist enabling leg cannon mode and use
     * this method, then you <i>MUST</i> follow these instructions. Failure to do so will
     * result in a {@link org.owasp.esapi.errors.NotConfiguredByDefaultException} being
     * thrown when you try to call it. Thus to make it work, you need to add the implementation
     * method corresponding to this interace (defined in the property "<b>ESAPI.Encoder</b>"
     * (wihch defaults to "org.owasp.esapi.reference.DefaultEncoder") in your "<b>ESAPI.properties</b>" file,
     * to the ESAPI property "<b>ESAPI.dangerouslyAllowUnsafeMethods.methodNames</b>". See
     * the Security Bulletin #13 document referenced below for additional details.
     * </p>
     * @see <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_2-mrel2-spec/jdbc4.2-fr-spec.pdf">JDBC Specification</a>
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html">java.sql.PreparedStatement</a>
     * @see <a href="https://github.com/ESAPI/esapi-java-legacy/blob/develop/documentation/ESAPI-security-bulletin13.pdf">ESAPI Security Bulletin #13</a>
     *
     * @param codec
     *      a {@link org.owasp.esapi.codecs.Codec} that declares which database 'input' is being encoded for (ie. MySQL, Oracle, etc.)
     * @param input
     *      the text to encode for SQL
     *
     * @return input encoded for use in SQL
     * @see <a href="https://github.com/ESAPI/esapi-java-legacy/blob/develop/documentation/ESAPI-security-bulletin13.pdf">
     *              ESAPI Security Bulletin #13</a>
     * @deprecated  This method is considered dangerous and not easily made safe and thus under strong
     *              consideration to be removed within 1 years time after the 2.7.0.0 release. Please
     *              see the referenced ESAPI Security Bulletin #13 for further details.
     */
     @Deprecated
     String encodeForSQL(Codec codec, String input);

    /**
     * Encode for an operating system command shell according to the selected codec (appropriate codecs include the WindowsCodec and UnixCodec).
     *
     * Please note the following recommendations before choosing to use this method:
     *
     * 1)      It is strongly recommended that applications avoid making direct OS system calls if possible as such calls are not portable, and they are potentially unsafe. Please use language provided features if at all possible, rather than native OS calls to implement the desired feature.
     * 2)      If an OS call cannot be avoided, then it is recommended that the program to be invoked be invoked directly (e.g., System.exec("nameofcommand" + "parameterstocommand");) as this avoids the use of the command shell. The "parameterstocommand" should of course be validated before passing them to the OS command.
     * 3)      If you must use this method, then we recommend validating all user supplied input passed to the command shell as well, in addition to using this method in order to make the command shell invocation safe.
     *
     * An example use of this method would be: System.exec("dir " + ESAPI.encodeForOS(WindowsCodec, "parameter(s)tocommandwithuserinput");
     *
     * @param codec
     *      a Codec that declares which operating system 'input' is being encoded for (ie. Windows, Unix, etc.)
     * @param input
     *      the text to encode for the command shell
     *
     * @return input encoded for use in command shell
     */
    String encodeForOS(Codec codec, String input);

    /**
     * Encode data for use in LDAP queries. Wildcard (*) characters will be encoded.
     *
     * This encoder operates according to RFC 4515, Section 3. RFC 4515 says the following character ranges
     * are valid: 0x01-0x27, 0x2B-0x5B and 0x5D-0x7F. Characters outside the ranges are hex encoded, and they
     * include 0x00 (NUL), 0x28 (LPAREN), 0x29 (RPAREN), 0x2A (ASTERISK), and 0x5C (ESC). The encoder will also
     * encode 0x2F (FSLASH), which is required by Microsoft Active Directory.
     *
     * NB: At ESAPI 2.5.3, {@code encodeForLDAP} began strict conformance with RFC 4515. Characters above 0x7F
     * are converted to UTF-8, and then the byte sequences are hex encoded according to the RFC.
     *
     * @param input
     *      the text to encode for LDAP
     *
     * @return input encoded for use in LDAP
     *
     * @see <a href="https://www.ietf.org/rfc/rfc4515.txt">RFC 4515, Lightweight Directory Access Protocol
     * (LDAP): String Representation of Search Filters</a>
     *
     * @since ESAPI 1.3
     */
    String encodeForLDAP(String input);

    /**
     * Encode data for use in LDAP queries. You have the option whether or not to encode wildcard (*) characters.
     *
     * This encoder operates according to RFC 4515, Section 3. RFC 4515 says the following character ranges
     * are valid: 0x01-0x27, 0x2B-0x5B and 0x5D-0x7F. Characters outside the ranges are hex encoded, and they
     * include 0x00 (NUL), 0x28 (LPAREN), 0x29 (RPAREN), 0x2A (ASTERISK), and 0x5C (ESC). The encoder will also
     * encode 0x2F (FSLASH), which is required by Microsoft Active Directory.
     *
     * NB: At ESAPI 2.5.3, {@code encodeForLDAP} began strict conformance with RFC 4515. Characters above 0x7F
     * are converted to UTF-8, and then the byte sequences are hex encoded according to the RFC.
     *
     * @param input
     *      the text to encode for LDAP
     * @param encodeWildcards
     *      whether or not wildcard (*) characters will be encoded.
     *
     * @return input encoded for use in LDAP
     *
     * @see <a href="https://www.ietf.org/rfc/rfc4515.txt">RFC 4515, Lightweight Directory Access Protocol
     * (LDAP): String Representation of Search Filters</a>
     *
     * @since ESAPI 1.3
     */
    String encodeForLDAP(String input, boolean encodeWildcards);

    /**
     * Encode data for use in an LDAP distinguished name.
     *
     * This encoder operates according to RFC 4514, Section 3. RFC 4514 says the following character ranges
     * are valid: 0x01-0x21, 0x23-0x2A, 0x2D-0x3A, 0x3D, 0x3F-0x5B, 0x5D-0x7F. Characters outside the ranges
     * are hex encoded, and they include 0x00 (NUL), 0x22 (DQUOTE), 0x2B (PLUS), 0x2C (COMMA),
     * 0x3B (SEMI), 0x3C (LANGLE), 0x3E (RANGLE) and 0x5C (ESC). The encoder will also encode 0x2F (FSLASH),
     * which is required by Microsoft Active Directory. The leading and trailing characters in a distinguished
     * name string will also have 0x20 (SPACE) and 0x23 (SHARP) encoded.
     *
     * NB: At ESAPI 2.5.3, {@code encodeForDN} began strict conformance with RFC 4514. Characters above 0x7F
     * are converted to UTF-8, and then the byte sequences are hex encoded according to the RFC.
     *
     *  @param input
     *          the text to encode for an LDAP distinguished name
     *
     *  @return input encoded for use in an LDAP distinguished name
     *
     *  @see <a href="https://www.ietf.org/rfc/rfc4514.txt">RFC 4514, Lightweight Directory Access Protocol
     *  (LDAP): String Representation of Distinguished Names</a>
     *
     *  @since ESAPI 1.3
     */
    String encodeForDN(String input);

    /**
     * Encode data for use in an XPath query.
     *
     * NB: The reference implementation encodes almost everything and may over-encode.
     *
     * The difficulty with XPath encoding is that XPath has no built-in mechanism for escaping
     * characters. It is possible to use XQuery in a parameterized way to
     * prevent injection.
     *
     * For more information, refer to <a
     * href="http://www.ibm.com/developerworks/xml/library/x-xpathinjection.html">this
     * article</a> which specifies the following list of characters as the most
     * dangerous: ^ & " * ' ; < > ( ) . <a
     * href="http://www.packetstormsecurity.org/papers/bypass/Blind_XPath_Injection_20040518.pdf">This
     * paper</a> suggests disallowing ' and " in queries.
     *
     * @see <a href="http://www.ibm.com/developerworks/xml/library/x-xpathinjection.html">XPath Injection [ibm.com]</a>
     * @see <a href="http://www.packetstormsecurity.org/papers/bypass/Blind_XPath_Injection_20040518.pdf">Blind XPath Injection [packetstormsecurity.org]</a>
     *
     * @param input
     *      the text to encode for XPath
     * @return
     *      input encoded for use in XPath
     */
    String encodeForXPath(String input);

    /**
     * Encode data for use in an XML element. The implementation should follow the <a
     * href="https://www.w3.org/TR/REC-xml/#charencoding">Character Encoding in Entities</a>
     * from W3C.
     * <p>
     * The use of a real XML parser is strongly encouraged. However, in the
     * hopefully rare case that you need to make sure that data is safe for
     * inclusion in an XML document and cannot use a parser, this method provides
     * a safe mechanism to do so.
     *
     * @see <a href="https://www.w3.org/TR/REC-xml/#charencoding">Character Encoding in Entities</a>
     *
     * @param input
     *          the text to encode for XML
     *
     * @return
     *          input encoded for use in XML
     */
    String encodeForXML(String input);

    /**
     * Encode data for use in an XML attribute. The implementation should follow the <a
     * href="https://www.w3.org/TR/REC-xml/#charencoding">Character Encoding in Entities</a>
     * from W3C.
     * <p>
     * The use of a real XML parser is highly encouraged. However, in the
     * hopefully rare case that you need to make sure that data is safe for
     * inclusion in an XML document and cannot use a parse, this method provides
     * a safe mechanism to do so.
     *
     * @see <a href="https://www.w3.org/TR/REC-xml/#charencoding">Character Encoding in Entities</a>
     *
     * @param input
     *          the text to encode for use as an XML attribute
     *
     * @return
     *          input encoded for use in an XML attribute
     */
    String encodeForXMLAttribute(String input);

    /**
     * Encode for use in a URL. This method performs <a
     * href="http://en.wikipedia.org/wiki/Percent-encoding">URL encoding</a>
     * on the entire string.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Percent-encoding">URL encoding</a>
     *
     * @param input
     *      the text to encode for use in a URL
     *
     * @return input
     *      encoded for use in a URL
     *
     * @throws EncodingException
     *      if encoding fails
     */
    String encodeForURL(String input) throws EncodingException;

    /**
     * Encode data for use in JSON strings. This method performs <a
     * href="https://datatracker.ietf.org/doc/html/rfc8259#section-7">String escaping</a>
     * on the entire string according to RFC 8259, Section 7.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-7">RFC 8259,
     * The JavaScript Object Notation (JSON) Data Interchange Format, Section 7</a>
     *
     * @param input
     *      the text to escape for JSON string
     *
     * @return input
     *      escaped for use in JSON string
     */
    String encodeForJSON(String input);

    /**
     * Decode from URL. Implementations should first canonicalize and
     * detect any double-encoding. If this check passes, then the data is decoded using URL
     * decoding.
     *
     * @param input
     *      the text to decode from an encoded URL
     *
     * @return
     *      the decoded URL value
     *
     * @throws EncodingException
     *      if decoding fails
     */
    String decodeFromURL(String input) throws EncodingException;

    /**
     * Encode for Base64.
     *
     * @param input
     *      the text to encode for Base64
     * @param wrap
     *      the encoder will wrap lines every 64 characters of output
     *
     * @return input encoded for Base64
     */
    String encodeForBase64(byte[] input, boolean wrap);

    /**
     * Decode data encoded with BASE-64 encoding.
     *
     * @param input
     *      the Base64 text to decode
     *
     * @return input decoded from Base64
     *
     * @throws IOException
     */
    byte[] decodeFromBase64(String input) throws IOException;

    /**
     * Get a version of the input URI that will be safe to run regex and other validations against.
     * It is not recommended to persist this value as it will transform user input.  This method
     * will not test to see if the URI is RFC-3986 compliant.
     *
     * @param dirtyUri
     *      the tainted URI
     * @return The canonicalized URI
     */
    String getCanonicalizedURI(URI dirtyUri);

    /**
     * Decode data encoded for JSON strings. This method removes <a
     * href="https://datatracker.ietf.org/doc/html/rfc8259#section-7">String escaping</a>
     * on the entire string according to RFC 8259, Section 7.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc8259#section-7">RFC 8259,
     * The JavaScript Object Notation (JSON) Data Interchange Format, Section 7</a>
     *
     * @param input
     *      the JSON string to decode
     *
     * @return input
     *      decoded from JSON string
     */
    String decodeFromJSON(String input);
}
