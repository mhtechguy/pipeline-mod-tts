package org.daisy.pipeline.cssinlining;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.tree.util.ProcInstParser;

import org.daisy.pipeline.tts.config.ConfigReader;

import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritablePipe;
import com.xmlcalabash.library.DefaultStep;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XAtomicStep;
import com.xmlcalabash.util.TreeWriter;

public class InlineCSSStep extends DefaultStep implements TreeWriterFactory {

	private String mStyleNsOption;
	private ReadablePipe mConfig;
	private ReadablePipe mSource = null;
	private WritablePipe mResult = null;
	private XProcRuntime mRuntime;

	public InlineCSSStep(XProcRuntime runtime, XAtomicStep step) {
		super(runtime, step);
		mRuntime = runtime;
	}

	public void setInput(String port, ReadablePipe pipe) {
		if ("config".equalsIgnoreCase(port))
			mConfig = pipe;
		else
			mSource = pipe;
	}

	@Override
	public void setOption(QName name, RuntimeValue value) {
		super.setOption(name, value);
		String optName = name.getLocalName();
		if ("style-ns".equalsIgnoreCase(optName)) {
			mStyleNsOption = value.getString();
		} else {
			mRuntime.error(new Throwable("unknown option " + optName));
			return;
		}
	}

	public void setOutput(String port, WritablePipe pipe) {
		mResult = pipe;
	}

	public void reset() {
		mSource.resetReader();
		mResult.resetWriter();
	}

	static URI buildAbsoluteURI(String path, XdmNode doc) {
		URI u;
		try {
			u = new URI(path);
		} catch (URISyntaxException e) {
			try {
				u = new URI("file://" + path);
			} catch (URISyntaxException e1) {
				return null;
			}
		}
		if (!u.isAbsolute()) {
			u = doc.getDocumentURI().resolve(u);
		}

		return u;
	}

	static Collection<URI> getCSSurisInContent(XdmNode doc) {
		Collection<URI> result = new ArrayList<URI>();

		//add the CSS stylesheet URI from the processing-instructions
		XdmSequenceIterator it = doc.axisIterator(Axis.CHILD);
		while (it.hasNext()) {
			XdmNode next = (XdmNode) it.next();
			if (next.getNodeKind() == XdmNodeKind.PROCESSING_INSTRUCTION) {
				String content = next.getStringValue();
				String href = ProcInstParser.getPseudoAttribute(content, "href");
				if ("xml-stylesheet".equals(next.getNodeName().getLocalName())
				        && href != null
				        && !href.isEmpty()
				        && "text/css".equals(ProcInstParser
				                .getPseudoAttribute(content, "type"))) {

					URI uri = buildAbsoluteURI(href, doc);
					if (uri != null)
						result.add(uri);
				}
			}
		}

		//add the CSS stylesheet URIs from the headers
		it = doc.axisIterator(Axis.DESCENDANT);
		while (it.hasNext()) {
			XdmNode next = (XdmNode) it.next();
			if (next.getNodeKind() == XdmNodeKind.ELEMENT
			        && "link".equals(next.getNodeName().getLocalName())
			        && "stylesheet".equals(next.getAttributeValue(new QName(null, "rel")))) {
				String href = next.getAttributeValue(new QName(null, "href"));
				URI uri = buildAbsoluteURI(href, doc);
				if (uri != null)
					result.add(uri);
			}
		}

		return result;
	}

	public void run() throws SaxonApiException {
		super.run();

		//read config
		ConfigReader cr = null;
		CSSConfigExtension cssExt = new CSSConfigExtension();
		while (mConfig.moreDocuments()) {
			cr = new ConfigReader(mConfig.read(), cssExt);
			break;
		}

		//read first document
		XdmNode doc = null;
		while (mSource.moreDocuments()) {
			doc = mSource.read();
			break;
		}

		Collection<URI> alluris = new ArrayList<URI>();
		alluris.addAll(getCSSurisInContent(doc));
		alluris.addAll(cssExt.getCSSstylesheetURIs());

		CSSInliner inliner = new CSSInliner();
		SpeechSheetAnalyser analyzer = new SpeechSheetAnalyser();
		try {
			analyzer.analyse(alluris, cssExt.getEmbeddedCSS(), cr.getConfigDocURI());
		} catch (Throwable t) {
			mRuntime.warning(t);
			mResult.write(doc);
			return;
		}

		// rebuild the document with the additional style info
		XdmNode rebuilt = inliner.inline(this, doc.getDocumentURI(), doc, analyzer,
		        mStyleNsOption);

		for (URI uri : alluris) {
			mRuntime.info(null, null, uri.toString() + " inlined");
		}

		mResult.write(rebuilt);
	}

	@Override
	public TreeWriter newInstance() {
		return new TreeWriter(mRuntime);
	}
}
